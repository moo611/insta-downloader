package com.igtools.insta.videodownloader

import android.content.ActivityNotFoundException
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.igtools.insta.videodownloader.base.BaseActivity
import com.igtools.insta.videodownloader.databinding.ActivityMainBinding
import com.igtools.insta.videodownloader.models.IntentEvent
import com.igtools.insta.videodownloader.modules.home.NewShortCodeFragment
import com.igtools.insta.videodownloader.modules.repost.RepostFragment
import com.igtools.insta.videodownloader.modules.search.SearchFragment
import com.igtools.insta.videodownloader.modules.setting.SettingFragment
import com.igtools.insta.videodownloader.room.RecordDB
import com.igtools.insta.videodownloader.utils.RegexUtils
import com.igtools.insta.videodownloader.utils.ShareUtils
import com.igtools.insta.videodownloader.widgets.dialog.MyDialog
import kotlinx.coroutines.launch
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
    lateinit var dialog: MyDialog
    private lateinit var firebaseAnalytics: FirebaseAnalytics


    override fun getLayoutId(): Int {
        return R.layout.activity_main
    }

    override fun initView() {

        firebaseAnalytics = Firebase.analytics

        //val typeface = Typeface.createFromAsset(assets, "fonts/DancingScript-Bold.ttf")
        //mBinding.appTitle.typeface = typeface
        initDialog()
        //添加imageviews
        imageViews.add(mBinding.imgHome)
        //imageViews.add(mBinding.imgSearch)
        imageViews.add(mBinding.imgRepost)
        imageViews.add(mBinding.imgMine)


        fragments.add(NewShortCodeFragment())
        //fragments.add(SearchFragment())
        fragments.add(RepostFragment())
        fragments.add(SettingFragment())

        showFragment(lastPos)
        selectPage(0)

        mBinding.llHome.setOnClickListener {
            //throw RuntimeException("Test Crash") // Force a crash
            showFragment(0)
            selectPage(0)
            lastPos = 0

        }
        mBinding.llRepost.setOnClickListener {

            showFragment(1)
            selectPage(1)
            lastPos = 1

        }
        mBinding.llMine.setOnClickListener {
            showFragment(2)
            selectPage(2)
            lastPos = 2
        }
//        mBinding.llMine.setOnClickListener {
//            showFragment(3)
//            selectPage(3)
//            lastPos = 3
//
//        }

    }

    fun initDialog() {

        dialog = MyDialog(this)
        val v = LayoutInflater.from(this).inflate(R.layout.view_rating, null)

        val rating = v.findViewById<RatingBar>(R.id.rating_bar)
        rating.setOnRatingBarChangeListener { ratingBar, rating, fromUser ->
            //go google play

        }
        val ok = v.findViewById<TextView>(R.id.tv_ok)
        ok.setOnClickListener {
            try {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=$packageName")
                    )
                )
            } catch (e: ActivityNotFoundException) {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                    )
                )
            }
            dialog.dismiss()
        }
        val cancel = v.findViewById<TextView>(R.id.tv_cancel)
        cancel.setOnClickListener {
            dialog.dismiss()
        }
        dialog.setUpView(v)
    }

    override fun initData() {
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        handleIntent(intent)

    }

    fun handleIntent(newintent: Intent?) {

        //这里要加延时，否则会获取不到intent或者clipboarditem
        mBinding.content.post {

            if (newintent?.action == Intent.ACTION_SEND) {
                Log.v(TAG, "handle intent")
                if ("text/plain" == newintent.type) {
                    newintent.getStringExtra(Intent.EXTRA_TEXT)?.let {
                        // Update UI to reflect text being shared

                        val urls = RegexUtils.extractUrls(it)
                        if (urls.size > 0) {

                            lifecycleScope.launch {
                                val record = RecordDB.getInstance().recordDao().findByUrl(urls[0])
                                if (record != null) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        getString(R.string.exist),
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                    return@launch
                                }
                                EventBus.getDefault().post(IntentEvent(urls[0]))
                            }

                        }
                    }
                }
            } else {
                val clipboard: ClipboardManager =
                    this.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.primaryClip?.getItemAt(0)?.let {
                    //fix null pointer
                    lifecycleScope.launch {
                        val record =
                            RecordDB.getInstance().recordDao().findByUrl(it.text.toString())
                        if (record != null) {
                            return@launch
                        }
                        EventBus.getDefault().post(IntentEvent(it.text.toString()))
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


    override fun onBackPressed() {
        super.onBackPressed()
        if (BaseApplication.showRating) {
            dialog.show()
            BaseApplication.showRating = false
            ShareUtils.putDataBool("showrating", false)
        } else {
            finish()
        }

    }
}