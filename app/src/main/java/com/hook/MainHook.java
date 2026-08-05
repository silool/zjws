package com.hook;

import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.nio.charset.StandardCharsets;

public class MainHook implements IXposedHookLoadPackage {

    private static final long MUL = 5;

    // (?i) 大小写不敏感，匹配所有Time/time变体
    private static final java.util.regex.Pattern TIMING =
        java.util.regex.Pattern.compile(
            "(?i)(?:\\{|,)\\s*\"(duration|elapsed|(?:total|play|battle|cost" +
            "|used|fight|use|round|client|server|sync|pass|remain|start|end" +
            "|create|delta)Time|dtime|etime|rtime|ctime|stime|time(?:stamp)?" +
            "|tm|clienttimes)" +
            "\"\\s*:\\s*(\\d+)");

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {
        if (!lp.packageName.equals("com.lhpand.zjws")) return;
        final ClassLoader cl = lp.classLoader;

        try {
            // Cocos2d HTTP
            XposedHelpers.findAndHookMethod(
                "org.cocos2dx.lib.Cocos2dxHttpURLConnection",
                cl, "sendRequest",
                java.net.HttpURLConnection.class, byte[].class,
                new BH(1));

            // Cocos2d WebSocket
            try {
                XposedHelpers.findAndHookMethod(
                    "org.cocos2dx.lib.Cocos2dxWebSocket",
                    cl, "send", String.class,
                    new BH(0));
            } catch (Throwable ignored) {}

        } catch (Throwable t) {
            android.util.Log.e("BH", "reg", t);
        }
    }

    static class BH extends XC_MethodHook {
        final int idx;
        BH(int i) { idx = i; }

        protected void beforeHookedMethod(MethodHookParam p) {
            try {
                Object raw = p.args[idx];
                byte[] data = (raw instanceof String)
                    ? ((String) raw).getBytes(StandardCharsets.UTF_8)
                    : (byte[]) raw;
                if (data == null || data.length < 10) return;

                String body = new String(data, StandardCharsets.UTF_8);
                // 快速跳过：不含time/duration/elapsed/dtime/stime/timestamp
                String lo = body.toLowerCase();
                if (!lo.contains("time") && !lo.contains("duration")
                    && !lo.contains("\"elapsed\"") && !lo.contains("\"dtime\"")
                    && !lo.contains("\"stime\"") && !lo.contains("\"timestamp\""))
                    return;
                // 跳过认证请求
                if (lo.contains("\"token\"") || lo.contains("\"sign\"")
                    || lo.contains("\"auth\"") || lo.contains("\"key\""))
                    return;

                java.util.regex.Matcher m = TIMING.matcher(body);
                if (!m.find()) return;
                m.reset();

                StringBuffer sb = new StringBuffer();
                while (m.find()) {
                    String grp0 = m.group(0);  // 完整匹配
                    String num  = m.group(2);   // 数值
                    try {
                        long v = Long.parseLong(num);
                        if (v >= 1000) {
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
            } catch (Throwable t) {
                android.util.Log.e("BH", "proc", t);
            }
        }
    }
}
