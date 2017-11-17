package hobby.wei.c.core

import android.app.Activity
import android.content.{Context, Intent}
import hobby.chenai.nakam.basis.TAG

/**
  * @author Chenakam (chenai.nakam@gmail.com)
  * @version 1.0, 17/11/2017
  */
trait StartMe {
  def startMe(context: Context, intent: Intent): Unit = {
    if (!context.isInstanceOf[Activity]) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
  }

  def show[P <: AbsDialogFragment with TAG.ClassName](acty: Activity, panel: P): Unit = {
    panel.show(acty.getFragmentManager, panel.className.toString, true)
  }
}
