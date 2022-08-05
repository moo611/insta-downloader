package com.igtools.downloader

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.igtools.downloader.activities.DownloadActivity
import com.igtools.downloader.databinding.ActivityMainBinding
import com.igtools.downloader.fragments.HomeFragment
import com.igtools.downloader.fragments.SettingFragment
import com.igtools.downloader.fragments.TagFragment


/**
 * @Author: desong
 * @Date: 2022/7/21
 */
class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    var imageViews: MutableList<ImageView> = ArrayList()
    var fragments: MutableList<Fragment> = ArrayList()
    var lastPos = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        //沉浸式状态栏
        if (Build.VERSION.SDK_INT >= 23) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            window.statusBarColor = Color.TRANSPARENT
        }

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        initViews()
        setListeners()


    }

    fun initViews() {
        //添加textviews

        //添加imageviews
        imageViews.add(binding.imgHome)
        imageViews.add(binding.imgTag)
        imageViews.add(binding.imgMine)


        fragments.add(HomeFragment())
        fragments.add(TagFragment())
        fragments.add(SettingFragment())

        showFragment(lastPos)
        selectPage(0)
    }

    private fun setListeners() {

        binding.llHome.setOnClickListener {
            showFragment(0)
            selectPage(0)
            lastPos = 0

        }
        binding.llTag.setOnClickListener {
            showFragment(1)
            selectPage(1)
            lastPos = 1

        }
        binding.llMine.setOnClickListener {
            showFragment(2)
            selectPage(2)
            lastPos = 2

        }

        binding.imgCamera.setOnClickListener {

            val launchIntent = packageManager.getLaunchIntentForPackage("com.instagram.android")
            launchIntent?.let { startActivity(it) }
        }
        binding.imgDownload.setOnClickListener {
            startActivity(Intent(this,DownloadActivity::class.java))
        }
    }


    /**
     * 切换fragment性能优化,使每个fragment只实例化一次
     */
    private fun showFragment(page: Int) {
        val ft: FragmentTransaction = supportFragmentManager.beginTransaction()

        // 想要显示一个fragment,先隐藏所有fragment，防止重叠
        hideFragments(ft)
        if (fragments[page].isAdded) {
            ft.show(fragments[page])
        } else {
            ft.add(R.id.content, fragments[page])
        }
        ft.commitAllowingStateLoss()
    }

    // 当fragment已被实例化，相当于发生过切换，就隐藏起来
    private fun hideFragments(ft: FragmentTransaction) {
        for (fm in fragments) {
            if (fm.isAdded) {
                ft.hide(fm)
            }
        }
    }

    //点击底部导航栏的图标文字变化
    private fun selectPage(pos: Int) {

        for (i in imageViews.indices) {
            if (i == pos) {
                imageViews[i].setColorFilter(
                    ContextCompat.getColor(this, R.color.app_color)
                );
            } else {
                imageViews[i].colorFilter = null
            }

        }


    }
}