package com.igtools.downloader.api.okhttp;

public interface OnDownloadListener {

    /**
     * 下载成功
     */
    void onDownloadSuccess(String path);

    /**
     * @param progress
     * 下载进度
     */
    void onDownloading(int progress);

    /**
     * 下载失败
     */
    void onDownloadFailed(String message);

}
