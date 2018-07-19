package com.example.user.appnetinfo;

import android.Manifest;
import android.app.AppOpsManager;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.TrafficStats;
import android.os.Build;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.app.usage.NetworkStats.Bucket.UID_REMOVED;
import static android.app.usage.NetworkStats.Bucket.UID_TETHERING;

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
    public String getAppNetInfos(Context context) throws JSONException, RemoteException {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return "";
        }
        String subId = tm.getSubscriberId();
        List<ApplicationInfo> apps = getInstallApps(context);
        JSONObject obj = new JSONObject();
        if (apps == null || apps.size() == 0) {
            Log.e("AppNetInfoUtil", "当前设备没有安装任何第三方软件");
            obj.putOpt("apps", new JSONArray());
        } else {
            JSONArray array = new JSONArray();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && hasPermissionToReadNetworkStats(context)) {
                NetworkStats.Bucket bucket = new NetworkStats.Bucket();
                NetworkStatsManager manager = (NetworkStatsManager) context.getSystemService(Context.NETWORK_STATS_SERVICE);
                // 获取到目前为止设备的手机流量统计
                bucket = manager.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE, subId, 0, System.currentTimeMillis());
                // wifi上传总流量
                obj.put("wifi_upload", bucket.getTxBytes());
                // wifi下载的总流量
                obj.put("wifi_download", bucket.getRxBytes());
                // 获取到目前为止设备的手机流量统计
                bucket = manager.querySummaryForDevice(ConnectivityManager.TYPE_WIFI, subId, 0, System.currentTimeMillis());
                //获取手机3g/2g网络上传的总流量
                obj.put("mobile_upload", bucket.getTxBytes());
                //手机2g/3g下载的总流量
                obj.put("mobile_download", bucket.getRxBytes());
                // 根据uid 来获取对应的app的流量信息
                NetworkStats mobileState = manager.querySummary(ConnectivityManager.TYPE_MOBILE, subId, 0, System.currentTimeMillis());
                NetworkStats wifiState = manager.querySummary(ConnectivityManager.TYPE_WIFI, subId, 0, System.currentTimeMillis());
                HashMap<Integer, JSONObject> map = new HashMap<>();
                for (ApplicationInfo app : apps) {
                    JSONObject appNet = new JSONObject();
                    appNet.put("app", app.packageName);
                    map.put(app.uid, appNet);
                    Log.e("app", "pName -> " + app.packageName + ", uid -> " + app.uid);
                }
                while (mobileState.hasNextBucket()) {
                    mobileState.getNextBucket(bucket);
                    if (!isSkipUid(bucket.getUid())) {
                        JSONObject json = map.get(bucket.getUid());
                        Log.e("app", "mobile state -> " + bucket.getUid() + ", tag:" + bucket.getTag());
                        if (json == null) {
                            json = new JSONObject();
                            json.put("uid", bucket.getUid());
                        }
                        json.put("mobile_upload", json.optLong("mobile_upload") + bucket.getTxBytes());
                        json.put("mobile_download", json.optLong("mobile_download") + bucket.getRxBytes());
                        map.put(bucket.getUid(), json);
                    }
                }
                while (wifiState.hasNextBucket()) {
                    wifiState.getNextBucket(bucket);
                    JSONObject json = map.get(bucket.getUid());
                    if (json == null) {
                        json = new JSONObject();
                    }
                    json.put("wifi_upload", json.optLong("wifi_upload") + bucket.getTxBytes());
                    json.put("wifi_download", json.optLong("wifi_download") + bucket.getRxBytes());
                    map.put(bucket.getUid(), json);
                }
                for (Map.Entry<Integer, JSONObject> entry : map.entrySet()) {
                    if (!isSkipUid(entry.getKey())) {
                        array.put(entry.getValue());
                    }
                }
                Log.e("app", "app 带有数据个数->" + array.length());
                obj.put("apps", array);
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
                obj.put("apps", array);
                //获取手机3g/2g网络上传的总流量
                obj.put("mobile_upload", TrafficStats.getMobileTxBytes());
                //手机2g/3g下载的总流量
                obj.put("mobile_download", TrafficStats.getMobileRxBytes());
                // 手机全部网络接口 包括wifi，3g、2g上传的总流量
                obj.put("upload", TrafficStats.getTotalTxBytes());
                // 手机全部网络接口 包括wifi，3g、2g下载的总流量
                obj.put("download", TrafficStats.getTotalRxBytes());
            }
        }
        return obj.toString();
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

    private boolean hasPermissionToReadNetworkStats(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        final AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), context.getPackageName());
        if (mode == AppOpsManager.MODE_ALLOWED) {
            return true;
        }
        // 打开“有权查看使用情况的应用”页面
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        context.startActivity(intent);
        return false;
    }

    public static long getTimesMonthMorning() {
        Calendar cal = Calendar.getInstance();
        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
        return cal.getTimeInMillis();
    }

    public static int getUidByPackageName(Context context, String packageName) {
        int uid = -1;
        PackageManager packageManager = context.getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA);

            uid = packageInfo.applicationInfo.uid;
            Log.i(MainActivity.class.getSimpleName(), packageInfo.packageName + " uid:" + uid);


        } catch (PackageManager.NameNotFoundException e) {
        }

        return uid;
    }

    private boolean isSkipUid(int uid) {
        return uid == UID_REMOVED || uid == UID_TETHERING || uid == android.os.Process.SYSTEM_UID;
    }
}
