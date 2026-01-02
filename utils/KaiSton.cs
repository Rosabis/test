using Newtonsoft.Json.Linq;
using System;
using HawkNet;
using System.Net;
using System.Text;
using System.Diagnostics;
using System.Security.Cryptography;
using System.IO;
using System.Net.Http;
using System.Threading;
using System.Threading.Tasks;

namespace KaiosMarketDownloader.utils
{
    public class KaiSton
    {
        public static string settingsStr = "{\"dev\":{\"model\":\"GoFlip2\",\"imei\":\"123456789012345\",\"type\":999999,\"brand\":\"AlcatelOneTouch\",\"os\":\"KaiOS\",\"version\":\"2.5\",\"ua\":\"Mozilla/5.0 (Mobile; GoFlip2; rv:48.0) Gecko/48.0 Firefox/48.0 KAIOS/2.5\",\"cu\":\"4044O-2BAQUS1-R\",\"mcc\":\"0\",\"mnc\":\"0\"},\"api\":{\"app\":{\"id\":\"CAlTn_6yQsgyJKrr-nCh\",\"name\":\"KaiOS Plus\",\"ver\":\"2.5.4\"},\"server\":{\"url\":\"https://api.kaiostech.com\"},\"ver\":\"2.0\"}}";

        public static string V3Str = "{\"dev\":{\"model\":\"2780 Flip\",\"imei\":\"123456789012345\",\"type\":999999,\"brand\":\"Nokia\",\"os\":\"KaiOS\",\"version\":\"3.1\",\"ua\":\"Mozilla/5.0 (Mobile; Nokia 2780 Flip; rv:84.0) Gecko/84.0 Firefox/84.0 KAIOS/3.1\",\"cu\":\"4044O-2BAQUS1-R\",\"mcc\":\"0\",\"mnc\":\"0\"},\"api\":{\"app\":{\"id\":\"CAlTn_6yQsgyJKrr-nCh\",\"name\":\"KaiOS Plus\",\"ver\":\"3.1.0\"},\"server\":{\"url\":\"https://api.kaiostech.com\"},\"ver\":\"3.0\"}}";
        public static string V2Str = "{\"dev\":{\"model\":\"GoFlip2\",\"imei\":\"123456789012345\",\"type\":999999,\"brand\":\"AlcatelOneTouch\",\"os\":\"KaiOS\",\"version\":\"2.5\",\"ua\":\"Mozilla/5.0 (Mobile; GoFlip2; rv:48.0) Gecko/48.0 Firefox/48.0 KAIOS/2.5\",\"cu\":\"4044O-2BAQUS1-R\",\"mcc\":\"0\",\"mnc\":\"0\"},\"api\":{\"app\":{\"id\":\"CAlTn_6yQsgyJKrr-nCh\",\"name\":\"KaiOS Plus\",\"ver\":\"2.5.4\"},\"server\":{\"url\":\"https://api.kaiostech.com\"},\"ver\":\"2.0\"}}";

        static string authmethod = "api-key";
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
            return getKeyAsync(CancellationToken.None).GetAwaiter().GetResult();
        }

        /// <summary>
        /// 格式化
        /// </summary>
        /// <param name="bytes"></param>
        /// <returns></returns>
        public static string FormatBytes(long bytes)
        {
            string[] Suffix = { "Byte", "KB", "MB", "GB", "TB" };
            int i = 0;
            double dblSByte = bytes;
            if (bytes > 1024)
                for (i = 0; (bytes / 1024) > 0; i++, bytes /= 1024)
                    dblSByte = bytes / 1024.0;
            return String.Format("{0:0.##}{1}", dblSByte, Suffix[i]);
        }

        public static string Request(string method, string path, string data)
        {
            return RequestAsync(method, path, data, CancellationToken.None).GetAwaiter().GetResult();
        }

        public static byte[] RequestDown(string method, string path, string data)
        {
            return RequestDownAsync(method, path, data, CancellationToken.None).GetAwaiter().GetResult();
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
            System.DateTime startTime = TimeZoneInfo.ConvertTimeToUtc(new System.DateTime(1970, 1, 1, 0, 0, 0, 0)).ToLocalTime();
            long t = (time.Ticks - startTime.Ticks) / 10000;   //除10000调整为13位      
            return t;
        }

        // 新增异步方法 - 使用 System.Net.Http.HttpClient
        public static async Task<string> getKeyAsync(CancellationToken cancellationToken)
        {
            if (jsonSetting == null)
            {
                jsonSetting = JObject.Parse(settingsStr);
            }

            model = jsonSetting["dev"]["model"].ToString();

            // 组装请求JSON
            var datajson = new JObject();
            datajson["brand"] = jsonSetting["dev"]["brand"];
            datajson["device_id"] = jsonSetting["dev"]["imei"];
            datajson["device_type"] = jsonSetting["dev"]["type"];
            datajson["model"] = jsonSetting["dev"]["model"];
            datajson["os"] = jsonSetting["dev"]["os"];
            datajson["os_version"] = jsonSetting["dev"]["version"];
            datajson["reference"] = jsonSetting["dev"]["cu"];

            string path = "/v3.0/applications/" + jsonSetting["api"]["app"]["id"] + "/tokens";
            string url = jsonSetting["api"]["server"]["url"] + path;

            var handler = new HttpClientHandler
            {
                AutomaticDecompression = DecompressionMethods.GZip | DecompressionMethods.Deflate,
                Proxy = CustomProxy
            };

            using (var client = new System.Net.Http.HttpClient(handler))
            {
                client.Timeout = TimeSpan.FromMilliseconds(30000);

                var request = new HttpRequestMessage(System.Net.Http.HttpMethod.Post, new Uri(url))
                {
                    Content = new StringContent(datajson.ToString(), Encoding.UTF8, "application/json")
                };

                // Headers
                request.Headers.Add("Authorization", "Key " + authkey);
                request.Headers.Add("Kai-API-Version", jsonSetting["api"]["ver"].ToString());
                var reqinfo =
                    string.Format("ct=\"wifi\", rt=\"auto\", utc=\"{0}\", utc_off=\"1\", mcc=\"{1}\", mnc=\"{2}\", net_mcc=\"null\", net_mnc=\"null\"",
                        GetTimeStamp(), jsonSetting["dev"]["mcc"], jsonSetting["dev"]["mnc"]);
                request.Headers.Add("Kai-Request-Info", reqinfo);
                request.Headers.Add("Kai-Device-Info",
                    string.Format("imei=\"{0}\", curef=\"{1}\"", jsonSetting["dev"]["imei"], jsonSetting["dev"]["cu"]));
                request.Headers.TryAddWithoutValidation("User-Agent", jsonSetting["dev"]["ua"].ToString());

                // 发送请求
                HttpResponseMessage response = await client.SendAsync(request, cancellationToken);
                string ret = await response.Content.ReadAsStringAsync();

                token = ret;
                return ret;
            }
        }

        public static async Task<string> RequestAsync(string method, string path, string data, CancellationToken cancellationToken)
        {
            if (jsonSetting == null)
            {
                jsonSetting = JObject.Parse(settingsStr);
            }

            // 拼接URL
            string url = path.StartsWith("http://") || path.StartsWith("https://")
                ? path
                : jsonSetting["api"]["server"]["url"].ToString() + path;

            var handler = new HttpClientHandler
            {
                AutomaticDecompression = DecompressionMethods.GZip | DecompressionMethods.Deflate,
                Proxy = CustomProxy
            };

            using (var httpClient = new System.Net.Http.HttpClient(handler))
            {
                httpClient.Timeout = TimeSpan.FromMilliseconds(30000);

                var request = new HttpRequestMessage(new System.Net.Http.HttpMethod(method), new Uri(url));

                // Headers
                request.Headers.Add("Kai-API-Version", jsonSetting["api"]["ver"].ToString());
                var reqinfo =
                    string.Format("ct=\"wifi\", rt=\"auto\", utc=\"{0}\", utc_off=\"1\", mcc=\"{1}\", mnc=\"{2}\", net_mcc=\"null\", net_mnc=\"null\"",
                        GetTimeStamp(), jsonSetting["dev"]["mcc"], jsonSetting["dev"]["mnc"]);
                request.Headers.Add("Kai-Request-Info", reqinfo);
                request.Headers.Add("Kai-Device-Info",
                    string.Format("imei=\"{0}\", curef=\"{1}\"", jsonSetting["dev"]["imei"], jsonSetting["dev"]["cu"]));
                request.Headers.TryAddWithoutValidation("User-Agent", jsonSetting["dev"]["ua"].ToString());

                // Hawk 授权
                if (!string.IsNullOrWhiteSpace(token))
                {
                    var jsontoken = JObject.Parse(token);
                    Uri uri = new Uri(url);
                    string host = uri.Host;
                    DateTime ts = DateTime.UtcNow;
                    string nonce = Hawk.GetRandomString(6);
                    string type = "header";
                    string text = ((int)Math.Floor(HawkNet.Hawk.ConvertToUnixTimestamp(ts))).ToString();

                    using (var hMAC = new HMACSHA256(Convert.FromBase64String(jsontoken["mac_key"].ToString())))
                    {
                        string text11 = host.Contains(":") ? host.Substring(0, host.IndexOf(':')) : host;
                        string signData =
                            string.Format("hawk.1.{0}\n{1}\n{2}\n{3}\n{4}\n{5}\n{6}\n\n\n",
                                type, text, nonce, method.ToUpper(), uri.PathAndQuery, text11, uri.Port);
                        string mac = Convert.ToBase64String(hMAC.ComputeHash(Encoding.UTF8.GetBytes(signData)));

                        string authHeader = string.Format("Hawk id=\"{0}\", ts=\"{1}\", nonce=\"{2}\", mac=\"{3}\"",
                            jsontoken["kid"], text, nonce, mac);
                        request.Headers.Add("Authorization", authHeader);
                    }
                }

                // POST数据
                if (method.Equals("POST", StringComparison.OrdinalIgnoreCase))
                {
                    var datajson = new JObject();
                    datajson["brand"] = jsonSetting["dev"]["brand"];
                    datajson["device_id"] = jsonSetting["dev"]["imei"];
                    datajson["device_type"] = jsonSetting["dev"]["type"];
                    datajson["model"] = jsonSetting["dev"]["model"];
                    datajson["os"] = jsonSetting["dev"]["os"];
                    datajson["os_version"] = jsonSetting["dev"]["version"];
                    datajson["reference"] = jsonSetting["dev"]["cu"];
                    request.Content = new StringContent(datajson.ToString(), Encoding.UTF8, "application/json");
                }

                // 发送请求
                HttpResponseMessage response = await httpClient.SendAsync(request, cancellationToken);
                return await response.Content.ReadAsStringAsync();
            }
        }

        public static async Task<byte[]> RequestDownAsync(string method, string path, string data, CancellationToken cancellationToken)
        {
            if (jsonSetting == null)
            {
                jsonSetting = JObject.Parse(settingsStr);
            }

            string url = path;
            var handler = new HttpClientHandler
            {
                AutomaticDecompression = DecompressionMethods.GZip | DecompressionMethods.Deflate,
                Proxy = CustomProxy
            };

            using (var client = new System.Net.Http.HttpClient(handler))
            {
                client.Timeout = TimeSpan.FromMilliseconds(30000);

                var request = new HttpRequestMessage(new System.Net.Http.HttpMethod(method), new Uri(url));

                // 基础 headers
                request.Headers.Add("Kai-API-Version", jsonSetting["api"]["ver"].ToString());
                var reqinfo =
                    string.Format("ct=\"wifi\", rt=\"auto\", utc=\"{0}\", utc_off=\"1\", mcc=\"{1}\", mnc=\"{2}\", net_mcc=\"null\", net_mnc=\"null\"",
                        GetTimeStamp(), jsonSetting["dev"]["mcc"], jsonSetting["dev"]["mnc"]);
                request.Headers.Add("Kai-Request-Info", reqinfo);
                request.Headers.Add("Kai-Device-Info",
                    string.Format("imei=\"{0}\", curef=\"{1}\"", jsonSetting["dev"]["imei"], jsonSetting["dev"]["cu"]));
                request.Headers.TryAddWithoutValidation("User-Agent", jsonSetting["dev"]["ua"].ToString());

                // Hawk 授权
                if (!string.IsNullOrWhiteSpace(token))
                {
                    var jsontoken = JObject.Parse(token);
                    Uri uri = new Uri(url);
                    string host = uri.Host;
                    DateTime ts = DateTime.UtcNow;
                    string nonce = Hawk.GetRandomString(6);
                    string type = "header";
                    string text = ((int)Math.Floor(HawkNet.Hawk.ConvertToUnixTimestamp(ts))).ToString();

                    using (var hMAC = new HMACSHA256(Convert.FromBase64String(jsontoken["mac_key"].ToString())))
                    {
                        string text11 = host.Contains(":") ? host.Substring(0, host.IndexOf(':')) : host;
                        string signData =
                            string.Format("hawk.1.{0}\n{1}\n{2}\n{3}\n{4}\n{5}\n{6}\n\n\n",
                                type, text, nonce, method.ToUpper(), uri.PathAndQuery, text11, uri.Port);
                        string mac = Convert.ToBase64String(hMAC.ComputeHash(Encoding.UTF8.GetBytes(signData)));

                        string authHeader = string.Format("Hawk id=\"{0}\", ts=\"{1}\", nonce=\"{2}\", mac=\"{3}\"",
                            jsontoken["kid"], text, nonce, mac);
                        request.Headers.Add("Authorization", authHeader);
                    }
                }

                // POST 内容
                if (method.Equals("POST", StringComparison.OrdinalIgnoreCase))
                {
                    var datajson = new JObject();
                    datajson["brand"] = jsonSetting["dev"]["brand"];
                    datajson["device_id"] = jsonSetting["dev"]["imei"];
                    datajson["device_type"] = jsonSetting["dev"]["type"];
                    datajson["model"] = jsonSetting["dev"]["model"];
                    datajson["os"] = jsonSetting["dev"]["os"];
                    datajson["os_version"] = jsonSetting["dev"]["version"];
                    datajson["reference"] = jsonSetting["dev"]["cu"];
                    request.Content = new StringContent(datajson.ToString(), Encoding.UTF8, "application/json");
                }

                HttpResponseMessage response = await client.SendAsync(request, HttpCompletionOption.ResponseHeadersRead, cancellationToken);
                using (Stream respStream = await response.Content.ReadAsStreamAsync())
                {
                    using (var ms = new MemoryStream())
                    {
                        await respStream.CopyToAsync(ms);
                        return ms.ToArray();
                    }
                }
            }
        }
    }
}
