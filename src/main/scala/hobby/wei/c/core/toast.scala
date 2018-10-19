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

import android.content.Context
import android.os.Looper
import android.widget.Toast
import hobby.chenai.nakam.lang.J2S.{NonNull, Run}
import hobby.wei.c.core.Ctx.%

/**
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 20/06/2018
  */
object toast extends %[AbsApp] {
  def apply(s: CharSequence, lang: Boolean = false, gravity: Int = -1, xOffset: Int = 0, yOffset: Int = 0)(implicit context: Context): Unit = {
    val runnable = {
      val toast = Toast.makeText(context, s, if (lang) Toast.LENGTH_LONG else Toast.LENGTH_SHORT)
      if (gravity != -1) toast.setGravity(gravity, xOffset, yOffset)
      toast.show()
    }.run$
    if (Looper.myLooper.isNull) getApp.mainHandler.post(runnable) else runnable.run()
  }
}
