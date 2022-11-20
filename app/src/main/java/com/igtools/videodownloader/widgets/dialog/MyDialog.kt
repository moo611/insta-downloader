package com.igtools.videodownloader.widgets.dialog

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.igtools.videodownloader.utils.ScreenUtils


class MyDialog : Dialog {

    var c: Context? = null
    var view: View? = null

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, themeResId: Int) : super(context, themeResId) {
        init(context)
    }

    constructor(
        context: Context,
        cancelable: Boolean,
        cancelListener: DialogInterface.OnCancelListener?
    ) : super(context, cancelable, cancelListener) {
        init(context)
    }

    private fun init(c: Context) {
        this.c = c
    }

    fun setUpView(v: View) {
        this.view = v
        setContentView(view!!)
    }

    override fun show() {
        super.show()
        val window = window
        window!!.setGravity(Gravity.CENTER)

        //获得window窗口的属性
        val params = window.attributes
        //设置窗口宽度为充满全屏
        params.width = (ScreenUtils.getScreenWidth(c as Activity) * 0.85).toInt() //宽度设置为屏幕的0.5
        params.height = WindowManager.LayoutParams.WRAP_CONTENT

        //将设置好的属性set回去
        window.attributes = params
    }


}