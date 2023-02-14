package com.igtools.instagram.videodownloader.modules.login

import android.annotation.SuppressLint
import android.content.Intent
import com.igtools.instagram.videodownloader.BaseApplication
import com.igtools.instagram.videodownloader.MainActivity
import com.igtools.instagram.videodownloader.R
import com.igtools.instagram.videodownloader.base.BaseActivity
import com.igtools.instagram.videodownloader.databinding.ActivitySplashBinding

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


        if (BaseApplication.firstLogin){
            startActivity(Intent(this, FirstActivity::class.java))
        }else{
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }


}