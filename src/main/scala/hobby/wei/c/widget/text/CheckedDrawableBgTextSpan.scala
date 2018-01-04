/*
 * Copyright (C) 2014-present, Wei Chou(weichou2010@gmail.com)
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

package hobby.wei.c.widget.text

import java.lang.ref.WeakReference
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.{Canvas, Paint, Rect}
import android.graphics.Paint.FontMetricsInt
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.text.style.{ImageSpan, TextAppearanceSpan}
import android.view.View
import android.widget.TextView
import hobby.chenai.nakam.lang.J2S.NonNull

/**
  * 注意事项：
  * 1. 如果drawable有paddings, 需要给`TextView`设置行高，如：`android:lineSpacingExtra="16dp"`；
  * 2. 需要给`TextView`去掉高亮，即：`android:textColorHighlight="#0fff"` 但不能是0，那就把透明度设为0即可。
  *
  * @author Wei Chou(weichou2010@gmail.com)
  * @version 2.0, 14/11/2017, 重构自2014年版本的 BackgroundImageSpan。
  */
object CheckedDrawableBgTextSpan {
  val CHECKED_STATE_SET = Array(android.R.attr.state_checked)
  val NORMAL_STATE_SET = Array(android.R.attr.stateNotNeeded)
}
class CheckedDrawableBgTextSpan(context: Context, drawableId: Int, linkColor: ColorStateList)(implicit textView: TextView)
  extends ImageSpan(context: Context, drawableId: Int) with SpanLinkable {
  private val mViewRef = new WeakReference(textView)
  private val mPadding, mDirty = new Rect()
  private val mPaint = new TextPaint()
  private var mTextStyle: TextAppearanceSpan = _
  private var mWidth: Int = _
  private var mChecked: Boolean = false
  private var mText: String = _

  def this(context: Context, drawableId: Int, linkColor: Int)(implicit textView: TextView) = this(context, drawableId,
    if (linkColor == 0) null else context.getColorStateList(linkColor))

  def this(context: Context, drawableId: Int)(implicit textView: TextView) = this(context, drawableId, 0)

  override protected val mLinkColor = linkColor

  override def onClick(widget: View): Unit = {}

  def toggle(): Boolean = {
    setChecked(!isChecked)
    isChecked
  }

  def setChecked(b: Boolean): Unit = {
    if (b != mChecked) {
      mChecked = b
      // 还是不刷新。
      // getCachedDrawable.setState(if (isChecked) CheckedDrawableBgTextSpan.CHECKED_STATE_SET else CheckedDrawableBgTextSpan.NORMAL_STATE_SET)
      Option(mViewRef.get()).foreach { view =>
        Option(view.getEditableText).foreach { s =>
          val span = CheckedDrawableBgTextSpan.this
          val sta = s.getSpanStart(span)
          val end = s.getSpanEnd(span)
          if (sta >= 0 && end >= 0) s.setSpan(span, sta, end, s.getSpanFlags(span))
        }
        if (mDirty.width() > 0 && mDirty.height() > 0) view.invalidate(mDirty) else view.invalidate()
      }
    }
  }

  def isChecked = mChecked

  def getText: String = mText

  def setStyle(style: TextAppearanceSpan): Unit = mTextStyle = style

  override def updateDrawState(tp: TextPaint): Unit = {
    super.updateDrawState(tp)
    mTextStyle.updateDrawState(tp)
    mPaint.reset()
    // 取回 mPaint.drawableState 这些属性。
    mPaint.set(tp)
  }

  override def updateMeasureState(tp: TextPaint): Unit = {
    super.updateMeasureState(tp)
    mTextStyle.updateMeasureState(tp)
    mPaint.reset()
    mPaint.set(tp)
  }

  /**
    * 这里仅返回宽度，而高度的设置是通过改变 ```DynamicDrawableSpan#getSize(Paint, CharSequence, int, int, FontMetricsInt)
    * fontMetricsInt``` 的相关参数。
    */
  override def getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: FontMetricsInt): Int = {
    val tp = obtainTextPaint(paint)
    val fmp = tp.getFontMetricsInt()
    val paddings = obtainDrawablePadding
    if (fm != null) {
      fm.top = fmp.top - paddings.top
      fm.bottom = fmp.bottom + paddings.bottom
      //            fm.ascent = fmp.ascent
      //            fm.descent = fmp.descent
    }
    mWidth = Math.round(paddings.left + tp.measureText(text, start, end) + paddings.right)
    mWidth
  }

  override def draw(canvas: Canvas, text: CharSequence, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
    mText = text.subSequence(start, end).toString
    val fm = paint.getFontMetrics()
    //        var realTop = top - y
    //        realTop = if(realTop < fm.ascent) Math.round((realTop + fm.ascent) / 2) else realTop
    //        realTop += y
    //        var realBottom = bottom - y
    //        realBottom = if(realBottom > fm.descent) Math.round((realBottom + fm.descent) / 2) else realBottom
    //        realBottom += y

    val paddings = obtainDrawablePadding
    val d = getCachedDrawable
    d.setBounds(Math.round(x), Math.round(y + fm.top) - paddings.top,
      Math.round(x) + mWidth, Math.round(y + fm.bottom) + paddings.bottom)
    d.setState(if (isChecked) CheckedDrawableBgTextSpan.CHECKED_STATE_SET else CheckedDrawableBgTextSpan.NORMAL_STATE_SET)
    d.draw(canvas)

    obtainTextPaint(paint)
    canvas.save()
    canvas.translate(paddings.left, 0)
    canvas.drawText(mText, x, y, mPaint)
    canvas.restore()

    updateDirtyBounds(d.getBounds)
  }

  def updateDirtyBounds(rect: Rect): Unit = {
    mDirty.set(rect)
    if (mDirty.width() > 0 && mDirty.height() > 0) {
      mDirty.left -= 1
      mDirty.top -= 1
      mDirty.right += 1
      mDirty.bottom += 1
    }
  }

  def obtainTextPaint(p: Paint) = {
    if (mTextStyle.isNull) {
      if (p != null) mPaint.set(p)
    } else mTextStyle.updateDrawState(mPaint)
    updateColor(mPaint, CheckedDrawableBgTextSpan.CHECKED_STATE_SET, isChecked, p.getColor)
    mPaint
  }

  def obtainDrawablePadding = {
    if (!getCachedDrawable.getPadding(mPadding)) {
      mPadding.setEmpty(); // 如果不支持padding，重置。
    }
    mPadding
  }

  def getCachedDrawable = {
    val wr = mDrawableRef
    var d: Drawable = null
    if (wr.nonNull) d = wr.get()
    if (d.isNull) {
      d = getDrawable
      Option(mViewRef.get()).foreach(d.setCallback(_))
      mDrawableRef = new WeakReference[Drawable](d)
    }
    d
  }

  private var mDrawableRef: WeakReference[Drawable] = _
}

/*
参数解析：

width:51	----测量"not"单词字符宽度
curText:not, x:237.0, y:39, top:0, bottom:48, height:48
x：绘制起点横坐标，y：基线baseline纵坐标，top：当前行最高字符在y轴相对于baseline的位置，bottom：当前行最低字符在y轴相对于baseline的位置。
ascent:-33.398438, descent:8.7890625, top:-38.021484, bottom:9.755859, height:47.777344, height2:42.1875, lineHeight:42.1875
ascent:-33, descent:9, top:-39, bottom:10, height:49, height2:42, lineHeight:42
这个是恒定值，基本每一行都一样。x, y, top, bottom意义同上，只是上面是实际布局坐标，而这里是坐标参考系。

width:120
curText:Sunday, x:196.0, y:81, top:48, bottom:91, height:43
ascent:-33.398438, descent:8.7890625, top:-38.021484, bottom:9.755859, height:47.777344, height2:42.1875, lineHeight:42.1875
ascent:-33, descent:9, top:-39, bottom:10, height:49, height2:42, lineHeight:42

 *重点* 但是对比这两行数据，会发现一个问题：
	单词not位于第一行，实际坐标y-top=39符合FontMetrics预设top=-39要求, bottom-y=9*不*符合FontMetrics预设bottom=10的要求;
	而单词Sunday位于第二行，实际坐标y-top=33*不*符合FontMetrics预设top=-39要求, bottom-y=10符合FontMetrics预设bottom=10的要求。

这个原因其实很简单：第一行根据预设来设定top位置，根据实际值来设定bottom位置，最后一行根据实际值来设定top位置，根据预设来设定bottom的位置，
其他行根据实际值来设定top/bottom位置，由于Sunday这一行都没有超过ascent位置，因此把top设为ascent。
不过这给我们带来了一些小麻烦：由于第一行top与实际字符最高点位置的不准确性，导致绘制第一行边框上边缘会偏大，上下不对称，
因此作一个小小的改动，取top和ascent的中间位置为绘制起点，虽然某些字符的最高点可能会到达top而超出边框边界，无妨。
 */
