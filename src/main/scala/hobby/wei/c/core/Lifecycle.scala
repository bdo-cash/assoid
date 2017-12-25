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
import android.os.{Bundle, PersistableBundle}
import android.util.AttributeSet
import android.view.View
import hobby.chenai.nakam.basis.TAG
import hobby.wei.c.LOG._

/**
  * @author Chenakam (chenai.nakam@gmail.com)
  * @version 1.0, 25/12/2017
  */
object Lifecycle {
  trait Acty extends AbsActy with TAG.ClassName {
    override protected def onCreate(savedInstanceState: Bundle): Unit = {
      super.onCreate(savedInstanceState)
      i("onCreate")
    }

    override protected def onCreate(savedInstanceState: Bundle, persistentState: PersistableBundle): Unit = {
      super.onCreate(savedInstanceState, persistentState)
      i("onCreate2")
    }

    override protected def onPostCreate(savedInstanceState: Bundle): Unit = {
      super.onPostCreate(savedInstanceState)
      i("onPostCreate")
    }

    override protected def onCreateView(name: String, context: Context, attrs: AttributeSet) = {
      i("onCreateView")
      super.onCreateView(name, context, attrs)
    }

    override protected def onCreateView(parent: View, name: String, context: Context, attrs: AttributeSet) = {
      i("onCreateView2")
      super.onCreateView(parent, name, context, attrs)
    }

    override protected def onRestart(): Unit = {
      super.onRestart()
      i("onRestart")
    }

    override protected def onStart(): Unit = {
      super.onStart()
      i("onStart")
    }

    override protected def onResume(): Unit = {
      super.onResume()
      i("onResume")
    }

    override protected def onPostResume(): Unit = {
      super.onPostResume()
      i("onPostResume")
    }

    override protected def onPause(): Unit = {
      i("onPause")
      super.onPause()
    }

    override protected def onStop(): Unit = {
      i("onStop")
      super.onStop()
    }

    override protected def onDestroy(): Unit = {
      i("onDestroy")
      super.onDestroy()
    }

    override protected def onDestroyToExit(): Unit = {
      i("onDestroyToExit")
      super.onDestroyToExit()
    }
  }
}
