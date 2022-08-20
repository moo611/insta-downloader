package com.igtools.igdownloader.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.google.gson.JsonObject
import com.igtools.igdownloader.MainActivity
import com.igtools.igdownloader.R
import com.igtools.igdownloader.api.okhttp.OkhttpHelper
import com.igtools.igdownloader.api.okhttp.OkhttpListener
import com.igtools.igdownloader.api.okhttp.Urls
import com.igtools.igdownloader.databinding.ActivityLoginBinding
import com.igtools.igdownloader.utils.ShareUtils

/**
 * @Author: desong
 * @Date: 2022/7/21
 */

class LoginActivity : AppCompatActivity() {

    lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)

        initViews()
        setListener()

    }

    private fun initViews() {


    }

    private fun setListener() {

        binding.btnLogin.setOnClickListener {

            doLogin()

        }

    }

    private fun doLogin() {
        val params: HashMap<String, Any> = HashMap()
        params["username"] = binding.etUsername.text.toString()
        params["password"] = binding.etPassword.text.toString()
        OkhttpHelper.getInstance().postJson(
            Urls.LOGIN, params, object :
                OkhttpListener {
            override fun onSuccess(jsonObject: JsonObject) {
                val token = jsonObject["data"]?.asString
                token?.let { ShareUtils.putData("token", it) }

                startActivity(Intent(this@LoginActivity,MainActivity::class.java))
            }

            override fun onFail(message: String?) {
                Toast.makeText(this@LoginActivity,getString(R.string.login_failed),Toast.LENGTH_SHORT).show()
            }


        })

    }
}