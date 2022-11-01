package com.igtools.videodownloader

import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.fagaia.farm.base.BaseActivity
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.igtools.videodownloader.api.retrofit.MyConfig
import com.igtools.videodownloader.api.retrofit.MyCookie
import com.igtools.videodownloader.databinding.ActivityFirstBinding
import com.igtools.videodownloader.utils.PermissionUtils
import com.igtools.videodownloader.utils.ShareUtils

class FirstActivity : BaseActivity<ActivityFirstBinding>() {
    private val PERMISSION_REQ = 1024
    lateinit var progressDialog: ProgressDialog
    override fun getLayoutId(): Int {
        return R.layout.activity_first
    }

    override fun initView() {

        progressDialog = ProgressDialog(this)
        progressDialog.setCancelable(false)

        mBinding.btnStart.setOnClickListener {

            if (!PermissionUtils.checkPermissionsForReadAndRight(this)) {
                PermissionUtils.requirePermissionsReadAndWrite(this, PERMISSION_REQ)
                return@setOnClickListener
            }

            progressDialog.show()
            val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig
            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = 3600 * 12
            }
            remoteConfig.setConfigSettingsAsync(configSettings)

            remoteConfig.fetchAndActivate()
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        progressDialog.dismiss()
                        val str = remoteConfig.getString("cookies")
                        ShareUtils.putData("configs", str)
                        val cookies = gson.fromJson(str, Array<MyCookie>::class.java)
                        MyConfig.cookies = cookies.toList()
                        //Log.v(TAG, MyConfig.cookies.toString())
                        val b = false
                        ShareUtils.putData("isFirst",b.toString())
                        startActivity(Intent(this, MainActivity::class.java))
                    }
                }

        }
    }

    override fun initData() {


    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQ) {
            for (grant in grantResults) {
                if (grant != PackageManager.PERMISSION_GRANTED) {
                    break
                }
            }
            Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
        }
    }
}