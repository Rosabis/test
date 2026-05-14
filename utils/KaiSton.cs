using EasyHttp.Http;
using Newtonsoft.Json.Linq;
using System;
using HawkNet;
using System.Net;
using System.Text;
using System.Security.Cryptography;
using System.IO;

namespace KaiosMarketDownloader.utils
{
    public class KaiSton
    {
        // 默认设置（使用V3.1作为默认）
        public static string settingsStr = V3_1Str;

        // Firefox 内核 37 - KaiOS/1.0
        public static string V1Str = "{\"dev\":{\"model\":\"GoFlip1.0\",\"imei\":\"123456789012345\",\"type\":999999,\"brand\":\"AlcatelOneTouch\",\"os\":\"KaiOS\",\"version\":\"1.0\",\"ua\":\"Mozilla/5.0 (Mobile; GoFlip1.0; rv:37.0) Gecko/37.0 Firefox/37.0 KAIOS/1.0\",\"cu\":\"4044O-2BAQUS1-R\",\"mcc\":\"0\",\"mnc\":\"0\"},\"api\":{\"app\":{\"id\":\"CAlTn_6yQsgyJKrr-nCh\",\"name\":\"KaiOS Plus\",\"ver\":\"2.5\"},\"server\":{\"url\":\"https://api.kaiostech.com\"},\"ver\":\"2.0\"}}";

        // Firefox 内核 48 - KaiOS 2.x 系列
        public static string V2_5Str = "{\"dev\":{\"model\":\"GoFlip2.5\",\"imei\":\"123456789012345\",\"type\":999999,\"brand\":\"AlcatelOneTouch\",\"os\":\"KaiOS\",\"version\":\"2.5\",\"ua\":\"Mozilla/5.0 (Mobile; GoFlip2.5; rv:48.0) Gecko/48.0 Firefox/48.0 KAIOS/2.5\",\"cu\":\"4044O-2BAQUS1-R\",\"mcc\":\"0\",\"mnc\":\"0\"},\"api\":{\"app\":{\"id\":\"CAlTn_6yQsgyJKrr-nCh\",\"name\":\"KaiOS Plus\",\"ver\":\"2.5\"},\"server\":{\"url\":\"https://api.kaiostech.com\"},\"ver\":\"2.0\"}}";
        public static string V2_5_1Str = "{\"dev\":{\"model\":\"GoFlip2.5.1\",\"imei\":\"123456789012345\",\"type\":999999,\"brand\":\"AlcatelOneTouch\",\"os\":\"KaiOS\",\"version\":\"2.5.1\",\"ua\":\"Mozilla/5.0 (Mobile; GoFlip2.5.1; rv:48.0) Gecko/48.0 Firefox/48.0 KAIOS/2.5.1\",\"cu\":\"4044O-2BAQUS1-R\",\"mcc\":\"0\",\"mnc\":\"0\"},\"api\":{\"app\":{\"id\":\"CAlTn_6yQsgyJKrr-nCh\",\"name\":\"KaiOS Plus\",\"ver\":\"2.5\"},\"server\":{\"url\":\"https://api.kaiostech.com\"},\"ver\":\"2.0\"}}";
        public static string V2_5_1_1Str = "{\"dev\":{\"model\":\"GoFlip2.5.1.1\",\"imei\":\"123456789012345\",\"type\":999999,\"brand\":\"AlcatelOneTouch\",\"os\":\"KaiOS\",\"version\":\"2.5.1.1\",\"ua\":\"Mozilla/5.0 (Mobile; GoFlip2.5.1.1; rv:48.0) Gecko/48.0 Firefox/48.0 KAIOS/2.5.1.1\",\"cu\":\"4044O-2BAQUS1-R\",\"mcc\":\"0\",\"mnc\":\"0\"},\"api\":{\"app\":{\"id\":\"CAlTn_6yQsgyJKrr-nCh\",\"name\":\"KaiOS Plus\",\"ver\":\"2.5\"},\"server\":{\"url\":\"https://api.kaiostech.com\"},\"ver\":\"2.0\"}}";
        public static string V2_5_1_2Str = "{\"dev\":{\"model\":\"GoFlip2.5.1.2\",\"imei\":\"123456789012345\",\"type\":999999,\"brand\":\"AlcatelOneTouch\",\"os\":\"KaiOS\",\"version\":\"2.5.1.2\",\"ua\":\"Mozilla/5.0 (Mobile; GoFlip2.5.1.2; rv:48.0) Gecko/48.0 Firefox/48.0 KAIOS/2.5.1.2\",\"cu\":\"4044O-2BAQUS1-R\",\"mcc\":\"0\",\"mnc\":\"0\"},\"api\":{\"app\":{\"id\":\"CAlTn_6yQsgyJKrr-nCh\",\"name\":\"KaiOS Plus\",\"ver\":\"2.5\"},\"server\":{\"url\":\"https://api.kaiostech.com\"},\"ver\":\"2.0\"}}";
        public static string V2_5_2Str = "{\"dev\":{\"model\":\"GoFlip2.5.2\",\"imei\":\"123456789012345\",\"type\":999999,\"brand\":\"AlcatelOneTouch\",\"os\":\"KaiOS\",\"version\":\"2.5.2\",\"ua\":\"Mozilla/5.0 (Mobile; GoFlip2.5.2; rv:48.0) Gecko/48.0 Firefox/48.0 KAIOS/2.5.2\",\"cu\":\"4044O-2BAQUS1-R\",\"mcc\":\"0\",\"mnc\":\"0\"},\"api\":{\"app\":{\"id\":\"CAlTn_6yQsgyJKrr-nCh\",\"name\":\"KaiOS Plus\",\"ver\":\"2.5\"},\"server\":{\"url\":\"https://api.kaiostech.com\"},\"ver\":\"2.0\"}}";
        public static string V2_5_2_1Str = "{\"dev\":{\"model\":\"GoFlip2.5.2.1\",\"imei\":\"123456789012345\",\"type\":999999,\"brand\":\"AlcatelOneTouch\",\"os\":\"KaiOS\",\"version\":\"2.5.2.1\",\"ua\":\"Mozilla/5.0 (Mobile; GoFlip2.5.2.1; rv:48.0) Gecko/48.0 Firefox/48.0 KAIOS/2.5.2.1\",\"cu\":\"4044O-2BAQUS1-R\",\"mcc\":\"0\",\"mnc\":\"0\"},\"api\":{\"app\":{\"id\":\"CAlTn_6yQsgyJKrr-nCh\",\"name\":\"KaiOS Plus\",\"ver\":\"2.5\"},\"server\":{\"url\":\"https://api.kaiostech.com\"},\"ver\":\"2.0\"}}";
        public static string V2_5_2_2Str = "{\"dev\":{\"model\":\"GoFlip2.5.2.2\",\"imei\":\"123456789012345\",\"type\":999999,\"brand\":\"AlcatelOneTouch\",\"os\":\"KaiOS\",\"version\":\"2.5.2.2\",\"ua\":\"Mozilla/5.0 (Mobile; GoFlip2.5.2.2; rv:48.0) Gecko/48.0 Firefox/48.0 KAIOS/2.5.2.2\",\"cu\":\"4044O-2BAQUS1-R\",\"mcc\":\"0\",\"mnc\":\"0\"},\"api\":{\"app\":{\"id\":\"CAlTn_6yQsgyJKrr-nCh\",\"name\":\"KaiOS Plus\",\"ver\":\"2.5\"},\"server\":{\"url\":\"https://api.kaiostech.com\"},\"ver\":\"2.0\"}}";
        public static string V2_5_3Str = "{\"dev\":{\"model\":\"GoFlip2.5.3\",\"imei\":\"123456789012345\",\"type\":999999,\"brand\":\"AlcatelOneTouch\",\"os\":\"KaiOS\",\"version\":\"2.5.3\",\"ua\":\"Mozilla/5.0 (Mobile; GoFlip2.5.3; rv:48.0) Gecko/48.0 Firefox/48.0 KAIOS/2.5.3\",\"cu\":\"4044O-2BAQUS1-R\",\"mcc\":\"0\",\"mnc\":\"0\"},\"api\":{\"app\":{\"id\":\"CAlTn_6yQsgyJKrr-nCh\",\"name\":\"KaiOS Plus\",\"ver\":\"2.5\"},\"server\":{\"url\":\"https://api.kaiostech.com\"},\"ver\":\"2.0\"}}";
        public static string V2_5_3_1Str = "{\"dev\":{\"model\":\"GoFlip2.5.3.1\",\"imei\":\"123456789012345\",\"type\":999999,\"brand\":\"AlcatelOneTouch\",\"os\":\"KaiOS\",\"version\":\"2.5.3.1\",\"ua\":\"Mozilla/5.0 (Mobile; GoFlip2.5.3.1; rv:48.0) Gecko/48.0 Firefox/48.0 KAIOS/2.5.3.1\",\"cu\":\"4044O-2BAQUS1-R\",\"mcc\":\"0\",\"mnc\":\"0\"},\"api\":{\"app\":{\"id\":\"CAlTn_6yQsgyJKrr-nCh\",\"name\":\"KaiOS Plus\",\"ver\":\"2.5\"},\"server\":{\"url\":\"https://api.kaiostech.com\"},\"ver\":\"2.0\"}}";
        public static string V2_5_3_2Str = "{\"dev\":{\"model\":\"GoFlip2.5.3.2\",\"imei\":\"123456789012345\",\"type\":999999,\"brand\":\"AlcatelOneTouch\",\"os\":\"KaiOS\",\"version\":\"2.5.3.2\",\"ua\":\"Mozilla/5.0 (Mobile; GoFlip2.5.3.2; rv:48.0) Gecko/48.0 Firefox/48.0 KAIOS/2.5.3.2\",\"cu\":\"4044O-2BAQUS1-R\",\"mcc\":\"0\",\"mnc\":\"0\"},\"api\":{\"app\":{\"id\":\"CAlTn_6yQsgyJKrr-nCh\",\"name\":\"KaiOS Plus\",\"ver\":\"2.5\"},\"server\":{\"url\":\"https://api.kaiostech.com\"},\"ver\":\"2.0\"}}";
        public static string V2_5_4Str = "{\"dev\":{\"model\":\"GoFlip2.5.4\",\"imei\":\"123456789012345\",\"type\":999999,\"brand\":\"AlcatelOneTouch\",\"os\":\"KaiOS\",\"version\":\"2.5.4\",\"ua\":\"Mozilla/5.0 (Mobile; GoFlip2.5.4; rv:48.0) Gecko/48.0 Firefox/48.0 KAIOS/2.5.4\",\"cu\":\"4044O-2BAQUS1-R\",\"mcc\":\"0\",\"mnc\":\"0\"},\"api\":{\"app\":{\"id\":\"CAlTn_6yQsgyJKrr-nCh\",\"name\":\"KaiOS Plus\",\"ver\":\"2.5\"},\"server\":{\"url\":\"https://api.kaiostech.com\"},\"ver\":\"2.0\"}}";
        public static string V2_5_4_1Str = "{\"dev\":{\"model\":\"GoFlip2.5.4.1\",\"imei\":\"123456789012345\",\"type\":999999,\"brand\":\"AlcatelOneTouch\",\"os\":\"KaiOS\",\"version\":\"2.5.4.1\",\"ua\":\"Mozilla/5.0 (Mobile; GoFlip2.5.4.1; rv:48.0) Gecko/48.0 Firefox/48.0 KAIOS/2.5.4.1\",\"cu\":\"4044O-2BAQUS1-R\",\"mcc\":\"0\",\"mnc\":\"0\"},\"api\":{\"app\":{\"id\":\"CAlTn_6yQsgyJKrr-nCh\",\"name\":\"KaiOS Plus\",\"ver\":\"2.5\"},\"server\":{\"url\":\"https://api.kaiostech.com\"},\"ver\":\"2.0\"}}";
        public static string V2_6Str = "{\"dev\":{\"model\":\"GoFlip2.6\",\"imei\":\"123456789012345\",\"type\":999999,\"brand\":\"AlcatelOneTouch\",\"os\":\"KaiOS\",\"version\":\"2.6\",\"ua\":\"Mozilla/5.0 (Mobile; GoFlip2.6; rv:48.0) Gecko/48.0 Firefox/48.0 KAIOS/2.6\",\"cu\":\"4044O-2BAQUS1-R\",\"mcc\":\"0\",\"mnc\":\"0\"},\"api\":{\"app\":{\"id\":\"CAlTn_6yQsgyJKrr-nCh\",\"name\":\"KaiOS Plus\",\"ver\":\"2.5\"},\"server\":{\"url\":\"https://api.kaiostech.com\"},\"ver\":\"2.0\"}}";

        // Firefox 内核 84 - KaiOS 3.x 系列
        public static string V3_0Str = "{\"dev\":{\"model\":\"GoFlip3.0\",\"imei\":\"123456789012345\",\"type\":999999,\"brand\":\"AlcatelOneTouch\",\"os\":\"KaiOS\",\"version\":\"3.0\",\"ua\":\"Mozilla/5.0 (Mobile; GoFlip3.0; rv:84.0) Gecko/84.0 Firefox/84.0 KAIOS/3.0\",\"cu\":\"4044O-2BAQUS1-R\",\"mcc\":\"0\",\"mnc\":\"0\"},\"api\":{\"app\":{\"id\":\"CAlTn_6yQsgyJKrr-nCh\",\"name\":\"KaiOS Plus\",\"ver\":\"2.5\"},\"server\":{\"url\":\"https://api.kaiostech.com\"},\"ver\":\"3.0\"}}";
        public static string V3_1Str = "{\"dev\":{\"model\":\"GoFlip3.1\",\"imei\":\"123456789012345\",\"type\":999999,\"brand\":\"AlcatelOneTouch\",\"os\":\"KaiOS\",\"version\":\"3.1\",\"ua\":\"Mozilla/5.0 (Mobile; GoFlip3.1; rv:84.0) Gecko/84.0 Firefox/84.0 KAIOS/3.1\",\"cu\":\"4044O-2BAQUS1-R\",\"mcc\":\"0\",\"mnc\":\"0\"},\"api\":{\"app\":{\"id\":\"CAlTn_6yQsgyJKrr-nCh\",\"name\":\"KaiOS Plus\",\"ver\":\"2.5\"},\"server\":{\"url\":\"https://api.kaiostech.com\"},\"ver\":\"3.0\"}}";
        public static string V3_2Str = "{\"dev\":{\"model\":\"GoFlip3.2\",\"imei\":\"123456789012345\",\"type\":999999,\"brand\":\"AlcatelOneTouch\",\"os\":\"KaiOS\",\"version\":\"3.2\",\"ua\":\"Mozilla/5.0 (Mobile; GoFlip3.2; rv:84.0) Gecko/84.0 Firefox/84.0 KAIOS/3.2\",\"cu\":\"4044O-2BAQUS1-R\",\"mcc\":\"0\",\"mnc\":\"0\"},\"api\":{\"app\":{\"id\":\"CAlTn_6yQsgyJKrr-nCh\",\"name\":\"KaiOS Plus\",\"ver\":\"2.5\"},\"server\":{\"url\":\"https://api.kaiostech.com\"},\"ver\":\"3.0\"}}";

        // Firefox 内核 123 - KaiOS 4.0
        public static string V4Str = "{\"dev\":{\"model\":\"GoFlip4.0\",\"imei\":\"123456789012345\",\"type\":999999,\"brand\":\"AlcatelOneTouch\",\"os\":\"KaiOS\",\"version\":\"4.0\",\"ua\":\"Mozilla/5.0 (Mobile; GoFlip4.0; rv:123.0) Gecko/123.0 Firefox/123.0 KAIOS/4.0\",\"cu\":\"4044O-2BAQUS1-R\",\"mcc\":\"0\",\"mnc\":\"0\"},\"api\":{\"app\":{\"id\":\"CAlTn_6yQsgyJKrr-nCh\",\"name\":\"KaiOS Plus\",\"ver\":\"2.5\"},\"server\":{\"url\":\"https://api.kaiostech.com\"},\"ver\":\"3.0\"}}";

        static string authkey = "baJ_nea27HqSskijhZlT";
        public static JObject jsonSetting = null;

        private static string token { get; set; }
        public static string model { get; set; }
        public static IWebProxy CustomProxy { get; private set; }
        public static void SetProxy(string proxyText)
        {
            try
            {
                if (string.IsNullOrWhiteSpace(proxyText))
                {
                    CustomProxy = null;
                    return;
                }
                var txt = proxyText.Trim();
                if (!txt.StartsWith("http://") && !txt.StartsWith("https://"))
                {
                    txt = "http://" + txt;
                }
                CustomProxy = new WebProxy(new Uri(txt));
            }
            catch
            {
                CustomProxy = null;
            }
        }

        public static string getKey()
        {
            if (jsonSetting == null)
            {
                jsonSetting = JObject.Parse(settingsStr);
            }
            var ret = "";
            
            model = jsonSetting["dev"]["model"].ToString();

            EasyHttp.Http.HttpClient httpClient = new EasyHttp.Http.HttpClient();

            httpClient.Request.Proxy = CustomProxy ?? WebProxy.GetDefaultProxy();

            var datajson = new JObject();
            datajson["brand"] = jsonSetting["dev"]["brand"];
            datajson["device_id"] = jsonSetting["dev"]["imei"];
            datajson["device_type"] = jsonSetting["dev"]["type"];
            datajson["model"] = jsonSetting["dev"]["model"];
            datajson["os"] = jsonSetting["dev"]["os"];
            datajson["os_version"] = jsonSetting["dev"]["version"];
            datajson["reference"] = jsonSetting["dev"]["cu"];

            var path = "/v3.0/applications/" + jsonSetting["api"]["app"]["id"].ToString() + "/tokens";

            httpClient.Request.AddExtraHeader("Authorization", "Key " + authkey);

            string url = jsonSetting["api"]["server"]["url"].ToString() + path;

            httpClient.Request.AddExtraHeader("Kai-API-Version", jsonSetting["api"]["ver"].ToString());


            var reqinfo = "ct=\"wifi\", rt=\"auto\", utc=\"" + GetTimeStamp() + "\", utc_off=\"1\", " + "mcc=\"" + jsonSetting["dev"]["mcc"].ToString() + "\", " + "mnc=\"" + jsonSetting["dev"]["mnc"].ToString() + "\", " + "net_mcc=\"null\", " + "net_mnc=\"null\"";

            httpClient.Request.AddExtraHeader("Kai-Request-Info", reqinfo);

            httpClient.Request.AddExtraHeader("Kai-Device-Info", "imei=\"" + jsonSetting["dev"]["imei"].ToString() + "\", curef = \"" + jsonSetting["dev"]["cu"].ToString() + "\"");
            httpClient.Request.UserAgent = jsonSetting["dev"]["ua"].ToString();
            httpClient.Request.ContentType = "application/json";


            ret = httpClient.Post(url, datajson.ToString(), "application/json").RawText;
            token = ret;
            return ret;

        }

        public static string Request(string method, string path, string data)
        {
            if (jsonSetting == null)
            {
                jsonSetting = JObject.Parse(settingsStr);
            }
            var ret = "";

            EasyHttp.Http.HttpClient httpClient = new EasyHttp.Http.HttpClient();
            httpClient.Request.Proxy = CustomProxy ?? WebProxy.GetDefaultProxy();
            var datajson = new JObject();
            datajson["brand"] = jsonSetting["dev"]["brand"];
            datajson["device_id"] = jsonSetting["dev"]["imei"];
            datajson["device_type"] = jsonSetting["dev"]["type"];
            datajson["model"] = jsonSetting["dev"]["model"];
            datajson["os"] = jsonSetting["dev"]["os"];
            datajson["os_version"] = jsonSetting["dev"]["version"];
            datajson["reference"] = jsonSetting["dev"]["cu"];

            //path = "/v3.0/applications/" + jsonSetting["api"]["app"]["id"].ToString() + "/tokens";

            //httpClient.Request.AddExtraHeader("Authorization", "Key " + authkey);
            string url = "";
            if (path.StartsWith("http://") || path.StartsWith("https://"))
            {
                url = path;

            }
            else
            {
                url = jsonSetting["api"]["server"]["url"].ToString() + path;

            }
            httpClient.Request.Timeout = 30000;

            httpClient.Request.AddExtraHeader("Kai-API-Version", jsonSetting["api"]["ver"].ToString());

            var reqinfo = "ct=\"wifi\", rt=\"auto\", utc=\"" + GetTimeStamp() + "\", utc_off=\"1\", " + "mcc=\"" + jsonSetting["dev"]["mcc"].ToString() + "\", " + "mnc=\"" + jsonSetting["dev"]["mnc"].ToString() + "\", " + "net_mcc=\"null\", " + "net_mnc=\"null\"";

            httpClient.Request.AddExtraHeader("Kai-Request-Info", reqinfo);

            httpClient.Request.AddExtraHeader("Kai-Device-Info", "imei=\"" + jsonSetting["dev"]["imei"].ToString() + "\", curef=\"" + jsonSetting["dev"]["cu"].ToString() + "\"");
            httpClient.Request.UserAgent = jsonSetting["dev"]["ua"].ToString();
            httpClient.Request.ContentType = "application/json";

            if (!string.IsNullOrWhiteSpace(token))
            {
                var jsontoken = JObject.Parse(token);
                string host = new Uri(url).Host;
                Uri uri = new Uri(url);
                DateTime? ts = null;
                string nonce = null;
                string payloadHash = null;
                string type = null;
                if (string.IsNullOrEmpty(nonce))
                {
                    nonce = Hawk.GetRandomString(6);
                }

                if (string.IsNullOrEmpty(type))
                {
                    type = "header";
                }
                //var auth = HawkNet.Hawk.GetAuthorizationHeader(new Uri(url).Host, method, new Uri(url), hawkCredential);
                string text = ((int)Math.Floor(HawkNet.Hawk.ConvertToUnixTimestamp(ts.HasValue ? ts.Value : DateTime.UtcNow))).ToString();


                HMAC hMAC = null;

                hMAC = new HMACSHA256();

                hMAC.Key = Convert.FromBase64String(jsontoken["mac_key"].ToString());
                string text11 = ((host.IndexOf(':') > 0) ? host.Substring(0, host.IndexOf(':')) : host);
                string text22 = "hawk.1." + type + "\n" + text + "\n" + nonce + "\n" + method.ToUpper() + "\n" + uri.PathAndQuery + "\n" + text11 + "\n" + uri.Port + "\n" + ((!string.IsNullOrEmpty(payloadHash)) ? payloadHash : "") + "\n" + "\n";

                string text33 = Convert.ToBase64String(hMAC.ComputeHash(Encoding.UTF8.GetBytes(text22)));

                string text3 = $"id=\"{jsontoken["kid"].ToString()}\", ts=\"{text}\", nonce=\"{nonce}\", mac=\"{text33}\"";
                if (!string.IsNullOrEmpty(payloadHash))
                {
                    text3 += $", hash=\"{payloadHash}\"";
                }
                httpClient.Request.AddExtraHeader("Authorization", "Hawk " + text3);

            }
            if (method == "POST")
            {
                ret = httpClient.Post(url, datajson.ToString(), "application/json").RawText;


            }
            else if (method == "GET")
            {

                ret = httpClient.Get(url).RawText;

            }
            return ret;
        }

        public static byte[] RequestDown(string method, string path, string data)
        {
            if (jsonSetting == null)
            {
                jsonSetting = JObject.Parse(settingsStr);
            }

            EasyHttp.Http.HttpClient httpClient = new EasyHttp.Http.HttpClient();
            httpClient.Request.Proxy = CustomProxy ?? WebProxy.GetDefaultProxy();

            var datajson = new JObject();
            datajson["brand"] = jsonSetting["dev"]["brand"];
            datajson["device_id"] = jsonSetting["dev"]["imei"];
            datajson["device_type"] = jsonSetting["dev"]["type"];
            datajson["model"] = jsonSetting["dev"]["model"];
            datajson["os"] = jsonSetting["dev"]["os"];
            datajson["os_version"] = jsonSetting["dev"]["version"];
            datajson["reference"] = jsonSetting["dev"]["cu"];

            //path = "/v3.0/applications/" + jsonSetting["api"]["app"]["id"].ToString() + "/tokens";

            //httpClient.Request.AddExtraHeader("Authorization", "Key " + authkey);

            string url = path;

            httpClient.Request.AddExtraHeader("Kai-API-Version", jsonSetting["api"]["ver"].ToString());


            var reqinfo = "ct=\"wifi\", rt=\"auto\", utc=\"" + GetTimeStamp() + "\", utc_off=\"1\", " + "mcc=\"" + jsonSetting["dev"]["mcc"].ToString() + "\", " + "mnc=\"" + jsonSetting["dev"]["mnc"].ToString() + "\", " + "net_mcc=\"null\", " + "net_mnc=\"null\"";

            httpClient.Request.AddExtraHeader("Kai-Request-Info", reqinfo);

            httpClient.Request.AddExtraHeader("Kai-Device-Info", "imei=\"" + jsonSetting["dev"]["imei"].ToString() + "\", curef=\"" + jsonSetting["dev"]["cu"].ToString() + "\"");
            httpClient.Request.UserAgent = jsonSetting["dev"]["ua"].ToString();
            httpClient.Request.ContentType = "application/json";
            httpClient.StreamResponse = true;
            if (!string.IsNullOrWhiteSpace(token))
            {
                var jsontoken = JObject.Parse(token);
                string host = new Uri(url).Host;
                Uri uri = new Uri(url);
                DateTime? ts = null;
                string nonce = null;
                string payloadHash = null;
                string type = null;
                if (string.IsNullOrEmpty(nonce))
                {
                    nonce = Hawk.GetRandomString(6);
                }

                if (string.IsNullOrEmpty(type))
                {
                    type = "header";
                }
                //var auth = HawkNet.Hawk.GetAuthorizationHeader(new Uri(url).Host, method, new Uri(url), hawkCredential);
                string text = ((int)Math.Floor(HawkNet.Hawk.ConvertToUnixTimestamp(ts.HasValue ? ts.Value : DateTime.UtcNow))).ToString();


                HMAC hMAC = null;

                hMAC = new HMACSHA256();

                hMAC.Key = Convert.FromBase64String(jsontoken["mac_key"].ToString());
                string text11 = ((host.IndexOf(':') > 0) ? host.Substring(0, host.IndexOf(':')) : host);
                string text22 = "hawk.1." + type + "\n" + text + "\n" + nonce + "\n" + method.ToUpper() + "\n" + uri.PathAndQuery + "\n" + text11 + "\n" + uri.Port + "\n" + ((!string.IsNullOrEmpty(payloadHash)) ? payloadHash : "") + "\n" + "\n";

                string text33 = Convert.ToBase64String(hMAC.ComputeHash(Encoding.UTF8.GetBytes(text22)));

                string text3 = string.Format("id=\"{0}\", ts=\"{1}\", nonce=\"{2}\", mac=\"{3}\"", jsontoken["kid"].ToString(), text, nonce, text33);
                if (!string.IsNullOrEmpty(payloadHash))
                {
                    text3 += string.Format(", hash=\"{0}\"", payloadHash);
                }
                httpClient.Request.AddExtraHeader("Authorization", "Hawk " + text3);

            }
            Stream retstream = null;
            if (method == "POST")
            {
                retstream = httpClient.Post(url, datajson.ToString(), "application/json").ResponseStream; ;

            }
            else if (method == "GET")
            {
                retstream = httpClient.Get(url).ResponseStream;
            }
            MemoryStream ms = new MemoryStream();
            // 手动复制流，兼容 .NET 4.0
            byte[] buffer = new byte[81920];
            int bytesRead;
            while ((bytesRead = retstream.Read(buffer, 0, buffer.Length)) > 0)
            {
                ms.Write(buffer, 0, bytesRead);
            }
            ms.Seek(0, SeekOrigin.Begin);

            return ms.ToArray();
        }
        /// <summary>
        /// 获得13位的时间戳
        /// </summary>
        /// <returns></returns>
        public static string GetTimeStamp()
        {
            System.DateTime time = System.DateTime.Now;
            long ts = ConvertDateTimeToInt(time);
            return ts.ToString();
        } /// <summary>  
          /// 将c# DateTime时间格式转换为Unix时间戳格式  
          /// </summary>  
          /// <param name="time">时间</param>  
          /// <returns>long</returns>  
        private static long ConvertDateTimeToInt(System.DateTime time)
        {
            System.DateTime startTime = TimeZone.CurrentTimeZone.ToLocalTime(new System.DateTime(1970, 1, 1, 0, 0, 0, 0));
            long t = (time.Ticks - startTime.Ticks) / 10000;   //除10000调整为13位      
            return t;
        }
    }
}
