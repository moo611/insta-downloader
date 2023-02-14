package com.igtools.instagram.videodownloader.base

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.google.gson.Gson


/**
 * @Author:  desong
 * @Date:  2022/9/8
 */
abstract class BaseActivity<T : ViewDataBinding> : AppCompatActivity() {

    lateinit var mBinding: T
    val gson = Gson()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //沉浸式状态栏
        if (Build.VERSION.SDK_INT >= 23) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            window.statusBarColor = Color.TRANSPARENT
        }

        mBinding = DataBindingUtil.setContentView(this, getLayoutId())

        initView()
        initData()

    }

    /**
     * 获取资源ID
     *
     * @return 布局资源ID
     */
    protected abstract fun getLayoutId(): Int


    protected abstract fun initView()


    protected abstract fun initData()
}
