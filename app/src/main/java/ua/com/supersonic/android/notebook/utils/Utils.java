package ua.com.supersonic.android.notebook.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Utils {

    private static final String TAG = Utils.class.getSimpleName().toUpperCase();
    private static final String RAW_TYPE = "raw";
    private static final String RESOURCE_PREFIX = ContentResolver.SCHEME_ANDROID_RESOURCE + "://";
    private static final String FILE_SEPARATOR = "\u002F";
    public static final String HTTP_GET_METHOD = "GET";
    public static final String HTTP_POST_METHOD = "POST";
    private static final int HTTP_CONNECT_TIMEOUT = 5000;
    private static final int HTTP_READ_TIMEOUT = 3000;
    private static final String EMPTY_STRING = "";


    public static int getRawResId(Context context, String fileName) {
        return context.getResources().getIdentifier(fileName, RAW_TYPE, context.getPackageName());
    }

    public static Uri getRawResUri(Context context, int resId) {
        return Uri.parse(getRawResPath(context, resId));
    }

    public static String getRawResPath(Context context, int resId) {
//        Log.i(TAG, "resourceName = " + context.getResources().getResourceName(resId));
//        Log.i(TAG, "resourceEntryName = " + context.getResources().getResourceEntryName(resId));
        return RESOURCE_PREFIX +
                context.getResources().getResourcePackageName(resId) +
                FILE_SEPARATOR +
                resId;
    }

    public static void copyFile(FileInputStream fromFile, FileOutputStream toFile) throws IOException {

        try (
                FileChannel fromChannel = fromFile.getChannel();
                FileChannel toChannel = toFile.getChannel()
        ) {
            fromChannel.transferTo(0, fromChannel.size(), toChannel);
        }
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
