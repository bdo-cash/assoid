/*
 * Copyright (C) 2017-present, Chenai Nakam(chenai.nakam@gmail.com)
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

import android.os
import android.content.Intent
import android.database.Observable
import android.os.{HandlerThread, Messenger, _}
import hobby.chenai.nakam.basis.TAG
import hobby.chenai.nakam.lang.J2S.{getRef, NonNull, Run}
import hobby.chenai.nakam.lang.TypeBring.AsIs
import hobby.wei.c.LOG._
import hobby.wei.c.core.AbsMsgrService._
import hobby.wei.c.tool.Magic.retryForceful

import scala.ref.WeakReference

/**
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 22/05/2018
  */
trait AbsMsgrService extends AbsSrvce with Ctx.Srvce {
  @volatile private var mAllClientDisconnected = true
  @volatile private var mStopRequested = false
  private var mCallStartCount, mCallStopCount = 0

  protected val MSG_REPLY_TO: Int

  protected val CMD_EXTRA_STOP_SERVICE: String
  protected val CMD_EXTRA_START_FOREGROUND: String
  protected val CMD_EXTRA_STOP_FOREGROUND: String

  /**
    * 子类重写本方法以处理`Client`端发过来的消息。
    *
    * @param msg `Client`端发过来的消息对象。
    */
  protected def handleClientMsg(msg: Message, handler: Handler): Unit = msg.what match {
    case 1234567 =>
      i("handleClientMsg | msg > what: %s, content: %s.", msg.what, msg.obj.as[Bundle].getString("msg_key").s)
      val answer = Message.obtain()
      answer.what = 7654321
      val b = new Bundle
      b.putString("msg_key", "<<< 这是一个测试`应答`消息 <<<。")
      answer.obj = b
      sendMsg2Client(answer)
    case _ =>
  }

  /**
    * 请求启动任务。
    * 由于服务需要时刻保持任务正常运行。Client可能会由于某些原因发出多次启动命令。如果本服务
    * 对任务管理较严谨，可忽略后面的（当`callCount > 0`时）命令；否则应该每次检查是否需要重新启动。
    *
    * @param callCount 当前回调被呼叫的次数，从`0`开始（`0`为第一次）。
    */
  protected def onStartWork(callCount: Int): Unit

  /**
    * 请求停止任务。
    *
    * @return `-1`表示不可以关闭（应该继续运行）；`0`表示可以关闭；`> 0`表示延迟该时间后再来询问。
    * @param callCount 当前回调被呼叫的次数，从`0`开始（`0`为第一次）。
    */
  protected def onStopWork(callCount: Int): Int

  /** 请求调用`startForeground()`。 */
  protected def onStartForeground(): Unit

  /** 请求调用`stopForeground()`。 */
  protected def onStopForeground(): Unit = stopForeground(true)

  /**
    * 子类重写该方法以消化特定命令。
    *
    * @return `true`表示消化了参数`intent`携带的命令（这意味着本父类不再继续处理命令），
    *         `false`表示没有消化（即：没有自己关注命令）。
    */
  protected def confirmIfCommandConsumed(intent: Intent): Boolean = false

  def sendMsg2Client(msg: Message): Unit = {
    def shouldFinish = isDestroyed || (mStopRequested && mAllClientDisconnected)

    if (!shouldFinish) clientHandler.post({
      retryForceful(1200) { _ =>
        if (hasClient) {
          mMsgObservable.sendMessage(msg)
          true
        } else if (shouldFinish) true /*中断*/ else false
      }(clientHandler)
    }.run$)
  }

  /** 在没有client bind的情况下，会停止Service，否则等待最后一个client取消bind的时候会自动断开。 **/
  def requestStopService(): Unit = {
    mStopRequested = true
    confirmIfSignify2Stop()
  }

  def hasClient = !mAllClientDisconnected

  def isStopRequested = mStopRequested

  private lazy val mMsgObservable = new MsgObservable

  private lazy val mHandlerThread = {
    val ht = new HandlerThread(getClass.getName, os.Process.THREAD_PRIORITY_BACKGROUND)
    ht.start()
    ht
  }

  /** 主要用于客户端消息的`接收`和`回复`。 */
  protected lazy val clientHandler: Handler = new Handler(mHandlerThread.getLooper) {
    override def handleMessage(msg: Message): Unit = {
      if (msg.what == MSG_REPLY_TO) {
        if (msg.replyTo.nonNull) {
          mAllClientDisconnected = false
          mMsgObservable.registerObserver(new MsgObserver(msg.replyTo, mMsgObservable))
        }
      } else if (isStopRequested || isDestroyed) {
        w("clientHandler.handleMessage | BLOCKED. >>> stopRequested: %s, destroyed: %s.", isStopRequested, isDestroyed)
      } else handleClientMsg(msg, clientHandler)
    }
  }

  override def onBind(intent: Intent) = {
    w("onBind | intent: %s.", intent)
    // 注意：`onUnbind()`之后，如果再次`bindService()`并不一定会再走这里。即：`onBind()`和`onUnbind()`并不对称。
    // 但只要`onUnbind()`返回`true`，下次会走`onRebind()`。
    mAllClientDisconnected = false
    new Messenger(clientHandler).getBinder
  }

  override def onRebind(intent: Intent): Unit = {
    mAllClientDisconnected = false
    super.onRebind(intent)
  }

  override def onUnbind(intent: Intent) = { // 当所有的bind连接都断开之后会回调
    mAllClientDisconnected = true
    mMsgObservable.unregisterAll()
    confirmIfSignify2Stop()
    super.onUnbind(intent)
    // 默认返回`false`（注意：下次`bind`的时候既不执行`onBind()`，也不执行`onRebind()`）。当返回`true`时，下次的`bind`操作将执行`onRebind()`。
    true
  }

  override def onStartCommand(intent: Intent, flags: Int, startId: Int) = {
    if (!confirmIfSignify2Stop(intent) // 注意这几个顺序所表达的优先级
      && !confirmIfSignify2ToggleForeground(intent)
      && !confirmIfCommandConsumed(intent)) {
      post {
        // 本服务就是要时刻保持连接畅通的
        onStartWork(mCallStartCount)
        mCallStartCount += 1
      }
    }
    super.onStartCommand(intent, flags, startId)
  }

  private def confirmIfSignify2ToggleForeground(intent: Intent): Boolean = if (intent.nonNull) {
    if (intent.getBooleanExtra(CMD_EXTRA_START_FOREGROUND, false)) {
      onStartForeground()
      true
    } else if (intent.getBooleanExtra(CMD_EXTRA_STOP_FOREGROUND, false)) {
      onStopForeground()
      true
    } else false
  } else false

  private def confirmIfSignify2Stop(): Boolean = confirmIfSignify2Stop(null)

  private def confirmIfSignify2Stop(intent: Intent): Boolean = {
    if (intent.nonNull) mStopRequested = intent.getBooleanExtra(CMD_EXTRA_STOP_SERVICE, false)
    // 让请求跟client的msg等排队执行
    if (!isDestroyed && mStopRequested) clientHandler.post(postStopSelf(0).run$)
    mStopRequested
  }

  private def postStopSelf(delay: Int): Unit = postDelayed(delay) {
    if (!isDestroyed && mStopRequested && mAllClientDisconnected) onStopWork(mCallStopCount) match {
      case -1 => // 可能又重新bind()了
        require(!mStopRequested || !mAllClientDisconnected, "根据当前状态应该关闭。您可以为`onCallStopWork()`返回`>0`的值以延迟该时间后再询问关闭。")
      case 0 => stopSelf() //完全准备好了，该保存的都保存了，那就关闭吧。
      case time => postStopSelf(time)
    }
    mCallStopCount += 1
  }

  override def onDestroy(): Unit = {
    super.onDestroy()
    clientHandler.post({
      mHandlerThread.quitSafely()
    }.run$)
  }
}

object AbsMsgrService {
  class MsgObservable extends Observable[MsgObserver] {
    def sendMessage(msg: Message): Unit = mObservers.synchronized {
      var i = mObservers.size() - 1
      while (i >= 0) {
        //Message不可重复发送，见`msg.markInUse()`
        mObservers.get(i).onMessage(Message.obtain(msg))
        i -= 1
      }
    }
  }

  class MsgObserver(msgr: Messenger, obs: MsgObservable) extends TAG.ClassName {
    private val obsRef: WeakReference[MsgObservable] = new WeakReference[MsgObservable](obs)

    def onMessage(msg: Message): Unit = {
      try {
        msgr.send(msg)
      } catch {
        case ex: RemoteException => e(ex)
          if (!msgr.getBinder.pingBinder()) {
            e("server ping to-client binder failed.")
            getRef(obsRef).foreach(_.unregisterObserver(this))
          }
      }
    }
  }
}
