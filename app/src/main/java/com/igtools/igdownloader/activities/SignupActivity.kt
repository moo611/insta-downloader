package com.igtools.igdownloader.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.google.gson.JsonObject
import com.igtools.igdownloader.MainActivity
import com.igtools.igdownloader.R
import com.igtools.igdownloader.api.okhttp.OkhttpHelper
import com.igtools.igdownloader.api.okhttp.OkhttpListener
import com.igtools.igdownloader.api.okhttp.Urls
import com.igtools.igdownloader.databinding.ActivitySignupBinding

class SignupActivity : AppCompatActivity() {

    lateinit var binding: ActivitySignupBinding
    var flag = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_signup)
        initViews()
        setListeners()

    }

    private fun initViews() {
        changeView()
    }

    private fun setListeners() {

        binding.btnNext.setOnClickListener {
            flag = 2
            changeView()
            sendCode()
        }

        binding.btnSignUp.setOnClickListener {

            signUp()

        }

        binding.imgBack.setOnClickListener {
            if (flag == 1) {
                finish()
            } else {
                flag = 1
                changeView()
            }
        }

    }

    private fun changeView() {
        if (flag == 1) {
            binding.llView1.visibility = View.VISIBLE
            binding.llView2.visibility = View.GONE
        } else {
            binding.llView2.visibility = View.VISIBLE
            binding.llView1.visibility = View.GONE
        }
    }

    private fun sendCode() {
        val map: HashMap<String,Any> = HashMap()
        map["username"] = binding.etUsername
        OkhttpHelper.getInstance().postJson(
            Urls.SEND_CODE,map,object :
                OkhttpListener {
            override fun onSuccess(jsonObject: JsonObject?) {



            }

            override fun onFail(message: String?) {

            }

        })

    }


    private fun signUp() {

        val map: HashMap<String, Any> = HashMap()
        map["username"] = binding.etUsername
        map["password"] = binding.etPassword

        OkhttpHelper.getInstance().postJson(
            Urls.SIGN_UP, map, object :
                OkhttpListener {
            override fun onSuccess(jsonObject: JsonObject?) {

                OkhttpHelper.getInstance().postJson(
                    Urls.LOGIN, map, object :
                        OkhttpListener {
                    override fun onSuccess(jsonObject: JsonObject?) {

                        startActivity(Intent(this@SignupActivity,MainActivity::class.java))
                    }

                    override fun onFail(message: String?) {
                        Toast.makeText(this@SignupActivity,getString(R.string.login_failed),Toast.LENGTH_SHORT).show()
                    }

                })

            }

            override fun onFail(message: String?) {
                Toast.makeText(this@SignupActivity,getString(R.string.signup_failed),Toast.LENGTH_SHORT).show()
            }


        })

    }

}