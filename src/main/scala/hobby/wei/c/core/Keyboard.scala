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

import androidx.fragment.app.{DialogFragment, Fragment}
import android.content.Context
import android.os.Bundle
import android.view.{View, ViewGroup}
import android.view.inputmethod.InputMethodManager
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup.LayoutParams
import android.widget.EditText
import hobby.chenai.nakam.lang.J2S.NonNull
import hobby.chenai.nakam.lang.TypeBring.AsIs

/**
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 17/11/2017
  */
object Keyboard {
  trait Acty extends AbsActy with Keyboard with Ctx.Acty {
    override def setContentView(layoutResID: Int): Unit = {
      super.setContentView(layoutResID)
      initClickBlankAreaHandler(getWindow.getDecorView.as[ViewGroup].getChildAt(0))
    }

    override def setContentView(view: View): Unit = {
      super.setContentView(view)
      initClickBlankAreaHandler(view)
    }

    override def setContentView(view: View, params: LayoutParams): Unit = {
      super.setContentView(view, params)
      initClickBlankAreaHandler(view)
    }
  }

  trait Fragmt extends Fragment with Keyboard with Ctx.Fragmt {
    override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
      super.onViewCreated(view, savedInstanceState)
      initClickBlankAreaHandler(view)
    }
  }

  trait Dialog extends DialogFragment with Fragmt with Ctx.Dialog
}

trait Keyboard extends Ctx.Abs {
  protected def getClickHideInputMethodViewIds: Array[Int] = null

  private[Keyboard] def initClickBlankAreaHandler(rootView: View): Unit = {
    rootView.setOnClickListener(mOnClickBlankAreaListener)
    val ids = getClickHideInputMethodViewIds
    if (ids != null) {
      var view: View = null
      for (id <- ids) {
        view = rootView.findViewById(id)
        if (view.nonNull) view.setOnClickListener(mOnClickBlankAreaListener)
      }
    }
  }

  private val mOnClickBlankAreaListener = new View.OnClickListener() {
    override def onClick(v: View): Unit = hideInputMethod()
  }

  protected def autoShowInputMethod(editText: EditText): Unit = {
    editText.setOnFocusChangeListener(new OnFocusChangeListener {
      override def onFocusChange(v: View, hasFocus: Boolean): Unit = mainHandler.postDelayed(new Runnable {
        override def run(): Unit = if (hasFocus) {
          showInputMethod(editText)
          editText.setSelection(editText.getText.length)
        }
      }, 300)
    })
  }

  protected def showInputMethod(focusView: View = if (window.nonNull) window.getCurrentFocus else null): Unit = {
    if (focusView.nonNull) { //是否存在焦点
      inputMethodMgr.showSoftInput(focusView, InputMethodManager.SHOW_IMPLICIT)
    }
  }

  protected def hideInputMethod(): Unit = {
    val windowToken = if (window.nonNull) window.getDecorView.getWindowToken else null
    if (windowToken.nonNull) {
      inputMethodMgr.hideSoftInputFromWindow(windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }
  }

  protected def inputMethodMgr = window.getContext.getSystemService(Context.INPUT_METHOD_SERVICE).as[InputMethodManager]
}
