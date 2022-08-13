package com.igtools.igdownloader.api.okhttp;

public interface OkhttpFileListener {
    public void onSuccess(String path);
    public void onFail(String message);
}
