package ua.com.supersonic.android.notebook.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

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
import java.util.TimeZone;

public class Utils {

    public static final String HTTP_GET_METHOD = "GET";
    public static final String HTTP_POST_METHOD = "POST";


    private static final String EMPTY_STRING = "";
    private static final String FILE_SEPARATOR = "\u002F";
    private static final int HTTP_CONNECT_TIMEOUT = 5000;
    private static final int HTTP_READ_TIMEOUT = 3000;
    private static final String RAW_TYPE = "raw";
    private static final String RESOURCE_PREFIX = ContentResolver.SCHEME_ANDROID_RESOURCE + "://";
    private static final String TAG = Utils.class.getSimpleName().toUpperCase();

    public enum FormatType {
        DB_DATE_TIME("yyyy-MM-dd HH:mm:ss"),
        RECORD_FIND_DATE("yyyy-MM-dd"),
        RECORD_FIND_TIME("HH:mm"),
        RECORD_ITEM_DATE("MMM d, yyyy"),
        RECORD_ITEM_TIME("E h:mm a")
        ;
        private final String pattern;

        FormatType(String pattern) {
            this.pattern = pattern;
        }

        public String getPattern() {
            return pattern;
        }

    }

    private static SimpleDateFormat dateFormat;

    public static DateFormat getDateFormatInstance(FormatType type) {
        if (dateFormat == null) dateFormat = new SimpleDateFormat();
        dateFormat.applyPattern(type.getPattern());
        return dateFormat;
    }

    public static void copyFile(FileInputStream fromFile, FileOutputStream toFile) throws IOException {

        try (
                FileChannel fromChannel = fromFile.getChannel();
                FileChannel toChannel = toFile.getChannel()
        ) {
            fromChannel.transferTo(0, fromChannel.size(), toChannel);
        }
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
}
