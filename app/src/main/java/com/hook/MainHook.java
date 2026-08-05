package com.hook;

import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {
        android.util.Log.e("TimFix", "PKG: " + lp.packageName);
        if (!lp.packageName.equals("com.lhpand.zjws")) return;
        android.util.Log.e("TimFix", "MATCHED — loading native...");
        try {
            System.loadLibrary("timfix");
            android.util.Log.e("TimFix", "OK");
        } catch (Throwable t) {
            android.util.Log.e("TimFix", "FAIL: " + t.getMessage());
        }
    }
}
