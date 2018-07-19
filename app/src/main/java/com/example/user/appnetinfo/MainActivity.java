package com.example.user.appnetinfo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.json.JSONException;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    AppNetInfoUtil.get().getAppNetInfos(MainActivity.this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }
}
