package com.example.user.appnetinfo;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.Formatter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * 获取手机的设备信息
 */
public class DeviceInfoUtil {
    private static final int INTERNAL_STORAGE = 0;
    private static final int EXTERNAL_STORAGE = 1;
    private static final String DEFAULT = "unknow";

    private static DeviceInfoUtil instance;

    public static synchronized DeviceInfoUtil get() {
        if (instance == null) {
            synchronized (DeviceInfoUtil.class) {
                if (instance == null) {
                    instance = new DeviceInfoUtil();
                }
            }
        }
        return instance;
    }

    private DeviceInfoUtil() {
    }

    public JSONObject getMobileDeviceInfo(Context context) throws JSONException {
        JSONObject obj = new JSONObject();
        //手机主板名
        obj.put("board", Build.BOARD);
        //手机品牌
        obj.put("brand", Build.BRAND);
        // 手机型号
        obj.put("model", Build.MODEL);
        // 设备名
        obj.put("device", Build.DEVICE);
        // 手机厂商
        obj.put("manufacturer", Build.MANUFACTURER);
        // 商品名
        obj.put("product", Build.PRODUCT);
        // 获取手机的硬件序列号
        obj.put("device_serial", Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? Build.getSerial() : Build.SERIAL);
        obj.put("display", Build.DISPLAY);
        obj.put("fingerprint", Build.FINGERPRINT);
        obj.put("hardware", Build.HARDWARE);
        obj.put("host", Build.HOST);
        obj.put("id", Build.ID);
        obj.put("tags", Build.TAGS);
        obj.put("time", Build.TIME);
        obj.put("user", Build.USER);
        obj.put("cpu_abi", Build.CPU_ABI);
        obj.put("apu_abi2", Build.CPU_ABI2);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            obj.put("supported_32_bit_abis", Build.SUPPORTED_32_BIT_ABIS);
            obj.put("supported_64_bit_abis", Build.SUPPORTED_64_BIT_ABIS);
            obj.put("supported_abis", Build.SUPPORTED_ABIS);
        }
        // 设备IMEI
        obj.put("imei", getIMEI(context));
        // android SDK 版本号
        obj.put("android_sdk", Build.VERSION.SDK_INT);
        // android版本
        obj.put("android_release", Build.VERSION.RELEASE);
        // 默认系统语言
        obj.put("default_language", Locale.getDefault().getLanguage());
        // 国家
        obj.put("country", context.getResources().getConfiguration().locale.getCountry());
        // 设备宽度
        obj.put("screen_width", context.getResources().getDisplayMetrics().widthPixels);
        //设备高度
        obj.put("screen_height", context.getResources().getDisplayMetrics().heightPixels);
        obj.put("density", context.getResources().getDisplayMetrics().density);

        // sd是否挂载
        obj.put("sd_state", isSDMount());
        // sd使用情况
        String internalSDUseInfo = getSDInfo(context, INTERNAL_STORAGE);
        String externalSDUseInfo = getSDInfo(context, EXTERNAL_STORAGE);
        if (!TextUtils.isEmpty(internalSDUseInfo)) {
            obj.put("internal_sd_use_info", internalSDUseInfo);
        }
        if (TextUtils.isEmpty(externalSDUseInfo)) {
            obj.put("external_sd_use_info", internalSDUseInfo);
        }
        // SIM卡信息
        HashMap<String, String> simMap = getSIMInfo(context);
        if (simMap != null && simMap.size() > 0) {
            for (String key : simMap.keySet()) {
                obj.put(key, simMap.get(key));
            }
        }
        obj.put("mac_address", getMacAddress(context));
        obj.put("ip_address", getIpAddress(context));
        return obj;
    }

    /**
     * 获取手机的IMEI
     */
    private String getIMEI(Context context) {
        TelephonyManager manager = (TelephonyManager) context.getApplicationContext().
                getSystemService(Context.TELEPHONY_SERVICE);
        String imei = manager.getDeviceId();
        if (imei == null) {
            imei = DEFAULT;
        }
        return imei;
    }

    /**
     * 获取SIM卡的相关信息
     *
     * @param context
     * @return
     */
    private HashMap<String, String> getSIMInfo(Context context) {
        TelephonyManager manager = (TelephonyManager) context.getApplicationContext().
                getSystemService(Context.TELEPHONY_SERVICE);
        HashMap<String, String> map = new HashMap();
        String simSerialNumber = manager.getSimSerialNumber();
        String imsi = manager.getSubscriberId();
        String line1Number = manager.getLine1Number();
        // 运营商编号
        String networkOperator = manager.getNetworkOperator();
        // 运营商名称
        String networkOperatorName = manager.getNetworkOperatorName();
        // 获取国家/区域编号
        String simCountryIso = manager.getSimCountryIso();
        // 网络运营商的国家编号和移动网络编号
        String simOperator = manager.getSimOperator();
        //服务提供者名称
        String simOperatorName = manager.getSimOperatorName();
//        //当前订阅的运营商ID
//        int simCarrierId = manager.getSimCarrierId();
//        //当前订阅的运营商ID名称。
//        CharSequence simCarrierIdName = manager.getSimCarrierIdName();
        map.put("sim_state", manager.getSimState() == TelephonyManager.SIM_STATE_READY ? "ready" : "error");
        map.put("simSerialNumber", TextUtils.isEmpty(simSerialNumber) ? DEFAULT : simSerialNumber);
        map.put("imsi", TextUtils.isEmpty(imsi) ? DEFAULT : imsi);
        map.put("line1Number", TextUtils.isEmpty(line1Number) ? DEFAULT : line1Number);
        map.put("networkOperator", TextUtils.isEmpty(networkOperator) ? DEFAULT : networkOperator);
        map.put("networkOperatorName", TextUtils.isEmpty(networkOperatorName) ? DEFAULT : networkOperatorName);
        map.put("simCountryIso", TextUtils.isEmpty(simCountryIso) ? DEFAULT : simCountryIso);
        map.put("simOperator", TextUtils.isEmpty(simOperator) ? DEFAULT : simOperator);
        map.put("simOperatorName", TextUtils.isEmpty(simOperatorName) ? DEFAULT : simOperatorName);
//        map.put("simCarrierId", String.valueOf(simCarrierId));
//        map.put("simCarrierIdName", TextUtils.isEmpty(simCarrierIdName) ? DEFAULT : simCarrierIdName.toString());
        return map;
    }


    /**
     * sd卡是否挂载
     *
     * @return
     */
    private boolean isSDMount() {
        return TextUtils.equals(Environment.MEDIA_MOUNTED, Environment.getExternalStorageState());
    }

    /**
     * 获取sd卡的使用信息
     *
     * @param context
     * @param sdType
     * @return
     */
    private String getSDInfo(Context context, int sdType) {
        String path = getStoragePath(context, sdType);
        if (!isSDMount() || TextUtils.isEmpty(path)) {
            return "无外置SD卡";
        }
        File file = new File(path);
        StatFs statFs = new StatFs(file.getPath());
        long totalSpace = 0, avaliableSpacve = 0;
        StringBuilder sb = new StringBuilder();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            totalSpace = statFs.getBlockCountLong() * statFs.getBlockSizeLong();
            avaliableSpacve = statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong();
            sb.append("可用/总共:").append(Formatter.formatFileSize(context, avaliableSpacve))
                    .append("/").append(Formatter.formatFileSize(context, totalSpace));
        }
        return sb.toString();
    }

    /**
     * 获取指定类别的存储路径
     *
     * @param context
     * @param type
     * @return
     */
    private String getStoragePath(Context context, int type) {
        StorageManager manager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        try {
            Method method = manager.getClass().getMethod("getVolumePaths");
            String[] paths = (String[]) method.invoke(manager);
            switch (type) {
                case INTERNAL_STORAGE:
                    return paths[type];
                case EXTERNAL_STORAGE:
                    if (paths.length > 1) {
                        return paths[type];
                    }
                default:
                    break;
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;

    }

    /**
     * 获取mac地址
     *
     * @param context
     * @return
     */
    private String getMacAddress(Context context) {
        WifiManager manager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo currentConnectInfo = manager.getConnectionInfo();
        String macAddress = "02:00:00:00:00:00";
        if (currentConnectInfo != null) {
            if (TextUtils.equals(macAddress, currentConnectInfo.getMacAddress())) {
                macAddress = getMacAddrByIpv6();
            }
        } else {
            macAddress = getMacAddrByIpv6();
        }
        return macAddress;
    }

    private String getMacAddrByIpv6() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;
                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }
                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(Integer.toHexString(b & 0xFF) + ":");
                }
                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    private String getIpAddress(Context context) {
        WifiManager manager = (WifiManager) context.getApplicationContext().getSystemService(
                Context.WIFI_SERVICE);
        WifiInfo currentConnectInfo = manager.getConnectionInfo();
        if (currentConnectInfo != null) {
            int i = currentConnectInfo.getIpAddress();
            StringBuilder sb = new StringBuilder();
            sb.append(i & 0xFF).append(".").append((i >> 8) & 0xFF).append(".")
                    .append((i >> 16) & 0xFF).append(i >> 24 & 0xFF);
            return sb.toString();
        }
        return DEFAULT;
    }

}
