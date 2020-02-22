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
import android.content.{Context, DialogInterface, Intent}
import android.content.res.Configuration
import android.os.{Bundle, IBinder, PersistableBundle}
import android.util.AttributeSet
import android.view.{LayoutInflater, View, ViewGroup}
import androidx.fragment.app.Fragment
import hobby.chenai.nakam.basis.TAG
import hobby.wei.c.LOG._

/**
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 25/12/2017
  */
object LifecycleLog {
  trait Srvce extends Ctx.Srvce with TAG.ClassName {
    override def onBind(intent: Intent): IBinder = {
      i("LifecycleLog | onBind")
      null // super 是个`abstract`方法
    }

    override def onRebind(intent: Intent): Unit = {
      i("LifecycleLog | onRebind")
      super.onRebind(intent)
    }

    override def onUnbind(intent: Intent) = {
      i("LifecycleLog | onUnbind")
      super.onUnbind(intent)
    }

    override def onStartCommand(intent: Intent, flags: Int, startId: Int) = {
      i("LifecycleLog | onStartCommand")
      super.onStartCommand(intent, flags, startId)
    }

    override def onTaskRemoved(rootIntent: Intent): Unit = {
      i("LifecycleLog | onTaskRemoved")
      super.onTaskRemoved(rootIntent)
    }

    override def onCreate(): Unit = {
      i("LifecycleLog | onCreate")
      super.onCreate()
    }

    override def onLowMemory(): Unit = {
      i("LifecycleLog | onLowMemory")
      super.onLowMemory()
    }

    override def onTrimMemory(level: Int): Unit = {
      i("LifecycleLog | onTrimMemory")
      super.onTrimMemory(level)
    }

    override def onDestroy(): Unit = {
      i("LifecycleLog | onDestroy")
      super.onDestroy()
    }

    override def onConfigurationChanged(newConfig: Configuration): Unit = {
      i("LifecycleLog | onConfigurationChanged")
      super.onConfigurationChanged(newConfig)
    }
  }

  trait Acty extends Ctx.Acty with TAG.ClassName {
    override protected def onCreate(savedInstanceState: Bundle): Unit = {
      i("LifecycleLog | onCreate")
      super.onCreate(savedInstanceState)
    }

    override def onCreate(savedInstanceState: Bundle, persistentState: PersistableBundle): Unit = {
      i("LifecycleLog | onCreate2")
      super.onCreate(savedInstanceState, persistentState)
    }

    override protected def onPostCreate(savedInstanceState: Bundle): Unit = {
      i("LifecycleLog | onPostCreate")
      super.onPostCreate(savedInstanceState)
    }

    override def onCreateView(name: String, context: Context, attrs: AttributeSet) = {
      i("LifecycleLog | onCreateView")
      super.onCreateView(name, context, attrs)
    }

    override def onCreateView(parent: View, name: String, context: Context, attrs: AttributeSet) = {
      i("LifecycleLog | onCreateView2")
      super.onCreateView(parent, name, context, attrs)
    }

    override protected def onActivityResult(requestCode: Int, resultCode: Int, data: Intent): Unit = {
      i("LifecycleLog | onActivityResult")
      super.onActivityResult(requestCode, resultCode, data)
    }

    override def onRequestPermissionsResult(requestCode: Int, permissions: Array[String], grantResults: Array[Int]): Unit = {
      i("LifecycleLog | onRequestPermissionsResult")
      super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override protected def onSaveInstanceState(outState: Bundle): Unit = {
      i("LifecycleLog | onSaveInstanceState")
      super.onSaveInstanceState(outState)
    }

    override def onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle): Unit = {
      i("LifecycleLog | onSaveInstanceState2")
      super.onSaveInstanceState(outState, outPersistentState)
    }

    override protected def onRestoreInstanceState(savedInstanceState: Bundle): Unit = {
      i("LifecycleLog | onRestoreInstanceState")
      super.onRestoreInstanceState(savedInstanceState)
    }

    override def onRestoreInstanceState(savedInstanceState: Bundle, persistentState: PersistableBundle): Unit = {
      i("LifecycleLog | onRestoreInstanceState2")
      super.onRestoreInstanceState(savedInstanceState, persistentState)
    }

    override def onConfigurationChanged(newConfig: Configuration): Unit = {
      i("LifecycleLog | onConfigurationChanged")
      super.onConfigurationChanged(newConfig)
    }

    override def onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration): Unit = {
      i("LifecycleLog | onMultiWindowModeChanged")
      super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
    }

    override def onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration): Unit = {
      i("LifecycleLog | onPictureInPictureModeChanged")
      super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    }

    override def onUserInteraction(): Unit = {
      i("LifecycleLog | onUserInteraction")
      super.onUserInteraction()
    }

    override protected def onUserLeaveHint(): Unit = {
      i("LifecycleLog | onUserLeaveHint")
      super.onUserLeaveHint()
    }

    override protected def onRestart(): Unit = {
      i("LifecycleLog | onRestart")
      super.onRestart()
    }

    override protected def onStart(): Unit = {
      i("LifecycleLog | onStart")
      super.onStart()
    }

    override protected def onResume(): Unit = {
      i("LifecycleLog | onResume")
      super.onResume()
    }

    override protected def onPostResume(): Unit = {
      i("LifecycleLog | onPostResume")
      super.onPostResume()
    }

    override protected def onPause(): Unit = {
      i("LifecycleLog | onPause")
      super.onPause()
    }

    override protected def onStop(): Unit = {
      i("LifecycleLog | onStop")
      super.onStop()
    }

    override protected def onDestroy(): Unit = {
      i("LifecycleLog | onDestroy")
      super.onDestroy()
    }

    override protected def onDestroyToExit(): Unit = {
      i("LifecycleLog | onDestroyToExit")
      super.onDestroyToExit()
    }

    override def onBackPressed(): Unit = {
      i("LifecycleLog | onBackPressed")
      super.onBackPressed()
    }
  }

  trait Fragmt extends Ctx.Fragmt with TAG.ClassName {
    override def onAttach(activity: Activity): Unit = {
      i("LifecycleLog | onAttach(activity)")
      super.onAttach(activity)
    }

    override def onAttach(context: Context): Unit = {
      i("LifecycleLog | onAttach(context)")
      super.onAttach(context)
    }

    override def onAttachFragment(childFragment: Fragment): Unit = {
      i("LifecycleLog | onAttachFragment")
      super.onAttachFragment(childFragment)
    }

    override def onActivityCreated(savedInstanceState: Bundle): Unit = {
      i("LifecycleLog | onActivityCreated")
      super.onActivityCreated(savedInstanceState)
    }

    override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent): Unit = {
      i("LifecycleLog | onActivityResult")
      super.onActivityResult(requestCode, resultCode, data)
    }

    override def onRequestPermissionsResult(requestCode: Int, permissions: Array[String], grantResults: Array[Int]): Unit = {
      i("LifecycleLog | onRequestPermissionsResult")
      super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override def onSaveInstanceState(outState: Bundle): Unit = {
      i("LifecycleLog | onSaveInstanceState")
      super.onSaveInstanceState(outState)
    }

    override def onViewStateRestored(savedInstanceState: Bundle): Unit = {
      i("LifecycleLog | onViewStateRestored")
      super.onViewStateRestored(savedInstanceState)
    }

    override def onCreate(savedInstanceState: Bundle): Unit = {
      i("LifecycleLog | onCreate")
      super.onCreate(savedInstanceState)
    }

    override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle) = {
      i("LifecycleLog | onCreateView")
      super.onCreateView(inflater, container, savedInstanceState)
    }

    override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
      i("LifecycleLog | onViewCreated")
      super.onViewCreated(view, savedInstanceState)
    }

    override def onStart(): Unit = {
      i("LifecycleLog | onStart")
      super.onStart()
    }

    override def onResume(): Unit = {
      i("LifecycleLog | onResume")
      super.onResume()
    }

    override def onPause(): Unit = {
      i("LifecycleLog | onPause")
      super.onPause()
    }

    override def onStop(): Unit = {
      i("LifecycleLog | onStop")
      super.onStop()
    }

    override def onDestroyView(): Unit = {
      i("LifecycleLog | onDestroyView")
      super.onDestroyView()
    }

    override def onDestroy(): Unit = {
      i("LifecycleLog | onDestroy")
      super.onDestroy()
    }

    override def onDetach(): Unit = {
      i("LifecycleLog | onDetach")
      super.onDetach()
    }
  }

  trait Dialog extends Ctx.Dialog with Fragmt {
    override def onCreateDialog(savedInstanceState: Bundle) = {
      i("LifecycleLog | onCreateDialog")
      super.onCreateDialog(savedInstanceState)
    }

    override def onShow(dialog: DialogInterface): Unit = {
      i("LifecycleLog | onShow")
      super.onShow(dialog)
    }

    override def onCancel(dialog: DialogInterface): Unit = {
      i("LifecycleLog | onCancel")
      super.onCancel(dialog)
    }

    override def onDismiss(dialog: DialogInterface): Unit = {
      i("LifecycleLog | onDismiss")
      super.onDismiss(dialog)
    }
  }
}
