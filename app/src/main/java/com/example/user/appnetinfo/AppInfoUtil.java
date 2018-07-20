package com.example.user.appnetinfo;

import android.Manifest;
import android.app.AppOpsManager;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.SimpleFormatter;

import static android.app.usage.NetworkStats.Bucket.UID_REMOVED;
import static android.app.usage.NetworkStats.Bucket.UID_TETHERING;

public class AppInfoUtil {

    private static AppInfoUtil instance;

    public static synchronized AppInfoUtil get() {
        if (instance == null) {
            synchronized (AppInfoUtil.class) {
                if (instance == null) {
                    instance = new AppInfoUtil();
                }
            }
        }
        return instance;
    }

    private AppInfoUtil() {
    }

    /**
     * 获取手机设备里面安装的app的流量信息
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
            Log.e("AppNetInfoUtil", "当前设备没有安装任何软件");
            obj.putOpt("apps", new JSONArray());
        } else {
            JSONArray array = new JSONArray();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && hasPermissionToReadNetworkStats(context)) {
                NetworkStats.Bucket bucket = new NetworkStats.Bucket();
                NetworkStatsManager manager = (NetworkStatsManager) context.getSystemService(Context.NETWORK_STATS_SERVICE);
                // 获取到目前为止设备的手机流量统计
                bucket = manager.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE, subId, 0, System.currentTimeMillis());
                // wifi上传总流量
                obj.put("mobile_upload", bucket.getTxBytes());
                // wifi下载的总流量
                obj.put("mobile_download", bucket.getRxBytes());
                // 获取到目前为止设备的手机流量统计
                bucket = manager.querySummaryForDevice(ConnectivityManager.TYPE_WIFI, subId, 0, System.currentTimeMillis());
                //获取手机3g/2g网络上传的总流量
                obj.put("wifi_upload", bucket.getTxBytes());
                //手机2g/3g下载的总流量
                obj.put("wifi_download", bucket.getRxBytes());
                // 根据uid 来获取对应的app的流量信息
                NetworkStats mobileState = manager.querySummary(ConnectivityManager.TYPE_MOBILE, subId, 0, System.currentTimeMillis());
                NetworkStats wifiState = manager.querySummary(ConnectivityManager.TYPE_WIFI, subId, 0, System.currentTimeMillis());
                HashMap<Integer, JSONObject> map = new HashMap<>();
                for (ApplicationInfo app : apps) {
                    JSONObject appNet = new JSONObject();
                    appNet.put("app", app.packageName);
                    map.put(app.uid, appNet);
                }
                while (mobileState.hasNextBucket()) {
                    mobileState.getNextBucket(bucket);
                    if (!isSkipUid(bucket.getUid())) {
                        JSONObject json = map.get(bucket.getUid());
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

    /**
     * 获取手机里面安装的app的信息
     *
     * @param context
     * @return
     * @throws JSONException
     */
    public JSONArray getInstallAppInfo(Context context) throws JSONException {
        PackageManager pm = context.getPackageManager();
        JSONArray array = new JSONArray();
        //List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        List<PackageInfo> apps = pm.getInstalledPackages(PackageManager.GET_META_DATA);
        for (PackageInfo app : apps) {
            JSONObject obj = new JSONObject();
            obj.put("package_name", app.packageName);
            obj.put("version_name", app.versionName);
            obj.put("version_code", app.versionCode);
            obj.put("firstInstallTime", app.firstInstallTime);
            obj.put("singInfo", getSingInfo(app.signingInfo.getApkContentsSigners()));
        }
        return array;
    }

    /**
     * 获取app的使用频率,频率按月查询
     */
    public JSONArray getAppUsageState(Context context) throws JSONException {
        JSONArray array = new JSONArray();
        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            UsageStatsManager manager = (UsageStatsManager) context.getApplicationContext()
                    .getSystemService(Context.USAGE_STATS_SERVICE);
            Calendar mCalendar = Calendar.getInstance();
            mCalendar.set(Calendar.HOUR, 0);
            mCalendar.set(Calendar.MINUTE, 0);
            mCalendar.set(Calendar.SECOND, 0);
            mCalendar.set(Calendar.MILLISECOND, 0);
            List<UsageStats> usageStatsList = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,
                    mCalendar.getTimeInMillis(), System.currentTimeMillis());
            HashMap<String, JSONObject> map = new HashMap<>();
            if (usageStatsList != null && usageStatsList.size() > 0) {
                for (UsageStats stats : usageStatsList) {
                    JSONObject obj = map.get(stats.getPackageName());
                    if (obj == null) {
                        obj = new JSONObject();
                    }
                    obj.put("package_name", stats.getPackageName());
                    if (obj.optLong("first_time") < stats.getFirstTimeStamp()) {
                        obj.put("start_time", stats.getFirstTimeStamp());
                        obj.put("end_time", stats.getLastTimeStamp());
                        obj.put("total_time", stats.getTotalTimeInForeground());
                    }
                    obj.put("count", obj.optInt("count") + 1);
                    map.put(stats.getPackageName(), obj);
                }
                for (JSONObject o : map.values()) {
                    o.put("start_time", dayFormat.format(new Date(o.optLong("start_time"))));
                    o.put("end_time", dayFormat.format(new Date(o.optLong("end_time"))));
                    o.put("total_time", formatTime(o.optLong("total_time")));
                    array.put(o);
                }
            }
        }
        return array;
    }

    /**
     * 获取app的签名信息
     */
    private String getSingInfo(Signature[] signs) {
        String tmp = null;
        for (Signature sig : signs) {
            tmp = getSignatureString(sig, "SHA1");
            break;
        }
        return tmp;
    }

    private List<ApplicationInfo> getInstallApps(Context context) {
        PackageManager manager = context.getPackageManager();
        return manager.getInstalledApplications(PackageManager.GET_META_DATA);
    }

    public boolean hasPermissionToReadNetworkStats(Context context) {
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

    private static long getTimesMonthMorning() {
        Calendar cal = Calendar.getInstance();
        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
        return cal.getTimeInMillis();
    }

    private static int getUidByPackageName(Context context, String packageName) {
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

    /**
     * 获取相应的类型的字符串（把签名的byte[]信息转换成16进制）
     */
    private String getSignatureString(Signature sig, String type) {
        byte[] hexBytes = sig.toByteArray();
        String fingerprint = "error!";
        try {
            MessageDigest digest = MessageDigest.getInstance(type);
            if (digest != null) {
                byte[] digestBytes = digest.digest(hexBytes);
                StringBuilder sb = new StringBuilder();
                for (byte digestByte : digestBytes) {
                    sb.append((Integer.toHexString((digestByte & 0xFF) | 0x100)).substring(1, 3));
                }
                fingerprint = sb.toString();
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return fingerprint;
    }

    /**
     * 格式化使用的时长
     *
     * @param time
     * @return
     */
    private String formatTime(long time) {
        if (time < 1000) {
            return "1秒";
        }
        time /= 1000;
        if (time < 60) {
            // 1分钟以内
            return time + "秒";
        } else if (time < 3600) {
            //1小时以内
            long minute = time / 60;
            long second = time % 60;
            return minute + "分" + second+"秒";
        } else if (time < 24 * 3600) {
            // 1天以内
            long hour = time / 3600;
            long rightTime = time % 3600;
            long minute = rightTime / 60;
            long second = rightTime % 60;
            return hour + "时" + minute + "分" + second+"秒";
        } else {
            long day = time / (24 * 3600);
            return day + "天";
        }
    }

}
