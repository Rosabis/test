package com.example.qtokenhook;

import android.util.Log;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Xposed模块：获取Q-Token相关信息
 * 提供HTTP接口供Python脚本调用
 */
public class QTokenHook implements IXposedHookLoadPackage {
    
    private static final String TAG = "QTokenHook";
    private static final int HTTP_PORT = 8888;
    
    // 存储获取到的值
    private static String qGuid = "";
    private static String qUa2 = "";
    private static String kingcard = "";
    private static String qToken = "";
    
    private static ServerSocket httpServer = null;
    private static ExecutorService executorService = null;
    private static boolean serverRunning = false;
    
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // 只Hook QQ浏览器
        if (!lpparam.packageName.equals("com.tencent.mtt")) {
            return;
        }
        
        XposedBridge.log(TAG + ": Hook QQ浏览器包名: " + lpparam.packageName);
        
        try {
            // Hook Q-GUID获取
            hookQGuid(lpparam);
            
            // Hook Q-UA2获取
            hookQUa2(lpparam);
            
            // Hook Kingcard获取
            hookKingcard(lpparam);
            
            // Hook Q-Token获取
            hookQToken(lpparam);
            
            // 启动HTTP服务器
            startHttpServer();
            
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Hook失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Hook Q-GUID获取
     */
    private void hookQGuid(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> wupG = XposedHelpers.findClass("com.tencent.mtt.base.wup.g", lpparam.classLoader);
            
            XposedHelpers.findAndHookMethod(wupG, "f", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    String guid = (String) param.getResult();
                    if (guid != null && !guid.isEmpty()) {
                        qGuid = guid;
                        XposedBridge.log(TAG + ": Q-GUID = " + guid);
                        Log.d(TAG, "Q-GUID = " + guid);
                    }
                }
            });
            
            XposedBridge.log(TAG + ": Hook Q-GUID成功");
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Hook Q-GUID失败: " + e.getMessage());
        }
    }
    
    /**
     * Hook Q-UA2获取
     */
    private void hookQUa2(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> qbinfoE = XposedHelpers.findClass("com.tencent.mtt.qbinfo.e", lpparam.classLoader);
            
            XposedHelpers.findAndHookMethod(qbinfoE, "a", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    String qua2 = (String) param.getResult();
                    if (qua2 != null && !qua2.isEmpty()) {
                        qUa2 = qua2;
                        XposedBridge.log(TAG + ": Q-UA2 = " + qua2);
                        Log.d(TAG, "Q-UA2 = " + qua2);
                    }
                }
            });
            
            XposedBridge.log(TAG + ": Hook Q-UA2成功");
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Hook Q-UA2失败: " + e.getMessage());
        }
    }
    
    /**
     * Hook Kingcard获取
     */
    private void hookKingcard(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> kingcardA = XposedHelpers.findClass("com.tencent.mtt.network.kingcard.a", lpparam.classLoader);
            
            XposedHelpers.findAndHookMethod(kingcardA, "e", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    String kc = (String) param.getResult();
                    kingcard = (kc != null) ? kc : "";
                    XposedBridge.log(TAG + ": Kingcard = " + (kc != null ? kc : "(空)"));
                    Log.d(TAG, "Kingcard = " + (kc != null ? kc : "(空)"));
                }
            });
            
            XposedBridge.log(TAG + ": Hook Kingcard成功");
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Hook Kingcard失败: " + e.getMessage());
        }
    }
    
    /**
     * Hook Q-Token获取
     */
    private void hookQToken(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> authA = XposedHelpers.findClass("com.tencent.mtt.external.tencentsim.auth.a", lpparam.classLoader);
            
            XposedHelpers.findAndHookMethod(authA, "c", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    String token = (String) param.getResult();
                    if (token != null && !token.isEmpty()) {
                        qToken = token;
                        XposedBridge.log(TAG + ": Q-Token = " + token);
                        Log.d(TAG, "Q-Token = " + token);
                    }
                }
            });
            
            XposedBridge.log(TAG + ": Hook Q-Token成功");
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Hook Q-Token失败: " + e.getMessage());
        }
    }
    
    /**
     * 启动HTTP服务器，提供API接口
     */
    private void startHttpServer() {
        if (serverRunning) {
            return;
        }
        
        try {
            httpServer = new ServerSocket(HTTP_PORT);
            executorService = Executors.newFixedThreadPool(10);
            serverRunning = true;
            
            // 在新线程中运行服务器
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (serverRunning && !httpServer.isClosed()) {
                            Socket client = httpServer.accept();
                            executorService.execute(new HttpRequestHandler(client));
                        }
                    } catch (IOException e) {
                        if (serverRunning) {
                            XposedBridge.log(TAG + ": HTTP服务器错误: " + e.getMessage());
                        }
                    }
                }
            }).start();
            
            XposedBridge.log(TAG + ": HTTP服务器启动成功，端口: " + HTTP_PORT);
            Log.d(TAG, "HTTP服务器启动成功，端口: " + HTTP_PORT);
            
        } catch (Exception e) {
            XposedBridge.log(TAG + ": 启动HTTP服务器失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * HTTP请求处理器
     */
    private static class HttpRequestHandler implements Runnable {
        private Socket client;
        
        public HttpRequestHandler(Socket client) {
            this.client = client;
        }
        
        @Override
        public void run() {
            try {
                handleRequest(client);
            } catch (IOException e) {
                // 忽略客户端断开连接等错误
            }
        }
        
        private void handleRequest(Socket client) throws IOException {
            java.io.BufferedReader in = new java.io.BufferedReader(
                new java.io.InputStreamReader(client.getInputStream()));
            OutputStream out = client.getOutputStream();
            
            String requestLine = in.readLine();
            if (requestLine == null) {
                client.close();
                return;
            }
            
            String[] parts = requestLine.split(" ");
            if (parts.length < 2) {
                client.close();
                return;
            }
            
            String path = parts[1];
            String response = "";
            String contentType = "application/json";
            
            if (path.equals("/api/info")) {
                Map<String, String> info = new HashMap<>();
                info.put("q_guid", qGuid);
                info.put("q_ua2", qUa2);
                info.put("kingcard", kingcard);
                info.put("q_token", qToken);
                response = formatJsonResponse(info);
            } else if (path.equals("/api/guid")) {
                Map<String, String> info = new HashMap<>();
                info.put("q_guid", qGuid);
                response = formatJsonResponse(info);
            } else if (path.equals("/api/qua2")) {
                Map<String, String> info = new HashMap<>();
                info.put("q_ua2", qUa2);
                response = formatJsonResponse(info);
            } else if (path.equals("/api/kingcard")) {
                Map<String, String> info = new HashMap<>();
                info.put("kingcard", kingcard);
                response = formatJsonResponse(info);
            } else if (path.equals("/api/token")) {
                Map<String, String> info = new HashMap<>();
                info.put("q_token", qToken);
                response = formatJsonResponse(info);
            } else if (path.equals("/")) {
                contentType = "text/html";
                response = "<html><body>" +
                        "<h1>Q-Token信息获取服务</h1>" +
                        "<p><a href='/api/info'>获取所有信息</a></p>" +
                        "<p><a href='/api/guid'>获取Q-GUID</a></p>" +
                        "<p><a href='/api/qua2'>获取Q-UA2</a></p>" +
                        "<p><a href='/api/kingcard'>获取Kingcard</a></p>" +
                        "<p><a href='/api/token'>获取Q-Token</a></p>" +
                        "</body></html>";
            } else {
                response = formatJsonResponse(new HashMap<String, String>() {{
                    put("error", "Not found");
                }});
            }
            
            String httpResponse = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: " + contentType + "; charset=utf-8\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Content-Length: " + response.getBytes("UTF-8").length + "\r\n" +
                    "\r\n" + response;
            
            out.write(httpResponse.getBytes("UTF-8"));
            out.flush();
            client.close();
        }
    }
    
    /**
     * 格式化JSON响应
     */
    private static String formatJsonResponse(Map<String, String> data) {
        StringBuilder json = new StringBuilder("{\n");
        boolean first = true;
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (!first) {
                json.append(",\n");
            }
            json.append("  \"").append(entry.getKey()).append("\": \"")
                .append(entry.getValue().replace("\"", "\\\"").replace("\n", "\\n"))
                .append("\"");
            first = false;
        }
        json.append("\n}");
        return json.toString();
    }
}

