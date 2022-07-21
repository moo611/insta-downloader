package com.igtools.downloader.api;

import com.google.gson.JsonObject;

public interface OkhttpListener {

    void onSuccess(JsonObject jsonObject);
    void onFail(String message);
}
