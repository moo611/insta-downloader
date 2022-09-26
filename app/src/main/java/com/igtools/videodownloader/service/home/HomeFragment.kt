package com.igtools.videodownloader.service.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.fagaia.farm.base.BaseFragment
import com.igtools.videodownloader.R
import com.igtools.videodownloader.databinding.FragmentHomeBinding

/**
 * @Author: desong
 * @Date: 2022/7/21
 */
class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    lateinit var mAdapter: ViewPagerAdapter
    lateinit var binding:FragmentHomeBinding
    val TAG="HomeFragment"



    override fun getLayoutId(): Int {
       return R.layout.fragment_home
    }

    override fun initView() {
        val fragments:ArrayList<Fragment> = ArrayList()
        fragments.add(ShortCodeFragment())
        fragments.add(UserFragment())
        val titles:ArrayList<String> = ArrayList()
        titles.add(getString(R.string.vp_shortcode))
        titles.add(getString(R.string.vp_username))
        mAdapter = ViewPagerAdapter(childFragmentManager,titles,fragments)
        binding.viewpager.adapter = mAdapter

        binding.tabLayout.setupWithViewPager(binding.viewpager)
    }

    override fun initData() {

    }

}