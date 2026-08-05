package com.hook;

import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.nio.charset.StandardCharsets;

public class MainHook implements IXposedHookLoadPackage {

    private static final long MULTIPLIER = 5;
    // JSON timing字段：前有{或,锚定
    private static final java.util.regex.Pattern TIMING_PATTERN =
        java.util.regex.Pattern.compile(
            "(?:\\{|,)\\s*\"(duration|elapsed|totalTime|playTime|battleTime" +
            "|costTime|usedTime|fightTime|useTime|roundTime|clientTime" +
            "|serverTime|syncTime|deltaTime|passTime|remainTime" +
            "|dtime|stime|etime|rtime|ctime|clienttimes" +
            "|time|timestamp|tm)\"\\s*:\\s*(\\d+)");

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals("com.lhpand.zjws")) return;
        final ClassLoader cl = lpparam.classLoader;

        try {
            XposedHelpers.findAndHookMethod(
                "org.cocos2dx.lib.Cocos2dxHttpURLConnection",
                cl, "sendRequest",
                java.net.HttpURLConnection.class, byte[].class,
                new XC_MethodHook() {
                    protected void beforeHookedMethod(MethodHookParam p) {
                        try {
                            byte[] data = (byte[]) p.args[1];
                            if (data == null || data.length < 10) return;
                            String body = new String(data, StandardCharsets.UTF_8);
                            if (!body.contains("\"duration\"") &&
                                !body.contains("\"time\"") &&
                                !body.contains("\"totalTime\"") &&
                                !body.contains("\"elapsed\"") &&
                                !body.contains("\"playTime\"") &&
                                !body.contains("\"battleTime\"") &&
                                !body.contains("\"costTime\"") &&
                                !body.contains("\"usedTime\"") &&
                                !body.contains("\"fightTime\"") &&
                                !body.contains("\"clientTime\"") &&
                                !body.contains("\"dtime\"") &&
                                !body.contains("\"stime\"") &&
                                !body.contains("\"timestamp\"")) return;
                            java.util.regex.Matcher m = TIMING_PATTERN.matcher(body);
                            if (!m.find()) return;
                            m.reset();
                            StringBuffer sb = new StringBuffer();
                            while (m.find()) {
                                String num = m.group(2);
                                try {
                                    long val = Long.parseLong(num);
                                    if (val >= 1000) {
                                        m.appendReplacement(sb,
                                            m.group(0).replaceFirst("\\d+",
                                                String.valueOf(val * MULTIPLIER)));
                                        continue;
                                    }
                                } catch (NumberFormatException e) {}
                                m.appendReplacement(sb, m.group(0));
                            }
                            m.appendTail(sb);
                            p.args[1] = sb.toString().getBytes(StandardCharsets.UTF_8);
                        } catch (Throwable ignored) {}
                    }
                }
            );
        } catch (Throwable t) {
            android.util.Log.e("BattleHook", "Hook failed", t);
        }
    }
}
