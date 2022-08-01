package com.igtools.downloader.widgets.dialog;

import android.app.Dialog;
import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @Author: desong
 * @Date: 2022/8/1
 */
public class ProgressDialog extends Dialog {

    Context c;
    public ProgressDialog(@NonNull Context context) {
        super(context);
        this.c=context;
    }

    public ProgressDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
        this.c=context;
    }

    protected ProgressDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        this.c=context;
    }

    public void setView(View view){

        setContentView(view);
    }



}
