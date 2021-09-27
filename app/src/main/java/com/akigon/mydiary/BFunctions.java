package com.akigon.mydiary;

import android.app.Activity;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import com.parse.ParseObject;

import org.apache.commons.text.WordUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BFunctions {

    public static final int PICK_IMAGE = 100;
    public static final int PICK_VIDEO = 101;
    public static final int PICK_AUDIO = 102;
    public static final int CHOOSE_CSV = 124;
    public static final int ADD_NOTE = 256;
    public static final int EDIT_NOTE = 257;
    public static final int SETTING_SEND = 258;
    public static final int FOLDERPICKER_CODE = 301;

    public static final String SP_LAST_SYNC_STAMP = "LAST_SYNC_STAMP";
    public static final String SP_AUTO_SYNC_KEY = "AUTO_SYNC";
    public static final String SP_PASS_CODE_KEY = "PASS_CODE";
    public static final String SP_SECURITY_QUESTION_KEY = "QUESTION";

    public static final String SP_DARK_MODE_KEY = "DARK_MODE";

    public static final String ACTION_RESTORED = "RESTORED";
    public static final String ACTION_AUTO_SYNC = "AUTO_SYNC";

    public static final String feedback_url = "https://forms.gle/2Yf9V12yuhzf3TWD9";
    public static final String open_source_license_url = "https://akigon-journals.web.app/credits.html";
    public static final String terms_condition_url = "https://akigon-journals.web.app/termsandcondition.html";
    public static final String privacy_policy_url = "https://akigon-journals.web.app/privacy_policy.html";


    public static final String DIARY_CLASS_NAME="Diaries";
    public static final String DIARY_CREATED_AT="created_at";
    public static final String DIARY_USER="user";
    public static final String DIARY_UPDATED_AT="updatedAt";
    public static final String DIARY_CONTENT="content";
    public static final String DIARY_TITLE="title";
    public static final String DIARY_CATEGORIES="categories";
    public static final String DIARY_FILES="file_list";
    public static final String DIARY_ARCHIVE="archive";
    public static final String DIARY_HTML="HTML";

    public static void savefile(String sourcePath, String destPath) {
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(sourcePath));
            bos = new BufferedOutputStream(new FileOutputStream(destPath, false));
            byte[] buf = new byte[1024];
            bis.read(buf);
            do {
                bos.write(buf);
            } while(bis.read(buf) != -1);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bis != null) bis.close();
                if (bos != null) bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getTimeStringFromDate(Date d) {
        String date_format = "EEE, d MMM yyyy HH:mm";
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTime(d);
        return DateFormat.format(date_format, cal).toString();
    }

    public static String getTimeStringTS(long d) {
        String date_format = "EEE, d MMM yyyy HH:mm";
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(d);
        return DateFormat.format(date_format, cal).toString();
    }

    private static final String TAG = "BFunctions";

    public static int minToRead(String s) {
        return (int) Math.ceil((double) s.split("\\s+").length / 275);
    }

    public static String getStringFromList(List<String> s){
        return TextUtils.join(",", s).replaceAll("\\[", "").replaceAll("\\]","");
    }

    public static ArrayList<String> getListFromString(String s){
            ArrayList<String> cats = new ArrayList<>();
            s = s.trim();
            if (s.isEmpty()) {
                return cats;
            } else {
                String[] tgs = s.split(",");
                for (String x : tgs) {
                    x = WordUtils.capitalize(x.trim());
                    if (!x.isEmpty() && !cats.contains(x)) {
                        cats.add(x.trim());
                    }
                }
                return cats;
            }
    }

    public static void showAlert(Activity activity, String title, String message){
        AlertDialog.Builder builder=new AlertDialog.Builder(activity);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("Dismiss",null)
                .show();

    }



}
