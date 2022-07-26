package com.igtools.downloader.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.igtools.downloader.R
import com.igtools.downloader.databinding.FragmentRecordBinding

/**
 * @Author: desong
 * @Date: 2022/7/21
 */
class RecordFragment : Fragment() {


   lateinit var binding:FragmentRecordBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_record, container, false)

        initViews()

        return binding.root
    }


    private fun initViews(){



    }

}