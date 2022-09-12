package com.igtools.igdownloader.fragments

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
import com.igtools.igdownloader.BaseApplication
import com.igtools.igdownloader.BuildConfig
import com.igtools.igdownloader.R
import com.igtools.igdownloader.databinding.FragmentSettingBinding
import com.igtools.igdownloader.utils.FileUtils
import com.igtools.igdownloader.utils.ShareUtils



/**
 * @Author: desong
 * @Date: 2022/7/21
 */
class SettingFragment : Fragment() {

    lateinit var binding: FragmentSettingBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_setting, container, false)

        initViews()
        setListeners()

        return binding.root
    }


    private fun initViews() {

        binding.tvVersion.text = BuildConfig.VERSION_NAME

    }

    override fun onResume() {
        super.onResume()

        val cookie = ShareUtils.getData("cookie")
        if (cookie == null){
            binding.llLogout.visibility = View.INVISIBLE
        }else{
            binding.llLogout.visibility = View.VISIBLE
        }

    }

    private fun setListeners() {

        binding.llShare.setOnClickListener {

            FileUtils.share(requireContext(), "app")

        }
        binding.llLogout.setOnClickListener {
            ShareUtils.getEdit().remove("cookie").apply()
            clearCookies(requireContext())
            binding.llLogout.visibility = View.INVISIBLE
            Toast.makeText(requireContext(),getString(R.string.log_out),Toast.LENGTH_SHORT).show()
        }

        binding.mySwitch.setOnCheckedChangeListener { _, b -> ShareUtils.putData("isAuto",b.toString()) }

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

}