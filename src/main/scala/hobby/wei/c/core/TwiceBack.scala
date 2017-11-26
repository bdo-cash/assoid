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
 *
 */

package hobby.wei.c.core

import android.os.SystemClock
import android.text.Html
import android.widget.Toast
import hobby.chenai.nakam.assoid.R

/**
  * @author Chenakam (chenai.nakam@gmail.com)
  * @version 1.0, 26/11/2017
  */
trait TwiceBack extends Ctx.Acty {
  private val TIME_INTERVAL_BACK_4_FINISH = 600
  private val TIME_INTERVAL_BACK_4_TOAST = 5000
  private var timeBackPressed: Long = 0

  protected def toastMsg4TwiceBackGuide: CharSequence = Html.fromHtml(getString(R.string.toast_message_4_twice_back_guide))

  override def onBackPressed(): Unit = {
    val interval = SystemClock.elapsedRealtime() - timeBackPressed
    if (interval <= TIME_INTERVAL_BACK_4_FINISH) super.onBackPressed()
    else if (interval <= TIME_INTERVAL_BACK_4_TOAST) {
      Toast.makeText(this, toastMsg4TwiceBackGuide, Toast.LENGTH_LONG).show()
      // 弹出提示的时候重置时间，是为了更好的贯彻两次点击操作感受。
      timeBackPressed = 0
    } else timeBackPressed = SystemClock.elapsedRealtime()
  }
}
