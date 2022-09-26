package com.fagaia.farm.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.GsonBuilder


/**
 * @Author:  desong
 * @Date:  2022/9/8
 */


abstract class BaseFragment<T : ViewDataBinding> : Fragment() {

    lateinit var mBinding: T
    val gson = Gson()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = DataBindingUtil.inflate(inflater, getLayoutId(), container, false)
        initGaoDe(savedInstanceState)
        initView()
        initData()

        return mBinding.root
    }

    open fun initGaoDe(savedInstanceState: Bundle?){

    }

    protected abstract fun getLayoutId(): Int


    protected abstract fun initView()

    protected abstract fun initData()

}
