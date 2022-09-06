package com.igtools.igdownloader.api.okhttp;

public class Urls {

    public static String BASE_URL_PY = "http://192.168.31.54:3000";

    //public static String BASE_URL_PY = "http://192.168.100.38:3000";

    //public static String BASE_URL_PY = "http://35.90.136.197:3000";

    public static String BASE_URL_JAVA = "http://192.168.100.209:8080";

    public static String SHORT_CODE = BASE_URL_PY + "/api/mediainfo";

    public static String USER_NAME = BASE_URL_PY + "/api/userinfo";

    public static String USER_TAG = BASE_URL_PY + "/api/taginfo";

    public static String LOGIN = BASE_URL_JAVA + "/auth/login";

    public static String SIGN_UP = BASE_URL_JAVA+"/auth/signup";

    public static String SEND_CODE = BASE_URL_JAVA+"/auth/code";


    public static String USER_INFO = "https://i.instagram.com/api/v1/users/web_profile_info/";

    public static String USER_INFO_MORE="https://www.instagram.com/graphql/query/";

    public static String USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 12_3_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 Instagram 105.0.0.11.118 (iPhone11,8; iOS 12_3_1; en_US; en-US; scale=2.00; 828x1792; 165586599)";

    public static String QUERY_HASH="69cba40317214236af40e7efa697781d";

    public static String MEDIA_INFO = "https://www.instagram.com/graphql/query/";


}
