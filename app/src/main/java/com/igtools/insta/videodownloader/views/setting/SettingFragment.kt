package com.igtools.insta.videodownloader.views.setting

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.view.View
import android.webkit.CookieManager
import android.webkit.CookieSyncManager
import android.widget.Toast
import com.igtools.insta.videodownloader.BaseApplication
import com.igtools.insta.videodownloader.BuildConfig
import com.igtools.insta.videodownloader.R
import com.igtools.insta.videodownloader.base.BaseFragment
import com.igtools.insta.videodownloader.databinding.FragmentSettingBinding
import com.igtools.insta.videodownloader.utils.FileUtils
import com.igtools.insta.videodownloader.utils.ShareUtils


/**
 * @Author: desong
 * @Date: 2022/7/21
 */
class SettingFragment : BaseFragment<FragmentSettingBinding>() {

    lateinit var alertDialog: AlertDialog

    override fun onResume() {
        super.onResume()

        val cookie = BaseApplication.cookie
        if (cookie == null) {
            mBinding.llLogout.visibility = View.INVISIBLE
        } else {
            mBinding.llLogout.visibility = View.VISIBLE
        }

    }


    @SuppressWarnings("deprecation")
    fun clearCookies(context: Context?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        } else if (context != null) {
            val cookieSyncManager = CookieSyncManager.createInstance(context)
            cookieSyncManager.startSync()
            val cookieManager: CookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookie()
            cookieManager.removeSessionCookie()
            cookieSyncManager.stopSync()
            cookieSyncManager.sync()
        }
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_setting
    }

    override fun initView() {
        mBinding.tvVersion.text = BuildConfig.VERSION_NAME
        mBinding.llShare.setOnClickListener {

            FileUtils.share(requireContext(), "https://play.google.com/store/apps/details?id=com.igtools.insta.videodownloader&hl=en")

        }
        mBinding.llLogout.setOnClickListener {
            ShareUtils.getEdit().remove("cookie").apply()
            BaseApplication.cookie = null
            clearCookies(requireContext())
            mBinding.llLogout.visibility = View.INVISIBLE
            Toast.makeText(requireContext(), getString(R.string.log_out), Toast.LENGTH_SHORT).show()
        }


        val builder = AlertDialog.Builder(context)

        builder.setMessage(getString(R.string.feedbackstr))
        builder.setPositiveButton("OK"
        ) { dialog, which -> dialog?.dismiss() }
        alertDialog = builder.create()

        mBinding.llFeedback.setOnClickListener {
            alertDialog.show()
        }
    }

    override fun initData() {

    }

}