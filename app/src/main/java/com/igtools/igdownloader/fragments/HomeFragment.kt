package com.igtools.igdownloader.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.android.gms.ads.AdRequest
import com.igtools.igdownloader.R
import com.igtools.igdownloader.adapter.ViewPagerAdapter
import com.igtools.igdownloader.databinding.FragmentHomeBinding

/**
 * @Author: desong
 * @Date: 2022/7/21
 */
class HomeFragment : Fragment() {

    lateinit var mAdapter:ViewPagerAdapter
    lateinit var binding:FragmentHomeBinding
    val TAG="HomeFragment"

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

        val adRequest: AdRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)

        val fragments:ArrayList<Fragment> = ArrayList()
        fragments.add(ShortCodeFragment())
        //fragments.add(UserNameFragment())
        fragments.add(UserFragment())
        val titles:ArrayList<String> = ArrayList()
        titles.add(getString(R.string.vp_shortcode))
        titles.add(getString(R.string.vp_username))
        mAdapter = ViewPagerAdapter(childFragmentManager,titles,fragments)
        binding.viewpager.adapter = mAdapter

        binding.tabLayout.setupWithViewPager(binding.viewpager)


    }

}