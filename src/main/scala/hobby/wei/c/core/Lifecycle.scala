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

import android.app.{Activity, Fragment}
import android.content.{Context, Intent}
import android.os.{Bundle, PersistableBundle}
import android.util.AttributeSet
import android.view.{LayoutInflater, View, ViewGroup}
import hobby.chenai.nakam.basis.TAG
import hobby.wei.c.LOG._

/**
  * @author Chenakam (chenai.nakam@gmail.com)
  * @version 1.0, 25/12/2017
  */
object Lifecycle {
  trait Acty extends AbsActy with TAG.ClassName {
    override protected def onCreate(savedInstanceState: Bundle): Unit = {
      i("onCreate")
      super.onCreate(savedInstanceState)
    }

    override protected def onCreate(savedInstanceState: Bundle, persistentState: PersistableBundle): Unit = {
      i("onCreate2")
      super.onCreate(savedInstanceState, persistentState)
    }

    override protected def onPostCreate(savedInstanceState: Bundle): Unit = {
      i("onPostCreate")
      super.onPostCreate(savedInstanceState)
    }

    override protected def onCreateView(name: String, context: Context, attrs: AttributeSet) = {
      i("onCreateView")
      super.onCreateView(name, context, attrs)
    }

    override protected def onCreateView(parent: View, name: String, context: Context, attrs: AttributeSet) = {
      i("onCreateView2")
      super.onCreateView(parent, name, context, attrs)
    }

    override protected def onActivityResult(requestCode: Int, resultCode: Int, data: Intent): Unit = {
      i("onActivityResult")
      super.onActivityResult(requestCode, resultCode, data)
    }

    override def onRequestPermissionsResult(requestCode: Int, permissions: Array[String], grantResults: Array[Int]): Unit = {
      i("onRequestPermissionsResult")
      super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override protected def onSaveInstanceState(outState: Bundle): Unit = {
      i("onSaveInstanceState")
      super.onSaveInstanceState(outState)
    }

    override protected def onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle): Unit = {
      i("onSaveInstanceState2")
      super.onSaveInstanceState(outState, outPersistentState)
    }

    override def onRestoreInstanceState(savedInstanceState: Bundle): Unit = {
      i("onRestoreInstanceState")
      super.onRestoreInstanceState(savedInstanceState)
    }

    override def onRestoreInstanceState(savedInstanceState: Bundle, persistentState: PersistableBundle): Unit = {
      i("onRestoreInstanceState2")
      super.onRestoreInstanceState(savedInstanceState, persistentState)
    }

    override protected def onUserInteraction(): Unit = {
      i("onUserInteraction")
      super.onUserInteraction()
    }

    override protected def onUserLeaveHint(): Unit = {
      i("onUserLeaveHint")
      super.onUserLeaveHint()
    }

    override protected def onRestart(): Unit = {
      i("onRestart")
      super.onRestart()
    }

    override protected def onStart(): Unit = {
      i("onStart")
      super.onStart()
    }

    override protected def onResume(): Unit = {
      i("onResume")
      super.onResume()
    }

    override protected def onPostResume(): Unit = {
      i("onPostResume")
      super.onPostResume()
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

    override def onBackPressed(): Unit = {
      i("onBackPressed")
      super.onBackPressed()
    }
  }

  trait Fragmt extends Fragment with TAG.ClassName {
    override def onAttach(activity: Activity): Unit = {
      i("onAttach")
      super.onAttach(activity)
    }

    override def onAttach(context: Context): Unit = {
      i("onAttach2")
      super.onAttach(context)
    }

    override def onAttachFragment(childFragment: Fragment): Unit = {
      i("onAttachFragment")
      super.onAttachFragment(childFragment)
    }

    override def onActivityCreated(savedInstanceState: Bundle): Unit = {
      i("onActivityCreated")
      super.onActivityCreated(savedInstanceState)
    }

    override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent): Unit = {
      i("onActivityResult")
      super.onActivityResult(requestCode, resultCode, data)
    }

    override def onRequestPermissionsResult(requestCode: Int, permissions: Array[String], grantResults: Array[Int]): Unit = {
      i("onRequestPermissionsResult")
      super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override def onSaveInstanceState(outState: Bundle): Unit = {
      i("onSaveInstanceState")
      super.onSaveInstanceState(outState)
    }

    override def onViewStateRestored(savedInstanceState: Bundle): Unit = {
      i("onViewStateRestored")
      super.onViewStateRestored(savedInstanceState)
    }

    override def onCreate(savedInstanceState: Bundle): Unit = {
      i("onCreate")
      super.onCreate(savedInstanceState)
    }

    override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle) = {
      i("onCreateView")
      super.onCreateView(inflater, container, savedInstanceState)
    }

    override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
      i("onViewCreated")
      super.onViewCreated(view, savedInstanceState)
    }

    override def onStart(): Unit = {
      i("onStart")
      super.onStart()
    }

    override def onResume(): Unit = {
      i("onResume")
      super.onResume()
    }

    override def onPause(): Unit = {
      i("onPause")
      super.onPause()
    }

    override def onStop(): Unit = {
      i("onStop")
      super.onStop()
    }

    override def onDestroyView(): Unit = {
      i("onDestroyView")
      super.onDestroyView()
    }

    override def onDestroy(): Unit = {
      i("onDestroy")
      super.onDestroy()
    }

    override def onDetach(): Unit = {
      i("onDetach")
      super.onDetach()
    }
  }

  trait Dialog extends AbsDialogFragment with Fragmt
}
