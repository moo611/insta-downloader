package com.igtools.videodownloader

import android.content.Intent
import android.util.Log
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.igtools.videodownloader.base.BaseActivity
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.igtools.videodownloader.service.history.HistoryActivity
import com.igtools.videodownloader.databinding.ActivityMainBinding
import com.igtools.videodownloader.service.home.HomeFragment
import com.igtools.videodownloader.service.setting.SettingFragment
import com.igtools.videodownloader.service.tag.TagFragment
import com.igtools.videodownloader.models.IntentEvent
import com.igtools.videodownloader.utils.RegexUtils
import com.igtools.videodownloader.utils.ShareUtils
import org.greenrobot.eventbus.EventBus


/**
 * @Author: desong
 * @Date: 2022/7/21
 */
class MainActivity : BaseActivity<ActivityMainBinding>() {
    val TAG = "MainActivityTest"

    var imageViews: MutableList<ImageView> = ArrayList()
    var fragments: MutableList<Fragment> = ArrayList()
    var lastPos = 0

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun getLayoutId(): Int {
        return R.layout.activity_main
    }

    override fun initView() {


        firebaseAnalytics = Firebase.analytics

        //val typeface = Typeface.createFromAsset(assets, "fonts/DancingScript-Bold.ttf")
        //mBinding.appTitle.typeface = typeface

        //添加imageviews
        imageViews.add(mBinding.imgHome)
        imageViews.add(mBinding.imgTag)
        imageViews.add(mBinding.imgMine)


        fragments.add(HomeFragment())
        fragments.add(TagFragment())
        fragments.add(SettingFragment())

        showFragment(lastPos)
        selectPage(0)

        mBinding.llHome.setOnClickListener {
            //throw RuntimeException("Test Crash") // Force a crash
            showFragment(0)
            selectPage(0)
            lastPos = 0

        }
        mBinding.llTag.setOnClickListener {

            showFragment(1)
            selectPage(1)
            lastPos = 1

        }
        mBinding.llMine.setOnClickListener {
            showFragment(2)
            selectPage(2)
            lastPos = 2

        }

        mBinding.imgCamera.setOnClickListener {

            val launchIntent = packageManager.getLaunchIntentForPackage("com.instagram.android")
            launchIntent?.let { startActivity(it) }
        }
        mBinding.imgDownload.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    override fun initData() {

        val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600 * 12
        }
        remoteConfig.setConfigSettingsAsync(configSettings)

        remoteConfig.fetchAndActivate()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val str = remoteConfig.getString("apikey")
                    ShareUtils.putData("apikey", str)
                    BaseApplication.APIKEY = str

                }

            }

    }


    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.v(TAG, "on new intent")
        //Toast.makeText(this,"new intent",Toast.LENGTH_SHORT).show()
        handleIntent(intent)

    }

    fun handleIntent(newintent: Intent?) {
        //这里要加延时，否则会获取不到intent或者clipboarditem
        mBinding.imgDownload.post {
            Log.v(TAG, newintent?.action + newintent?.type + "")
            if (newintent?.action == Intent.ACTION_SEND) {
                if ("text/plain" == newintent.type) {
                    newintent.getStringExtra(Intent.EXTRA_TEXT)?.let {
                        // Update UI to reflect text being shared

                        val urls = RegexUtils.extractUrls(it)
                        if (urls.size > 0) {

                            EventBus.getDefault().post(IntentEvent(urls[0]))
                        }

                    }
                }
            }
        }
    }


    /**
     * 切换fragment性能优化,使每个fragment只实例化一次
     */
    private fun showFragment(page: Int) {
        supportFragmentManager.executePendingTransactions()
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