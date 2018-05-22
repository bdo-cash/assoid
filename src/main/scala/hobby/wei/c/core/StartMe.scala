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
import hobby.chenai.nakam.basis.TAG
import hobby.chenai.nakam.lang.TypeBring.AsIs
import hobby.wei.c.LOG._

/**
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 17/11/2017
  */
object StartMe {
  trait Srvce extends TAG.ClassName {
    def start[S <: Service](ctx: Context, clazz: Class[S]): Unit = ctx.startService(new Intent(ctx, clazz))

    /**
      * 停止`clazz`参数指定的`Service`。
      *
      * @param CMD_EXTRA_OF_STOP 必须与重写的`AbsService.CMD_EXTRA_OF_STOP`一致。
      */
    def stop[S <: Service](ctx: Context, clazz: Class[S], CMD_EXTRA_OF_STOP: String): Unit = {
      val intent = new Intent(ctx, clazz)
      intent.putExtra(CMD_EXTRA_OF_STOP, true)
      ctx.startService(intent) // 发送一个请求让其自己关闭，而不是直接stopService()。
      /*
       * 几种stopService()的异同：
       *
       * Context.stopService()
       * 不论之前调用过多少次startService()，都会在调用一次本语句后关闭Service.
       * 但是如果有还没断开的bind连接，则会一直等到全部断开后自动关闭Service.
       *
       * Service.stopSelf()完全等同于Context.stopService().
       *
       * stopSelfResult(startId)
       * 只有startId是最后一次onStartCommand()所传来的时，才会返回true并执行与stopSelf()相同的操作.
       *
       * stopSelf(startId)等同于stopSelfResult(startId)，只是没有返回值.
       */
    }

    def bind[S <: Service](ctx: Context, conn: ServiceConnection, clazz: Class[S]): Unit = {
      start(ctx, clazz)
      i("start::bindService| %s.", clazz.getName.s)
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
      * @param sender       `Client`取得的面向`Service`的信使对象。
      * @param handler      `Client`用来处理`Service`发来的`Message`的`Handler`。
      * @param MSG_REPLY_TO 必须与重写的`AbsService.MSG_REPLY_TO`一致。
      * @return 建立信使是否成功。
      */
    def replyToClient(sender: Messenger, handler: Handler, MSG_REPLY_TO: Int): Boolean = {
      val msg = Message.obtain()
      msg.what = MSG_REPLY_TO
      msg.replyTo = new Messenger(handler)
      try {
        sender.send(msg)
        true
      } catch {
        case ex: RemoteException => e(ex)
          false
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
        context.startActivity(intent)
      }
    }
  }

  trait Fragmt {
    // nothing...
  }

  trait Dialog {
    def show[P <: AbsDialogFragment with TAG.ClassName](acty: Activity, panel: P): Unit = {
      panel.show(acty.getFragmentManager, panel.className.toString, true)
    }
  }
}
