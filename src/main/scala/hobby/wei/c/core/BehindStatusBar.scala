/*
 * Copyright (C) 2020-present, Chenai Nakam(chenai.nakam@gmail.com)
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

import android.graphics.Color
import android.os.Build
import android.view.{View, ViewGroup, WindowManager}

/**
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 07/02/2020
  */
trait BehindStatusBar extends Ctx.Acty {
  protected def setContentViewBehindStatusBar(layout: ViewGroup) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
      window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
      window.getDecorView.setSystemUiVisibility(
        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
      )
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.setStatusBarContrastEnforced(true)
      }
      layout.setFitsSystemWindows(false)
      changeStatusBarColor(statusBarColor)
    }
  }

  def changeStatusBarColor(color: Int) {
    window.setStatusBarColor(color)
  }

  protected def statusBarColor: Int = Color.TRANSPARENT
}
