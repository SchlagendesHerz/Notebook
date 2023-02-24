package ua.com.supersonic.android.notebook;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import ua.com.supersonic.android.notebook.adapters.NotebookPagerAdapter;
import ua.com.supersonic.android.notebook.db.DBManager;
import ua.com.supersonic.android.notebook.fragments.NotebookCategoriesFragment;
import ua.com.supersonic.android.notebook.fragments.NotebookRecordsFragment;
import ua.com.supersonic.android.notebook.widgets.NonSwipeableViewPager;

public class MainActivity extends AppCompatActivity {
    public static final String PREFERENCE_FILE_KEY = "pref_file_key";
    public static final String APP_LOCALE = "en";
    private static final String TAG = MainActivity.class.getSimpleName().toUpperCase();

    public static MainActivity mainInstance;
    public static NotebookCategoriesFragment categoriesFragment;
    public static NotebookRecordsFragment recordsFragment;

    public static void hideKeyboard() {
//        Log.d(TAG, "hideKeyBoard invoked");
        InputMethodManager imm = (InputMethodManager) mainInstance.getSystemService(Activity.INPUT_METHOD_SERVICE);
//        //Find the currently focused view, so we can grab the correct window token from it.
        View view = mainInstance.getCurrentFocus();
//        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
//            Log.d(TAG, "view = null");
            view = new View(mainInstance);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
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

    public static void showKeyboardOnFocus(View view) {
        view.requestFocus();
        InputMethodManager imm = (InputMethodManager) mainInstance.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }

    private Toast mToast;
    private NotebookPagerAdapter mPagerAdapter;
    private ViewPager mViewPager;

    public ViewPager getViewPager() {
        return mViewPager;
    }

    public SharedPreferences getSharedPreferences() {
        String sharedPrefFileName = getApplicationContext().getPackageName() + "." + MainActivity.PREFERENCE_FILE_KEY;
        return getSharedPreferences(sharedPrefFileName, Context.MODE_PRIVATE);
    }

    public NotebookPagerAdapter getPagerAdapter() {
        return mPagerAdapter;
    }

    public void showDialogBox(int msgId, DialogInterface.OnClickListener yesBtListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(msgId)
                .setPositiveButton(R.string.dialog_box_yes_bt, yesBtListener)
                .setNegativeButton(R.string.dialog_box_no_bt, (dialogInterface, i) -> dialogInterface.dismiss())
                .show();
    }

    public void showToastMessage(String message) {
        if (mToast != null) mToast.cancel();
        mToast = Toast.makeText(this, message, Toast.LENGTH_LONG);
        mToast.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainInstance = this;
        if (categoriesFragment == null) categoriesFragment = new NotebookCategoriesFragment();
        if (recordsFragment == null) recordsFragment = new NotebookRecordsFragment();
        setContentView(R.layout.activity_main);
        init();
    }

//    @Override
//    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
//        super.onRestoreInstanceState(savedInstanceState);
//        boolean isDropboxImportPerformed = savedInstanceState.getBoolean(IS_DROPBOX_IMPORT_PERFORMED_KEY);
//        showToastMessage("isDropboxImportPerformed = " + isDropboxImportPerformed);
//        if (!isDropboxImportPerformed) {
//            mDropboxSynchronizer.performDropboxImportTask();
//        }
//    }

    @Override
    protected void onPause() {
        super.onPause();
//        if (!isAppRunning()) {
//            DropboxDBSynchronizer.getInstance().performDropboxExportTask();
//        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DBManager.getInstance().closeDB();
//        showToastMessage("isAppRunning = " + isAppRunning());

    }

    @SuppressLint("ClickableViewAccessibility")
    private void init() {
        /*Editable editable = new SpannableStringBuilder("abc");
        editable.replace(0, 0, "o");
        Log.d("MAINACTIVITY", "editable = " + editable);*/
        setLocale(APP_LOCALE);
        DBManager.getInstance().openDB();
        mPagerAdapter = new NotebookPagerAdapter(getSupportFragmentManager());
        mViewPager = findViewById(R.id.view_pager);
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        mViewPager.setAdapter(mPagerAdapter);
        tabLayout.setupWithViewPager(mViewPager);
//        ((NonSwipeableViewPager) mViewPager).setSwipeEnabled(false);
        disableTabsOnTouch(tabLayout);
//        if (!isDropboxImportPerformed) {
//            DropboxDBSynchronizer.getInstance().performDropboxImportTask();
//            isDropboxImportPerformed = true;
//        }
    }

    private void setLocale(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Resources resources = getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    @SuppressLint("ClickableViewAccessibility")
    private void disableTabsOnTouch(TabLayout tabLayout) {
        LinearLayout tabStrip = ((LinearLayout) tabLayout.getChildAt(0));
        for (int i = 0; i < tabStrip.getChildCount(); i++) {
            tabStrip.getChildAt(i).setOnTouchListener((view, motionEvent) -> true);
        }
    }

    private boolean isAppRunning() {
        ActivityManager m = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTaskInfoList = m.getRunningTasks(10);
        Iterator<ActivityManager.RunningTaskInfo> itr = runningTaskInfoList.iterator();
        int n = 0;
        while (itr.hasNext()) {
            n++;
            if (n > 1) return true;
            itr.next();
        }
        // App is killed
        return false;// App is in background or foreground
    }

}