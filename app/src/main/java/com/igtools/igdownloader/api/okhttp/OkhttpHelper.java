package com.igtools.igdownloader.api.okhttp;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OkhttpHelper {

    private static OkhttpHelper instance;
    private OkHttpClient client;
    private Gson gson = new Gson();
    private Handler handler = new Handler(Looper.getMainLooper());
    private JsonParser jsonParser = new JsonParser();

    public OkhttpHelper() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(60, TimeUnit.SECONDS);
        builder.writeTimeout(60, TimeUnit.SECONDS);
        builder.readTimeout(60, TimeUnit.SECONDS);

        client = builder.build();


    }

    public static OkhttpHelper getInstance() {
        if (instance == null) {
            instance = new OkhttpHelper();
        }
        return instance;
    }

    public void getJson(String url, OkhttpListener okhttpListener) {
        Request request = new Request.Builder().url(url).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                handler.post(() -> okhttpListener.onFail(e.getMessage()));
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                handleResponse(response, okhttpListener);
            }
        });

    }

    public void getJsonWithHeader(String url, OkhttpListener okhttpListener) {
        Request request = new Request.Builder().addHeader("authorization", "Bearer ${Account.accessTokenStr}").url(url).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                handler.post(() -> okhttpListener.onFail(e.getMessage()));
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                handleResponse(response, okhttpListener);
            }
        });
    }

    public void postJson(String url, HashMap<String, Object> params, OkhttpListener okhttpListener) {

        String json = gson.toJson(params);

        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json);
        Request request = new Request.Builder().url(url).post(body).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                handler.post(() -> okhttpListener.onFail(e.getMessage()));
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                handleResponse(response, okhttpListener);
            }
        });

    }

    public void postJsonWithHeader(String url, String method, String token, HashMap<String, Object> params, OkhttpListener okhttpListener) {

        String json = gson.toJson(params);

        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json);
        Request request = new Request.Builder().url(url).post(body).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                handler.post(() -> okhttpListener.onFail(e.getMessage()));
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                handleResponse(response, okhttpListener);
            }
        });
    }

    public void postFile(String url, File file, OkhttpFileListener okhttpFileListener) {

        RequestBody image = RequestBody.create(MediaType.parse("image/png"), file);
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), image)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                handler.post(() -> okhttpFileListener.onFail(e.getMessage()));
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                handleUpload(response, okhttpFileListener);
            }
        });


    }


    public void download(final String url, File file, final OnDownloadListener listener) {
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 下载失败
                listener.onDownloadFailed(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                InputStream is = null;
                byte[] buf = new byte[2048];
                int len = 0;
                FileOutputStream fos = null;
                // 储存下载文件的目录

                try {

                    is = response.body().byteStream();
                    long total = response.body().contentLength();

                    fos = new FileOutputStream(file);
                    long sum = 0;
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                        sum += len;
                        int progress = (int) (sum * 1.0f / total * 100);
                        // 下载中
                        listener.onDownloading(progress);
                    }
                    fos.flush();
                    // 下载完成
                    listener.onDownloadSuccess(file.getAbsolutePath());
                } catch (Exception e) {
                    listener.onDownloadFailed(e.getMessage());
                } finally {
                    try {
                        if (is != null)
                            is.close();
                    } catch (IOException e) {
                    }
                    try {
                        if (fos != null)
                            fos.close();
                    } catch (IOException e) {
                    }
                }
            }
        });
    }


    public void handleResponse(Response response, OkhttpListener okhttpListener) {
        try {
            String json = response.body().string();
            if (response.code() == HttpServletResponse.OK) {

                JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();

                handler.post(() -> okhttpListener.onSuccess(jsonObject));

            } else if (response.code() == HttpServletResponse.BAD_REQUEST) {
                handler.post(() -> okhttpListener.onFail("请求参数错误"));
            } else if (response.code() == HttpServletResponse.NOT_FOUND) {
                handler.post(() -> okhttpListener.onFail("404"));
            } else if (response.code() == HttpServletResponse.INTERNAL_ERROR) {
                handler.post(() -> okhttpListener.onFail("内部错误"));
            } else if (response.code() == HttpServletResponse.UNAUTHORIZED) {
                handler.post(() -> okhttpListener.onFail("未登录或token过期"));
            } else {
                handler.post(() -> okhttpListener.onFail("未知错误"));
            }
        } catch (Exception e) {
            handler.post(() -> okhttpListener.onFail(e.getMessage()));

        }
    }

    private void handleUpload(Response response, OkhttpFileListener okhttpFileListener) {
        try {
            String json = response.body().string();
            if (response.code() == HttpServletResponse.OK) {
                handler.post(() -> okhttpFileListener.onSuccess(json));
            } else {
                handler.post(() -> okhttpFileListener.onFail("上传失败"));
            }
        } catch (Exception e) {
            handler.post(() -> okhttpFileListener.onFail(e.getMessage()));
        }

    }


}
