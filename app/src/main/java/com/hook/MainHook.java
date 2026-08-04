package com.hook;

import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    private static final long MULTIPLIER = 5;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals("com.lhpand.zjws")) return;
        final ClassLoader cl = lpparam.classLoader;

        try {
            // HTTP 上报 duration ×5
            XposedHelpers.findAndHookMethod(
                "org.cocos2dx.lib.Cocos2dxHttpURLConnection",
                cl, "sendRequest",
                java.net.HttpURLConnection.class, byte[].class,
                new XC_MethodHook() {
                    protected void beforeHookedMethod(MethodHookParam p) {
                        byte[] data = (byte[]) p.args[1];
                        if (data == null || data.length == 0) return;
                        // 取URL判断是否为战斗上报，避免拦截认证请求
                        try {
                            java.net.HttpURLConnection conn =
                                (java.net.HttpURLConnection) p.args[0];
                            String url = conn.getURL().toString();
                            // 只处理战斗上报相关URL，认证/心跳原封不动
                            if (url.contains("report") || url.contains("battle")
                                || url.contains("fight") || url.contains("pve")
                                || url.contains("pvp") || url.contains("stage")
                                || url.contains("round") || url.contains("game")) {
                                // OK — 落到下面的修改逻辑
                            } else {
                                return; // 跳过，不修改
                            }
                        } catch (Exception e) {
                            // 获取URL失败则保守跳过
                            return;
                        }
                        java.util.regex.Pattern pattern =
                            java.util.regex.Pattern.compile(
                                "\"(duration|elapsed|totalTime|playTime|battleTime|costTime|usedTime|fightTime|useTime|roundTime)\"\\s*:\\s*(\\d+)");
                        java.util.regex.Matcher matcher =
                            pattern.matcher(new String(data, "UTF-8"));
                        StringBuffer sb = new StringBuffer();
                        while (matcher.find()) {
                            String num = matcher.group(2);
                            try {
                                long val = Long.parseLong(num);
                                if (val >= 10000) {
                                    matcher.appendReplacement(sb,
                                        "\"" + matcher.group(1) + "\":" + (val * MULTIPLIER));
                                    continue;
                                }
                            } catch (NumberFormatException e) {}
                            matcher.appendReplacement(sb, matcher.group(0));
                        }
                        matcher.appendTail(sb);
                        p.args[1] = sb.toString().getBytes("UTF-8");
                    }
                }
            );
        } catch (Throwable t) {
            android.util.Log.e("BattleHook", "Hook failed", t);
        }
    }
}
