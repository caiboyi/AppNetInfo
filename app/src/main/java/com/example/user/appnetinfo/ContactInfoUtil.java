package com.example.user.appnetinfo;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.support.v4.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.SimpleFormatter;

/**
 * 读取手机的联系人信息
 */
public class ContactInfoUtil {

    private static ContactInfoUtil instance;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);

    public static synchronized ContactInfoUtil get() {
        if (instance == null) {
            synchronized (ContactInfoUtil.class) {
                if (instance == null) {
                    instance = new ContactInfoUtil();
                }
            }
        }
        return instance;
    }

    private ContactInfoUtil() {
    }

    public JSONArray getContactInfo(Context context) throws JSONException {
        JSONArray array = new JSONArray();
        ContentResolver resolver = context.getContentResolver();
        Cursor cursorContacts = resolver.query(ContactsContract.Contacts.CONTENT_URI, null,
                null, null, null);
        if (cursorContacts != null) {
            while (cursorContacts.moveToNext()) {
                JSONObject obj = new JSONObject();
                int contactId = cursorContacts.getInt(cursorContacts.getColumnIndex(
                        ContactsContract.Contacts._ID));
                obj.put("contactId", contactId);
                String name = cursorContacts.getString(cursorContacts.getColumnIndex(
                        ContactsContract.Contacts.DISPLAY_NAME));
                obj.put("name", name);
                int contactCount = cursorContacts.getInt(cursorContacts.getColumnIndex(ContactsContract.Contacts.TIMES_CONTACTED));
                obj.put("contactCount", contactCount);
                long lastContactTimeStamp = cursorContacts.getLong(cursorContacts.getColumnIndex(ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP));
                obj.put("lastContactTimeStamp", dateFormat.format(new Date(lastContactTimeStamp)));
                // 查询电话号码
                String[] phone = getData(resolver, ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + contactId,
                        ContactsContract.CommonDataKinds.Phone.NUMBER);
                obj.put("phone", (phone != null && phone.length > 0) ? phone[0] : "");
                // 查询邮箱
                String[] email = getData(resolver, ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + contactId,
                        ContactsContract.CommonDataKinds.Email.ADDRESS);
                obj.put("email", (email != null && email.length > 0) ? email[0] : "");
                // 查询公司和职位
                StringBuilder sb = new StringBuilder();
                sb.append(ContactsContract.Data.CONTACT_ID).append("=").append(contactId);
                String[] companyInfo = getData(resolver, ContactsContract.Data.CONTENT_URI, sb.toString(),
                        ContactsContract.CommonDataKinds.Organization.DATA,
                        ContactsContract.CommonDataKinds.Organization.TITLE);
                if (companyInfo != null && companyInfo.length > 0) {
                    obj.put("company", companyInfo.length >= 1 ? companyInfo[0] : "");
                    obj.put("job_title", companyInfo.length >= 2 ? companyInfo[1] : "");
                }
                array.put(obj);
            }
        }
        return array;
    }

    private String[] getData(ContentResolver resolver, Uri uri, String whereArgs, String... columNames) {
        String[] array = null;
        if (columNames == null || columNames.length == 0) {
            return array;
        }
        Cursor cursor = resolver.query(uri, null, whereArgs, null, null);
        if (cursor != null) {
            int size = columNames.length;
            int pos = 0;
            array = new String[size];
            while (cursor.moveToNext()) {
                array[pos] = cursor.getString(cursor.getColumnIndex(columNames[pos]));
                pos++;
            }
            cursor.close();
        }
        return array;
    }

    /**
     * 可以获取更完整的联系人信息
     *
     * @param context
     * @return
     * @throws JSONException
     */
    public JSONArray getContactInfo1(Context context) throws JSONException {
        JSONArray array = new JSONArray();
        ContentResolver resolver = context.getContentResolver();

        Cursor cursorData = resolver.query(ContactsContract.Data.CONTENT_URI, null,
                null, null, ContactsContract.Data.RAW_CONTACT_ID);
        while (cursorData.moveToNext()) {
            JSONObject obj = new JSONObject();
            int rawContactId = cursorData.getInt(cursorData.getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID));
            int contactId = cursorData.getInt(cursorData.getColumnIndex(ContactsContract.Data.CONTACT_ID));

            obj.put("rawContactId", rawContactId);
            obj.put("contactId", contactId);
            String mimeType = cursorData.getString(cursorData.getColumnIndex(ContactsContract.Data.MIMETYPE));
            if (ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE.equals(mimeType)) {
                String name = cursorData.getString(cursorData.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME));
                obj.put("name", name);
            }
            if (ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE.equals(mimeType)) {
                String phone = cursorData.getString(cursorData.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                obj.put("phone", phone);
            }
            array.put(obj);
        }
        return array;
    }

    public JSONArray getSMSInfo(Context context) throws JSONException {
        JSONArray array = new JSONArray();
        ContentResolver resolver = context.getContentResolver();
        Uri uri = Uri.parse("content://sms/");
        String[] projection = new String[]{"_id", "thread_id", "address", "person", "date", "protocol",
                "read", "status", "type", "body", "service_center"};
        String sort = "date desc";
        /**
         * _id 短信序号
         * thread_id 对话序号
         * address 发件人地址，手机号
         * person 发件人姓名
         * date long,日期
         * protocol 短信协议 0:SMS_PROTO 短信 1:MMS_PROTO 彩信
         * read: s是否阅读 0: 未读，1:已读
         * status: 短信状态 -1接收，0 complete, 64 pending, 128 failed
         * type 短信类型 1 接收的 2 发出的
         * body 短信内容
         * service_center 短信服务中心号码编号
         */
        Cursor cursor = resolver.query(uri, projection, null, null, sort);
        while (cursor.moveToNext()) {
            JSONObject obj = new JSONObject();
            obj.put("_id", cursor.getInt(cursor.getColumnIndex("_id")));
            obj.put("thread_id", cursor.getInt(cursor.getColumnIndex("thread_id")));
            obj.put("address", cursor.getString(cursor.getColumnIndex("address")));
            obj.put("person", cursor.getString(cursor.getColumnIndex("person")));
            obj.put("body", cursor.getString(cursor.getColumnIndex("body")));
            obj.put("date", dateFormat.format(new Date(cursor.getLong(cursor.getColumnIndex("date")))));
            obj.put("protocol", cursor.getInt(cursor.getColumnIndex("protocol")) == 0 ? "短信" : "彩信");
            obj.put("read", cursor.getInt(cursor.getColumnIndex("read")) == 0 ? "未读" : "已读");
            obj.put("type", cursor.getInt(cursor.getColumnIndex("type")) == 1 ? "接收" : "发送");
            obj.put("service_center", cursor.getString(cursor.getColumnIndex("service_center")));
            array.put(obj);
        }
        return array;
    }

    public JSONArray getCallLogInfo(Context context) throws JSONException {
        JSONArray array = new JSONArray();
        ContentResolver resolver = context.getContentResolver();
        String[] projection = {"name", "number", "type", "date", "duration"};
        Cursor cursor = resolver.query(CallLog.Calls.CONTENT_URI, projection, null, null,
                CallLog.Calls.DEFAULT_SORT_ORDER);

        while (cursor.moveToNext()) {
            JSONObject obj = new JSONObject();
            obj.put("name", cursor.getString(cursor.getColumnIndex("name")));
            obj.put("number", cursor.getString(cursor.getColumnIndex("number")));
            // 1 呼入 2 呼出 3 未接
            String type = cursor.getString(cursor.getColumnIndex("type"));
            obj.put("type", getType(type));
            obj.put("date", dateFormat.format(new Date(cursor.getLong(
                    cursor.getColumnIndex("date")))));
            obj.put("duration", getDuration(cursor.getInt(cursor.getColumnIndex("duration"))));

            array.put(obj);
        }

        return array;
    }

    private String getType(String type) {
        switch (type) {  //呼入1/呼出2/未接3
            case "1":
                return "呼入";
            case "2":
                return "呼出";
            case "3":
                return "未接";
            default:
                return "";
        }
    }

    private String getDuration(int callDuration) {
        if (callDuration <= 0) return "00:00:00";
        int h = callDuration / 3600;
        int m = callDuration % 3600 / 60;
        int s = callDuration % 60;
        String hour = h + "";
        String min = m + "";
        String sec = s + "";
        if (h < 10) {
            hour = "0" + h;
        }
        if (m < 10) {
            min = "0" + m;
        }
        if (s < 10) {
            sec = "0" + s;
        }
        return hour + ":" + min + ":" + sec;
    }


}
