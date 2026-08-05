package com.hook;

import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.nio.charset.StandardCharsets;

/**
 * 网络抓包版 — 记录所有 HTTP/WebSocket 流量到 logcat
 * 使用: adb logcat -s NetSniff > traffic.txt
 * 打完一关后 Ctrl+C, 搜索 "duration" / "time" / "battle"
 */
public class MainHook implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {
        if (!lp.packageName.equals("com.lhpand.zjws")) return;
        final ClassLoader cl = lp.classLoader;

        try {
            // HTTP
            XposedHelpers.findAndHookMethod(
                "org.cocos2dx.lib.Cocos2dxHttpURLConnection",
                cl, "sendRequest",
                java.net.HttpURLConnection.class, byte[].class,
                new XC_MethodHook() {
                    protected void beforeHookedMethod(MethodHookParam p) {
                        try {
                            String url = ((java.net.HttpURLConnection)p.args[0]).getURL().toString();
                            byte[] data = (byte[]) p.args[1];
                            String body = (data != null && data.length > 0)
                                ? new String(data, StandardCharsets.UTF_8) : "(empty)";
                            android.util.Log.i("NetSniff", "HTTP>>> " + url + "\n" + body.substring(0, Math.min(2000, body.length())));
                        } catch (Throwable t) {}
                    }
                });

            // WebSocket text
            try {
                XposedHelpers.findAndHookMethod(
                    "org.cocos2dx.lib.Cocos2dxWebSocket", cl, "send", String.class,
                    new XC_MethodHook() {
                        protected void beforeHookedMethod(MethodHookParam p) {
                            try {
                                String s = (String) p.args[0];
                                if (s != null) android.util.Log.i("NetSniff", "WS>>> " + s.substring(0, Math.min(2000, s.length())));
                            } catch (Throwable t) {}
                        }
                    });
            } catch (Throwable ignored) {}

            // WebSocket binary
            try {
                XposedHelpers.findAndHookMethod(
                    "org.cocos2dx.lib.Cocos2dxWebSocket", cl, "send", byte[].class,
                    new XC_MethodHook() {
                        protected void beforeHookedMethod(MethodHookParam p) {
                            try {
                                String s = new String((byte[])p.args[0], StandardCharsets.UTF_8);
                                android.util.Log.i("NetSniff", "WSB>>> " + s.substring(0, Math.min(2000, s.length())));
                            } catch (Throwable t) {}
                        }
                    });
            } catch (Throwable ignored) {}

        } catch (Throwable t) {
            android.util.Log.e("NetSniff", "fail", t);
        }
    }
}
