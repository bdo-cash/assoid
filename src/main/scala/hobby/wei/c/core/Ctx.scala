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

import android.content.Context
import android.os.{Build, Handler, MessageQueue}
import android.view.Window
import androidx.fragment.app.Fragment
import hobby.chenai.nakam.basis.TAG.ThrowMsg
import hobby.chenai.nakam.lang.J2S.{NonNull, Run}
import hobby.chenai.nakam.lang.TypeBring.AsIs

/**
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 18/11/2017
  */
object Ctx {

  trait Abs {
    implicit def context: Context

    def mainHandler: Handler = AbsApp.get[AbsApp].mainHandler

    def post(f: => Any) = mainHandler.post(f.run$)

    def postDelayed(delayed: Long)(f: => Any) = mainHandler.postDelayed(f.run$, delayed)
  }

  trait AbsUi extends Abs {
    implicit def activity: AbsActy
    implicit def context: Context
    implicit def window: Window = activity.getWindow

    def runOnUiThread(f: => Any): Unit = activity.runOnUiThread(f.run$)

    /**
      * 在`消息循环`（`UI`线程）空闲时执行`action`操作`times`次。<br>
      * 注意：只能在`Activity`进程中调用。
      *
      * @param times  要重复执行`[[action]]`的次数（但无论是几，至少执行一次）。
      * @param action 要在空闲时执行操作。
      */
    def postOnIdle(times: Int = -1)(action: => Any) {
      mainHandler.getLooper.getQueue.addIdleHandler(new MessageQueue.IdleHandler {
        private var t = 0
        override def queueIdle() = {
          action
          t += 1
          t < times
        }
      })
    }
  }

  trait %[A <: AbsApp] {
    def getApp: A = AbsApp.get
  }

  trait Srvce extends AbsSrvce with Abs {
    implicit def context: Context = this
  }

  trait Acty extends AbsActy with AbsUi {
    override implicit def activity: AbsActy = this
    override implicit def context: Context = this
  }

  trait Fragmt extends Fragment with AbsUi {
    private lazy val msg = "调用时机是不是不对？".tag

    // 由于 Activity 在 Fragment 的生命周期中，可能会重建。所以不能定义为 val。
    override implicit def activity: AbsActy = getActivity.as[AbsActy].ensuring(_.nonNull, msg)
    override implicit def context: Context = (if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) activity else getContext).ensuring(_.nonNull, msg)
  }

  trait Dialog extends AbsDialogFragment with Fragmt {
    override implicit def window: Window = getDialog.getWindow
  }
}
