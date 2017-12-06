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

package hobby.wei.c.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.Scroller;

import hobby.chenai.nakam.assoid.R;

/**
 * @author 周伟 Wei Chou(weichou2010@gmail.com)
 */
public class OverturnLoadingView extends ImageView {
    private boolean mAutoStart = true;
    private boolean mStarted = false;
    private boolean mRotationX = false;
    private boolean mLevelTurned = false;

    private int mMinLevel = -1;
    private int mMaxLevel = -1;
    private int mCurrLevel = -1;

    private Scroller mScroller;
    private int mCyclicality = 1000;
    private int mRotationDegreesX;
    private int mRotationDegreesY;

    public OverturnLoadingView(Context context) {
        super(context);
        setInterpolator(null);
        init();
    }

    public OverturnLoadingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public OverturnLoadingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.OverturnLoadingView, defStyleAttr, 0);
        mMinLevel = a.getInt(R.styleable.OverturnLoadingView_overturnMinLevel, mMinLevel);
        mMaxLevel = a.getInt(R.styleable.OverturnLoadingView_overturnMaxLevel, mMaxLevel);
        mCyclicality = a.getInt(R.styleable.OverturnLoadingView_overturnCyclicality, mCyclicality);
        mAutoStart = a.getBoolean(R.styleable.OverturnLoadingView_overturnAutoStart, mAutoStart);

        setInterpolator(a.getResourceId(R.styleable.OverturnLoadingView_overturnInterpolator, 0));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            float cameraDistance = a.getDimension(R.styleable.OverturnLoadingView_overturnCameraDistance, Float.MIN_VALUE);
            if (cameraDistance != Float.MIN_VALUE) setCameraDistance(cameraDistance);
        }
        a.recycle();
        init();
    }

    private void init() {
    }

    public void setInterpolator(int interpolatorId) {
        Interpolator interpolator = null;
        if (interpolatorId > 0) try {
            interpolator = AnimationUtils.loadInterpolator(getContext(), interpolatorId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        setInterpolator(interpolator);
    }

    public void setInterpolator(Interpolator interpolator) {
        mScroller = new Scroller(getContext(), interpolator);
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        if (!(drawable instanceof LevelListDrawable)) throw new IllegalArgumentException("drawable必须是LevelListDrawable");
        super.setImageDrawable(drawable);
    }

    public void start() {
        if (mMinLevel == -1) throw new IllegalArgumentException("没有设置mMinLevel");
        if (mMaxLevel == -1) throw new IllegalArgumentException("没有设置mMaxLevel");
        if (!mStarted) {
            turnLevel();
            overturnOritation();
            mStarted = true;
        }
    }

    public void stop() {
        mScroller.abortAnimation();
        mStarted = false;
        mAutoStart = false;
    }

    public boolean isTurning() {
        return mStarted;
    }

    public void setAutoStart() {
        mAutoStart = true;
    }

    private void overturnOritation() {
        mRotationX = !mRotationX;
        adjustRotation();
        mScroller.startScroll(mRotationX ? mRotationDegreesX : mRotationDegreesY, 0, mRotationX ? -180 : 180, 0, mCyclicality);
        mLevelTurned = false;
        invalidate();
    }

    private void turnLevel() {
        mCurrLevel++;
        if (mCurrLevel > mMaxLevel || mCurrLevel < mMinLevel) {
            mCurrLevel = mMinLevel;
        }
        setImageLevel(mCurrLevel);
        mLevelTurned = true;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void doRotation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (mRotationX) {
                setRotationX(mRotationDegreesX);
            } else {
                setRotationY(mRotationDegreesY);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void adjustRotation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (mRotationDegreesX > -270 && mRotationDegreesX < -90) {
                mRotationDegreesX = -180;
            } else if (mRotationDegreesX > 90 && mRotationDegreesX < 270) {
                mRotationDegreesX = 180;
            } else {
                mRotationDegreesX = 0;
            }
            if (mRotationDegreesY > -270 && mRotationDegreesY < -90) {
                mRotationDegreesY = -180;
            } else if (mRotationDegreesY > 90 && mRotationDegreesY < 270) {
                mRotationDegreesY = 180;
            } else {
                mRotationDegreesY = 0;
            }
            setRotationX(mRotationDegreesX);
            setRotationY(mRotationDegreesY);
        }
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset()) {
            if (mRotationX) {
                mRotationDegreesX = mScroller.getCurrX();
                if (!mLevelTurned && (mRotationDegreesX <= -90 && mRotationDegreesX > -180
                        || mRotationDegreesX <= -270 && mRotationDegreesX > -360)) turnLevel();
            } else {
                mRotationDegreesY = mScroller.getCurrX();
                if (!mLevelTurned && (mRotationDegreesY >= 90 && mRotationDegreesY < 180
                        || mRotationDegreesY >= 270 && mRotationDegreesY < 360)) turnLevel();
            }
            doRotation();
            if (mScroller.isFinished() && mStarted) {
                overturnOritation();
            }
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!mStarted && mAutoStart) start();
        super.onDraw(canvas);
    }
}
