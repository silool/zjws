package com.toggle;

import android.app.*;
import android.content.*;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.io.*;

public class NetToggle extends Service {

    private WindowManager wm;
    private View floatView;
    private Switch sw;
    private static final String UID = "10250"; // com.lhpand.zjws

    @Override
    public IBinder onBind(Intent i) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        // 悬浮窗布局
        floatView = LayoutInflater.from(this).inflate(R.layout.float_view, null);
        sw = floatView.findViewById(R.id.switch1);
        ImageView close = floatView.findViewById(R.id.close);

        sw.setOnCheckedChangeListener((btn, on) -> {
            exec("iptables -D OUTPUT -m owner --uid-owner " + UID + " -j DROP 2>/dev/null");
            if (on) exec("iptables -A OUTPUT -m owner --uid-owner " + UID + " -j DROP");
        });

        close.setOnClickListener(v -> stopSelf());

        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.START;
        p.x = 50; p.y = 200;
        wm.addView(floatView, p);
    }

    @Override
    public void onDestroy() {
        if (floatView != null) wm.removeView(floatView);
        // 退出时恢复网络
        exec("iptables -D OUTPUT -m owner --uid-owner " + UID + " -j DROP 2>/dev/null");
        super.onDestroy();
    }

    private void exec(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            p.waitFor();
        } catch (Exception ignored) {}
    }

    @Override
    public int onStartCommand(Intent i, int flags, int id) {
        return START_STICKY;
    }
}
