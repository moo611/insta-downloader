package com.igtools.igdownloader.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.igtools.igdownloader.R
import com.igtools.igdownloader.adapter.ViewPagerAdapter
import com.igtools.igdownloader.databinding.FragmentHomeBinding

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
        fragments.add(UserNameFragment())
        val titles:ArrayList<String> = ArrayList()
        titles.add(getString(R.string.vp_shortcode))
        titles.add(getString(R.string.vp_username))
        mAdapter = ViewPagerAdapter(childFragmentManager,titles,fragments)
        binding.viewpager.adapter = mAdapter

        binding.tabLayout.setupWithViewPager(binding.viewpager)


    }

}