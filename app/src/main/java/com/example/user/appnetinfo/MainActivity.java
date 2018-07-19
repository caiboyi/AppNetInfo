package com.example.user.appnetinfo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import org.json.JSONException;

public class MainActivity extends AppCompatActivity {
    private TextView text;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text = findViewById(R.id.text);
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    final String data = AppNetInfoUtil.get().getAppNetInfos(MainActivity.this);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            text.setText(data);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }
}
