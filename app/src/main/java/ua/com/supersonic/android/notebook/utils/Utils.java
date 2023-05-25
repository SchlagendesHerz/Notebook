package ua.com.supersonic.android.notebook.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.icu.util.Calendar;
import android.net.Uri;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;

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

import ua.com.supersonic.android.notebook.MainActivity;
import ua.com.supersonic.android.notebook.R;

public class Utils {

    public static final String HTTP_GET_METHOD = "GET";
    public static final String HTTP_POST_METHOD = "POST";
    public static final String AGO_FORMAT_PATTERN_HM = "%dh; %dm ago";
    private static final String AGO_FORMAT_PATTERN_D = "%dd ago";
    private static final String AGO_FORMAT_PATTERN_MOD = "%dm; %dd ago";
    private static final String AGO_FORMAT_PATTERN_YMD = "%dy;%dm;%dd ago";

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
        DateTime start = new DateTime(input);
        DateTime end = new DateTime(Calendar.getInstance().getTime());

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
        int hours = 0;
        int minutes = 0;

        if (days == 0) {
            hours = period.getHours();
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
                : String.format(Locale.US, AGO_FORMAT_PATTERN_D, days)
                : String.format(Locale.US, AGO_FORMAT_PATTERN_MOD, months, days)
                : String.format(Locale.US, AGO_FORMAT_PATTERN_YMD, years, months, days);
    }

    public static String formatDouble(double input, int round) {
        return Math.abs(input) - Math.abs(((int) input)) < Math.pow(10, -1 * round)
                ? String.format(Locale.US, "%d", (int) input)
                : String.format(Locale.US, "%." + round + "f", input);
    }

    public static DateFormat getDateFormatInstance(FormatType type) {
        if (dateFormat == null) dateFormat = new SimpleDateFormat();
        dateFormat.applyPattern(type.getPattern());
        return dateFormat;
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

    public static SharedPreferences getSharedPreferences(Context appContext) {
        String sharedPrefFileName = appContext.getPackageName() + "." + MainActivity.PREFERENCE_FILE_KEY;
        return appContext.getSharedPreferences(sharedPrefFileName, Context.MODE_PRIVATE);
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

    public static void showDialogBox(Context context, int msgId, DialogInterface.OnClickListener yesBtListener) {
        showDialogBox(context, context.getString(msgId), yesBtListener);
    }

    public static void showDialogBox(Context context, String msg, DialogInterface.OnClickListener yesBtListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(msg)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_box_yes_bt, yesBtListener)
                .setNegativeButton(R.string.dialog_box_no_bt, (dialogInterface, i) -> dialogInterface.dismiss())
                .show();
    }

    public enum FormatType {
        DB_DATE_TIME("yyyy-MM-dd HH:mm:ss"),
        RECORD_FIND_ET("yyyy-MM-dd"),
        RECORD_FIND_TIME("HH:mm"),
        RECORD_ITEM_DATE("MMM d, yyyy"),
        RECORD_ITEM_TIME("E h:mm a");
        private final String pattern;

        FormatType(String pattern) {
            this.pattern = pattern;
        }

        public String getPattern() {
            return pattern;
        }

    }
}
