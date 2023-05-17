package ua.com.supersonic.android.notebook.db.dropbox;

import static android.content.Context.CONNECTIVITY_SERVICE;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.WriteMode;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

import ua.com.supersonic.android.notebook.db.DBConstants;
import ua.com.supersonic.android.notebook.utils.Utils;

public class DropboxDBSynchronizer {

    private static final String SQLITE_DB_FILEPATH_FORMAT_STRING = "/data/data/%s/databases/%s";
    private static DropboxDBSynchronizer instance;

    private DbxClientV2 mDropboxClient;
    private DropboxTokenHolder mDropboxTokenHolder;

    private Context appContext;

    private DropboxDBSynchronizer(Context appContext) {
//        this.mDropboxTokenHolder = new DropboxTokenHolder();
//        this.mDropboxTokenHolder.initToken();
        this.appContext = appContext;
    }

    public static DropboxDBSynchronizer getInstance(Context context) {
        if (instance == null) {
            instance = new DropboxDBSynchronizer(context);
        }
        return instance;
    }

    public void performDropboxExportTask() {
        boolean isDropboxReady = performDropboxCheck();
        if (isDropboxReady) {
            Predicate<String[]> dropboxExportPredicate = (args) -> {
                File fromFile = new File(args[0]);

                try (
                        InputStream in = new FileInputStream(fromFile)
                ) {
                    mDropboxClient.files().uploadBuilder("/" + args[1])
                            .withMode(WriteMode.OVERWRITE)
                            .uploadAndFinish(in);
                    return true;
                } catch (IOException | DbxException e) {
                    throw new RuntimeException(e);
                }
            };
            boolean isExportSucceed = performPredicateTask(dropboxExportPredicate, generateSqliteDBPath(DBConstants.DB_NAME), DBConstants.DB_NAME);
            Utils.showToastMessage("EXPORT TASK IS " + String.valueOf(isExportSucceed).toUpperCase(), appContext);
        }
    }

    public void performDropboxImportTask() {
        boolean isDropboxReady = performDropboxCheck();
        if (isDropboxReady) {
            Predicate<String[]> dropboxImportPredicate = (args) -> {
                File toFile = new File(args[1]);
                try (
                        OutputStream outStream = new FileOutputStream(toFile, false)
                ) {
                    mDropboxClient.files()
                            .downloadBuilder("/" + args[0]).download(outStream);
                    return true;
                } catch (IOException | DbxException e) {
                    throw new RuntimeException(e);
                }
            };
            boolean isImportSucceed = performPredicateTask(dropboxImportPredicate, DBConstants.DB_NAME, generateSqliteDBPath(DBConstants.DB_NAME));
            Utils.showToastMessage("IMPORT TASK IS " + String.valueOf(isImportSucceed).toUpperCase(), appContext);
        }
    }

    private String generateSqliteDBPath(String dbName) {
        return String.format(SQLITE_DB_FILEPATH_FORMAT_STRING, appContext.getPackageName(), dbName);
    }

    private boolean isConnected() {
        ConnectivityManager connectManager = (ConnectivityManager) appContext.getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private boolean performDropboxCheck() {
        String toastMessage;
        if (!isConnected()) {
            toastMessage = "NO INTERNET CONNECTION";
            Utils.showToastMessage(toastMessage, appContext);
            return false;
        }
        if (mDropboxTokenHolder == null) {
            mDropboxTokenHolder = new DropboxTokenHolder(appContext);
        }
        if (!mDropboxTokenHolder.isTokenValid()) {
            try {
                performTokenRefreshTask();
            } catch (ExecutionException | InterruptedException | RuntimeException e) {
                toastMessage = "EXCEPTION OCCURRED " + e.getMessage();
                Utils.showToastMessage(toastMessage, appContext);
                return false;
            }
        } else if (mDropboxClient == null) {
            DbxRequestConfig config = DbxRequestConfig.newBuilder("dropbox/app_notebook").build();
            mDropboxClient = new DbxClientV2(config, mDropboxTokenHolder.getShortTermToken());
        }
        return true;
    }

    private boolean performPredicateTask(Predicate<String[]> action, String... args) {
        String toastMessage;
        boolean isTaskSucceed;

        PredicateTask toDoTask = new PredicateTask(action);
        toDoTask.execute(args);
        try {
            isTaskSucceed = toDoTask.get();
            toastMessage = toDoTask.getErrorMessage();
        } catch (ExecutionException | InterruptedException e) {
            toastMessage = "EXCEPTION OCCURRED " + e.getMessage();
            isTaskSucceed = false;
        }
        if (toastMessage != null) {
            Utils.showToastMessage(toastMessage, appContext);
        }
        return isTaskSucceed;
    }

    private void performTokenRefreshTask() throws ExecutionException, InterruptedException {
        Predicate<String[]> refreshTokenPredicate = (args) -> {
            try {
                mDropboxTokenHolder.refreshToken();
                return true;
            } catch (IOException | JSONException e) {
                throw new RuntimeException(e);
            }
        };
        boolean isRefreshSucceed = performPredicateTask(refreshTokenPredicate);
        if (isRefreshSucceed) {
            mDropboxTokenHolder.persistToken();
            DbxRequestConfig config = DbxRequestConfig.newBuilder("dropbox/app_notebook").build();
            mDropboxClient = new DbxClientV2(config, mDropboxTokenHolder.getShortTermToken());
        }
    }

    private static class DropboxTokenHolder {
        private static final String JSON_EXPIRES_PROPERTY = "expires_in";
        private static final String JSON_TOKEN_PROPERTY = "access_token";
        private static final String NOTEBOOK_APP_KEY = "tf3377o8jkshd0v";
        private static final String NOTEBOOK_APP_SECRET_KEY = "8lhw776coxy2m3o";
        private static final String REFRESH_TOKEN = "K0evLC8UlMgAAAAAAAAAAZS5ZwMMvJBJdvIDacta3bxluhskx6dj6O3qHPm_S1Tj";
        private static final String ST_TOKEN_END_KEY = "st_token_dur_key";
        private static final int ST_TOKEN_END_OFFSET = 10000;
        private static final String ST_TOKEN_KEY = "st_token_key";
        private static final String TOKEN_REFRESH_LINK_FORMAT = "https://api.dropboxapi.com/oauth2/token?refresh_token=%s&grant_type=refresh_token&client_id=%s&client_secret=%s";

        private long expireTermEnd;
        private String shortTermToken;

        private Context appContext;

        private DropboxTokenHolder(Context appContext) {
            this.appContext = appContext;
            initToken();
        }

        String getShortTermToken() {
            return shortTermToken;
        }

        void initToken() {
            SharedPreferences sharedPref = Utils.getSharedPreferences(appContext);
            shortTermToken = sharedPref.getString(ST_TOKEN_KEY, null);
            expireTermEnd = sharedPref.getLong(ST_TOKEN_END_KEY, -1);
        }

        boolean isTokenValid() {
            return shortTermToken != null
                    && (System.currentTimeMillis() < expireTermEnd);
        }

        String[] parseJSONResponse(String serverResponse) throws JSONException {
            String[] result = new String[2];
            JSONObject rootJSON = new JSONObject(serverResponse);
            result[0] = rootJSON.optString(JSON_TOKEN_PROPERTY);
            result[1] = rootJSON.optString(JSON_EXPIRES_PROPERTY);
            return result;
        }

        void persistToken() {
            SharedPreferences sharedPref = Utils.getSharedPreferences(appContext);
            sharedPref.edit()
                    .putString(ST_TOKEN_KEY, shortTermToken)
                    .putLong(ST_TOKEN_END_KEY, expireTermEnd)
                    .apply();
        }

        void refreshToken() throws IOException, JSONException {
            String tokenRefreshLink = String.format(TOKEN_REFRESH_LINK_FORMAT, REFRESH_TOKEN,
                    NOTEBOOK_APP_KEY, NOTEBOOK_APP_SECRET_KEY);
            URL url = new URL(tokenRefreshLink);
            String[] response = parseJSONResponse(Utils.makeHttpRequest(url, Utils.HTTP_POST_METHOD));
            expireTermEnd = Long.parseLong(response[1]) * 1000 + System.currentTimeMillis() - ST_TOKEN_END_OFFSET;
            shortTermToken = response[0];
        }
    }

    public static class PredicateTask extends AsyncTask<String, Void, Boolean> {

        private final Predicate<String[]> action;
        private String errorMessage;

        public PredicateTask(Predicate<String[]> action) {
            super();
            this.action = action;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        @Override
        protected Boolean doInBackground(String... args) {
            try {
                return action.test(args);
            } catch (Exception e) {
                errorMessage = "EXCEPTION OCCURRED " + e.getMessage();
                return false;
            }
        }
    }

}
