package com.igtools.igdownloader

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.igtools.igdownloader.activities.HistoryActivity
import com.igtools.igdownloader.databinding.ActivityMainBinding
import com.igtools.igdownloader.fragments.HomeFragment
import com.igtools.igdownloader.fragments.SettingFragment
import com.igtools.igdownloader.fragments.TagFragment
import com.igtools.igdownloader.models.IntentEvent
import com.igtools.igdownloader.utils.RegexUtils
import com.igtools.igdownloader.utils.ShareUtils
import org.greenrobot.eventbus.EventBus


/**
 * @Author: desong
 * @Date: 2022/7/21
 */
class MainActivity : AppCompatActivity() {
    val TAG="MainActivityTest"
    lateinit var binding: ActivityMainBinding
    var imageViews: MutableList<ImageView> = ArrayList()
    var fragments: MutableList<Fragment> = ArrayList()
    var lastPos = 0

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        //Android 点击 Home 键后再点击 APP 图标，APP 显示退出之前的界面
        if (!isTaskRoot) {
            finish();
            return;
        }

        firebaseAnalytics = Firebase.analytics

        initViews()
        setListeners()
        handleText(intent)

    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.v(TAG,"on new intent")
        //Toast.makeText(this,"new intent",Toast.LENGTH_SHORT).show()
        handleText(intent)

    }

    fun handleText(newintent:Intent?) {
        Log.v(TAG,newintent?.action+newintent?.type+"")
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
            item?.text?.toString()?.let {
                    EventBus.getDefault().post(IntentEvent(it))
            }

        }
    }

    //app 退出时，让 app 在后台运行，类似于 home 键的功能，最小化
//    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
//        when (keyCode) {
//            KeyEvent.KEYCODE_BACK -> {
//                moveTaskToBack(true)
//                return true
//            }
//        }
//        return false
//    }


    fun initViews() {

        //沉浸式状态栏
        if (Build.VERSION.SDK_INT >= 23) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            window.statusBarColor = Color.TRANSPARENT
        }

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        val typeface = Typeface.createFromAsset(assets, "fonts/DancingScript-Bold.ttf")
        binding.appTitle.typeface = typeface

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
            //throw RuntimeException("Test Crash") // Force a crash
            showFragment(0)
            selectPage(0)
            lastPos = 0

        }
        binding.llTag.setOnClickListener {
            val bundle = Bundle()
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "123")
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "hashtag")
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "image")
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM, bundle)

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
            startActivity(Intent(this, HistoryActivity::class.java))
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