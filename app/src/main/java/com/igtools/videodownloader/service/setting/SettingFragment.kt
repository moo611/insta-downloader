package com.igtools.videodownloader.service.setting

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.CookieSyncManager

import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.fagaia.farm.base.BaseFragment
import com.igtools.videodownloader.BuildConfig
import com.igtools.videodownloader.R
import com.igtools.videodownloader.databinding.FragmentSettingBinding
import com.igtools.videodownloader.utils.FileUtils
import com.igtools.videodownloader.utils.ShareUtils



/**
 * @Author: desong
 * @Date: 2022/7/21
 */
class SettingFragment : BaseFragment<FragmentSettingBinding>() {

    
    override fun onResume() {
        super.onResume()

        val cookie = ShareUtils.getData("cookie")
        if (cookie == null){
            mBinding.llLogout.visibility = View.INVISIBLE
        }else{
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

            FileUtils.share(requireContext(), "app")

        }
        mBinding.llLogout.setOnClickListener {
            ShareUtils.getEdit().remove("cookie").apply()
            clearCookies(requireContext())
            mBinding.llLogout.visibility = View.INVISIBLE
            Toast.makeText(requireContext(),getString(R.string.log_out),Toast.LENGTH_SHORT).show()
        }

        mBinding.mySwitch.setOnCheckedChangeListener { _, b -> ShareUtils.putData("isAuto",b.toString()) }

    }

    override fun initData() {

    }

}