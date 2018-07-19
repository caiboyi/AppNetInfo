package com.example.user.appnetinfo;

import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.TrafficStats;
import android.os.Build;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class AppNetInfoUtil {

    private static AppNetInfoUtil instance;

    public static synchronized AppNetInfoUtil get() {
        if (instance == null) {
            synchronized (AppNetInfoUtil.class) {
                if (instance == null) {
                    instance = new AppNetInfoUtil();
                }
            }
        }
        return instance;
    }

    private AppNetInfoUtil() {
    }

    /**
     * TrafficStats Android n 返回-1
     *
     * @param context
     * @throws JSONException
     */
    public void getAppNetInfos(Context context) throws JSONException, RemoteException {
        List<ApplicationInfo> apps = getInstallApps(context);
        JSONObject obj = new JSONObject();
        if (apps == null || apps.size() == 0) {
            Log.e("AppNetInfoUtil", "当前设备没有安装任何第三方软件");
            obj.putOpt("apps", new JSONArray());
        } else {
            JSONArray array = new JSONArray();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                NetworkStatsManager manager = (NetworkStatsManager) context.getSystemService(Context.NETWORK_STATS_SERVICE);
                NetworkStats.Bucket bucket = null;
                // 获取到目前为止设备的手机流量统计
                bucket = manager.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE, "", 0, System.currentTimeMillis());
                // wifi上传总流量
                obj.put("wifi_upload", bucket.getTxBytes());
                // wifi下载的总流量
                obj.put("wifi_download", bucket.getRxBytes());
                // 获取到目前为止设备的手机流量统计
                bucket = manager.querySummaryForDevice(ConnectivityManager.TYPE_WIFI, "", 0, System.currentTimeMillis());
                //获取手机3g/2g网络上传的总流量
                obj.put("mobile_upload", bucket.getTxBytes());
                //手机2g/3g下载的总流量
                obj.put("mobile_download", bucket.getRxBytes());
                for (ApplicationInfo app : apps) {
                    if (TextUtils.equals("com.jiaoliuqu.lsp", app.packageName)) {
                        NetworkStats states = manager.queryDetailsForUid(ConnectivityManager.TYPE_MOBILE, "", 0, System.currentTimeMillis(), app.uid);
                        long[] mobileInfos = getNetInfo(states);
                        NetworkStats states1 = manager.queryDetailsForUid(ConnectivityManager.TYPE_WIFI, "", 0, System.currentTimeMillis(), app.uid);
                        long[] wifiInfos = getNetInfo(states1);
                        JSONObject appNet = new JSONObject();
                        appNet.put("app", app.packageName);
                        appNet.put("mobile_upload", mobileInfos[0]);
                        appNet.put("mobile_download", mobileInfos[1]);
                        appNet.put("wifi_upload", wifiInfos[0]);
                        appNet.put("wifi_download", wifiInfos[1]);
                        Log.e("app", appNet.toString());
                        array.put(appNet);
                    }
                }
            } else {
                for (ApplicationInfo app : apps) {
                    // 上传流量
                    long upload = TrafficStats.getUidTxBytes(app.uid);
                    // 下载流量
                    long download = TrafficStats.getUidRxBytes(app.uid);
                    JSONObject appNet = new JSONObject();
                    appNet.put("app", app.packageName);
                    appNet.put("upload", upload);
                    appNet.put("download", download);
                    Log.e("app", appNet.toString());
                    array.put(appNet);
                }
                //获取手机3g/2g网络上传的总流量
                obj.put("mobile_upload", TrafficStats.getMobileTxBytes());
                //手机2g/3g下载的总流量
                obj.put("mobile_download", TrafficStats.getMobileRxBytes());
                // 手机全部网络接口 包括wifi，3g、2g上传的总流量
                obj.put("upload", TrafficStats.getTotalTxBytes());
                // 手机全部网络接口 包括wifi，3g、2g下载的总流量
                obj.put("download", TrafficStats.getTotalRxBytes());
            }
            //obj.putOpt("apps", array);
        }
        Log.e("app", obj.toString());
    }

    private long[] getNetInfo(NetworkStats states) {
        NetworkStats.Bucket bucket = null;
        long[] points = new long[2];
        points[0] = points[1] = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            while (states.hasNextBucket()) {
                states.getNextBucket(bucket);
                if (bucket != null) {
                    points[0] += bucket.getTxBytes();
                    points[0] += bucket.getRxBytes();
                }
            }
        }
        return points;
    }

    private List<ApplicationInfo> getInstallApps(Context context) {
        PackageManager manager = context.getPackageManager();
        return manager.getInstalledApplications(PackageManager.GET_META_DATA);
    }


}
