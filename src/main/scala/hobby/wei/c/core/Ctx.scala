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

import android.app.{DialogFragment, Fragment}
import android.content.Context
import android.os.{Build, Handler, Looper, MessageQueue}
import android.support.v4.app
import android.view.Window
import hobby.chenai.nakam.basis.TAG.ThrowMsg
import hobby.chenai.nakam.lang.J2S.{NonNull, Run}
import hobby.chenai.nakam.lang.TypeBring.AsIs

/**
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 18/11/2017
  */
object Ctx {
  trait Abs {
    implicit def activity: AbsActy
    implicit def context: Context
    implicit def window: Window = activity.getWindow

    def mainHandler: Handler = AbsApp.get[AbsApp].mainHandler
    def runOnUiThread(f: => Any): Unit = activity.runOnUiThread(f.run$)

    def post(f: => Any) = mainHandler.post(f.run$)
    def postDelayed(delayed: Long)(f: => Any) = mainHandler.postDelayed(f.run$, delayed)
    /**
      * 注意只能在`Activity`进程中调用。
      */
    def postOnIdle(times: Int = -1)(action: => Any) {
      // mainHandler.getLooper.getQueue // requires API level 23
      runOnUiThread(Looper.myQueue().addIdleHandler(new MessageQueue.IdleHandler {
        private var t = 0
        override def queueIdle() = {
          action
          t += 1
          t < times
        }
      }))
    }
  }

  trait %[A <: AbsApp] {
    def getApp: A = AbsApp.get
  }

  trait Srvce extends AbsSrvce with Abs {
    implicit def activity: AbsActy = ???
    implicit def context: Context = this
  }

  trait Acty extends AbsActy with Abs {
    override implicit lazy val activity: AbsActy = this
    override implicit def context: Context = this
  }

  trait Fragmt extends Fragment with Abs {
    private lazy val msg = "调用时机是不是不对？".tag

    // 由于 Activity 在 Fragment 的生命周期中，可能会重建。所以不能定义为 val。
    override implicit def activity: AbsActy = getActivity.as[AbsActy].ensuring(_.nonNull, msg)
    override implicit def context: Context = (if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) activity else getContext).ensuring(_.nonNull, msg)
  }

  trait FragmtV4 extends app.Fragment with Abs {
    private lazy val msg = "调用时机是不是不对？".tag

    // 由于 Activity 在 Fragment 的生命周期中，可能会重建。所以不能定义为 val。
    override implicit def activity: AbsActy = getActivity.as[AbsActy].ensuring(_.nonNull, msg)
    override implicit def context: Context = (if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) activity else getContext).ensuring(_.nonNull, msg)
  }

  trait Dialog extends DialogFragment with Fragmt {
    override implicit def window: Window = getDialog.getWindow
  }

  trait DialogV4 extends app.DialogFragment with FragmtV4 {
    override implicit def window: Window = getDialog.getWindow
  }
}
