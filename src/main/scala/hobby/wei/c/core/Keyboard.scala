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

  trait Acty extends Ctx.Acty with Keyboard {

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

  trait Fragmt extends Ctx.Fragmt with Keyboard {

    override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
      super.onViewCreated(view, savedInstanceState)
      initClickBlankAreaHandler(view)
    }
  }

  trait Dialog extends Ctx.Dialog with Fragmt
}

trait Keyboard extends Ctx.AbsUi {
  protected def getClickHideInputMethodViewIds: Array[Int] = Array()
  protected def getClickHideInputMethodViews: Array[View]  = Array()

  private[Keyboard] def initClickBlankAreaHandler(rootView: View): Unit = {
    rootView.setOnClickListener(mOnClickBlankAreaListener)
    for (id <- getClickHideInputMethodViewIds) {
      val view: View = rootView.findViewById(id)
      if (view.nonNull) view.setOnClickListener(mOnClickBlankAreaListener)
    }
    getClickHideInputMethodViews.foreach { view =>
      if (view.nonNull) view.setOnClickListener(mOnClickBlankAreaListener)
    }
  }

  private val mOnClickBlankAreaListener = new View.OnClickListener() {
    override def onClick(v: View): Unit = hideInputMethod()
  }

  protected def autoShowInputMethod(editText: EditText): Unit = {
    editText.setOnFocusChangeListener(new OnFocusChangeListener {
      override def onFocusChange(v: View, hasFocus: Boolean): Unit = postOnIdle() {
        if (hasFocus) {
          showInputMethod(editText)
          editText.setSelection(editText.getText.length)
        }
      }
    })
    editText.requestFocus()
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
