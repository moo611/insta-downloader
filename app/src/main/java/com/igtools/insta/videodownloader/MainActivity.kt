package com.igtools.insta.videodownloader

import android.content.ActivityNotFoundException
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
import com.igtools.insta.videodownloader.base.BaseActivity
import com.igtools.insta.videodownloader.databinding.ActivityMainBinding
import com.igtools.insta.videodownloader.download.DownloadService
import com.igtools.insta.videodownloader.models.IntentEvent
import com.igtools.insta.videodownloader.views.home.LinkFragment
import com.igtools.insta.videodownloader.views.record.RecordFragment
import com.igtools.insta.videodownloader.views.user.UserFragment
import com.igtools.insta.videodownloader.views.setting.SettingFragment
import com.igtools.insta.videodownloader.db.RecordDB
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



    override fun getLayoutId(): Int {
        return R.layout.activity_main
    }

    override fun initView() {

        //val typeface = Typeface.createFromAsset(assets, "fonts/DancingScript-Bold.ttf")
        //mBinding.appTitle.typeface = typeface
        initDialog()
        //添加imageviews
        imageViews.add(mBinding.imgHome)
        imageViews.add(mBinding.imgSearch)
        imageViews.add(mBinding.imgRepost)
        imageViews.add(mBinding.imgMine)


        fragments.add(LinkFragment())
        fragments.add(UserFragment())
        fragments.add(RecordFragment())
        fragments.add(SettingFragment())

        showFragment(lastPos)
        selectPage(0)

        mBinding.llHome.setOnClickListener {
            //throw RuntimeException("Test Crash") // Force a crash
            showFragment(0)
            selectPage(0)
            lastPos = 0

        }
        mBinding.llSearch.setOnClickListener {

            showFragment(1)
            selectPage(1)
            lastPos = 1

        }
        mBinding.llRepost.setOnClickListener {
            showFragment(2)
            selectPage(2)
            lastPos = 2
        }
        mBinding.llMine.setOnClickListener {
            showFragment(3)
            selectPage(3)
            lastPos = 3

        }

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

    private fun handleIntent(newIntent: Intent?) {

        if (DownloadService.isDownloading){
            Toast.makeText(this@MainActivity,getString(R.string.only_one),Toast.LENGTH_SHORT).show()
            return
        }

        //这里要加延时，否则会获取不到intent或者clipboard
        mBinding.content.post {

            if (newIntent?.action == Intent.ACTION_SEND) {
                Log.v(TAG, "handle intent")
                if ("text/plain" == newIntent.type) {
                    newIntent.getStringExtra(Intent.EXTRA_TEXT)?.let {
                        // Update UI to reflect text being shared

                        val urls = RegexUtils.extractUrls(it)
                        if (urls.isNotEmpty()) {

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

        if (BaseApplication.showRating) {
            dialog.show()
            BaseApplication.showRating = false
            ShareUtils.putDataBool("showRating", false)
        } else {
            super.onBackPressed()
        }

    }
}