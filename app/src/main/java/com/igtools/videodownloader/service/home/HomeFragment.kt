package com.igtools.videodownloader.service.home

import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.igtools.videodownloader.BaseApplication
import com.igtools.videodownloader.base.BaseFragment
import com.igtools.videodownloader.R
import com.igtools.videodownloader.databinding.FragmentHomeBinding
import com.igtools.videodownloader.utils.ShareUtils

/**
 * @Author: desong
 * @Date: 2022/7/21
 */
class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    lateinit var mAdapter: ViewPagerAdapter
    lateinit var linkView: View
    lateinit var userView: View
    val TAG = "HomeFragment"


    override fun getLayoutId(): Int {
        return R.layout.fragment_home
    }

    override fun initView() {
        val fragments: ArrayList<Fragment> = ArrayList()
        fragments.add(ShortCodeFragment())
        fragments.add(UserFragment())

        mAdapter = ViewPagerAdapter(childFragmentManager, fragments)
        mBinding.viewpager.adapter = mAdapter

        mBinding.tabLayout.setupWithViewPager(mBinding.viewpager)

        linkView = LayoutInflater.from(requireContext()).inflate(R.layout.view_link, null)
        userView = LayoutInflater.from(requireContext()).inflate(R.layout.view_user, null)
//        val circle = userView.findViewById<View>(R.id.view_circle)


        mBinding.tabLayout.getTabAt(0)!!.customView = linkView
        mBinding.tabLayout.getTabAt(1)!!.customView = userView


        mBinding.imgCamera.setOnClickListener {

            val launchIntent = requireActivity().packageManager.getLaunchIntentForPackage("com.instagram.android")
            launchIntent?.let { startActivity(it) }
        }



        mBinding.tabLayout.addOnTabSelectedListener(object :TabLayout.OnTabSelectedListener{
            override fun onTabSelected(tab: TabLayout.Tab?) {
//                if (BaseApplication.userUpdate) {
//                    BaseApplication.userUpdate = false
//                    ShareUtils.putDataBool("user-update", false)
//                    circle.visibility = View.INVISIBLE
//                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

        })

    }

    override fun initData() {

    }

}