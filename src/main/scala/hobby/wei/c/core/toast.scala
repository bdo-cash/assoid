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
import hobby.chenai.nakam.lang.J2S.Run
import hobby.chenai.nakam.lang.TypeBring.AsIs
import hobby.wei.c.core.Ctx.%

/**
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 20/06/2018
  */
object toast extends %[AbsApp] {

  // 现在已经不支持`Gravity`等其它属性，见`Toast.isSystemRenderedTextToast()`。官方推荐使用`Snackbar`代替。
  def apply(
    s: CharSequence,
    long: Boolean = false,
    @deprecated gravity: Int = -1,
    @deprecated xOffset: Int = 0,
    @deprecated yOffset: Int = 0
  )(implicit ctx: Context): Unit = {
    val runnable = {
      val toast = Toast.makeText(ctx, s, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT)
      if (gravity != -1) toast.setGravity(gravity, xOffset, yOffset)
      toast.show()
    }.run$

    if (Looper.getMainLooper.isCurrentThread) runnable.run()
    else if (ctx.isInstanceOf[AbsActy]) ctx.as[AbsActy].runOnUiThread(runnable)
    else getApp.mainHandler.post(runnable)
  }
}
