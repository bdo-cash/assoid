package hobby.wei.c.util

import android.app.Activity
import android.content.Context
import android.util.TypedValue
import android.view.{View, ViewGroup, Window, WindowManager}
import hobby.chenai.nakam.lang.TypeBring.AsIs

/**
  * @author Wei.Chou
  * @version 1.0, 19/02/2020
  */
object UiUtils {

  implicit class DipPixelConventions(value: Float) {
    def dp(implicit context: Context): Float = dp2px(context, value)
    def dps(implicit context: Context): Int  = dp2pxSize(context, value)
    def dpo(implicit context: Context): Int  = dp2pxOffset(context, value)
    def sp(implicit context: Context): Float = sp2px(context, value)
    def sps(implicit context: Context): Int  = sp2pxSize(context, value)
    def spo(implicit context: Context): Int  = sp2pxOffset(context, value)
  }

  implicit class ViewImplicit(view: View) {

    def updateLayoutParams[LP <: ViewGroup.LayoutParams](block: LP => Unit): Unit = {
      val lp = view.getLayoutParams.as[LP]; block(lp); view.setLayoutParams(lp)
    }
  }

  def dp2px(context: Context, value: Float): Float =
    TypedValue.applyDimension(
      TypedValue.COMPLEX_UNIT_DIP,
      value,
      context.getResources.getDisplayMetrics
    )

  def dp2pxOffset(context: Context, value: Float): Int = dp2px(context, value).toInt

  def dp2pxSize(context: Context, value: Float): Int = {
    val f        = dp2px(context, value)
    val res: Int = (if (f >= 0) f + 0.5f else f - 0.5f).toInt
    if (res != 0) return res
    if (value == 0f) return 0
    if (value > 0) return 1
    -1
  }

  def sp2px(context: Context, value: Float): Float =
    TypedValue.applyDimension(
      TypedValue.COMPLEX_UNIT_SP,
      value,
      context.getResources.getDisplayMetrics
    )

  def sp2pxOffset(context: Context, value: Float): Int = sp2px(context, value).toInt

  def sp2pxSize(context: Context, value: Float): Int = {
    val f        = sp2px(context, value)
    val res: Int = (if (f >= 0) f + 0.5f else f - 0.5f).toInt
    if (res != 0) return res
    if (value == 0f) return 0
    if (value > 0) return 1
    -1
  }

  def getLocationCenterX(view: View): Int = {
    val arrLocation = Array.ofDim[Int](2)
    view.getLocationInWindow(arrLocation)
    view.getMeasuredWidth / 2 + arrLocation(0)
  }

  def setScreenOnFlag(activity: Activity) {
    setScreenOnFlag(activity.getWindow)
  }

  def setScreenOnFlag(window: Window) {
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
  }

  def setWindowBrightness(activity: Activity, screenBrightness: Float) {
    setWindowBrightness(activity.getWindow, screenBrightness)
  }

  def setWindowBrightness(window: Window, screenBrightness: Float) {
    val layoutParams = window.getAttributes
    layoutParams.screenBrightness = screenBrightness
    window.setAttributes(layoutParams)
  }

  def getStatusBarHeight(context: Context): Int = {
    try {
      context.getResources.getDimensionPixelSize(
        IdGetter.getIdSys(context, R_ID_STATUS_BAR_HEIGHT, IdGetter.dimen)
      )
    } catch {
      case _: Exception =>
        dp2pxSize(context, 24f)
    }
  }

  private val R_ID_STATUS_BAR_HEIGHT = "status_bar_height"
}
