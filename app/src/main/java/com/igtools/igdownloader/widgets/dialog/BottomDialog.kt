package com.igtools.igdownloader.widgets.dialog

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager

class BottomDialog : Dialog {
    var c: Context? = null
    var view: View? = null

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, themeResId: Int) : super(context, themeResId) {
        init(context)
    }

    protected constructor(
        context: Context,
        cancelable: Boolean,
        cancelListener: DialogInterface.OnCancelListener?
    ) : super(context, cancelable, cancelListener) {
        init(context)
    }

    private fun init(c: Context) {
        this.c = c
    }

    fun setContent(layoutId: Int) {
        view = LayoutInflater.from(c).inflate(layoutId, null)
        setContentView(view!!)
    }

    fun setContent(v: View) {
        view = v
        setContentView(view!!)
    }

    override fun show() {
        super.show()
        val window = window
        window!!.setGravity(Gravity.BOTTOM)

        //获得window窗口的属性
        val params = window.attributes
        //设置窗口宽度为充满全屏
        params.width = WindowManager.LayoutParams.MATCH_PARENT //如果不设置,可能部分机型出现左右有空隙,也就是产生margin的感觉
        //设置窗口高度为包裹内容
        params.height = WindowManager.LayoutParams.WRAP_CONTENT

        //将设置好的属性set回去
        window.attributes = params
    }



}