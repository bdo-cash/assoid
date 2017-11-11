/*
 * Copyright (C) 2014-present, Wei Chou (weichou2010@gmail.com)
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

package hobby.wei.c.widget.text;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;

import java.lang.ref.WeakReference;

public class BackgroundImageSpan extends ImageSpan {
    private final Rect mPadding = new Rect();
    private int mWidth;
    private int mColor;
    private Paint mPaint;

    public BackgroundImageSpan(Context context, int drawableId) {
        super(context, drawableId);
    }

    public void setColor(int color) {
        mColor = color;
    }

    /**
     * 这里仅返回宽度，而高度的设置是通过改变 {@link DynamicDrawableSpan#getSize(Paint, CharSequence, int, int, FontMetricsInt)
     * fontMetricsInt} 的相关参数。
     */
    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, FontMetricsInt fm) {
        mWidth = Math.round(paint.measureText(text, start, end));
        return mWidth;
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        FontMetrics fm = paint.getFontMetrics();
        int realTop = top - y;
        realTop = realTop < fm.ascent ? (int) ((realTop + fm.ascent) / 2) : realTop;
        realTop += y;
        int realBottom = bottom - y;
        realBottom = realBottom > fm.descent ? (int) ((realBottom + fm.descent) / 2) : realBottom;
        realBottom += y;

        Drawable d = getCachedDrawable();
        if (!d.getPadding(mPadding)) {
            //如果不支持padding，重置
            mPadding.setEmpty();
        }
        d.setBounds((int) x - mPadding.left, realTop - mPadding.top, (int) x + mWidth + mPadding.right, realBottom + mPadding.bottom);
        d.draw(canvas);

        if (mPaint == null) mPaint = new Paint();
        mPaint.set(paint);
        if (mColor != 0) mPaint.setColor(mColor);
        canvas.drawText(text.subSequence(start, end).toString(), x, y, mPaint);
        mPaint.reset();
    }

    private Drawable getCachedDrawable() {
        WeakReference<Drawable> wr = mDrawableRef;
        Drawable d = null;
        if (wr != null) d = wr.get();
        if (d == null) {
            d = getDrawable();
            mDrawableRef = new WeakReference<Drawable>(d);
        }
        return d;
    }

    private WeakReference<Drawable> mDrawableRef;
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
