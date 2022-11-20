package com.igtools.videodownloader.api.okhttp;

public class Urls {

    public static String INSTAGRAM = "https://www.instagram.com/";

    public static String USER_INFO = "https://i.instagram.com/api/v1/users/web_profile_info/";

    public static String GRAPH_QL = "https://www.instagram.com/graphql/query/";

    public static String USER_AGENT = "Mozilla/5.0 (Linux; Android 10; SM-G973F Build/QP1A.190711.020; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/86.0.4240.198 Mobile Safari/537.36 Instagram 166.1.0.42.245 Android (29/10; 420dpi; 1080x2042; samsung; SM-G973F; beyond1; exynos9820; en_GB; 256099204)";

    public static String QUERY_HASH = "477b65a610463740ccdb83135b2014db";

    public static String QUERY_HASH_USER = "69cba40317214236af40e7efa697781d";

    public static String QUERY_HASH_TAG = "f92f56d47dc7a55b606908374b43a314";

    public static String PRIVATE_API = "https://i.instagram.com/api/v1";

    public static String TAG_INFO = "https://i.instagram.com/api/v1/tags/web_info/";


    //1.moo61147 ok2.moo611534 ok3.moo61111 ok 4.moo61143 no 5.moo61123 ok 6.moo61154 no 7.moo61138 ok8.moo61115 ok 9.moo611759 ok10.moo61118ok
    public static String[] Cookies = {
            "ig_did=9357DF16-490B-45BF-B60E-A0A6158CC5BD; ig_nrcb=1; dpr=3; datr=3BlTY4doVcbY96Pg3mml1ZEl; mid=Y1MZ4QABAAETK6iKK7DSM_h98JVO; csrftoken=mnQMPkYvlgyvIbBbUljH3OCLF8q5xmD6; sessionid=55111971277%3A2OEW4o6wqq6KwF%3A7%3AAYfsFBGP6gedZNIkKL-IeoratvHygqHv6PGAegjDgQ; ds_user_id=55111971277; rur=\"EAG\\05455111971277\\0541697926617:01f74dc35893fd0c8f8fa1f4f0edfa0c24917ef975834823ade0334857a80c49e8ea7f0f\"",

            "ig_did=2FE36F06-68C8-4BE7-8ABE-18C0C9049536; ig_nrcb=1; dpr=3; mid=Y1MnhAABAAFfILmUTuBcRJqi5bfG; sessionid=54892279902%3AwVqO84yeZego23%3A19%3AAYeSgSGh2sa7Sc_29TJLed7K_uzoi_oBlnF1aqwpZg; ds_user_id=54892279902; csrftoken=A21IIOwaPuagqee8yh4QfLsOAUWkxCLk; shbid=\"5641\\05454892279902\\0541697930014:01f7c4b9b71b0c6a67b96e616384919dce65cc22f55b9fa17e37a3904a239921823013db\"; shbts=\"1666394014\\05454892279902\\0541697930014:01f73ed2758c5a48fc250bd1215221c0f1be99a720f01fddec2fafead77c4542b51980fa\"; rur=\"PRN\\05454892279902\\0541697930014:01f7a67d414253760c7beda7dc8c83526fcdb0a972afdeecbd15d2ffe3e9d3a447c3bd6d\"",

            "ig_did=32D9CA78-EB3F-4493-8364-E80E3F7B603F; ig_nrcb=1; dpr=3; mid=Y1Mn7AABAAFZR8TFddbfh0hcQstA; datr=6ydTY1liZze8UdtP-6yzwS6y; csrftoken=mzp2VRfRsvdmbs21VyJGxKzPWXlnnpVl; rur=\"CCO\\05455149330773\\0541697930115:01f72104199eff9066b708bb261ed28e245ab4ec02511dcbce2cf5c02752387801c4d593\"; sessionid=55149330773%3AVSNII57naKmJQX%3A11%3AAYehjdhtv21smAaVF6c4hdy6-e2WvHot2nz8-iePyg; ds_user_id=55149330773",

            "ig_did=15254D71-D6BD-40E7-8E43-E80D1C70B8BA; ig_nrcb=1; mid=YyaJWAABAAHw7RNcpS8kYxfrTZar; csrftoken=0qCE0bqd8hidgE8JiHKYnPGUOTdqxwsn; ds_user_id=54919939633; sessionid=54919939633%3Afdlwts2dKfBzFC%3A19%3AAYdSj3c9o60d5C8uoOmF9ZZO6DOo-LpjLa1RtGOPGA; shbid=\"4420\\05454919939633\\0541695005983:01f7a41c2d5763d5fdc05e9aea88dc72b12a8bf8f76315b611eebe092f1147d45269b4c6\"; shbts=\"1663469983\\05454919939633\\0541695005983:01f75b4a5155f153830cde975275b9843e2a158e158f370ab7bcaf8f6cf25f888179838d\"; rur=\"NAO\\05454919939633\\0541695005984:01f764eaa202a718801129fe251096abbc71bc5446b45d92d75816b1f1dfee89e89a71ee\"",

            "ig_did=2829A3B7-149C-464E-8AB6-B60203713818; ig_nrcb=1; dpr=3; mid=Y1MoQAABAAF5XGtnfEW4Ulmu0u6t; csrftoken=WeN5MBPlzatAd7KTiCrcztzC4Ciexl00; sessionid=55246299093%3ARNYNTK8aboVcVs%3A24%3AAYdBnmjpw10-0_iy3IMRBUggmn0C0Ky7TSzBroy3iw; ds_user_id=55246299093; shbid=\"13683\\05455246299093\\0541697930217:01f7e771351556e006d445a4415fb661de94dc08f51d183e7eac980ff58cea2e992006dd\"; shbts=\"1666394217\\05455246299093\\0541697930217:01f7f17d6902f83387c842a1482e1e5f4c35ae4ee90840428aacd0790264f22850cbc21f\"; rur=\"CCO\\05455246299093\\0541697930217:01f7c35bc7bcb4e99f3fe5b58d43860c7473c0cfb606da3d512550f5d9793a71759e9c56\"",

            "ig_did=B56D8A5F-D787-4029-BA65-F2914E2C311F; ig_nrcb=1; mid=Y0GK0AABAAGjFypx_ujHNLszOhH-; csrftoken=wffp6MzK6kRA9aVHWvM7oQIZVFQaCHfc; rur=\"NAO\\05454929315072\\0541696775777:01f73f9fc01fee46eaae5b7ac895550045792db2048165ffe46ccad37ef523e7ed363e60\"; sessionid=54929315072%3A5ttA4xXvvFriiC%3A9%3AAYcUEf-WNfNuhe6eNyofLqfBVoUyb96BZUbUXY2DCA; ds_user_id=54929315072",

            "ig_did=E5F97D20-1C45-4F2E-9C8A-80BCF6BC6A99; ig_nrcb=1; dpr=3; mid=Y1M3CwABAAFrGu14QfW44rBR_w2N; csrftoken=oEtQP0mDaHzyEESX0NKICDFHYFUgwid0; sessionid=55450202024%3AZH9mU5cA5odSjF%3A2%3AAYcDV5DXA47nci20aHCyh9RXp99ud_xsz1OCeJYdEQ; ds_user_id=55450202024; shbid=\"7654\\05455450202024\\0541697933983:01f7fbb86c5dc5411cf9cc6cf7888777d5e1c3289b4cb6356da52debc1b16327cb691266\"; shbts=\"1666397983\\05455450202024\\0541697933983:01f743d8e6d9b22c85013fb762c2d99e7174f2ef9c9b72872d2b2ded72424cca127ba929\"; rur=\"EAG\\05455450202024\\0541697933983:01f73ce05ca845c90ba15d56639bd93abcd1b262d071d6543d1a7504fcab7f946069d947\"",

            "ig_did=78760039-92AB-4A47-83B8-E572C8CBD1D3; ig_nrcb=1; dpr=3; mid=Y1MpjwABAAG2wRYBQ6zLr1VwURbo; datr=jilTYwmusEJfkkY0fhlN123a; csrftoken=ox9xiFytl5WcmQaSrGePwPj9NmowFNe0; sessionid=55049373549%3AFxBlCMj7bbDNnG%3A12%3AAYe-GBcHpb3ZnIHf3iYk3bQuIeRsvafbILWOwaY4UA; ds_user_id=55049373549; shbid=\"8947\\05455049373549\\0541697930536:01f7a6209a3fd181c54d5c5ec58a31aa09e58087bd88e22ce81037b0938f24ff94e05f8a\"; shbts=\"1666394536\\05455049373549\\0541697930536:01f7375ba5e384cac1810b81fa047f5d9f682f927e81d83f354110125dc294fdc5eb4beb\"; rur=\"PRN\\05455049373549\\0541697930536:01f75a93a27ce90aa60caa7d1e5ab9dce42e5cc1f4c1a98fc7a41a302afd4d7318da8496\"",

            "ig_did=DC677ABB-C2DF-4BF4-A9CF-A6D6C2359A77; ig_nrcb=1; dpr=3; mid=Y1Mp0wABAAEDFRiRG92c7hvpltRh; datr=0ilTY6af--YgbgHgVp08xIlV; csrftoken=lX3px2OF0gz9RTnltaeB7Olqj7XkkYa7; sessionid=55108707585%3AABNnnEYA2VD1uc%3A24%3AAYfJnAGh_cc_buyph5kfB8__sII8QrlQ5C_sET5Yag; ds_user_id=55108707585; rur=\"CCO\\05455108707585\\0541697930611:01f76d46577f4d52f683b0890f52a547e45fe0ec02b32f505b57542360459a6768934d0f\"",

            "ig_did=FC051C50-43BF-43AD-969F-9E3B1C15B3C1; ig_nrcb=1; dpr=3; mid=Y1MqFQABAAGmISFC_ipO8pW9ctgr; datr=EypTY7GxNkK9PGGeUebupt9r; csrftoken=wtRY9ZcvYQVgzFSXXhTKm9Dr0IAyIWdW; sessionid=55079333711%3Asnvc3i3SISF6ha%3A23%3AAYfkZT-IMwEDM80mluhmdC-UuaxP251uZnTVL2wUCA; ds_user_id=55079333711; shbid=\"147\\05455079333711\\0541697930664:01f77862c434de6941bc66f82dd6d2b1cb0f0d87588dca67cbedf8896ff15c980e0ef2ea\"; shbts=\"1666394664\\05455079333711\\0541697930664:01f7b7c7a2e3c0826f23f054ac408efc4ea9824d9140f3b0549823df80e0837930091b99\"; rur=\"EAG\\05455079333711\\0541697930664:01f73da9ccacba2c116b72ee8a6ee0f472ebfcd4c8e17d57bc558d35e0373dbc49706633\""

    };


    public static String CsrfToken = "DYdwG9H0qtPH52juH9QXQCdxpeyLSQaC";

}
