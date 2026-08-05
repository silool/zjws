package com.hook;

import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {
        if (!lp.packageName.equals("com.lhpand.zjws")) return;
        android.util.Log.e("TimFix", "Java hook loaded");
        try {
            System.loadLibrary("timfix");
            android.util.Log.e("TimFix", "Native lib loaded OK");
        } catch (Throwable t) {
            android.util.Log.e("TimFix", "Native load FAILED", t);
        }
    }
}
