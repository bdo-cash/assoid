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

package hobby.wei.c.widget.text.magnify;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

import hobby.chenai.nakam.assoid.R;
import hobby.wei.c.widget.text.BackgroundImageSpan;
import hobby.wei.c.widget.text.IMotionEventObserver;
import hobby.wei.c.widget.text.SpanUtils;
import hobby.wei.c.widget.text.WordSpan;

public class WordsTextView extends TextView implements IMotionEventObserver {
    protected static final String TAG = "WordsTextView";
    protected static final boolean LOG = true;

    private static final String mRegExp = "\\W{2,}|[^\\w']+|^\\W+|\\W+$";    //分解单词和带有单引号的词如：I'm.注意顺序不能颠倒

    private boolean mInited = false;
    private OnWordMotionListener mOnWordMotionListener;
    /**
     * 用Span不用Word字符串的原因是没法区分一个句子里两个相同的单词是同一个坐标还是两个不同的坐标。
     */
    private WordSpan mPrevWordSpan;
    private BackgroundImageSpan mPrevBgSpan;
    private int mWordBgDrawableId;

    public WordsTextView(Context context) {
        super(context);
        init(context, null);
    }

    public WordsTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public WordsTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (mInited) return;
        mInited = true;

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WordsTextView);
            mWordBgDrawableId = a.getResourceId(R.styleable.WordsTextView_wordBgDrawableId, 0);
            a.recycle();
        }
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        Spannable spannable = makeSpanWords(text);
        if (mPrevBgSpan != null) {    //mPrevWordSpan一定不为空
            removeBgSpan(spannable);
            WordSpan wordSpan = getSpan(spannable, mPrevWordSpan.start, mPrevWordSpan.end, WordSpan.class);
            if (wordSpan != null) {
                setBgSpan(spannable, wordSpan);    //mPrevBgSpan存在，mWordBgDrawableId肯定不为0
                if (wordSpan.word.equals(mPrevWordSpan.word)) {    //单词相同，说明start和end也相同，不然getSpan找不到，且认为坐标也相同
                    //do nothing
                } else {    //不同，但找到了另一个词
                    onWordOut(mPrevWordSpan.word);
                    onWordEnter(wordSpan.word);
                }
            } else {
                onWordOut(mPrevWordSpan.word);
            }
            mPrevWordSpan = wordSpan;
        }
        setTextInner(spannable);
    }

    @Override
    public Spannable getText() {
        return (Spannable) super.getText();
    }

    private void setTextInner(Spannable text) {
        //必须要用BufferType.SPANNABLE，不然getText()返回的是Spanned而不是Spannable
        super.setText(text, BufferType.SPANNABLE);
    }

    /**
     * 分解所有的单词，并用Span包装。
     */
    protected static Spannable makeSpanWords(CharSequence text) {
        Spannable spannable = SpannableString.valueOf(text);

        String content = text.toString();
        String[] words = content.split(mRegExp);
        int searchStart = 0;
        for (String word : words) {
            if (!TextUtils.isEmpty(word)) {
                int[] loc = SpanUtils.getWordLocation(content, word, searchStart);
                if (loc[0] >= 0) {
                    searchStart = SpanUtils.setSpan(spannable, new WordSpan(word, loc[0], loc[1]), loc);
                }
            }
        }
        return spannable;
    }

    /**
     * @return true/false 表示视图是否有改变（如：单词选中状态改变了），以确定是否需要重建绘图缓存。
     */
    @Override
    public boolean onMotionAbove(int motionX, int motionY) {
        boolean change = false;
        WordSpan wordSpan = getWordMotionAbove(motionX, motionY);
        Spannable spannable = getText();

        if (wordSpan != null) {
            if (mPrevWordSpan != null && wordSpan.equals(mPrevWordSpan)) {    //上一个已经处理了
                change = false;
            } else {
                change = true;
            }
        } else {
            if (mPrevWordSpan != null) {
                change = true;
            } else {
                change = false;
            }
        }
        if (change) {
            final WordSpan enterWordSpan = wordSpan, outWordSpan = mPrevWordSpan;

            removeBgSpan(spannable);
            if (enterWordSpan != null && mWordBgDrawableId > 0) {
                setBgSpan(spannable, enterWordSpan);
            }
            setTextInner(spannable);

            if (outWordSpan != null) onWordOut(outWordSpan.word);
            if (enterWordSpan != null) onWordEnter(enterWordSpan.word);

            mPrevWordSpan = enterWordSpan;
        }
        return change;
    }

    private void setBgSpan(Spannable spannable, WordSpan wordSpan) {
        mPrevBgSpan = new BackgroundImageSpan(getContext(), mWordBgDrawableId);
        mPrevBgSpan.setColor(getWordColor(spannable, wordSpan));
        SpanUtils.setSpan(spannable, mPrevBgSpan, wordSpan.start, wordSpan.end);
    }

    protected WordSpan getWordMotionAbove(int motionX, int motionY) {
        motionX -= getTotalPaddingLeft();
        motionY -= getTotalPaddingTop();

        motionX += getScrollX();
        motionY += getScrollY();

        Layout layout = getLayout();
        int line = layout.getLineForVertical(motionY);
        int offset = layout.getOffsetForHorizontal(line, motionX);

        return getSpan(getText(), offset, offset, WordSpan.class);
    }

    private int getWordColor(Spannable spannable, WordSpan wordSpan) {
        ForegroundColorSpan span = getSpan(spannable, wordSpan.start, wordSpan.end, ForegroundColorSpan.class);
        if (span != null)
            return span.getForegroundColor();
        return 0;
    }

    private <S> S getSpan(Spannable spannable, int start, int end, Class<S> spanClazz) {
        S[] spans = spannable.getSpans(start, end, spanClazz);
        if (spans != null && spans.length > 0)
            return spans[0];
        return null;
    }

    @Override
    public boolean onMotionOut() {
        if (mPrevWordSpan != null) {
            Spannable spannable = (Spannable) getText();
            removeBgSpan(spannable);
            setTextInner(spannable);
            onWordOut(mPrevWordSpan.word);
            mPrevWordSpan = null;
            return true;
        }
        return false;
    }

    @Override
    public void onMotionEnd() {
        if (mPrevWordSpan != null) {
            Spannable spannable = (Spannable) getText();
            removeBgSpan(spannable);
            setTextInner(spannable);
            onWordUp(mPrevWordSpan.word);
            mPrevWordSpan = null;
        }
    }

    private void removeBgSpan(Spannable spannable) {
        spannable.removeSpan(mPrevBgSpan);
        mPrevBgSpan = null;
    }

    protected void onWordEnter(String word) {
        if (LOG) Log.i(TAG, "word:" + word + "-------onWordEnter--------");
        if (mOnWordMotionListener != null) mOnWordMotionListener.onWordEnter(this, word);
    }

    protected void onWordOut(String word) {
        if (LOG) Log.i(TAG, "word:" + word + "+++++++onWordOut+++++++++++");
        if (mOnWordMotionListener != null) mOnWordMotionListener.onWordOut(this, word);
    }

    protected void onWordUp(String word) {
        if (LOG) Log.i(TAG, "word:" + word + "*******onWordUp************");
        if (mOnWordMotionListener != null) mOnWordMotionListener.onWordUp(this, word);
    }

    public void setOnWordMotionListener(OnWordMotionListener l) {
        mOnWordMotionListener = l;
    }

    /**
     * 注意只在长按的时候触发，因此没有down事件。
     */
    public interface OnWordMotionListener {
        void onWordEnter(WordsTextView v, String word);

        void onWordOut(WordsTextView v, String word);

        void onWordUp(WordsTextView v, String word);
    }
}
