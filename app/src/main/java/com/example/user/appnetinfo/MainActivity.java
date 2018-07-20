package com.example.user.appnetinfo;

import android.Manifest;
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
                            text.setText(array.toString());
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

}
