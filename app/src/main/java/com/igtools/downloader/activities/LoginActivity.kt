package com.igtools.downloader.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.google.gson.JsonObject
import com.igtools.downloader.MainActivity
import com.igtools.downloader.R
import com.igtools.downloader.api.OkhttpHelper
import com.igtools.downloader.api.OkhttpListener
import com.igtools.downloader.api.Urls
import com.igtools.downloader.databinding.ActivityLoginBinding
import com.igtools.downloader.utils.ShareUtils

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
        OkhttpHelper.getInstance().postJson(Urls.LOGIN, params, object : OkhttpListener {
            override fun onSuccess(jsonObject: JsonObject) {
                val token = jsonObject["data"]?.asString
                token?.let { ShareUtils.putData("token", it) }

                startActivity(Intent(this@LoginActivity,MainActivity::class.java))
            }

            override fun onFail(message: String?) {
                Toast.makeText(this@LoginActivity,"Login Failed",Toast.LENGTH_SHORT).show()
            }


        })

    }
}