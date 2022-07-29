package com.igtools.downloader.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.igtools.downloader.R
import com.igtools.downloader.adapter.MultiTypeAdapter
import com.igtools.downloader.databinding.FragmentShortCodeBinding
import com.igtools.downloader.models.MediaModel
import com.youth.banner.indicator.CircleIndicator

/**
 * @Author: desong
 * @Date: 2022/7/21
 */

class ShortCodeFragment : Fragment() {

    lateinit var binding: FragmentShortCodeBinding
    var selectedIndex = 0
    var medias: ArrayList<MediaModel> = ArrayList()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_short_code, container, false)

        initViews()
        setListeners()
        return binding.root;
    }

    private fun initViews() {

        binding.tvDownload.isSelected = false
        binding.tvPaste.isSelected = true
        binding.tvDownload.setTextColor(requireContext().resources!!.getColor(R.color.black))
        binding.tvPaste.setTextColor(requireContext().resources!!.getColor(R.color.white))

        medias.add(MediaModel())
        medias.add(MediaModel())

        val adapter = MultiTypeAdapter(context, medias)
        binding.banner.addBannerLifecycleObserver(this).setIndicator(CircleIndicator(context))
            .setAdapter(adapter)


    }


    private fun setListeners() {

        binding.tvDownload.setOnClickListener {

            selectedIndex = 1
            setButtonColor()
        }
        binding.tvPaste.setOnClickListener {
            selectedIndex = 0
            setButtonColor()
        }

    }

    private fun setButtonColor() {

        if (selectedIndex == 0) {
            binding.tvDownload.isSelected = false
            binding.tvPaste.isSelected = true
            binding.tvDownload.setTextColor(requireContext().resources!!.getColor(R.color.black))
            binding.tvPaste.setTextColor(requireContext().resources!!.getColor(R.color.white))
        } else {
            binding.tvDownload.isSelected = true
            binding.tvPaste.isSelected = false
            binding.tvDownload.setTextColor(requireContext().resources!!.getColor(R.color.white))
            binding.tvPaste.setTextColor(requireContext().resources!!.getColor(R.color.black))
        }
    }


}