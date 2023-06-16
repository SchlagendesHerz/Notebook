package ua.com.supersonic.android.notebook.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.icu.util.Calendar;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.dropbox.core.android.Auth;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.oauth.DbxRefreshResult;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.users.FullAccount;
import com.google.gson.Gson;

import org.joda.time.DateTime;
import org.joda.time.DurationFieldType;
import org.joda.time.Period;
import org.joda.time.PeriodType;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import ua.com.supersonic.android.notebook.MainActivity;
import ua.com.supersonic.android.notebook.R;

public class Utils {

    public static final String HTTP_GET_METHOD = "GET";
    public static final String HTTP_POST_METHOD = "POST";

    public static final String AGO_FORMAT_PATTERN_HM = "%dh; %dm";
    private static final String AGO_FORMAT_PATTERN_DH = "%dd; %dh";
    private static final String AGO_FORMAT_PATTERN_MD = "%dM; %dd";
    private static final String AGO_FORMAT_PATTERN_YMD = "%dy;%dM;%dd";

    private static final String EMPTY_STRING = "";
    private static final String FILE_SEPARATOR = "\u002F";
    private static final int HTTP_CONNECT_TIMEOUT = 5000;
    private static final int HTTP_READ_TIMEOUT = 3000;
    private static final String RAW_TYPE = "raw";
    private static final String RESOURCE_PREFIX = ContentResolver.SCHEME_ANDROID_RESOURCE + "://";
    private static final String TAG = Utils.class.getSimpleName().toUpperCase();
    private static SimpleDateFormat dateFormat;
    private static Toast mToast;

    public static void copyFile(FileInputStream fromFile, FileOutputStream toFile) throws IOException {

        try (
                FileChannel fromChannel = fromFile.getChannel();
                FileChannel toChannel = toFile.getChannel()
        ) {
            fromChannel.transferTo(0, fromChannel.size(), toChannel);
        }
    }

    public static String formatAgoDate(Date input) {
        return formatAgoDate(Calendar.getInstance().getTime(), input);
    }

    public static String formatAgoDate(Date cur, Date prev) {
        DateTime start = new DateTime(prev);
        DateTime end = new DateTime(cur);

//        Period period = new Period(start, end, PeriodType.yearMonthDay());
        DurationFieldType[] durFields = new DurationFieldType[5];
        durFields[0] = DurationFieldType.years();
        durFields[1] = DurationFieldType.months();
        durFields[2] = DurationFieldType.days();
        durFields[3] = DurationFieldType.hours();
        durFields[4] = DurationFieldType.minutes();

        Period period = new Period(start, end, PeriodType.forFields(durFields));

        int years = period.getYears();
        int months = period.getMonths();
        int days = period.getDays();
        int hours = period.getHours();
        int minutes = 0;

        if (days == 0) {
            minutes = period.getMinutes();
        }

//        return String.format(Locale.ROOT, AGO_FORMAT_PATTERN_FULL, years, months, days);
/*        return years == 0
                ? months == 0
                    ? String.format(Locale.US, AGO_FORMAT_PATTERN_D, days)
                    : String.format(Locale.US, AGO_FORMAT_PATTERN_MD, months, days)
                : String.format(Locale.US, AGO_FORMAT_PATTERN_YMD, years, months, days); */

        return years == 0
                ? months == 0
                ? days == 0
                ? String.format(Locale.US, AGO_FORMAT_PATTERN_HM, hours, minutes)
                : String.format(Locale.US, AGO_FORMAT_PATTERN_DH, days, hours)
                : String.format(Locale.US, AGO_FORMAT_PATTERN_MD, months, days)
                : String.format(Locale.US, AGO_FORMAT_PATTERN_YMD, years, months, days);
    }

    public static void toggleViewVisibility(View view) {
        if (view.getVisibility() == View.VISIBLE) {
            view.setVisibility(View.GONE);
        } else if (view.getVisibility() == View.GONE) {
            view.setVisibility(View.VISIBLE);
        }
    }

    public static String formatDouble(double input, int round) {
        return Math.abs(input) - Math.abs(((int) input)) < Math.pow(10, -1 * round)
                ? String.format(Locale.US, "%d", (int) input)
                : String.format(Locale.US, "%." + round + "f", input);
    }

    public static boolean getBoolFromSharedPrefs(Context appContext, String key) {
        return getSharedPrefs(appContext).getBoolean(key, false);
    }

    public static DateFormat getDateFormatInstance(FormatType type) {
        return getDateFormatInstance(type, TimeZone.getDefault());
    }

    public static DateFormat getDateFormatInstance(FormatType type, TimeZone zone) {
        if (dateFormat == null) dateFormat = new SimpleDateFormat();
        dateFormat.applyPattern(type.getPattern());
        dateFormat.setTimeZone(zone);
        return dateFormat;
    }

    public static void runAsync(Runnable action) {
        new Thread() {
            @Override
            public void run() {
                action.run();
            }
        }.start();
    }

    public static <T> T getObjectFromSharedPrefs(Context appContext, String key, Class<T> objectClass) {
        SharedPreferences prefs = getSharedPrefs(appContext);
        Gson gson = new Gson();
        String json = prefs.getString(key, "");
        return objectClass.cast(gson.fromJson(json, objectClass));
    }

    public static int getRawResId(Context context, String fileName) {
        return context.getResources().getIdentifier(fileName, RAW_TYPE, context.getPackageName());
    }

    public static String getRawResPath(Context context, int resId) {
//        Log.i(TAG, "resourceName = " + context.getResources().getResourceName(resId));
//        Log.i(TAG, "resourceEntryName = " + context.getResources().getResourceEntryName(resId));
        return RESOURCE_PREFIX +
                context.getResources().getResourcePackageName(resId) +
                FILE_SEPARATOR +
                resId;
    }

    public static Uri getRawResUri(Context context, int resId) {
        return Uri.parse(getRawResPath(context, resId));
    }

    public static SharedPreferences getSharedPrefs(Context appContext) {
        String sharedPrefFileName = appContext.getPackageName() + "." + MainActivity.PREFERENCE_FILE_KEY;
        return appContext.getSharedPreferences(sharedPrefFileName, Context.MODE_PRIVATE);
    }

    public static String getStringFromSharedPrefs(Context appContext, String key) {
        return getSharedPrefs(appContext).getString(key, null);
    }

    public static View getViewByPosition(int pos, ListView listView) {
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

        if (pos < firstListItemPosition || pos > lastListItemPosition) {
            return listView.getAdapter().getView(pos, null, listView);
        } else {
            final int childIndex = pos - firstListItemPosition;
            return listView.getChildAt(childIndex);
        }
    }

    public static void hideKeyboard(@NonNull Activity activity) {
//        Log.d(TAG, "hideKeyBoard invoked");
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
//        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
//        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
//            Log.d(TAG, "view = null");
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static void logDbxCredentials() {
        logDbxCredentials(Auth.getDbxCredential());
    }

    public static void logDbxListFolderResult(ListFolderResult list) {
        Log.d("DROPBOX", "-----------------LIST FOLDER RESULT-----------------");
        for (Metadata meta : list.getEntries()) {
            Log.d("DROPBOX", meta.toString());
        }
        Log.d("DROPBOX", "----------------------------------------------------");
    }

    public static void logDbxCredentials(DbxCredential dbxCredential) {
//        Log.d("DROPBOX", "UID = " + Auth.getUid());
        Log.d("DROPBOX", "-----------------DBX CREDENTIAL-----------------");
        if (dbxCredential != null) {
            Log.d("DROPBOX", "ACCESS TOKEN = " + dbxCredential.getAccessToken());
            Log.d("DROPBOX", "REFRESH TOKEN = " + dbxCredential.getRefreshToken());
            Log.d("DROPBOX", "EXPIRES AT = " + dbxCredential.getExpiresAt());
            Log.d("DROPBOX", "EXPIRES AT FORMATTED = " +
                    Utils.getDateFormatInstance(Utils.FormatType.DEFAULT_DATE_TIME)
                            .format(new Date(dbxCredential.getExpiresAt())));
        }
        Log.d("DROPBOX", "------------------------------------------------");
    }

    public static void logDbxAccount(FullAccount account) {
        Log.d("DROPBOX", "-----------------DBX ACCOUNT-----------------");
        if (account != null) {
            Log.d("DROPBOX", "ACCOUNT ID = " + account.getAccountId());
            Log.d("DROPBOX", "ACCOUNT EMAIL = " + account.getEmail());
            Log.d("DROPBOX", "ACCOUNT NAME = " + account.getName().toString());
            Log.d("DROPBOX", "ACCOUNT SUMMARY = " + account.toString());
        }
        Log.d("DROPBOX", "------------------------------------------------");

    }

    public static void logDbxRefreshResults(DbxRefreshResult refreshResult) {
        Log.d("DROPBOX", "ACCESS TOKEN = " + refreshResult.getAccessToken());
        Log.d("DROPBOX", "EXPIRES AT = " + refreshResult.getExpiresAt());
        Log.d("DROPBOX", "EXPIRES AT FORMATTED = " +
                Utils.getDateFormatInstance(Utils.FormatType.DEFAULT_DATE_TIME)
                        .format(new Date(refreshResult.getExpiresAt())));
    }

    public static String makeHttpRequest(URL url, String methodType) throws IOException {
        String serverResponse = EMPTY_STRING;

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(methodType);
        connection.setConnectTimeout(HTTP_CONNECT_TIMEOUT);
        connection.setReadTimeout(HTTP_READ_TIMEOUT);
        connection.connect();

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            return serverResponse;
        }

        try (
                InputStream inputStream = connection.getInputStream()
        ) {
            serverResponse = readFromStream(inputStream);
        }
        return serverResponse;
    }

    public static void putObjectToSharedPrefs(Context appContext, String key, Object objToSerialize) {
        SharedPreferences prefs = getSharedPrefs(appContext);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(objToSerialize);
        prefsEditor.putString(key, json);
        prefsEditor.apply();
    }

    public static void putToSharedPrefs(Context appContext, String key, boolean value) {
        getSharedPrefs(appContext).edit().putBoolean(key, value).apply();
    }

    public static void putToSharedPrefs(Context appContext, String key, String value) {
        getSharedPrefs(appContext).edit().putString(key, value).apply();
    }

    private static String readFromStream(InputStream inputStream) throws IOException {
        StringBuilder builder = new StringBuilder();
        String curLine;
        BufferedReader bufReader = new BufferedReader(new InputStreamReader(inputStream));
        while ((curLine = bufReader.readLine()) != null) {
            builder.append(curLine).append("\n");
        }
        if (builder.length() != 0) {
            builder.delete(builder.length() - 1, builder.length());
        }
        return builder.toString();
    }

    public static void showTwoButtonDialogBox(Context context, int msgId, DialogInterface.OnClickListener yesBtListener) {
        showTwoButtonDialogBox(context, context.getString(msgId), yesBtListener);
    }

    public static void showTwoButtonDialogBox(Context context, String msg, DialogInterface.OnClickListener yesBtListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(msg)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_box_yes_bt, yesBtListener)
                .setNegativeButton(R.string.dialog_box_no_bt, (dialogInterface, i) -> dialogInterface.dismiss())
                .show();
    }

    public static void showOneButtonDialogBox(Context context, String msg, DialogInterface.OnClickListener yesBtListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(msg)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_box_ok_bt, yesBtListener)
                .show();
    }

    public static void showOneButtonDialogBox(Context context, String msg) {
        showOneButtonDialogBox(context, msg, (dialogInterface, i) -> dialogInterface.dismiss());
    }

    public static void deleteFromSharedPrefs(Context appContext, String key) {
        getSharedPrefs(appContext).edit().remove(key).apply();
    }

    public static void showKeyboardOnFocus(View view, Context appContext) {
        view.requestFocus();
        InputMethodManager imm = (InputMethodManager) appContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }

    public static void showToastMessage(String message, Context context) {
        if (mToast != null) mToast.cancel();
        mToast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        mToast.show();
    }

    public enum FormatType {
        DEFAULT_DATE_TIME("yyyy-MM-dd HH:mm:ss zzz"),
        DB_DATE_TIME("yyyy-MM-dd HH:mm:ss.SSS"),
        RECORD_FIND_ET("yyyy-MM-dd"),
        RECORD_FIND_TIME("HH:mm"),
        RECORD_ITEM_DATE("MMM d, yyyy"),
        RECORD_ITEM_TIME("E h:mm a"),
        FORMATTER_HM("%dh; %dm"),
        DATE_DAY("%dd"),
        DATE_DAY_MIN("%dd; %dm"),
        DATE_MON_DAY("%dm; %dd"),
        AGO_YMD("%dy;%dm;%dd");

        private final String pattern;

        FormatType(String pattern) {
            this.pattern = pattern;
        }

        public String getPattern() {
            return pattern;
        }

    }
}
