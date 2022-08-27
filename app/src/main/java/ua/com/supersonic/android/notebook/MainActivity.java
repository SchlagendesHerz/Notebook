package ua.com.supersonic.android.notebook;

import android.app.Activity;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.WriteMode;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import ua.com.supersonic.android.notebook.db.DBConstants;
import ua.com.supersonic.android.notebook.db.DBManager;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        AdapterView.OnItemSelectedListener, View.OnFocusChangeListener {
    public static final String PREFERENCE_FILE_KEY = "pref_file_key";
    private static final String SQLITE_DB_FILEPATH_FORMAT_STRING = "/data/data/%s/databases/%s";
    private static final String TAG = MainActivity.class.getSimpleName().toUpperCase();

    public static MainActivity mainInstance;

    public static void hideKeyboard(Activity activity) {
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

    private FloatingActionButton mAddBt;
    private Spinner mCategorySpinner;
    private DBManager mDBManager;
    private DbxClientV2 mDropboxClient;
    private EditText mNewCategoryEt;
    private FloatingActionButton mRemoveBt;
    private ArrayAdapter<String> mSpinnerAdapter;
    private Toast mToast;
    private DropboxTokenHolder mDropboxTokenHolder;

    private String generateSqliteDBPath(String dbName) {
        return String.format(SQLITE_DB_FILEPATH_FORMAT_STRING, getPackageName(), dbName);
    }

    private boolean isConnected() {
        ConnectivityManager connectManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private List<String> loadCategories() {
        String addCategString = getResources().getString(R.string.category_add);
        List<String> categories = new ArrayList<>(Collections.singletonList(addCategString));
        categories.addAll(mDBManager.readFromDB());
        return categories;

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bt_action_add:
                if (mCategorySpinner.getSelectedItemPosition() == 0) {
                    mDBManager.insertToDB(mNewCategoryEt.getText().toString());
                    mNewCategoryEt.setText("");
                    refreshSpinner();
                    mCategorySpinner.setSelection(mSpinnerAdapter.getCount() - 1);
//                    Toast.makeText(this, "ADD CATEGORY SELECTED", Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.bt_action_remove:
                String toDelete = mSpinnerAdapter.getItem(mCategorySpinner.getSelectedItemPosition());
//                Log.d(TAG, "toDelete = " + toDelete);
                mDBManager.deleteFromDB(toDelete);
                refreshSpinner();
                break;
            case R.id.bt_export_db:
                unsetOnClick(R.id.bt_export_db);
                new InitDropboxTask(InitDropboxTask.UPLOAD_ON_POST, R.id.bt_export_db).execute();
                break;
            case R.id.bt_import_db:
                unsetOnClick(R.id.bt_import_db);
                InitDropboxTask initTask = new InitDropboxTask(InitDropboxTask.DOWNLOAD_ON_POST, R.id.bt_import_db);
                initTask.execute();
                try {
                    initTask.get();
//                    setOnClick(R.id.bt_import_db);
                } catch (ExecutionException | InterruptedException e) {
                    showToastMessage("ERROR OCCURRED: " + e.getMessage());
                }
                break;
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainInstance = this;
        setContentView(R.layout.activity_main);
        mCategorySpinner = findViewById(R.id.spinner_category);
        mAddBt = findViewById(R.id.bt_action_add);
        mNewCategoryEt = findViewById(R.id.et_new_category);
        mRemoveBt = findViewById(R.id.bt_action_remove);

        mDBManager = new DBManager(this);
        mDBManager.openDB();

        mAddBt.setOnClickListener(this);
        mRemoveBt.setOnClickListener(this);
        mCategorySpinner.setOnItemSelectedListener(this);
        findViewById(R.id.bt_export_db).setOnClickListener(this);
        findViewById(R.id.bt_import_db).setOnClickListener(this);
        findViewById(R.id.root_container).setOnFocusChangeListener(this);

        mSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, loadCategories());
        mCategorySpinner.setAdapter(mSpinnerAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
//        mDBManager.openDB();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        mDBManager.dropTable();
        mDBManager.closeDB();
        mainInstance = null;
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        switch (view.getId()) {
            case R.id.root_container:
                if (hasFocus) {
                    hideKeyboard(this);
                    if (mToast != null) mToast.cancel();
                }
                break;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        if (i == 0) {
//            Log.d(TAG, "IN onItemSelected item = 0");

            mNewCategoryEt.setVisibility(View.VISIBLE);
            mRemoveBt.setVisibility(View.GONE);
        } else {
            mNewCategoryEt.setVisibility(View.GONE);
            mRemoveBt.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    private void refreshSpinner() {
        mSpinnerAdapter.clear();
        mSpinnerAdapter.addAll(loadCategories());
        mSpinnerAdapter.notifyDataSetChanged();
    }

    private void setOnClick(int id) {
        findViewById(id).setOnClickListener(this);
    }

    private void showToastMessage(String message) {
        if (mToast != null) mToast.cancel();
        mToast = Toast.makeText(this, message, Toast.LENGTH_LONG);
        mToast.show();
    }

    private void unsetOnClick(int id) {
        findViewById(id).setOnClickListener(null);
    }

    public static class DownloadTask extends AsyncTask<String, Void, Void> {

        private String errorMessage;

        @Override
        protected Void doInBackground(String... strings) {

            if (errorMessage != null) {
                return null;
            }

            File toFile = new File(strings[1]);
            try (
                    OutputStream outStream = new FileOutputStream(toFile, false)
            ) {
                mainInstance.mDropboxClient.files()
                        .downloadBuilder("/" + strings[0]).download(outStream);
            } catch (IOException | DbxException e) {
                errorMessage = "EXCEPTION OCCURRED: " + e.getMessage();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
//            mainInstance.showToastMessage("mDropboxClient = " + mainInstance.mDropboxClient);
            if (!mainInstance.isConnected()) {
                errorMessage = "NO INTERNET CONNECTION";
                return;
            }

//            if (mainInstance.mDropboxClient == null) {
//                Log.d(TAG, "Checking mDropboxClient for null");
//                try {
//                    new InitDropboxTask().execute().get();
////                    wait(1000);
//                } catch (ExecutionException | InterruptedException e) {
//                    errorMessage = "EXCEPTION OCCURRED: " + e.getMessage();
//                }
//            }
            mainInstance.mDBManager.closeDB();
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
            if (!mainInstance.mDBManager.isOpened()) mainInstance.mDBManager.openDB();
            if (errorMessage == null) {
                mainInstance.refreshSpinner();
            }
            mainInstance.setOnClick(R.id.bt_import_db);
            mainInstance.showToastMessage(errorMessage != null ? errorMessage : "DOWNLOAD TASK FINISHED");
//            Log.i(TAG, "DOWNLOAD TASK FINISHED");
        }

    }

    public static class InitDropboxTask extends AsyncTask<Void, Void, Void> {
        public static final int NOTHING_ON_POST = 0;
        public static final int DOWNLOAD_ON_POST = 1;
        public static final int UPLOAD_ON_POST = 2;

        private final int onPost;
        private final int viewId;
        private String errorMessage;
        private boolean isRefreshed;

        public InitDropboxTask(int onPost, int viewId) {
            super();
            this.onPost = onPost;
            this.viewId = viewId;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (errorMessage != null) return null;
            if (!mainInstance.mDropboxTokenHolder.isTokenValid()) {
                try {
                    mainInstance.mDropboxTokenHolder.refreshToken();
                    isRefreshed = true;
                } catch (IOException | JSONException e) {
                    errorMessage = "EXCEPTION OCCURRED: " + e.getMessage();
                }
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (!mainInstance.isConnected()) {
                errorMessage = "NO INTERNET CONNECTION";
                return;
            }
            if (mainInstance.mDropboxTokenHolder == null)
                mainInstance.mDropboxTokenHolder = DropboxTokenHolder.getInstance(mainInstance.getApplicationContext());
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
            mainInstance.showToastMessage(errorMessage != null ? errorMessage : "DROPBOX INIT TASK FINISHED");
            if (errorMessage == null) {
                if (isRefreshed) {
                    mainInstance.mDropboxTokenHolder.persistToken(MainActivity.mainInstance.getApplicationContext());
                    DbxRequestConfig config = DbxRequestConfig.newBuilder("dropbox/app_notebook").build();
                    mainInstance.mDropboxClient = new DbxClientV2(config, mainInstance.mDropboxTokenHolder.getShortTermToken());
                    mainInstance.showToastMessage("DOWNLOAD EXECUTED WHILE INIT DROPBOX");
                } else if (mainInstance.mDropboxClient == null) {
                    DbxRequestConfig config = DbxRequestConfig.newBuilder("dropbox/app_notebook").build();
                    mainInstance.mDropboxClient = new DbxClientV2(config, mainInstance.mDropboxTokenHolder.getShortTermToken());
                }
                switch (onPost) {
                    case DOWNLOAD_ON_POST:
                        new DownloadTask().execute(DBConstants.DB_NAME, mainInstance.generateSqliteDBPath(DBConstants.DB_NAME));
                        break;
                    case UPLOAD_ON_POST:
                        new UploadTask().execute(mainInstance.generateSqliteDBPath(DBConstants.DB_NAME), DBConstants.DB_NAME);
                        break;
                    default:
                }
            }
            mainInstance.setOnClick(viewId);
        }
    }

    public static class UploadTask extends AsyncTask<String, Void, FileMetadata> {

        private String errorMessage;

        @Override
        protected FileMetadata doInBackground(String... strings) {

            if (errorMessage != null) {
                return null;
            }
            File fromFile = new File(strings[0]);

            try (
                    InputStream in = new FileInputStream(fromFile)
            ) {
                return MainActivity.mainInstance.mDropboxClient.files().uploadBuilder("/" + strings[1])
                        .withMode(WriteMode.OVERWRITE)
                        .uploadAndFinish(in);
            } catch (IOException | DbxException e) {
                errorMessage = "EXCEPTION OCCURRED: " + e;
//                Log.e(TAG, e.toString());
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (!MainActivity.mainInstance.isConnected()) {
                errorMessage = "NO INTERNET CONNECTION";
                return;
            }
//            if (mainInstance.mDropboxClient == null) {
//                try {
//                    new InitDropboxTask().execute().get();
//                } catch (ExecutionException | InterruptedException e) {
//                    errorMessage = "EXCEPTION OCCURRED: " + e.getMessage();
//                }
//            }
        }

        @Override
        protected void onPostExecute(FileMetadata fileMetadata) {
            super.onPostExecute(fileMetadata);
            mainInstance.setOnClick(R.id.bt_export_db);
            mainInstance.showToastMessage(errorMessage != null ? errorMessage : "UPLOAD TASK FINISHED");
//            Log.i(TAG, "UPLOAD TASK FINISHED");
        }

    }

}