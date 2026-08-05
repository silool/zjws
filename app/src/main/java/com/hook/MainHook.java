package com.hook;

import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.net.InetAddress;

/**
 * DNS拦截 — 反作弊域名 → 127.0.0.1
 * 覆盖 native SO 直连
 */
public class MainHook implements IXposedHookLoadPackage {

    private static final String[] BLOCK = {
        "modtext.tiandivip.cc",
        "modgameapi.tiandivip.cc",
        "modgameapi.lovetom.top"
    };

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {
        if (!lp.packageName.equals("com.lhpand.zjws")) return;

        try {
            XposedHelpers.findAndHookMethod(
                InetAddress.class, "getAllByName", String.class,
                new XC_MethodHook() {
                    protected void beforeHookedMethod(MethodHookParam p) {
                        try {
                            String host = (String) p.args[0];
                            if (host == null) return;
                            for (String b : BLOCK) {
                                if (host.contains(b)) {
                                    p.setResult(new InetAddress[]{
                                        InetAddress.getByAddress(new byte[]{127,0,0,1})
                                    });
                                    return;
                                }
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            );
        } catch (Throwable t) {
            android.util.Log.e("DNSCut", "fail", t);
        }
    }
}
