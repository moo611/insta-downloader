package com.igtools.insta.videodownloader.views

import android.annotation.SuppressLint
import android.content.Intent
import com.igtools.insta.videodownloader.MainActivity
import com.igtools.insta.videodownloader.R
import com.igtools.insta.videodownloader.base.BaseActivity
import com.igtools.insta.videodownloader.databinding.ActivitySplashBinding

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity<ActivitySplashBinding>() {


    override fun getLayoutId(): Int {
        return R.layout.activity_splash
    }

    override fun initView() {

    }

    override fun initData() {
        //Android 点击 Home 键后再点击 APP 图标，APP 显示退出之前的界面
        if (!isTaskRoot) {
            finish();
            return;
        }

        startActivity(Intent(this, MainActivity::class.java))

        finish()
    }


}