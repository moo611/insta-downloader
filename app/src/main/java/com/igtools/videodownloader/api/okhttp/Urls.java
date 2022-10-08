package com.igtools.videodownloader.api.okhttp;

public class Urls {


    public static String USER_INFO = "https://i.instagram.com/api/v1/users/web_profile_info/";

    public static String GRAPH_QL = "https://www.instagram.com/graphql/query/";

    public static String USER_AGENT = "Mozilla/5.0 (Linux; Android 10; SM-G973F Build/QP1A.190711.020; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/86.0.4240.198 Mobile Safari/537.36 Instagram 166.1.0.42.245 Android (29/10; 420dpi; 1080x2042; samsung; SM-G973F; beyond1; exynos9820; en_GB; 256099204)";

    public static String QUERY_HASH = "477b65a610463740ccdb83135b2014db";

    public static String QUERY_HASH_USER = "69cba40317214236af40e7efa697781d";

    public static String PRIVATE_API = "https://i.instagram.com/api/v1";

    public static String TAG_INFO = "https://i.instagram.com/api/v1/tags/web_info/";


    //1.moo61147 ok2.moo611534 ok3.moo61111 ok 4.moo61143 no 5.moo61123 ok 6.moo61154 ok 7.moo61138 ok8.moo61115 ok 9.moo611759 ok10.moo61118ok
    public static String[] Cookies = {
            "ig_did=D210266D-0B6A-4DCB-891E-BFA656ADA614; ig_nrcb=1; mid=Y0GFHAABAAFbcFLDxH8KbrXZD2O-; csrftoken=JKli3RNXkoDa5O3POjuwAjwqeEEo43Td; sessionid=55111971277%3A5h41FffbTSmAXo%3A12%3AAYenuOSBfK98clJBt6P996SYhsZfRkxD2G7faTG7FA; ds_user_id=55111971277; rur=\"NAO\\05455111971277\\0541696774340:01f795f4a7bab252224725c48dc61bf023d4cbde9eedd71144ad9405b7ff97d535bf4dae\"",

            "ig_did=899D00BF-ED0D-4E7D-A5BC-11A66259A60A; ig_nrcb=1; mid=Y0GFagABAAH3dHONBYi1YMJjLSIc; csrftoken=B2aZWXphVIhCUPI9Z65VIuw9zDGzwaCG; sessionid=54892279902%3AF3GdlpGGhmLUo3%3A21%3AAYdxlq8E0JEFYIbyLwcWpa0Rxn3GVX-F8rPpUM58yg; ds_user_id=54892279902; shbid=\"5641\\05454892279902\\0541696774397:01f728eafee2a4cf88868beb28902bbbd1243a97524b17bd0b4cb70f332f3356d11b7f58\"; shbts=\"1665238397\\05454892279902\\0541696774397:01f727ed05dce46cd138c39786933f91ec70edc25f07502493b88dbdb2a4161f7f7aa7ee\"; rur=\"NAO\\05454892279902\\0541696774397:01f7d9a9f82ba88c0fdffdb4ecd94d9bab88e77a8bd773ac6aa1923ad15bf7a80feaede7\"",

            "ig_did=AD7BCF62-D5D1-483B-B366-636FDD89DC97; ig_nrcb=1; mid=Y0GGdAABAAHq6f4a6mSldQVamMlM; csrftoken=eG2eAmNpOrufU32a21gd1m9rmuE5kN03; sessionid=55149330773%3AS6RgN7VzKcTijU%3A17%3AAYd954t6IAFg3DgN9jri4jCGKF8GRK5cUGBIcdfMZw; ds_user_id=55149330773; rur=\"NAO\\05455149330773\\0541696774670:01f7fedf7af233b932bbfa5bed0a5392b90738fb0d101456d0f4da88ed0adfa450446689\"",

            "ig_did=15254D71-D6BD-40E7-8E43-E80D1C70B8BA; ig_nrcb=1; mid=YyaJWAABAAHw7RNcpS8kYxfrTZar; csrftoken=0qCE0bqd8hidgE8JiHKYnPGUOTdqxwsn; ds_user_id=54919939633; sessionid=54919939633%3Afdlwts2dKfBzFC%3A19%3AAYdSj3c9o60d5C8uoOmF9ZZO6DOo-LpjLa1RtGOPGA; shbid=\"4420\\05454919939633\\0541695005983:01f7a41c2d5763d5fdc05e9aea88dc72b12a8bf8f76315b611eebe092f1147d45269b4c6\"; shbts=\"1663469983\\05454919939633\\0541695005983:01f75b4a5155f153830cde975275b9843e2a158e158f370ab7bcaf8f6cf25f888179838d\"; rur=\"NAO\\05454919939633\\0541695005984:01f764eaa202a718801129fe251096abbc71bc5446b45d92d75816b1f1dfee89e89a71ee\"",

            "ig_did=EC15050B-5DB5-4C89-844A-C1C0081D3B5F; ig_nrcb=1; mid=Y0GIPgABAAGfuwE3Ew3ImDB4tGuS; csrftoken=rIRQdrmd8BHqxxJJS1lRLgJAii7CAezy; rur=\"NAO\\05455246299093\\0541696775125:01f7cb2311f2512e09398c04fd1695842802ea18c0185d65c74b8f7cc5d68ff6c09f3147\"; sessionid=55246299093%3AELheYI4spWoAD2%3A26%3AAYd-QvpKlEiH3vRVa2CZoc1_HEcHq3d4-iU27YsTHg; ds_user_id=55246299093",

            "ig_did=B56D8A5F-D787-4029-BA65-F2914E2C311F; ig_nrcb=1; mid=Y0GK0AABAAGjFypx_ujHNLszOhH-; csrftoken=wffp6MzK6kRA9aVHWvM7oQIZVFQaCHfc; rur=\"NAO\\05454929315072\\0541696775777:01f73f9fc01fee46eaae5b7ac895550045792db2048165ffe46ccad37ef523e7ed363e60\"; sessionid=54929315072%3A5ttA4xXvvFriiC%3A9%3AAYcUEf-WNfNuhe6eNyofLqfBVoUyb96BZUbUXY2DCA; ds_user_id=54929315072",

            "ig_did=7B22DACC-671F-48BB-A08D-3AFCCDA61837; ig_nrcb=1; mid=Y0GMbQABAAHZBk21RjK9a451acpa; csrftoken=pOAFl7jG8YDsbLe8fs7RlG6pga4OfT05; sessionid=55450202024%3AE5m1uVpv3AoEFL%3A29%3AAYdVbI2aKIEsgwWCJ8ZYuoIEVkCK1YUHxBDp2bDQhQ; ds_user_id=55450202024; rur=\"NAO\\05455450202024\\0541696776192:01f745565615b7dba39bba7ee0bc53c49c0ca05a28e18363fde4153cd6273a1d0d41f46a\"",

            "ig_did=7E724194-E4C8-4EDE-99DA-C783F59F2795; ig_nrcb=1; mid=Y0GLYwABAAG5YeLZhPHXTkgSGDZY; csrftoken=CWY27nQcaFkZOdLZGYI4GnZfyOsNuvvu; rur=\"NAO\\05455049373549\\0541696775926:01f760b029691700d9e50b26e713a2a458db2a07e75b0fc8181f8adf5ed96be0f3050a57\"; sessionid=55049373549%3Atwmed8SCDQCKS9%3A28%3AAYflrYlTRH9vmoK_XbmhSvQkbuHXzzXJ4DOTV2fkXA; ds_user_id=55049373549",

            "ig_did=C527DBF7-7FB5-44B8-9095-8E0BD75400C7; ig_nrcb=1; mid=Y0GL0gABAAHy2vLqW_IvpoYErgF2; csrftoken=TQQ37Wx6xPX7Aw1GfvKLvHlFsVywuroj; sessionid=55108707585%3AGMFvxdeNbVJwRY%3A5%3AAYf9frorgzt6AXV_O3guJ2Rrp8qWr-hk4JqHddA0kQ; ds_user_id=55108707585; rur=\"NAO\\05455108707585\\0541696776040:01f7299c7819652bccbe54b4192e566534b3f8f1e2d3cf870311a5517accd9c685089a5c\"",

            "ig_did=35EE1D83-D823-4B90-96BD-189C82D91D75; ig_nrcb=1; mid=Y0GMFAABAAEHvtuDS5T5ZCozP9EO; csrftoken=e04EqqJFyjvzqcdZVbipEZXZ5GxjwBLU; sessionid=55079333711%3AaylhkI1QjdRhGm%3A7%3AAYcmYZd62b45K13yHPiq_EyrWmR7yUdIYo7mGb7hxQ; ds_user_id=55079333711; rur=\"NAO\\05455079333711\\0541696776102:01f793a327a3642d9a0895352f7d20612f3ccdc9515195ca4e867e4b3721a5b0ae49e7de\"",
    };


    public static String CsrfToken = "DYdwG9H0qtPH52juH9QXQCdxpeyLSQaC";

}
