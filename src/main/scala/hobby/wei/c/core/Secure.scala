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
import android.os.Bundle
import android.view.{Window, WindowManager}

/**
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 17/11/2017
  */
object Secure {
  trait Abs {
    def secure(window: Window): Unit = {
      window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
  }

  trait Acty extends Activity with Abs {
    override def onCreate(savedInstanceState: Bundle): Unit = {
      secure(getWindow)
      super.onCreate(savedInstanceState)
    }
  }

  trait Fragmt extends AbsDialogFragment with Abs {
    override def onCreateDialog(savedInstanceState: Bundle) = {
      val dialog = super.onCreateDialog(savedInstanceState)
      secure(dialog.getWindow)
      dialog
    }
  }
}
