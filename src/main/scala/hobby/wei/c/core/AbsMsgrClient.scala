/*
 * Copyright (C) 2020-present, Chenai Nakam(chenai.nakam@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hobby.wei.c.core

import android.content.{ComponentName, ServiceConnection}
import android.os._
import hobby.chenai.nakam.lang.J2S.{NonNull, Run}
import hobby.wei.c.tool.RetryByHandler
import hobby.wei.c.LOG._

/**
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 23/09/2020, 从`AbsMsgrActy`重构过来。
  */
trait AbsMsgrClient extends Ctx.Abs with RetryByHandler {
  protected def serviceStarter: StartMe.MsgrSrvce

  protected def msgrServiceClazz: Class[_ <: AbsMsgrService]

  /** Maybe returning `isFinishing || isDestroyed`. */
  protected def isThisClientClosed: Boolean

  protected def onMsgChannelConnected(): Unit = {
    val msg = new Message
    msg.what = 1234567
    val b = new Bundle
    b.putString("msg_key", ">>> 这是一个测试`请求`消息 >>>。")
    msg.setData(b)
    sendMsg2Server(msg)
  }

  /** 断开与`Service`的连接时，触发该回调。 */
  protected def onMsgChannelDisconnected(): Unit = {}

  /** 该回调表示[临时]断开了与`Service`的连接（可能是`Service`异常崩溃之类的原因引起，会再次自动重启`Service`并重连）。 */
  protected def onDisconnectTemporarily(): Unit = {}

  protected def handleServerMsg(msg: Message, handler: Handler): Boolean = msg.what match {
    case 7654321 =>
      d("handleServerMsg | msg > what: %s, content: %s.", msg.what, msg.getData.getString("msg_key").s)
      true
    case _ => false
  }

  private lazy val msgHandler: Handler = new Handler() {
    override def handleMessage(msg: Message): Unit = if (isThisClientClosed) {
      w("msgHandler.handleMessage | BLOCKED. >>> isThisClientClosed: %s.", isThisClientClosed)
    } else handleServerMsg(msg, msgHandler)
  }

  @volatile private var sender: Messenger = _
  @volatile private var replyTo: Messenger = _
  @volatile private var connected: Boolean = false
  @volatile private var temporarily: Boolean = false

  def isChannelConnected = connected && !temporarily

  def sendMsg2Server(msg: Message): Unit = {
    if (!isThisClientClosed) msgHandler.post({
      retryForceful(1000) { _ =>
        val msgr = sender
        if (msgr.nonNull) {
          try {
            msgr.send(msg)
            true
          } catch {
            case ex: RemoteException => e(ex)
              if (!msgr.getBinder.pingBinder()) {
                e("client ping to-server binder failed.")
                tryOrRebind()
                true // 中断 retry
              } else false
          }
        } else if (isThisClientClosed) true /*中断*/
        else false
      }
    }.run$)
  }

  override implicit protected def delayerHandler: Handler = msgHandler

  private lazy val serviceConn: ServiceConnection = new ServiceConnection {
    override def onServiceConnected(name: ComponentName, service: IBinder): Unit = {
      sender = serviceStarter.binder2Sender(service)
      val msgr = serviceStarter.replyToClient(sender, msgHandler)
      if (msgr.isDefined) {
        replyTo = msgr.get
        d("onServiceConnected | 正常建立连接 -->")
        if (!connected) {
          connected = true
          temporarily = false
          d("onServiceConnected | 正常建立连接 | DONE.")
          onMsgChannelConnected()
        }
      } else {
        e("onServiceConnected | bindService 失败")
        tryOrRebind()
      }
    }

    override def onServiceDisconnected(name: ComponentName): Unit = {
      e("onServiceDisconnected | 临时断开连接 -->")
      // 这里不需要手动断开再重连，会自动重新回调`onServiceConnected()`。
      // 由`crash`导致，for example。
      // if (confirmUnbind()) tryOrRebind()
      // else {
      //   // 说明是 force unbind, 正常。
      // }
      disconnectTemporarily()
    }

    override def onBindingDied(name: ComponentName): Unit = {
      super.onBindingDied(name)
      e("onBindingDied | 断开连接 -->")
      if (confirmUnbind()) tryOrRebind()
      else {
        // 说明是 force unbind, 正常。
      }
    }

    override def onNullBinding(name: ComponentName): Unit = super.onNullBinding(name)
  }

  protected def tryOrRebind(): Unit = {
    confirmUnbind()
    serviceStarter.bind(context, serviceConn, msgrServiceClazz)
  }

  private def disconnectTemporarily(): Unit = {
    temporarily = true
    sender = null
    onDisconnectTemporarily()
  }

  protected def confirmUnbind(): Boolean = if (connected) {
    connected = false
    w("confirmUnbind | 断开连接 | DONE.")
    if (sender.nonNull && replyTo.nonNull) {
      serviceStarter.unReplyToClient(sender, replyTo)
    }
    serviceStarter.unbind(context, serviceConn)
    sender = null
    replyTo = null
    onMsgChannelDisconnected()
    true
  } else false
}
