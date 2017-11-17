package hobby.wei.c.core

import android.app.{DialogFragment, Fragment}
import android.os.Bundle
import android.view.{View, ViewGroup, Window}
import android.view.inputmethod.InputMethodManager
import android.view.ViewGroup.LayoutParams
import hobby.chenai.nakam.lang.J2S.NonNull
import hobby.chenai.nakam.lang.TypeBring.AsIs

/**
  * @author Chenakam (chenai.nakam@gmail.com)
  * @version 1.0, 17/11/2017
  */
object Keyboard {
  trait Acty extends AbsActy with Keyboard {
    override private[core] def currentWindow: Window = getWindow

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

  trait Fragmt extends Fragment with Keyboard {
    override private[core] def currentWindow: Window = getActivity.getWindow

    override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
      super.onViewCreated(view, savedInstanceState)
      initClickBlankAreaHandler(view)
    }
  }

  trait Dialog extends DialogFragment with Fragmt {
    override private[core] def currentWindow: Window = getDialog.getWindow
  }
}

trait Keyboard {
  private[core] def currentWindow: Window

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

  protected def showInputMethod(): Unit = {
    val focusView = if (currentWindow.nonNull) currentWindow.getCurrentFocus else null
    if (focusView.nonNull) { //是否存在焦点
      inputMethodMgr.showSoftInput(focusView, InputMethodManager.SHOW_IMPLICIT)
    }
  }

  protected def hideInputMethod(): Unit = {
    val windowToken = if (currentWindow.nonNull) currentWindow.getDecorView.getWindowToken else null
    if (windowToken.nonNull) {
      inputMethodMgr.hideSoftInputFromWindow(windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }
  }

  protected def inputMethodMgr = currentWindow.getContext.getSystemService(classOf[InputMethodManager])
}
