package com.example.user.appnetinfo;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int CODE = 1001;
    private TextView text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text = findViewById(R.id.text);
        try {
            JSONArray array = ContactInfoUtil.get().getCallLogInfo(this);
            Log.e("app", array != null ? array.toString() : "array is null");
            text.setText(array.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkPermission()) {
            getAppNetInfo();
        }
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            this.requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, CODE);
            return false;
        }
        return AppInfoUtil.get().hasPermissionToReadNetworkStats(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CODE) {
            if (grantResults != null && grantResults.length > 0) {
                int grantedSize = 0;
                for (int item : grantResults) {
                    if (item == PackageManager.PERMISSION_GRANTED) {
                        grantedSize++;
                    }
                }
                if (grantedSize == grantResults.length) {
                    getAppNetInfo();
                }
            }
        }
    }

    private void getAppNetInfo() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    final JSONArray array = AppInfoUtil.get().getAppUsageState(MainActivity.this);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
//                            text.setText(array.toString());
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }


    private void getRecentActivies() throws JSONException {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            List<ActivityManager.AppTask> data = manager.getAppTasks();
            if (data != null && data.size() > 0) {
                for (ActivityManager.AppTask task : data) {
                    StringBuilder sb = new StringBuilder();
                    ActivityManager.RecentTaskInfo info = task.getTaskInfo();
                    sb.append(info.affiliatedTaskId).append(", ").append(info.id);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (info.baseActivity != null) {
                            sb.append(", ").append(info.baseActivity.getPackageName());
                        }
                    }
                    Log.e("task", sb.toString());
                }


            }
        }

        List<ActivityManager.RecentTaskInfo> apps = manager.getRecentTasks(128, ActivityManager.RECENT_IGNORE_UNAVAILABLE);
        // 获取当前最近使用的app_IGNORE_UNAVAILABLE);
        if (apps != null && apps.size() > 0) {
            for (ActivityManager.RecentTaskInfo app : apps) {
                JSONObject obj = new JSONObject();
                obj.put("package_name", app.baseIntent.getComponent().getPackageName());
                obj.put("open_activities", app.numActivities);
                Log.e("app", obj.toString());
            }
        }

        List<ActivityManager.RunningAppProcessInfo> list = manager.getRunningAppProcesses();
        if (list != null && list.size() > 0) {
            for (ActivityManager.RunningAppProcessInfo task : list) {
                Log.e("tag", "pkg_name -> " + task.processName + ", ");
            }
        }
    }

}
