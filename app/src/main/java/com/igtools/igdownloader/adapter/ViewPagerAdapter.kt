package com.igtools.igdownloader.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

/**
 * @Author: desong
 * @Date: 2022/7/21
 */
class ViewPagerAdapter(fm: FragmentManager?, var titles: ArrayList<String>, var fragments: ArrayList<Fragment>) :
    FragmentPagerAdapter(
        fm!!
    ) {

    override fun getItem(position: Int): Fragment {
        return fragments[position]
    }

    override fun getCount(): Int {
        return fragments.size
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return titles[position]
    }
}