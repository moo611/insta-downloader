package com.igtools.downloader.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.android.material.tabs.TabLayout
import com.igtools.downloader.R
import com.igtools.downloader.adapter.ViewPagerAdapter
import com.igtools.downloader.databinding.FragmentHomeBinding

/**
 * @Author: desong
 * @Date: 2022/7/21
 */
class HomeFragment : Fragment() {

    lateinit var mAdapter:ViewPagerAdapter
    val TAG="HomeFragment"
    lateinit var binding:FragmentHomeBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)

        initViews()


        return binding.root
    }


    fun initViews(){

        val fragments:ArrayList<Fragment> = ArrayList()
        fragments.add(ShortCodeFragment())
        fragments.add(RecordFragment())
        val titles:ArrayList<String> = ArrayList()
        titles.add("shortcode")
        titles.add("username")
        mAdapter = ViewPagerAdapter(childFragmentManager,titles,fragments)
        binding.viewpager.adapter = mAdapter

        binding.tabLayout.setupWithViewPager(binding.viewpager)


    }

}