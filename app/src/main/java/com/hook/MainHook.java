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
            // ===== 1. HTTP上报 duration ×5 =====
            XposedHelpers.findAndHookMethod(
                "org.cocos2dx.lib.Cocos2dxHttpURLConnection",
                cl, "sendRequest",
                java.net.HttpURLConnection.class, byte[].class,
                new XC_MethodHook() {
                    protected void beforeHookedMethod(MethodHookParam p) {
                        byte[] data = (byte[]) p.args[1];
                        if (data == null || data.length == 0) return;
                        String body = new String(data);
                        java.util.regex.Matcher matcher =
                            java.util.regex.Pattern.compile("\\b(\\d{5,})\\b")
                                .matcher(body);
                        StringBuffer sb = new StringBuffer();
                        while (matcher.find()) {
                            String num = matcher.group(1);
                            try {
                                long val = Long.parseLong(num);
                                matcher.appendReplacement(sb, String.valueOf(val * MULTIPLIER));
                            } catch (NumberFormatException e) {
                                matcher.appendReplacement(sb, matcher.group(0));
                            }
                        }
                        matcher.appendTail(sb);
                        body = sb.toString();
                        p.args[1] = body.getBytes();
                    }
                }
            );

            // ===== 2. 音频时间戳屏蔽 =====
            XposedHelpers.findAndHookMethod(
                "android.media.AudioTrack", cl, "getTimestamp",
                android.media.AudioTimestamp.class,
                new XC_MethodHook() {
                    protected void afterHookedMethod(MethodHookParam p) {
                        if (!(boolean) p.getResult()) return;
                        android.media.AudioTimestamp ts =
                            (android.media.AudioTimestamp) p.args[0];
                        if (ts != null) {
                            ts.nanoTime /= MULTIPLIER;
                            ts.framePosition /= MULTIPLIER;
                        }
                    }
                }
            );

            XposedHelpers.findAndHookMethod(
                "android.media.AudioTrack", cl, "getPlaybackHeadPosition",
                new XC_MethodHook() {
                    protected void afterHookedMethod(MethodHookParam p) {
                        p.setResult(((int) p.getResult()) / (int) MULTIPLIER);
                    }
                }
            );

        } catch (Throwable t) {
            android.util.Log.e("BattleHook", "Hook failed", t);
        }
    }
}
