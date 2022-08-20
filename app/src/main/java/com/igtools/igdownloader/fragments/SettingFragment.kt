package com.igtools.igdownloader.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.igtools.igdownloader.BaseApplication
import com.igtools.igdownloader.BuildConfig
import com.igtools.igdownloader.R
import com.igtools.igdownloader.databinding.FragmentSettingBinding
import com.igtools.igdownloader.utils.FileUtils


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