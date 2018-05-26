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
import hobby.chenai.nakam.lang.J2S
import hobby.chenai.nakam.lang.J2S.NonNull
import hobby.wei.c.LOG._
import hobby.wei.c.core.AbsService._

import scala.ref.WeakReference

/**
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 22/05/2018
  */
trait AbsService extends AbsSrvce with Ctx.Srvce {
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
  def handleClientMsg(msg: Message, handler: Handler): Unit

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

  def sendMsg2Client(msg: Message): Unit = clientHandler.post(new Runnable {
    override def run(): Unit = mMsgObservable.sendMessage(msg)
  })

  /** 在没有client bind的情况下，会停止Service，否则等待最后一个client取消bind的时候会自动断开。 **/
  def requestStopService(): Unit = {
    mStopRequested = true
    confirmIfSignify2Stop()
  }

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
        if (msg.replyTo.nonNull) mMsgObservable.registerObserver(new MsgObserver(msg.replyTo, mMsgObservable))
      } else {
        handleClientMsg(msg, clientHandler)
      }
    }
  }

  private lazy val mMessenger = new Messenger(clientHandler)

  override def onBind(intent: Intent) = {
    w("onBind | intent: %s.", intent)
    mAllClientDisconnected = false
    mMessenger.getBinder
  }

  override def onUnbind(intent: Intent) = { // 当所有的bind连接都断开之后会回调
    mAllClientDisconnected = true
    mMsgObservable.unregisterAll()
    confirmIfSignify2Stop()
    super.onUnbind(intent) // 默认返回false. 当返回true时，下次的的bind操作将执行onRebind()。
  }

  override def onStartCommand(intent: Intent, flags: Int, startId: Int) = {
    confirmIfSignify2ToggleForeground(intent)
    if (!confirmIfSignify2Stop(intent)) post {
      // 本服务就是要时刻保持连接畅通的
      onStartWork(mCallStartCount)
      mCallStartCount += 1
    }
    super.onStartCommand(intent, flags, startId)
  }

  private def confirmIfSignify2ToggleForeground(intent: Intent): Unit = if (intent.nonNull) {
    if (intent.getBooleanExtra(CMD_EXTRA_START_FOREGROUND, false)) onStartForeground()
    else if (intent.getBooleanExtra(CMD_EXTRA_STOP_FOREGROUND, false)) onStopForeground()
  }

  private def confirmIfSignify2Stop(): Boolean = confirmIfSignify2Stop(null)

  private def confirmIfSignify2Stop(intent: Intent): Boolean = {
    if (intent.nonNull) mStopRequested = intent.getBooleanExtra(CMD_EXTRA_STOP_SERVICE, false)
    if (!isDestroyed && mStopRequested && mAllClientDisconnected) {
      // 让请求跟client的msg等排队执行
      clientHandler.post(new Runnable() {
        override def run(): Unit = {
          postStopSelf(0)
        }
      })
    }
    mStopRequested
  }

  private def postStopSelf(delay: Int): Unit = postDelayed(delay) {
    if (!isDestroyed) onStopWork(mCallStopCount) match {
      case -1 => // 可能又重新bind()了
        require(!mStopRequested || !mAllClientDisconnected, "根据当前状态应该关闭。您可以为`onCallStopWork()`返回`>0`的值以延迟该时间后再询问关闭。")
      case 0 => stopSelf() //完全准备好了，该保存的都保存了，那就关闭吧。
      case time => if (mStopRequested && mAllClientDisconnected) postStopSelf(time)
    }
    mCallStopCount += 1
  }

  override def onDestroy(): Unit = {
    super.onDestroy()
    mHandlerThread.quitSafely()
  }
}

object AbsService {
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

  class MsgObserver(msger: Messenger, obs: MsgObservable) extends TAG.ClassName {
    private val obsRef: WeakReference[MsgObservable] = new WeakReference[MsgObservable](obs)

    def onMessage(msg: Message): Unit = {
      try {
        msger.send(msg)
      } catch {
        case ex: RemoteException => e(ex)
          if (!msger.getBinder.pingBinder()) {
            e("server ping to-client binder failed.")
            J2S.getRef(obsRef).foreach(_.unregisterObserver(this))
          }
      }
    }
  }
}
