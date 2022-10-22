package com.igtools.videodownloader

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.fagaia.farm.base.BaseActivity
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.igtools.videodownloader.api.retrofit.MyConfig
import com.igtools.videodownloader.api.retrofit.MyCookie
import com.igtools.videodownloader.service.history.HistoryActivity
import com.igtools.videodownloader.databinding.ActivityMainBinding
import com.igtools.videodownloader.service.home.HomeFragment
import com.igtools.videodownloader.service.setting.SettingFragment
import com.igtools.videodownloader.service.tag.TagFragment
import com.igtools.videodownloader.models.IntentEvent
import com.igtools.videodownloader.utils.RegexUtils
import org.greenrobot.eventbus.EventBus


/**
 * @Author: desong
 * @Date: 2022/7/21
 */
class MainActivity : BaseActivity<ActivityMainBinding>(){
    val TAG = "MainActivityTest"
    
    var imageViews: MutableList<ImageView> = ArrayList()
    var fragments: MutableList<Fragment> = ArrayList()
    var lastPos = 0

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun getLayoutId(): Int {
        return R.layout.activity_main
    }

    override fun initView() {
        //Android 点击 Home 键后再点击 APP 图标，APP 显示退出之前的界面
        if (!isTaskRoot) {
            finish();
            return;
        }

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
        handleText(intent)

        val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)

        remoteConfig.fetchAndActivate()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val str = remoteConfig.getString("cookies")
                    val cookies = gson.fromJson(str,Array<MyCookie>::class.java)
                    MyConfig.cookies = cookies.toList()
                    Log.v(TAG, MyConfig.cookies.toString())
                }

            }

    }

    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.v(TAG, "on new intent")
        //Toast.makeText(this,"new intent",Toast.LENGTH_SHORT).show()
        handleText(intent)

    }

    fun handleText(newintent: Intent?) {
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
            } else {

                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val item = clipboard.primaryClip?.getItemAt(0)
//                Log.v(TAG, "item:$item")
                if (item?.text?.toString() != null && item.text.toString().isNotEmpty()) {
//                    Log.v(TAG, "text:" + item.text.toString())
                    EventBus.getDefault().post(IntentEvent(item.text.toString()))
                }
            }

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