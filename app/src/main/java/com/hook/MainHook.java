package com.hook;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals("com.lhpand.zjws")) return;

        try {
            de.robv.android.xposed.XposedHelpers.findAndHookMethod(
                "org.cocos2dx.lib.Cocos2dxHttpURLConnection",
                lpparam.classLoader,
                "sendRequest",
                java.net.HttpURLConnection.class, byte[].class,
                new de.robv.android.xposed.XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        byte[] data = (byte[]) param.args[1];
                        if (data == null || data.length == 0) return;
                        String body = new String(data);
                        try {
                            // 只乘 ≥10000 的数字 (毫秒时长, 5倍速 -> 正常耗时)
                            String patched = body.replaceAll(
                                "\\b(\\d{5,})\\b",
                                m -> String.valueOf(Long.parseLong(m.group(1)) * 5)
                            );
                            param.args[1] = patched.getBytes();
                        } catch (Throwable ignored) {}
                    }
                }
            );
        } catch (Throwable t) {
            android.util.Log.e("BattleHook", "Hook failed", t);
        }
    }
}
