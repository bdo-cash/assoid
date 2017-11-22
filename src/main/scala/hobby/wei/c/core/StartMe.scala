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

import android.app.Activity
import android.content.{Context, Intent}
import android.os.Bundle
import hobby.chenai.nakam.basis.TAG
import hobby.chenai.nakam.lang.TypeBring.AsIs

/**
  * @author Chenakam (chenai.nakam@gmail.com)
  * @version 1.0, 17/11/2017
  */
trait StartMe {
  protected def startMe(context: Context, intent: Intent, forResult: Boolean = false, requestCode: Int = 0, options: Bundle = null): Unit = {
    if (forResult) context.ensuring(_.is[Activity]).as[Activity].startActivityForResult(intent, requestCode, options)
    else {
      if (!context.isInstanceOf[Activity]) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(intent)
    }
  }

  def show[P <: AbsDialogFragment with TAG.ClassName](acty: Activity, panel: P): Unit = {
    panel.show(acty.getFragmentManager, panel.className.toString, true)
  }
}
