package com.igtools.videodownloader.service.home

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

/**
 * @Author: desong
 * @Date: 2022/7/21
 */
class ViewPagerAdapter(fm: FragmentManager?, var fragments: ArrayList<Fragment>) :
    FragmentPagerAdapter(
        fm!!
    ) {

    override fun getItem(position: Int): Fragment {
        return fragments[position]
    }

    override fun getCount(): Int {
        return fragments.size
    }


}