/*
 * Copyright (C) 2016-present, Wei Chou (weichou2010@gmail.com)
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

package hobby.wei.c.core;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;

import hobby.chenai.nakam.assoid.R;

/**
 * @author Wei.Chou
 * @version 1.0, 22/03/2016
 */
public abstract class AbsDialogFragment extends DialogFragment implements DialogInterface.OnShowListener {
    private OnDialogListener mOnDialogListener;

    public void show(FragmentManager manager, String tag, boolean allowStateLoss) {
        if (allowStateLoss) {
            manager.beginTransaction().add(this, tag).commitAllowingStateLoss();
        } else {
            super.show(manager, tag);
        }
    }

    /**
     * @deprecated 请替换成 {@link #show(FragmentManager, String, boolean)}
     */
    @Deprecated
    public void show(FragmentManager manager, String tag) {
        throw new RuntimeException();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(cancelable());
        setStyle(STYLE_NO_TITLE, myTheme());
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setCanceledOnTouchOutside(canceledOnTouchOutside());
        dialog.setOnShowListener(this);
        // 不可以设置这两个监听
//        dialog.setOnCancelListener(this);
//        dialog.setOnDismissListener(this);
        return dialog;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // 在这里进行的 mDialog.setContentView(view)
        super.onActivityCreated(savedInstanceState);
        // 所以修改尺寸放在这
        getDialog().getWindow().setLayout(getLayoutWidth(), getLayoutHeight());
    }

    protected int myTheme() {
        return R.style.Theme_Wei_C_Dialog_Alert;
    }

    protected abstract boolean cancelable();

    protected abstract int getLayoutWidth();

    protected abstract int getLayoutHeight();

    /**
     * 仅当{@link #cancelable()} 返回true的时候起作用。
     */
    protected boolean canceledOnTouchOutside() {
        return false;
    }

    public void delayDismiss(final boolean allowingStateLoss, final int timeDelayed) {
        AbsApp.get().mainHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (allowingStateLoss) {
                    dismissAllowingStateLoss();
                } else {
                    dismiss();
                }
            }
        }, timeDelayed);
    }

    @Override
    public void onShow(DialogInterface dialog) {
        if (mOnDialogListener != null) mOnDialogListener.onShow(this);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        if (mOnDialogListener != null) mOnDialogListener.onCancel(this);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mOnDialogListener != null) mOnDialogListener.onDismiss(this);
    }

    public void setOnDialogListener(OnDialogListener listener) {
        mOnDialogListener = listener;
    }

    public interface OnDialogListener {
        void onShow(AbsDialogFragment fragment);

        void onCancel(AbsDialogFragment fragment);

        void onDismiss(AbsDialogFragment fragment);
    }
}
