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

    public static String SIGN_UP = BASE_URL_JAVA + "/auth/signup";

    public static String SEND_CODE = BASE_URL_JAVA + "/auth/code";


    public static String USER_INFO = "https://i.instagram.com/api/v1/users/web_profile_info/";

    public static String USER_INFO_MORE = "https://www.instagram.com/graphql/query/";

    public static String USER_AGENT = "Mozilla/5.0 (Linux; Android 10; SM-G973F Build/QP1A.190711.020; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/86.0.4240.198 Mobile Safari/537.36 Instagram 166.1.0.42.245 Android (29/10; 420dpi; 1080x2042; samsung; SM-G973F; beyond1; exynos9820; en_GB; 256099204)";

    public static String QUERY_HASH = "477b65a610463740ccdb83135b2014db";

    public static String QUERY_HASH_USER = "69cba40317214236af40e7efa697781d";

    public static String MEDIA_INFO = "https://www.instagram.com/graphql/query/";

    public static String STORY_INFO = "https://i.instagram.com/api/v1";

    public static String TAG_INFO = "https://i.instagram.com/api/v1/tags/web_info/";

    public static String TAG_INFO_MORE = "https://i.instagram.com/api/v1";

    public static String[] Cookies = {
            "ig_did=A2BE5747-4C82-4218-8674-0EFCC56660A7; ig_nrcb=1; mid=YyHECgABAAF-7KdHOI_jASsNSWED; csrftoken=jzifji7XlGWxOofURjQY4R3c7VdwyMEj; ds_user_id=55111971277; sessionid=55111971277%3AwXAETLKBa9N6Yk%3A24%3AAYeCFZLdNy61DKPcb6d9DEqmGsHTuIX8rLZqPMgXlQ; dpr=3; datr=UsQhYzz7SZjp4DQ3m-O4EDJ9; rur=\"NAO\\05455111971277\\0541694693343:01f710867a08bcaf48bca48ed3dc705f79a46358955020570b860d5aa53b33821cd86e24\"",
            "mid=Yxg6CgAEAAG-qgaoY8uDRBH9AvV8; ig_did=85CA0E17-404A-4ACA-A39B-C288B5F31808; ig_nrcb=1; dpr=2; fbm_124024574287414=base_domain=.instagram.com; datr=-lEYY6jtFjs_3KUoql-hwbnZ; ds_user_id=54892279902; shbid=\"5641\\05454892279902\\0541694614369:01f7941f54ab83a8269380d7421b5c8c8559edecd2c503db4a97098bd9b18161535811d1\"; shbts=\"1663078369\\05454892279902\\0541694614369:01f75efbc401737c7eb37ebbf106dafbcd041266e66f43987a625fa6328d9c7c266b0292\"; fbsr_124024574287414=WVNxBpB82zPRtc12hbTaVsgGLj17y9U-8DUTJjfDP6g.eyJ1c2VyX2lkIjoiMTAwMDI4NTMwMzkzMDA3IiwiY29kZSI6IkFRQzNwbS11anpkWkJxRUNRMnlwWUd0M1VsRmZtSlFnWGd1enp0d0pESHVUWHhGVlhvSHNhU3ZCU2VlVFl3ZFl1OHI5M2lQTDVXNzRBWDZuMzhZN2FoeFdfU2FVMm5BVFFhTWUxVDVLMzhnZjMwb3Y0dzNNa0tGenZIaEZHZEtONVliUF9SUzVqd254UVZuWGtVRnpmcDR0M1hNM1Q4a0RRcUZReEV1OTdFam1jRDZhaFRoUjZQUS1zTkZEQVcxckVFanEyaWV3TmZwQ1ZxYnRFZmJFQlJlQmgxWml0YVcyNms2MlNwckVtdUNzdWhkSW1CTVc3aHVXZDRMa3RyUXVobThWNVl4N0tuOEtnSkVDYnREeGJKV2wtYUpSUU1EWHZEaTR3VExOb3drNk9aeXlsN19FdFAtc3FLaExjY0FBU09LaG1DUC1SdE5NUlY2WEVuQzBhdVNxIiwib2F1dGhfdG9rZW4iOiJFQUFCd3pMaXhuallCQUpVZVhRQXpIRk1iMnVOVnFrWkFsd2FqY0RFWG92TjNQZmU0QjRENnFCeG1tMTJPbE1CZWhxY1lGRkVBeUU5dUJiUk9NS0xITzNZUUI4S3I5dElmdDhtWkFWN2F2elpBY1IyeGQ4T1FKdFJtWkI0WkNyaFB4b3pUekxpd1pDSGlnQUJsR0gzNnVRdHQ4dDE1YXhEWVJOb0ZTSUlqcHgwWkJvTlNzN1pDUWZFQURucHFmaXlKSEJvWkQiLCJhbGdvcml0aG0iOiJITUFDLVNIQTI1NiIsImlzc3VlZF9hdCI6MTY2MzA3ODk5Mn0; csrftoken=EEbnDkAAmfgfXS5znzETrwTe8J8CidvU; sessionid=54892279902%3A1fFB49Fg4Prqgc%3A18%3AAYeKL-n9l7AakWeWg6WoeRLeYQXCKQwJR2VHtgfMdA; rur=\"PRN\\05454892279902\\0541694615180:01f7feb7daf805bf3c077e8c45239d2f18eb21a76298f812c235f95671957f058a221077\"",
            "mid=YyCcxgAEAAEOdVloEUuq8M3W3i9h; ig_did=9F8A2AE1-B4F9-410D-80B4-D9690D1FAFA5; ig_nrcb=1; dpr=2; datr=bqAgY07Br8fYCsE_2pGVROsk; csrftoken=tUeuoj3FAXXkF3LVQRxQGH3Ft8oYm4MW; ds_user_id=55149330773; rur=\"NAO\\05455149330773\\0541694694258:01f70e867507c764f40016cc67d5cc4b0f2657cd94c9f1778e101fb4bfabb2a1fbc48100\""

    };


    public static String CsrfToken = "DYdwG9H0qtPH52juH9QXQCdxpeyLSQaC";

}
