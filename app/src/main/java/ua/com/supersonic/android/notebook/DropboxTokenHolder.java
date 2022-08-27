package ua.com.supersonic.android.notebook;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.provider.Settings;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;

import ua.com.supersonic.android.notebook.utils.Utils;

public class DropboxTokenHolder {
    private static final String TOKEN_REFRESH_LINK_FORMAT = "https://api.dropboxapi.com/oauth2/token?refresh_token=%s&grant_type=refresh_token&client_id=%s&client_secret=%s";
    private static final String REFRESH_TOKEN = "K0evLC8UlMgAAAAAAAAAAZS5ZwMMvJBJdvIDacta3bxluhskx6dj6O3qHPm_S1Tj";
    private static final String NOTEBOOK_APP_KEY = "tf3377o8jkshd0v";
    private static final String NOTEBOOK_APP_SECRET_KEY = "8lhw776coxy2m3o";

    private static final String JSON_TOKEN_PROPERTY = "access_token";
    private static final String JSON_EXPIRES_PROPERTY = "expires_in";

    private static final String ST_TOKEN_KEY = "st_token_key";
    private static final String ST_TOKEN_END_KEY = "st_token_dur_key";
    private static final int ST_TOKEN_END_OFFSET = 10000;

    private static DropboxTokenHolder instance;

    private long expireTermEnd;
    private String shortTermToken;

    public static DropboxTokenHolder getInstance(Context context) {
        if (instance == null) {
            instance = new DropboxTokenHolder();
            instance.initToken(context);
        }
        return instance;
    }

    private SharedPreferences getSharedPreferences(Context context) {
        String sharedPrefFileName = context.getPackageName() + "." + MainActivity.PREFERENCE_FILE_KEY;
        return context.getSharedPreferences(sharedPrefFileName, Context.MODE_PRIVATE);
    }

    private void initToken(Context context) {
        SharedPreferences sharedPref = getSharedPreferences(context);
        shortTermToken = sharedPref.getString(ST_TOKEN_KEY, null);
        expireTermEnd = sharedPref.getLong(ST_TOKEN_END_KEY, -1);
    }

    public void persistToken(Context context) {
        SharedPreferences sharedPref = getSharedPreferences(context);
        sharedPref.edit()
                .putString(ST_TOKEN_KEY, instance.shortTermToken)
                .putLong(ST_TOKEN_END_KEY, instance.expireTermEnd)
                .apply();
    }

    private DropboxTokenHolder() {
    }

    public String getShortTermToken() {
        return shortTermToken;
    }

    public boolean isTokenValid() {
        return shortTermToken != null
                && (System.currentTimeMillis() < instance.expireTermEnd);
    }

    public void refreshToken() throws IOException, JSONException {
        String tokenRefreshLink = String.format(TOKEN_REFRESH_LINK_FORMAT, REFRESH_TOKEN,
                NOTEBOOK_APP_KEY, NOTEBOOK_APP_SECRET_KEY);
        URL url = new URL(tokenRefreshLink);
        String[] response = parseJSONResponse(Utils.makeHttpRequest(url, Utils.HTTP_POST_METHOD));
        instance.expireTermEnd = Long.parseLong(response[1]) * 1000 + System.currentTimeMillis() - ST_TOKEN_END_OFFSET;
        instance.shortTermToken = response[0];
    }

    private String[] parseJSONResponse(String serverResponse) throws JSONException {
        String[] result = new String[2];
        JSONObject rootJSON = new JSONObject(serverResponse);
        result[0] = rootJSON.optString(JSON_TOKEN_PROPERTY);
        result[1] = rootJSON.optString(JSON_EXPIRES_PROPERTY);
        return result;
    }
}
