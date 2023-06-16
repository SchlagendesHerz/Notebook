package ua.com.supersonic.android.notebook.db.dropbox;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static ua.com.supersonic.android.notebook.utils.Utils.FormatType.DEFAULT_DATE_TIME;
import static ua.com.supersonic.android.notebook.utils.Utils.HTTP_GET_METHOD;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.android.Auth;
import com.dropbox.core.android.AuthActivity;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.oauth.DbxRefreshResult;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.WriteMode;
import com.dropbox.core.v2.users.FullAccount;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

import ua.com.supersonic.android.notebook.R;
import ua.com.supersonic.android.notebook.utils.Utils;

public class DropboxDBSynchronizer {
    private static final String DBX_ACCOUNT_KEY = "dbx_account";
    //    private static final String DBX_ACCOUNT_CLASS_NAME = "com.dropbox.core.v2.users.FullAccount";
//    private static final String DBX_CREDENTIAL_CLASS_NAME = "com.dropbox.core.oauth.DbxCredential";
//    public static final String DBX_AUTH_LAUNCHED_KEY = "dbx_auth_activity_launched";
    private static final String DBX_CREDENTIAL_KEY = "dbx_credential";
    private static final long EXPIRE_MARGIN = 5 * 60 * 1000; // 5 minutes
    private static final String SQLITE_DB_FILEPATH_FORMAT_STRING = "/data/data/%s/databases/%s";

    private static DropboxDBSynchronizer instance;

    public static DropboxDBSynchronizer getInstance(Context context, String dbxAppName) {
        if (instance == null) {
            instance = new DropboxDBSynchronizer(context, dbxAppName);
        }
        return instance;
    }

    private final Context appContext;
    private final String dbxAppName;
    private long expiresAt = -1;
    private DbxClientV2 mDropboxClient;
    private String mMessage;
    private String refreshToken;

    private DropboxDBSynchronizer(Context appContext, String dbxAppName) {
//        this.mDropboxTokenHolder = new DropboxTokenHolder();
//        this.mDropboxTokenHolder.initToken();
        this.appContext = appContext;
        this.dbxAppName = dbxAppName;
        initDbxClient();
//        this.mDbxTokenHolder = new DbxCredentialHolder(appContext);
    }

    public FullAccount getDbxAccount() {
        return Utils.getObjectFromSharedPrefs(appContext, DBX_ACCOUNT_KEY, FullAccount.class);
    }

    public DbxCredential getDbxCredential() {
        return Utils.getObjectFromSharedPrefs(appContext, DBX_CREDENTIAL_KEY, DbxCredential.class);
    }

    public String getMessage() {
        return mMessage;
    }

    public boolean isAuthDone() {
        return refreshToken != null;
    }

    public boolean resetAuth() {
        mMessage = null;
        if (isTokenValid()) {
            boolean isDropboxReady = checkIfDbxReady();
            if (isDropboxReady) {
                try {
                    Log.d("DROPBOX", "TOKEN REVOKE");
                    mDropboxClient.auth().tokenRevoke();
                } catch (DbxException e) {
                    mMessage = e.getMessage();
                    return false;
                }
            } else return false;
        }
        Utils.deleteFromSharedPrefs(appContext, DBX_CREDENTIAL_KEY);
        Utils.deleteFromSharedPrefs(appContext, DBX_ACCOUNT_KEY);
        AuthActivity.result = null;
        mDropboxClient = null;
        refreshToken = null;
        expiresAt = -1;
        return true;
    }

    public void performAuth() {
        performOAuth2PKCEAuthentication();
    }

    public boolean performDbxAccountRequest() {
        mMessage = null;
        boolean isDropboxReady = checkIfDbxReady();
        if (isDropboxReady) {
            try {
                FullAccount account = mDropboxClient.users().getCurrentAccount();
                Utils.putObjectToSharedPrefs(appContext, DBX_ACCOUNT_KEY, account);
                Utils.logDbxAccount(account);
            } catch (DbxException e) {
                mMessage = e.getMessage();
                return false;
            }
        }
        return true;
    }

    public boolean performDbxExport(String dbName) {
        mMessage = null;
        boolean isDropboxReady = checkIfDbxReady();
        if (isDropboxReady) {
            File fromFile = new File(generateSqliteDBPath(dbName));
            try (
                    InputStream in = new FileInputStream(fromFile)
            ) {
                mDropboxClient.files().uploadBuilder("/" + dbName)
                        .withMode(WriteMode.OVERWRITE)
                        .uploadAndFinish(in);
                return true;
            } catch (IOException | DbxException e) {
                mMessage = e.getMessage();
            }
        }
        return false;
    }

    public boolean performDbxImport(String fromFileName) {
        mMessage = null;
        boolean isDbxReady = checkIfDbxReady();
        if (isDbxReady) {

            try {
                File toFile = new File(generateSqliteDBPath(fromFileName));
                if (!checkIfFilePresent(fromFileName)) {
                    mMessage = String.format(appContext.getString(R.string.error_msg_file_absent_format), fromFileName);
                    return false;
                }
                try (
                        OutputStream outStream = new FileOutputStream(toFile, false)
                ) {

                    Log.d("DROPBOX", mDropboxClient.toString());
                    Utils.logDbxCredentials(Auth.getDbxCredential());
                    mDropboxClient.files()
                            .downloadBuilder("/" + fromFileName).download(outStream);
                    return true;
                }
            } catch (DbxException | IOException e) {
                mMessage = e.getMessage();
            }
        }
        return false;
    }


    /*public void performDropboxExportTask() {
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
    }*/

    /*public void performDropboxImportTask() {
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
    }*/

    public void updateDbxCredential(DbxRefreshResult result) {
        DbxCredential toUpdate = getDbxCredential();
        if (toUpdate == null) return;

        toUpdate = new DbxCredential(
                result.getAccessToken(),
                result.getExpiresAt(),
                toUpdate.getRefreshToken(),
                appContext.getString(R.string.dbx_api_app_key),
                appContext.getString(R.string.dbx_api_app_secret)
        );
        Utils.putObjectToSharedPrefs(appContext, DBX_CREDENTIAL_KEY, toUpdate);
        initDbxClient(toUpdate);
    }

    public void updateDbxCredential(DbxCredential credential) {
        Utils.putObjectToSharedPrefs(appContext, DBX_CREDENTIAL_KEY, credential);
        initDbxClient(credential);
    }

    void logOutByHttpRequest() throws IOException, JSONException {
        String logOutLink = "https://www.dropbox.com/logout?access_token=%s";
        URL url = new URL(String.format(logOutLink, getDbxCredential().getAccessToken()));
        Utils.makeHttpRequest(url, HTTP_GET_METHOD);
//        Log.d("DROPBOX", Utils.makeHttpRequest(url, HTTP_GET_METHOD));
    }

    private boolean checkIfDbxReady() {
        if (!isAuthDone()) {
            mMessage = appContext.getString(R.string.error_msg_dbx_auth_required);
            return false;
        }
        if (!isConnected()) {
            mMessage = appContext.getString(R.string.error_msg_no_internet);
            return false;
        }
//        if (mDropboxClient == null) {
//            initDbxClient();
//        }
        if (!isTokenValid()) {
            try {
                refreshToken();
            } catch (Exception e) {
                mMessage = e.getMessage();
                return false;
            }
        }

        return true;
    }

    private boolean checkIfFilePresent(String fromFileName) throws DbxException {
        for (Metadata meta : mDropboxClient.files().listFolder("").getEntries()) {
            if (meta.getName().equals(fromFileName)) return true;
        }
        return false;
    }

    private String generateSqliteDBPath(String dbName) {
        return String.format(SQLITE_DB_FILEPATH_FORMAT_STRING, appContext.getPackageName(), dbName);
    }

    private void initDbxClient() {
        initDbxClient(getDbxCredential());
    }

    private void initDbxClient(DbxCredential dbxCredential) {

        Log.d("DROPBOX", "INIT");
        Utils.logDbxCredentials(dbxCredential);

        if (dbxCredential != null) {

            DbxRequestConfig config = DbxRequestConfig
                    .newBuilder("dropbox/" + dbxAppName)
                    .build();
            mDropboxClient = new DbxClientV2(config, dbxCredential);
            this.expiresAt = dbxCredential.getExpiresAt();
            this.refreshToken = dbxCredential.getRefreshToken();
        }
    }

    private boolean isConnected() {
        ConnectivityManager connectManager = (ConnectivityManager) appContext.getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

/*    private boolean performDropboxCheck() {
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
    }*/

    private boolean isTokenValid() {
        Log.d("DROPBOX", "IS VALID");
        Log.d("DROPBOX", "IS VALID = " + (expiresAt > System.currentTimeMillis() + EXPIRE_MARGIN));
        Log.d("DROPBOX", "EXPIRES AT = " + Utils.getDateFormatInstance(DEFAULT_DATE_TIME)
                .format(new Date(expiresAt)));
        Log.d("DROPBOX", "CURRENT = " + Utils.getDateFormatInstance(DEFAULT_DATE_TIME)
                .format(new Date(System.currentTimeMillis() + EXPIRE_MARGIN)));

        return expiresAt > System.currentTimeMillis() + EXPIRE_MARGIN;
    }

    private void performOAuth2Authentication() {
//        Utils.putToSharedPrefs(appContext, DBX_AUTH_LAUNCHED_KEY, true);
        String appKey = appContext.getString(R.string.dbx_api_app_key);
        Auth.startOAuth2Authentication(appContext, appKey);
    }

    private void performOAuth2PKCEAuthentication() {
//        Utils.putToSharedPrefs(appContext, DBX_AUTH_LAUNCHED_KEY, true);
        String appKey = appContext.getString(R.string.dbx_api_app_key);

        DbxRequestConfig dbxRequestConfig = new DbxRequestConfig("db-" + appKey);
        List<String> scopes = List.of(
                "account_info.read",
                "files.metadata.read",
                "files.content.write",
                "files.content.read"
        );
        Auth.startOAuth2PKCE(appContext, appKey, dbxRequestConfig, scopes);
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

    /*private void performTokenRefreshTask() throws ExecutionException, InterruptedException {
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
    }*/

    private void refreshToken() throws DbxException {
        updateDbxCredential(mDropboxClient.refreshAccessToken());

        Log.d("DROPBOX", "REFRESH");
        Utils.logDbxCredentials(getDbxCredential());
    }

    private static class DbxCredentialHolder {
        private static final String JSON_EXPIRES_PROPERTY = "expires_in";
        private static final String JSON_TOKEN_PROPERTY = "access_token";
        private static final String LT_REFRESH_TOKEN_KEY = "lt_refresh_token_key";
        private static final String ST_TOKEN_END_KEY = "st_token_dur_key";
        private static final int ST_TOKEN_END_OFFSET = 10000;
        //        private static final String DROPBOX_AUTH_DONE_KEY = "dbx_auth_done_key";
        private static final String ST_TOKEN_KEY = "st_token_key";
        private static final String TOKEN_REFRESH_LINK_FORMAT = "https://api.dropboxapi.com/oauth2/token?refresh_token=%s&grant_type=refresh_token&client_id=%s&client_secret=%s";

        private final Context appContext;
        private String appKey;
        private String appSecret;
        private long expireTermEnd;
        private String refreshToken;
        private String shortTermToken;

        private DbxCredentialHolder(Context appContext) {
            this.appContext = appContext;
            initToken();
        }

        String getShortTermToken() {
            return shortTermToken;
        }

        private void setShortTermToken(String shortTermToken) {
            this.shortTermToken = shortTermToken;
        }

        void initToken() {
            appKey = appContext.getString(R.string.dbx_api_app_key);
            appSecret = appContext.getString(R.string.dbx_api_app_secret);
            SharedPreferences sharedPref = Utils.getSharedPrefs(appContext);
            shortTermToken = sharedPref.getString(ST_TOKEN_KEY, null);
            expireTermEnd = sharedPref.getLong(ST_TOKEN_END_KEY, -1);
            refreshToken = sharedPref.getString(LT_REFRESH_TOKEN_KEY, null);

            Log.d("DROPBOX", "-----------------------------------");
            Log.d("DROPBOX", "INIT TOKEN");
            Utils.logDbxCredentials();
            Log.d("DROPBOX", "-----------------------------------");

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

        void performOAuth2Authentication() {
//            Utils.putToSharedPrefs(appContext, DBX_AUTH_LAUNCHED_KEY, true);
            Auth.startOAuth2Authentication(appContext, appKey);
        }

        void performOAuth2PKCEAuthentication() {
//            Utils.putToSharedPrefs(appContext, DBX_AUTH_LAUNCHED_KEY, true);

            DbxRequestConfig dbxRequestConfig = new DbxRequestConfig("db-" + appKey);
            List<String> scopes = List.of(
                    "account_info.read",
                    "files.metadata.read",
                    "files.content.write",
                    "files.content.read"
            );
//            BuildConfig.APPLICATION_ID
            Auth.startOAuth2PKCE(appContext, appKey, dbxRequestConfig, scopes);
        }

        void persistCredentials() {

            Utils.getSharedPrefs(appContext)
                    .edit()
                    .putString(ST_TOKEN_KEY, shortTermToken)
                    .putLong(ST_TOKEN_END_KEY, expireTermEnd)
                    .putString(LT_REFRESH_TOKEN_KEY, refreshToken)
                    .apply();

            Log.d("DROPBOX", "-----------------------------------");
            Log.d("DROPBOX", "PERSISTING TOKEN");
            Log.d("DROPBOX", "shortTermToken = " + shortTermToken);
            Log.d("DROPBOX", "refreshToken = " + refreshToken);
            Log.d("DROPBOX", "expireTermEnd = " + Utils
                    .getDateFormatInstance(DEFAULT_DATE_TIME)
                    .format(new Date(expireTermEnd)));
            Log.d("DROPBOX", "-----------------------------------");
        }

        void refreshToken() {

            persistCredentials();
        }

        void refreshTokenByHttpRequest() throws IOException, JSONException {
            String tokenRefreshLink = String.format(TOKEN_REFRESH_LINK_FORMAT, refreshToken,
                    appKey, appSecret);
            URL url = new URL(tokenRefreshLink);
            String[] response = parseJSONResponse(Utils.makeHttpRequest(url, Utils.HTTP_POST_METHOD));
            expireTermEnd = Long.parseLong(response[1]) * 1000 + System.currentTimeMillis();
            shortTermToken = response[0];
            persistCredentials();

        }

        private boolean isAuthDone() {
            return refreshToken != null;
        }

        private void setExpireTermEnd(long expireTermEnd) {
            this.expireTermEnd = expireTermEnd;
        }

        private void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
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
