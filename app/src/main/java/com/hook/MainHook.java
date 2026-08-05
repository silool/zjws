package com.hook;

import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.nio.charset.StandardCharsets;

public class MainHook implements IXposedHookLoadPackage {

    private static final long MUL = 5;

    // JSON timing字段: 前有{或, 锚定
    private static final java.util.regex.Pattern TIMING =
        java.util.regex.Pattern.compile(
            "(?:\\{|,)\\s*\"(d(?:uration|eltaTime|time)|elapsed|totalTime" +
            "|playTime|battleTime|costTime|usedTime|fightTime|useTime" +
            "|roundTime|clientTime(?:s)?|serverTime|syncTime|passTime" +
            "|remainTime|s(?:time|tartTime)|e(?:time|ndTime)|r(?:time|tTime)" +
            "|c(?:time|reateTime)|time(?:stamp)?|tm)" +
            "\"\\s*:\\s*(\\d+)");

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {
        if (!lp.packageName.equals("com.lhpand.zjws")) return;
        final ClassLoader cl = lp.classLoader;

        try {
            // Cocos2d HTTP sendRequest(HttpURLConnection, byte[])
            XposedHelpers.findAndHookMethod(
                "org.cocos2dx.lib.Cocos2dxHttpURLConnection",
                cl, "sendRequest",
                java.net.HttpURLConnection.class, byte[].class,
                new BodyHook(1));

            // Cocos2d WebSocket send(String)
            try {
                XposedHelpers.findAndHookMethod(
                    "org.cocos2dx.lib.Cocos2dxWebSocket",
                    cl, "send", String.class,
                    new BodyHook(0));
            } catch (Throwable ignored) {}

        } catch (Throwable t) {
            android.util.Log.e("BH", "reg", t);
        }
    }

    static class BodyHook extends XC_MethodHook {
        final int idx;
        BodyHook(int i) { idx = i; }

        protected void beforeHookedMethod(MethodHookParam p) {
            try {
                Object raw = p.args[idx];
                byte[] data;
                if (raw instanceof String) {
                    data = ((String) raw).getBytes(StandardCharsets.UTF_8);
                } else {
                    data = (byte[]) raw;
                }
                if (data == null || data.length < 10) return;

                String body = new String(data, StandardCharsets.UTF_8);
                // 快速跳过：不含任何疑似timing字段
                if (!hasTiming(body)) return;
                // 跳过认证请求
                if (body.contains("\"token\"") || body.contains("\"sign\"")
                    || body.contains("\"auth\"") || body.contains("\"key\""))
                    return;

                java.util.regex.Matcher m = TIMING.matcher(body);
                if (!m.find()) return;
                m.reset();

                StringBuffer sb = new StringBuffer();
                while (m.find()) {
                    String grp0 = m.group(0);   // 完整匹配含{或,前缀
                    String name = m.group(1);    // 字段名
                    String num  = m.group(2);    // 数值
                    try {
                        long v = Long.parseLong(num);
                        if (v >= 1000) {
                            // 重建: {/,"field":value*5
                            m.appendReplacement(sb,
                                grp0.substring(0, grp0.length() - num.length())
                                + (v * MUL));
                            continue;
                        }
                    } catch (NumberFormatException e) {}
                    m.appendReplacement(sb, grp0);
                }
                m.appendTail(sb);
                String out = sb.toString();
                p.args[idx] = (raw instanceof String) ? out
                    : out.getBytes(StandardCharsets.UTF_8);
            } catch (Throwable ignored) {}
        }

        private boolean hasTiming(String s) {
            return s.contains("\"duration\"") || s.contains("\"time")
                || s.contains("\"elapsed\"") || s.contains("\"dtime\"")
                || s.contains("\"stime\"") || s.contains("\"timestamp\"");
        }
    }
}
