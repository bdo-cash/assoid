/*
 * Copyright (C) 2018-present, Chenai Nakam(chenai.nakam@gmail.com)
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
import hobby.chenai.nakam.basis.TAG
import hobby.chenai.nakam.lang.J2S.NonNull
import hobby.chenai.nakam.lang.TypeBring.AsIs
import hobby.wei.c.LOG._
import hobby.wei.c.tool.Magic.retryForceful

/**
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 26/05/2018
  */
abstract class AbsMsgrActy extends AbsActy with TAG.ClassName {
  protected override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    tryOrReBind()
  }

  protected override def onDestroy(): Unit = {
    confirmUnbind()
    super.onDestroy()
  }

  protected def startService: StartMe.Srvce

  protected def msgrServiceClazz: Class[_ <: AbsMsgrService]

  protected def onMsgChannelConnected(): Unit = {
    val msg = new Message
    msg.what = 1234567
    val b = new Bundle
    b.putString("msg_key", ">>> 这是一个测试`请求`消息 >>>。")
    msg.obj = b
    sendMsg2Server(msg)
  }

  protected def onMsgChannelDisconnected(): Unit = {}

  protected def handleServerMsg(msg: Message, handler: Handler): Unit = msg.what match {
    case 7654321 =>
      i("handleServerMsg | msg > what: %s, content: %s.", msg.what, msg.obj.as[Bundle].getString("msg_key").s)
    case _ =>
  }

  private lazy val msgHandler: Handler = new Handler() {
    override def handleMessage(msg: Message): Unit = handleServerMsg(msg, msgHandler)
  }

  @volatile private var sender: Messenger = _
  @volatile private var connected: Boolean = false

  def isChannelConnected = connected

  def sendMsg2Server(msg: Message): Unit = retryForceful(1000) { _ =>
    val msgr = sender
    if (msgr.nonNull) {
      try {
        msgr.send(msg)
        true
      } catch {
        case ex: RemoteException => e(ex)
          if (!msgr.getBinder.pingBinder()) {
            e("client ping to-server binder failed.")
            tryOrReBind()
            true // 中断 retry
          } else false
      }
    } else false
  }(msgHandler)

  private lazy val serviceConn: ServiceConnection = new ServiceConnection {
    override def onServiceConnected(name: ComponentName, service: IBinder): Unit = {
      assert(!connected, "测试 onServiceConnected 会不会重复多次")
      sender = startService.binder2Sender(service)
      if (startService.replyToClient(sender, msgHandler)) {
        e("onServiceConnected | 正常建立连接 -->")
        if (!connected) {
          connected = true
          e("onServiceConnected | 正常建立连接 | DONE.")
          onMsgChannelConnected()
        }
      } else {
        e("onServiceConnected | bindService 失败")
        tryOrReBind()
      }
    }

    override def onServiceDisconnected(name: ComponentName): Unit = {
      e("onServiceDisconnected | 断开连接 -->")
      if (confirmUnbind()) tryOrReBind()
      else {
        // 说明是 force unbind, 正常。
      }
    }
  }

  private def tryOrReBind(): Unit = {
    confirmUnbind()
    startService.bind(this, serviceConn, msgrServiceClazz)
  }

  private def confirmUnbind(): Boolean = if (connected) {
    connected = false
    e("confirmUnbind | 断开连接 | DONE.")
    onMsgChannelDisconnected()
    sender = null
    startService.unbind(this, serviceConn)
    true
  } else false
}
