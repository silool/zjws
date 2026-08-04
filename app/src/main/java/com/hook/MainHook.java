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
                        java.util.regex.Pattern pattern =
                            java.util.regex.Pattern.compile(
                                "\"(duration|elapsed|totalTime|playTime|battleTime|costTime|usedTime|fightTime|useTime|roundTime)\"\\s*:\\s*(\\d+)");
                        java.util.regex.Matcher matcher = pattern.matcher(new String(data));
                        StringBuffer sb = new StringBuffer();
                        while (matcher.find()) {
                            String num = matcher.group(2);
                            try {
                                long val = Long.parseLong(num);
                                // 只乘 ≥10000 的(>=10秒级毫秒值)，避免改小值ID
                                if (val >= 10000) {
                                    matcher.appendReplacement(sb,
                                        "\"" + matcher.group(1) + "\":" + (val * MULTIPLIER));
                                    continue;
                                }
                            } catch (NumberFormatException e) {}
                            matcher.appendReplacement(sb, matcher.group(0));
                        }
                        matcher.appendTail(sb);
                        p.args[1] = sb.toString().getBytes();
                    }
                }
            );
        } catch (Throwable t) {
            android.util.Log.e("BattleHook", "Hook failed", t);
        }
    }
}
