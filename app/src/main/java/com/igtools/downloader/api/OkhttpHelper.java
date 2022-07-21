package com.igtools.downloader.api;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
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
        builder.connectTimeout(20, TimeUnit.SECONDS);
        builder.writeTimeout(20, TimeUnit.SECONDS);
        builder.readTimeout(20, TimeUnit.SECONDS);

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

    public void postJson(String url, String method, HashMap<String, Object> params, OkhttpListener okhttpListener) {

        HashMap<String, Object> reqBody = new HashMap<>();
        reqBody.put("jsonrpc", "2.0");
        reqBody.put("method", method);
        reqBody.put("id", UUID.randomUUID().toString());
        reqBody.put("params", params);
        String json = gson.toJson(reqBody);

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
        HashMap<String, Object> reqBody = new HashMap<>();
        reqBody.put("jsonrpc", "2.0");
        reqBody.put("method", method);
        reqBody.put("id", UUID.randomUUID().toString());
        params.put("_UserToken", token);
        reqBody.put("params", params);
        String json = gson.toJson(reqBody);

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


    public void handleResponse(Response response, OkhttpListener okhttpListener) {
        try {
            String json = response.body().string();
            if (response.code() == HttpServletResponse.OK) {
                JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
                if (jsonObject.get("error") != null) {
                    String errMsg = jsonObject.get("error").getAsJsonObject().get("message").getAsString();
                    handler.post(() -> okhttpListener.onFail(errMsg));

                } else {
                    JsonObject result = jsonObject.get("result").getAsJsonObject();
                    handler.post(() -> okhttpListener.onSuccess(result));
                }

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
        } catch (IOException e) {
            handler.post(()->okhttpListener.onFail(e.getMessage()));

        }
    }

    private void handleUpload(Response response, OkhttpFileListener okhttpFileListener){
        try {
            String json = response.body().string();
            if (response.code() == HttpServletResponse.OK) {
                handler.post(() -> okhttpFileListener.onSuccess(json));
            }else{
                handler.post(() -> okhttpFileListener.onFail("上传失败"));
            }
        }catch (Exception e){
            handler.post(() ->okhttpFileListener.onFail(e.getMessage()));
        }

    }


}
