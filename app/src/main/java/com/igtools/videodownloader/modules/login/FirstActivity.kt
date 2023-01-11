package com.igtools.videodownloader.modules.login

import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.igtools.videodownloader.base.BaseActivity
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.igtools.videodownloader.BaseApplication
import com.igtools.videodownloader.BuildConfig
import com.igtools.videodownloader.MainActivity
import com.igtools.videodownloader.R
import com.igtools.videodownloader.databinding.ActivityFirstBinding
import com.igtools.videodownloader.utils.PermissionUtils
import com.igtools.videodownloader.utils.ShareUtils

class FirstActivity : BaseActivity<ActivityFirstBinding>() {
    val TAG = "FirstActivity"

    lateinit var progressDialog: ProgressDialog
    lateinit var myAlert:AlertDialog
    private val PERMISSION_REQ = 1024

    override fun getLayoutId(): Int {
        return R.layout.activity_first
    }

    override fun initView() {

        progressDialog = ProgressDialog(this)
        progressDialog.setCancelable(false)

        mBinding.btnStart.setOnClickListener {

            startNow()

        }
        myAlert = AlertDialog.Builder(this)
            .setMessage(getString(R.string.need_permission))
            .setPositiveButton(
                R.string.settings
            ) { dialog, _ ->
                val intent = Intent();
                intent.action = "android.settings.APPLICATION_DETAILS_SETTINGS";
                intent.data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
                startActivity(intent);
                dialog.dismiss()
            }
            .setNegativeButton(
                R.string.cancel
            ) { dialog, _ -> dialog.dismiss() }
            .create()
    }

    private fun startNow() {

        if (!PermissionUtils.checkPermissionsForReadAndRight(this)) {
            PermissionUtils.requirePermissionsReadAndWrite(this, PERMISSION_REQ)
            return
        }

//        progressDialog.show()
//        val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig
//        val configSettings = remoteConfigSettings {
//            minimumFetchIntervalInSeconds = 3600 * 12
//        }
//        remoteConfig.setConfigSettingsAsync(configSettings)
//
//        remoteConfig.fetchAndActivate()
//            .addOnCompleteListener(this) { task ->
//                if (task.isSuccessful) {
//                    progressDialog.dismiss()
//                    val str = remoteConfig.getString("apikey2")
//                    ShareUtils.putData("apikey", str)
//                    BaseApplication.APIKEY = str
//
//                    ShareUtils.putDataBool("firstLogin", false)
//                    BaseApplication.firstLogin = false
//                    startActivity(Intent(this, MainActivity::class.java))
//                    finish()
//                }
//            }.addOnFailureListener {
//                //Log.e(TAG, it.message + "")
//                sendToFirebase(it)
//                Toast.makeText(this, getString(R.string.login_failed), Toast.LENGTH_SHORT).show()
//                progressDialog.dismiss()
//            }

        ShareUtils.putDataBool("firstLogin", false)
        BaseApplication.firstLogin = false
        startActivity(Intent(this, MainActivity::class.java))
        finish()

    }

    override fun initData() {
        if (!PermissionUtils.checkPermissionsForReadAndRight(this)) {
            PermissionUtils.requirePermissionsReadAndWrite(this, PERMISSION_REQ)
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQ){
            for (grantResult in grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    myAlert.show()
                    return
                }
            }
        }

    }

}
