package com.igtools.downloader.api.okhttp;

public interface OkhttpFileListener {
    public void onSuccess(String path);
    public void onFail(String message);
}
