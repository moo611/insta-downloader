package com.igtools.insta.videodownloader.utils

import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase

object Analytics {

    val ERROR_KEY = "error"
    val EVENT_KEY = "event"
    fun sendException(name: String, key: String, value: String) {

        val analytics = Firebase.analytics
        analytics.logEvent(name) {
            param(key, value)
        }

    }

    fun sendEvent(name:String, key:String, value:String){
        val analytics = Firebase.analytics
        analytics.logEvent(name) {
            param(key, value)
        }
    }

}