package com.igtools.videodownloader

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.fagaia.farm.base.BaseActivity
import com.igtools.videodownloader.databinding.ActivitySplashBinding
import com.igtools.videodownloader.utils.ShareUtils

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

        if (ShareUtils.getData("isFirst") != null && !ShareUtils.getData("isFirst").toBoolean()) {
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            startActivity(Intent(this, FirstActivity::class.java))
        }
        finish()
    }


}