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

import android.app.{Activity, Service}
import android.content.{Context, Intent, ServiceConnection}
import android.os._
import androidx.fragment.app.{Fragment, FragmentActivity}
import hobby.chenai.nakam.basis.TAG
import hobby.chenai.nakam.lang.TypeBring.AsIs
import hobby.wei.c.LOG._
import hobby.wei.c.core.StartMe.MsgrSrvce.Const

/**
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 17/11/2017
  */
object StartMe {
  object MsgrSrvce {
    trait Const {
      protected val MSG_REPLY_TO: Int //= 999999999
      protected val MSG_UN_REPLY: Int

      protected val CMD_EXTRA_STOP_SERVICE: String //= getApp.withPackageNamePrefix("CMD_EXTRA_STOP_SERVICE")
      protected val CMD_EXTRA_START_FOREGROUND: String
      protected val CMD_EXTRA_STOP_FOREGROUND: String
    }
  }

  trait MsgrSrvce extends Const with TAG.ClassName {
    def start[S <: Service](ctx: Context, clazz: Class[S]): Unit = ctx.startService(new Intent(ctx, clazz))

    def startFg[S <: Service](ctx: Context, clazz: Class[S]): Unit = {
      val intent = new Intent(ctx, clazz)
      intent.putExtra(CMD_EXTRA_START_FOREGROUND, true)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        ctx.startForegroundService(intent)
      } else {
        ctx.startService(intent)
      }
    }

    def stopFg[S <: Service](ctx: Context, clazz: Class[S]): Unit = {
      val intent = new Intent(ctx, clazz)
      intent.putExtra(CMD_EXTRA_STOP_FOREGROUND, true)
      ctx.startService(intent)
    }

    /**
      * 停止`clazz`参数指定的`Service`（发送一个请求让其自己关闭，而不是直接`stopService()`）。
      * <p>
      * 几种`stopService`方法的异同：
      * <br>
      * 1. `Context.stopService()`
      * 不论之前调用过多少次`startService()`，都会在调用一次本语句后关闭`Service`，
      * 但是如果有还没断开的`bind`连接，则会一直等到全部断开后自动关闭`Service`；
      * <br>
      * 2. `Service.stopSelf()`完全等同于`Context.stopService()`；
      * <br>
      * 3. `stopSelfResult(startId)`
      * 只有`startId`是最后一次`onStartCommand()`所传来的时，才会返回`true`并执行与`stopSelf()`相同的操作；
      * <br>
      * 4. `stopSelf(startId)`等同于`stopSelfResult(startId)`，只是没有返回值。
      */
    def stop[S <: Service](ctx: Context, clazz: Class[S]): Unit = {
      val intent = new Intent(ctx, clazz)
      intent.putExtra(CMD_EXTRA_STOP_SERVICE, true)
      ctx.startService(intent)
    }

    def bind[S <: Service](ctx: Context, conn: ServiceConnection, clazz: Class[S]): Unit = {
      start(ctx, clazz)
      i("start::bindService | %s.", clazz.getName.s)
      ctx.bindService(new Intent(ctx, clazz), conn, Context.BIND_AUTO_CREATE)
    }

    def unbind(ctx: Context, conn: ServiceConnection): Unit = try { // 如果Service已经被系统销毁，则这里会出现异常。
      ctx.unbindService(conn)
    } catch {
      case e: Exception => i(e)
    }

    /**
      * 生成能够向`Service`端传递消息的信使对象。
      *
      * @param service `bindService()`之后通过回调传回来的调用通道`IBinder`（详见`ServiceConnection`）。
      * @return 向`Service`端传递消息的信使对象。
      */
    def binder2Sender(service: IBinder): Messenger = new Messenger(service)

    /**
      * `Client`端调用本方法以使`Service`端可以向`Client`发送`Message`。
      *
      * @param sender  `Client`取得的面向`Service`的信使对象。
      * @param handler `Client`用来处理`Service`发来的`Message`的`Handler`。
      * @return 建立信使是否成功。
      */
    def replyToClient(sender: Messenger, handler: Handler): Option[Messenger] = {
      val msg = Message.obtain()
      msg.what = MSG_REPLY_TO
      msg.replyTo = new Messenger(handler)
      try {
        sender.send(msg)
        Option(msg.replyTo)
      } catch {
        case ex: RemoteException => e(ex)
          None
      }
    }

    def unReplyToClient(sender: Messenger, replyTo: Messenger) {
      val msg = Message.obtain()
      msg.what = MSG_UN_REPLY
      msg.replyTo = replyTo
      try {
        sender.send(msg)
      } catch {
        case ex: RemoteException => e(ex)
      }
    }
  }

  trait Acty {
    protected def startMe(ctx: Context, intent: Intent, options: Bundle): Unit = startMe(ctx, intent, false, 0, options)

    protected def startMe(context: Context, intent: Intent, forResult: Boolean = false, requestCode: Int = 0, options: Bundle = null): Unit = {
      if (forResult) {
        assert(context.isInstanceOf[Activity])
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
          context.as[Activity].startActivityForResult(intent, requestCode)
        else context.as[Activity].startActivityForResult(intent, requestCode, options)
      } else {
        if (!context.isInstanceOf[Activity]) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
          context.startActivity(intent)
        else context.startActivity(intent, options)
      }
    }
  }

  trait Fragmt {
    def startMe(fragmt: Fragment, intent: Intent, options: Bundle): Unit = startMe(fragmt, intent, false, 0, options)

    def startMe(fragmt: Fragment, intent: Intent, forResult: Boolean = false, requestCode: Int = 0, options: Bundle = null): Unit = {
      if (forResult) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
          fragmt.startActivityForResult(intent, requestCode)
        else fragmt.startActivityForResult(intent, requestCode, options)
      } else {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
          fragmt.startActivity(intent)
        else fragmt.startActivity(intent, options)
      }
    }
  }

  trait Dialog {
    def show[P <: AbsDialogFragment with TAG.ClassName](acty: FragmentActivity, panel: P): Unit = {
      panel.show(acty.getSupportFragmentManager, panel.className.toString, true)
    }
  }
}
