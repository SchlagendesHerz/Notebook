package ua.com.supersonic.android.notebook;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.WriteMode;
import com.dropbox.core.v2.users.FullAccount;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import ua.com.supersonic.android.notebook.db.DBConstants;

public class DropboxExperimentsActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String ACCESS_TOKEN_LONG_TERM = "sl.BNGoaEaZ7-1jBWr6fv0mo6BW-pWRfMFQsefdOuy-Oiv1SdMAmGD4lq8ilKYAkSQ99q59E14GgHWZCHy9YhY7kkNr9Iza0jIF83MSD0QKPzfX5OiXEVjByTIdG-qyK4P7eSmpE7_5KAE";
    public static final String ACCESS_TOKEN_LONG_TERM_2 = "sl.BNFhwRUadCRiKkgQnOMzVWH5exHez-Eu2_xdQfHR30-VvgYDchigR2UUthyKFyEeYWbKj_837s6RlYtkCedyZqTEEcb1i62mcGEgajY9PMJ7d2sh6uHRQfccCjc_U6mTDWUzFBd4Vew";
    private static final String DB_FILEPATH_FORMAT_STRING = "/data/data/%s/databases/%s";
    private static final String FROM_FILE_NAME = "test";
    private static final String TO_FILE_NAME = "test.txt";
    private static final String TAG = DropboxExperimentsActivity.class.getSimpleName().toUpperCase();

    private DbxClientV2 mClient;

    public static class UploadTask extends AsyncTask<String, Void, FileMetadata> {
        private DbxClientV2 mDropboxClient;

        public UploadTask(DbxClientV2 mDropboxClient) {
            super();
            this.mDropboxClient = mDropboxClient;
        }

        @Override
        protected FileMetadata doInBackground(String... strings) {
            File fromFile = new File(strings[0]);

            try (
                    InputStream in = new FileInputStream(fromFile)
            ) {
                return mDropboxClient.files().uploadBuilder("/" + strings[1])
                        .withMode(WriteMode.OVERWRITE)
                        .uploadAndFinish(in);
            } catch (IOException | DbxException e) {
                Log.e(TAG, e.toString());
            }
            return null;
        }

        @Override
        protected void onPostExecute(FileMetadata fileMetadata) {
            super.onPostExecute(fileMetadata);
            Log.i(TAG, "UPLOAD TASK FINISHED");
        }
    }


    public static class ConnectionTestTask extends AsyncTask<Void, Void, Void> {
        private DbxClientV2 mClient;

        public ConnectionTestTask(DbxClientV2 mClient) {
            super();
            this.mClient = mClient;
        }

        @Override
        protected Void doInBackground(Void... voids) {

            try {
                FullAccount account = mClient.users().getCurrentAccount();
                Log.i(TAG, "name = " + account.getName().getDisplayName());


                ListFolderResult result = mClient.files().listFolder("");
                while (true) {
                    for (Metadata metadata : result.getEntries()) {
                        Log.i(TAG, metadata.getPathLower());
                    }

                    if (!result.getHasMore()) {
                        break;
                    }

                    result = mClient.files().listFolderContinue(result.getCursor());
                }
            } catch (DbxException e) {
                Log.e(TAG, e.toString());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
            Log.i(TAG, "CONNECTION TEST TASK FINISHED");
        }
    }

    @Override
    public void onClick(View view) {
        Log.i(TAG, "db_path = " + generateDBPath(DBConstants.DB_NAME));
//        new Thread(() -> uploadFile(generateDBPath(DBConstants.DB_NAME), DBConstants.DB_NAME));
//        new Thread(this::connectionTest);
        new ConnectionTestTask(mClient).execute();
        new UploadTask(mClient).execute(generateDBPath(DBConstants.DB_NAME), DBConstants.DB_NAME);
    }

    private String generateDBPath(String dbName) {
//        return getApplicationContext().getFilesDir().getPath();
        return String.format(DB_FILEPATH_FORMAT_STRING, getPackageName(), dbName);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initDropbox();
        findViewById(R.id.bt_export_db).setOnClickListener(this);

//        new Thread(() -> uploadFile(FROM_FILE_NAME, TO_FILE_NAME)).start();
//        new Thread(this::clearAppFolder).start();
    }

    private void initDropbox() {
        DbxRequestConfig config = DbxRequestConfig.newBuilder("dropbox/app_notebook").build();
        mClient = new DbxClientV2(config, ACCESS_TOKEN_LONG_TERM);
    }

    private void connectionTest() {

        try {
            FullAccount account = mClient.users().getCurrentAccount();
            Log.i(TAG, "name = " + account.getName().getDisplayName());


            ListFolderResult result = mClient.files().listFolder("");
            while (true) {
                for (Metadata metadata : result.getEntries()) {
                    Log.i(TAG, metadata.getPathLower());
                }

                if (!result.getHasMore()) {
                    break;
                }

                result = mClient.files().listFolderContinue(result.getCursor());
            }
        } catch (DbxException e) {
            Log.e(TAG, e.toString());
        }
    }

    private void clearAppFolder() {
        try {
            ListFolderResult result = mClient.files().listFolder("");
            while (true) {
                for (Metadata metadata : result.getEntries()) {
                    Log.i(TAG, metadata.getPathLower());
                    mClient.files().deleteV2(metadata.getPathLower());
                }

                if (!result.getHasMore()) {
                    break;
                }

                result = mClient.files().listFolderContinue(result.getCursor());
            }
        } catch (DbxException e) {
            Log.e(TAG, e.toString());
        }
    }

    private void uploadFile(String fromFilePath, String toFileName) {
        connectionTest();
//        int fromFileId = Utils.getRawResId(this, fromFilePath);
        File fromFile = new File(fromFilePath);

        try (
                InputStream in = new FileInputStream(fromFile)
        ) {
            FileMetadata metadata = mClient.files().uploadBuilder("/" + toFileName)
                    .withMode(WriteMode.OVERWRITE)
                    .uploadAndFinish(in);
        } catch (IOException | DbxException e) {
            Log.e(TAG, e.toString());
        }
    }

    private StringBuilder readFromResFile(int fromFileId) {
        StringBuilder builder = new StringBuilder();
        try (
                InputStream in = getResources().openRawResource(fromFileId);
                BufferedReader bufReader = new BufferedReader(new InputStreamReader(in))
        ) {
            String curLine;
            while ((curLine = bufReader.readLine()) != null) {
                builder.append(curLine);
                while ((curLine = bufReader.readLine()) != null) {
                    builder.append(curLine).append("\n");
                }
                if (builder.length() != 0) {
                    builder.delete(builder.length() - 1, builder.length());
                }
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "CAUGHT FileNotFoundException" + "\n" + e);
        } catch (IOException e) {
            Log.e(TAG, "CAUGHT IOException" + "\n" + e);
        }
        return builder;
    }
}
