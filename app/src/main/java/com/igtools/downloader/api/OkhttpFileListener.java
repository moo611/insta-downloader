package com.igtools.downloader.api;

public interface OkhttpFileListener {
    public void onSuccess(String path);
    public void onFail(String message);
}
