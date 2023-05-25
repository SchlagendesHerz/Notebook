package ua.com.supersonic.android.notebook;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import ua.com.supersonic.android.notebook.adapters.NotebookPagerAdapter;
import ua.com.supersonic.android.notebook.db.DBManager;

public class MainActivity extends AppCompatActivity {
    public static final String PREFERENCE_FILE_KEY = "pref_file_key";
    public static final String APP_LOCALE = "en";
    private static final String TAG = MainActivity.class.getSimpleName().toUpperCase();

    private NotebookPagerAdapter mPagerAdapter;
    private ViewPager mViewPager;

    public ViewPager getViewPager() {
        return mViewPager;
    }

    public NotebookPagerAdapter getPagerAdapter() {
        return mPagerAdapter;
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        DBManager.getInstance(getApplicationContext()).closeDB();
//        showToastMessage("isAppRunning = " + isAppRunning());

    }

    @SuppressLint("ClickableViewAccessibility")
    private void init() {
        /*Editable editable = new SpannableStringBuilder("abc");
        editable.replace(0, 0, "o");
        Log.d("MAINACTIVITY", "editable = " + editable);*/
//        getSupportFragmentManager().beginTransaction().remove(new NotebookCategoriesFragment()).commit();
//        getSupportFragmentManager().beginTransaction().remove(new NotebookRecordsFragment()).commit();


//        if (categoriesFragment == null) categoriesFragment = new NotebookCategoriesFragment();
//        if (recordsFragment == null) recordsFragment = new NotebookRecordsFragment();


        setLocale(APP_LOCALE);
        DBManager.getInstance(getApplicationContext()).openDB();
        mPagerAdapter = new NotebookPagerAdapter(getSupportFragmentManager(), this);
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