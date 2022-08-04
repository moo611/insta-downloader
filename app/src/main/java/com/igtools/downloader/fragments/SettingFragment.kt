package com.igtools.downloader.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.igtools.downloader.BaseApplication
import com.igtools.downloader.BuildConfig
import com.igtools.downloader.R
import com.igtools.downloader.databinding.FragmentSettingBinding
import com.igtools.downloader.utils.FileUtils


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

    private fun setListeners() {

        binding.llShare.setOnClickListener {

            FileUtils.share(requireContext(), "app")

        }

        binding.mySwitch.setOnCheckedChangeListener { _, b -> BaseApplication.isAuto = b }

    }

}