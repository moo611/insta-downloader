package com.igtools.downloader.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class FileUtils {

    public static File createDir(Context c, String path) {

        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }


    /**
     * API 29及以下保存图片到相册的方法
     *
     * @param toBitmap 要保存的图片
     */
    public static void saveImage(Context c, Bitmap toBitmap, String fileName) {
        String insertImage = MediaStore.Images.Media.insertImage(c.getContentResolver(), toBitmap, fileName, null);
        // 发送广播，通知刷新图库的显示
        c.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + fileName)));

        if (!TextUtils.isEmpty(insertImage)) {

            Toast.makeText(c, "图片保存成功!" + insertImage, Toast.LENGTH_SHORT).show();
            Log.e("打印保存路径", insertImage + "-");
        }
    }


    public static void saveImage29(Context c, Bitmap toBitmap) {
        //开始一个新的进程执行保存图片的操作

        Uri insertUri = c.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new ContentValues());
        //使用use可以自动关闭流
        try {
            OutputStream outputStream = c.getContentResolver().openOutputStream(insertUri, "rw");
            if (toBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)) {
                Log.e("保存成功", "success");
            } else {
                Log.e("保存失败", "fail");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void saveVideo(Context c, File videoFile) {

        Uri uriSavedVideo;

        ContentResolver resolver = c.getContentResolver();

        ContentValues valuesvideos;
        valuesvideos = new ContentValues();

        if (Build.VERSION.SDK_INT >= 29) {
            valuesvideos.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/" + "Folder");
            valuesvideos.put(MediaStore.Video.Media.TITLE, videoFile.getName());
            valuesvideos.put(MediaStore.Video.Media.DISPLAY_NAME, videoFile.getName());
            valuesvideos.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            valuesvideos.put(
                    MediaStore.Video.Media.DATE_ADDED,
                    System.currentTimeMillis() / 1000);

            Uri collection =
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
            uriSavedVideo = resolver.insert(collection, valuesvideos);
        } else {


            valuesvideos.put(MediaStore.Video.Media.TITLE, videoFile.getName());
            valuesvideos.put(MediaStore.Video.Media.DISPLAY_NAME, videoFile.getName());
            valuesvideos.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            valuesvideos.put(
                    MediaStore.Video.Media.DATE_ADDED,
                    System.currentTimeMillis() / 1000);
            valuesvideos.put(MediaStore.Video.Media.DATA, videoFile.getAbsolutePath());

            uriSavedVideo = c.getContentResolver().insert(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    valuesvideos);
        }

        if (Build.VERSION.SDK_INT >= 29) {
            valuesvideos.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis());
            valuesvideos.put(MediaStore.Video.Media.IS_PENDING, 1);
        }

        ParcelFileDescriptor pfd;
        try {
            pfd = c.getContentResolver().openFileDescriptor(uriSavedVideo, "w");

            FileOutputStream out = new FileOutputStream(pfd.getFileDescriptor());

            FileInputStream in = new FileInputStream(videoFile);

            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }

            out.close();
            in.close();
            pfd.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (Build.VERSION.SDK_INT >= 29) {
            valuesvideos.clear();
            valuesvideos.put(MediaStore.Video.Media.IS_PENDING, 0);
            c.getContentResolver().update(uriSavedVideo, valuesvideos, null, null);
        }

    }


}
