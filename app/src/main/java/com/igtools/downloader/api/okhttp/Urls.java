package com.igtools.downloader.api.okhttp;

public class Urls {

    public static String BASE_URL_PY = "http://192.168.0.100:3000/";

    //public static String BASE_URL_PY = "http://192.168.100.209:3000/";

    //public static String BASE_URL_PY = "http://35.90.136.197:3000/";

    public static String BASE_URL_JAVA = "http://192.168.100.209:8080/";

    public static String SHORT_CODE = BASE_URL_PY + "api/mediainfo";

    public static String USER_NAME = BASE_URL_PY + "api/userinfo";

    public static String USER_TAG = BASE_URL_PY + "api/taginfo";

    public static String LOGIN = BASE_URL_JAVA + "/auth/login";

    public static String SIGN_UP = BASE_URL_JAVA+"/auth/signup";

    public static String SEND_CODE = BASE_URL_JAVA+"/auth/code";


}
