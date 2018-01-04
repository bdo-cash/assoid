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
import android.os.Handler
import android.view.Window
import hobby.chenai.nakam.basis.TAG.ThrowMsg
import hobby.chenai.nakam.lang.J2S.NonNull
import hobby.chenai.nakam.lang.TypeBring.AsIs

/**
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 18/11/2017
  */
object Ctx {
  trait Abs {
    implicit def activity: AbsActy
    implicit def context: Context
    implicit def window: Window
    def mainHandler: Handler = AbsApp.get.mainHandler

    def post(f: => Any) = mainHandler.post(new Runnable {
      override def run(): Unit = f
    })

    def postDelayed(delayed: Long)(f: => Any) = mainHandler.postDelayed(new Runnable {
      override def run(): Unit = f
    }, delayed)
  }

  trait %[A <: AbsApp] {
    def getApp: A = AbsApp.get
  }

  trait Acty extends AbsActy with Abs {
    override implicit lazy val activity: AbsActy = this
    override implicit def context: Context = this
    override implicit def window: Window = getWindow
  }

  trait Fragmt extends Fragment with Abs {
    private lazy val msg = "调用时机是不是不对？".tag

    // 由于 Activity 在 Fragment 的生命周期中，可能会重建。所以不能定义为 val。
    override implicit def activity: AbsActy = getActivity.as[AbsActy].ensuring(_.nonNull, msg)
    override implicit def context: Context = getContext.ensuring(_.nonNull, msg)
    override implicit def window: Window = activity.getWindow
  }

  trait Dialog extends DialogFragment with Fragmt {
    override implicit def window: Window = getDialog.getWindow
  }
}
