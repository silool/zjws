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
                        java.util.regex.Matcher matcher =
                            java.util.regex.Pattern.compile("\\b(\\d{5,})\\b")
                                .matcher(new String(data));
                        StringBuffer sb = new StringBuffer();
                        while (matcher.find()) {
                            String num = matcher.group(1);
                            try {
                                matcher.appendReplacement(sb,
                                    String.valueOf(Long.parseLong(num) * MULTIPLIER));
                            } catch (NumberFormatException e) {
                                matcher.appendReplacement(sb, matcher.group(0));
                            }
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
