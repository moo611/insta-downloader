package com.igtools.videodownloader.api.okhttp;

import com.google.gson.JsonObject;

public interface OkhttpListener {

    void onSuccess(JsonObject jsonObject);
    void onFail(String message);
}
