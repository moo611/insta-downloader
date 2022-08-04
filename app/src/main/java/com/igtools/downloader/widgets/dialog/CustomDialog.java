package com.igtools.downloader.widgets.dialog;

import android.app.ActionBar;
import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @Author: desong
 * @Date: 2022/8/1
 */
public class CustomDialog extends Dialog {

    Context c;

    public CustomDialog(@NonNull Context context) {
        super(context);
        this.c = context;
    }

    public CustomDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
        this.c = context;
    }

    protected CustomDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        this.c = context;
    }

    public void setView(View view) {

        setContentView(view);
    }


    @Override
    public void show() {
        super.show();

        Window window = getWindow();
        window.setGravity(Gravity.CENTER);
        WindowManager.LayoutParams params = window.getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setAttributes(params);
    }
}
