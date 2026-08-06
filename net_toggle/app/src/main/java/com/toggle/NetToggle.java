package com.toggle;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.concurrent.Executors;

public class NetToggle extends Service {

    private WindowManager wm;
    private View floatView;
    private Switch sw;
    private String uid;
    private static final String PKG = "com.lhpand.zjws";
    private final java.util.concurrent.ExecutorService exec =
        Executors.newSingleThreadExecutor();

    @Override
    public IBinder onBind(Intent i) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                "net", "网络控制", NotificationManager.IMPORTANCE_MIN);
            ch.setDescription("");
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
            startForeground(1, new Notification.Builder(this, "net")
                .setContentTitle("NetToggle").setSmallIcon(android.R.drawable.ic_menu_manage).build());
        }

        try {
            uid = String.valueOf(getPackageManager()
                .getApplicationInfo(PKG, 0).uid);
        } catch (PackageManager.NameNotFoundException e) {
            uid = "10250";
        }

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        floatView = LayoutInflater.from(this).inflate(R.layout.float_view, null);
        sw = floatView.findViewById(R.id.switch1);
        View close = floatView.findViewById(R.id.close);

        sw.setOnCheckedChangeListener((btn, on) -> {
            exec.execute(() -> {
                sh("iptables -D OUTPUT -m owner --uid-owner " + uid + " -j DROP 2>/dev/null");
                if (on) sh("iptables -A OUTPUT -m owner --uid-owner " + uid + " -j DROP");
            });
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
        exec.execute(() -> sh("iptables -D OUTPUT -m owner --uid-owner " + uid + " -j DROP 2>/dev/null"));
        exec.shutdown();
        super.onDestroy();
    }

    private void sh(String cmd) {
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
