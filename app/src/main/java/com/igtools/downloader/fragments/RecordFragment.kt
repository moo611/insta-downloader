package com.igtools.downloader.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.igtools.downloader.R

/**
 * @Author: desong
 * @Date: 2022/7/21
 */
class RecordFragment : Fragment() {


   lateinit var mView:View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        mView= inflater.inflate(R.layout.fragment_record, container, false)

        return mView
    }


}