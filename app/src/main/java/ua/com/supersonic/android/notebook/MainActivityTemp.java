/*
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
import java.util.function.Predicate;

import ua.com.supersonic.android.notebook.db.DBConstants;
import ua.com.supersonic.android.notebook.db.DBManager;

public class MainActivityTemp extends AppCompatActivity implements View.OnClickListener,
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
    private DropboxTokenHolder mDropboxTokenHolder;
    private EditText mNewCategoryEt;
    private FloatingActionButton mRemoveBt;
    private ArrayAdapter<String> mSpinnerAdapter;
    private Toast mToast;

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
        categories.addAll(mDBManager.readFromDB(DBConstants.TABLE_CATEGORIES));
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
                mDBManager.deleteFromDB(toDelete);
                refreshSpinner();
                break;
            case R.id.bt_export_db:
                unsetOnClick(R.id.bt_export_db);
                performDropboxExportTask();
                setOnClick(R.id.bt_export_db);
                break;
            case R.id.bt_import_db:
                unsetOnClick(R.id.bt_import_db);
                performDropboxImportTask();
                refreshSpinner();
                setOnClick(R.id.bt_import_db);
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

    private boolean performDropboxCheck() {
        String toastMessage;
        if (!mainInstance.isConnected()) {
            toastMessage = "NO INTERNET CONNECTION";
            mainInstance.showToastMessage(toastMessage);
            return false;
        }
        if (mDropboxTokenHolder == null) {
            mDropboxTokenHolder = DropboxTokenHolder.getInstance(mainInstance.getApplicationContext());
        }
        if (!mDropboxTokenHolder.isTokenValid()) {
            try {
                performTokenRefreshTask();
            } catch (ExecutionException | InterruptedException | RuntimeException e) {
                toastMessage = "EXCEPTION OCCURRED " + e.getMessage();
                mainInstance.showToastMessage(toastMessage);
                return false;
            }
        } else if (mDropboxClient == null) {
            DbxRequestConfig config = DbxRequestConfig.newBuilder("dropbox/app_notebook").build();
            mDropboxClient = new DbxClientV2(config, mainInstance.mDropboxTokenHolder.getShortTermToken());
        }
        return true;
    }

    private void performDropboxExportTask() {
        boolean isDropboxReady = performDropboxCheck();
        if (isDropboxReady) {
            Predicate<String[]> dropboxExportPredicate = (args) -> {
                File fromFile = new File(args[0]);

                try (
                        InputStream in = new FileInputStream(fromFile)
                ) {
                    MainActivity.mainInstance.mDropboxClient.files().uploadBuilder("/" + args[1])
                            .withMode(WriteMode.OVERWRITE)
                            .uploadAndFinish(in);
                    return true;
                } catch (IOException | DbxException e) {
                    throw new RuntimeException(e);
                }
            };
            boolean isExportSucceed = performPredicateTask(dropboxExportPredicate, mainInstance.generateSqliteDBPath(DBConstants.DB_NAME), DBConstants.DB_NAME);
            mainInstance.showToastMessage("EXPORT TASK IS " + String.valueOf(isExportSucceed).toUpperCase());
        }
    }

    private void performDropboxImportTask() {
        boolean isDropboxReady = performDropboxCheck();
        if (isDropboxReady) {
            Predicate<String[]> dropboxImportPredicate = (args) -> {
                File toFile = new File(args[1]);
                try (
                        OutputStream outStream = new FileOutputStream(toFile, false)
                ) {
                    mainInstance.mDropboxClient.files()
                            .downloadBuilder("/" + args[0]).download(outStream);
                    return true;
                } catch (IOException | DbxException e) {
                    throw new RuntimeException(e);
                }
            };
            boolean isImportSucceed = performPredicateTask(dropboxImportPredicate, DBConstants.DB_NAME, mainInstance.generateSqliteDBPath(DBConstants.DB_NAME));
            mainInstance.showToastMessage("IMPORT TASK IS " + String.valueOf(isImportSucceed).toUpperCase());
        }
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
            mainInstance.showToastMessage(toastMessage);
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
            mDropboxTokenHolder.persistToken(getApplicationContext());
            DbxRequestConfig config = DbxRequestConfig.newBuilder("dropbox/app_notebook").build();
            mDropboxClient = new DbxClientV2(config, mainInstance.mDropboxTokenHolder.getShortTermToken());
        }
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

    public static class PredicateTask extends AsyncTask<String, Void, Boolean> {

        private final Predicate<String[]> action;
        private String errorMessage;

        public PredicateTask(Predicate<String[]> action) {
            super();
            this.action = action;
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

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}*/
