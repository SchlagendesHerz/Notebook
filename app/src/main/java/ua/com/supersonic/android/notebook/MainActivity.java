package ua.com.supersonic.android.notebook;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.MotionEventCompat;
import androidx.viewpager.widget.ViewPager;

import com.dropbox.core.android.Auth;
import com.dropbox.core.v2.users.FullAccount;
import com.google.android.material.tabs.TabLayout;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;

import ua.com.supersonic.android.notebook.adapters.NotebookPagerAdapter;
import ua.com.supersonic.android.notebook.custom_views.InterceptConstraintLayout;
import ua.com.supersonic.android.notebook.db.DBConstants;
import ua.com.supersonic.android.notebook.db.DBManager;
import ua.com.supersonic.android.notebook.db.dropbox.DropboxDBSynchronizer;
import ua.com.supersonic.android.notebook.utils.Utils;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener,
        InterceptConstraintLayout.DirectMotionListener, GestureDetector.OnGestureListener, View.OnClickListener {
    public static final String PREFERENCE_FILE_KEY = "pref_file_key";
    public static final String APP_LOCALE = "en";
    public static final String DBX_AUTH_ACTIVITY_LAUNCHED_KEY = "dbx_auth_activity_launched";
    private static final int LONG_PRESS_DURATION = (int) (1.5 * 1000);
    private static final String TAG = MainActivity.class.getSimpleName().toUpperCase();

    //    views
    private NotebookPagerAdapter mPagerAdapter;
    private ViewPager mViewPager;
    private View mTransparentView;
    private InterceptConstraintLayout mMainRootContainer;

    //    vars
    private GestureDetector mGestureDetector;
    private AsyncExecutor<String, Void, Void> mAsyncExecutor;
    private boolean isTransparentViewOn;
    private boolean isAuthLaunched;

    private float mDownPointX;
    private float mDownPointY;


    public NotebookPagerAdapter getPagerAdapter() {
        return mPagerAdapter;
    }

    public ViewPager getViewPager() {
        return mViewPager;
    }

    @Override
    public void onClick(View view) {
        Log.d("ON_TOUCH", "ON CLICK TRANSPARENT VIEW");
        performDbxReLog();
        toggleTransparentView();
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {
        Log.d("ON_TOUCH", "LONG PRESS");
        if (!isTransparentViewOn) {
            toggleTransparentView();
        }
    }

    private void toggleTransparentView() {
        isTransparentViewOn = !isTransparentViewOn;
        if (isTransparentViewOn) {
            mTransparentView.setVisibility(View.VISIBLE);
            mTransparentView.setOnTouchListener(this);
            Utils.hideKeyboard(this);
        } else {
            mTransparentView.setVisibility(View.INVISIBLE);
            mTransparentView.setOnTouchListener((view, event) -> isTransparentViewOn);
        }
    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    private boolean isClick(float startX, float startY, float endX, float endY) {
//        Log.d("ON_TOUCH", "startX = " + startX + "; endX = " + endX);
//        Log.d("ON_TOUCH", "startY = " + startY + "; endY = " + endY);

        return Float.compare(startX, endX) == 0 && Float.compare(startY, endY) == 0;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
//        Log.d("ON_TOUCH", "ON TOUCH TRANSPARENT VIEW");
        float curX = motionEvent.getX();
        float curY = motionEvent.getY();

        int action = MotionEventCompat.getActionMasked(motionEvent);

        if (action == MotionEvent.ACTION_DOWN) {
            mDownPointX = curX;
            mDownPointY = curY;
        }

        if (action == MotionEvent.ACTION_UP) {
            if (isClick(mDownPointX, mDownPointY, curX, curY)) {
//                Log.d("ON_TOUCH", "CLICK DETECTED");
                toggleTransparentView();
                return isTransparentViewOn;
            } else if (isUpSwipe(mDownPointX, mDownPointY, curX, curY)) {
//                Log.d("ON_TOUCH", "UP SWIPE DETECTED");
                toggleTransparentView();
                performDbxExport();
            } else if (isDownSwipe(mDownPointX, mDownPointY, curX, curY)) {
//                Log.d("ON_TOUCH", "DOWN SWIPE DETECTED");
                toggleTransparentView();
                performDbxImport();
            }
        }

        return isTransparentViewOn;
    }

    @Override
    public void receiveEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    @Override
    protected void onPause() {
        super.onPause();
//        if (!isAppRunning()) {
//            DropboxDBSynchronizer.getInstance().performDropboxExportTask();
//        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isAuthLaunched) {
            isAuthLaunched = false;

            DropboxDBSynchronizer
                    .getInstance(this, getString(R.string.dbx_api_app_name))
                    .updateDbxCredential(Auth.getDbxCredential());

            Log.d("DROPBOX", "ON RESUME AUTH");
            Utils.logDbxCredentials(DropboxDBSynchronizer
                    .getInstance(this, getString(R.string.dbx_api_app_name)).getDbxCredential());

            performDbxLogIn();
        }
    }

    private void performDbxLogIn() {
        String dbxAppName = getString(R.string.dbx_api_app_name);
        DropboxDBSynchronizer synchronizer = DropboxDBSynchronizer
                .getInstance(this, dbxAppName);
        if (!synchronizer.isAuthDone()) {
            isAuthLaunched = true;
            Utils.runAsync(synchronizer::performAuth);
            return;
        }
        FullAccount account = synchronizer.getDbxAccount();
        if (account != null) {
            String name = account.getName().getDisplayName();
            String email = account.getEmail();
            String postMsg = String.format(getString(R.string.msg_dbx_login_already_format), name, email);
            Utils.showOneButtonDialogBox(this, postMsg);
            return;
        }

        mAsyncExecutor.setMainAction((args) -> synchronizer.performDbxAccountRequest());
        mAsyncExecutor.setPostAction((args) -> {
            FullAccount dbxAccount = synchronizer.getDbxAccount();
            String name = dbxAccount.getName().getDisplayName();
            String email = dbxAccount.getEmail();
            String postMsg = getString(R.string.msg_dbx_login_success) + "\n" +
                    String.format(getString(R.string.msg_dbx_login_cur_format), name, email);
            Utils.showOneButtonDialogBox(this, postMsg);
        });
        mAsyncExecutor.setErrorAction((args) -> Utils.showOneButtonDialogBox(this, getErrorMsgForAlertBox(synchronizer)));
        mAsyncExecutor.execute();
        refreshTransparentView();

    }

    private String getErrorMsgForAlertBox(DropboxDBSynchronizer synchronizer) {
        StringBuilder msgBuilder = new StringBuilder(getString(R.string.error_msg_exception_occurred));
        String errorMsg = synchronizer.getMessage();
        if (errorMsg == null) errorMsg = mAsyncExecutor.getErrorMsg();
        return msgBuilder.append(errorMsg).toString();
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
        DBManager.getInstance(this).openDB();
        mPagerAdapter = new NotebookPagerAdapter(getSupportFragmentManager(), this);
        mViewPager = findViewById(R.id.view_pager);
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        mViewPager.setAdapter(mPagerAdapter);
        tabLayout.setupWithViewPager(mViewPager);
//        ((NonSwipeableViewPager) mViewPager).setSwipeEnabled(false);
        disableTabsOnTouch(tabLayout);

        mGestureDetector = new GestureDetector(this, this);
        mGestureDetector.setIsLongpressEnabled(true);

        mMainRootContainer = findViewById(R.id.main_root_container);
        mMainRootContainer.addDirectMotionListener(this);
        mTransparentView = findViewById(R.id.transparent_view);
        mTransparentView.findViewById(R.id.tv_dbx_login).setOnClickListener(this);
//        mTransparentView.setOnClickListener(this);

        mTransparentView.setOnTouchListener((view, event) -> isTransparentViewOn);
        mTransparentView.setBackground(AppCompatResources.getDrawable(this, R.drawable.transparent_bk));
        mTransparentView.setAlpha(0.9f);
        refreshTransparentView();

        mAsyncExecutor = new AsyncExecutor<>();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DBManager.getInstance(this).closeDB();
//        showToastMessage("isAppRunning = " + isAppRunning());

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

    private boolean isDownSwipe(float startX, float startY, float endX, float endY) {
        return isVerticalSwipe(startX, startY, endX, endY) && startY < endY;
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

    private boolean isUpSwipe(float startX, float startY, float endX, float endY) {
        return isVerticalSwipe(startX, startY, endX, endY) && startY > endY;
    }

    private boolean isVerticalSwipe(float startX, float startY, float endX, float endY) {
        return Math.abs(startX - endX) < Math.abs(startY - endY);
    }

    private void performDbxExport() {
        String dbxAppName = getString(R.string.dbx_api_app_name);
        DropboxDBSynchronizer synchronizer = DropboxDBSynchronizer
                .getInstance(this, dbxAppName);

        if (!synchronizer.isAuthDone()) {
            String msg = getString(R.string.error_msg_dbx_auth_required) +
                    "\n\n" + getString(R.string.msg_dbx_login_required);
            Utils.showTwoButtonDialogBox(this, msg, (var1, var2) -> performDbxLogIn());
            return;
        }

        Predicate<String[]> mainExportAction = args -> synchronizer.performDbxExport(args[0]);
        Consumer<Void[]> postExportAction = args -> Utils
                .showToastMessage(getString(R.string.msg_dbx_export_success), this);

        mAsyncExecutor.setMainAction(mainExportAction, DBConstants.DB_NAME, getString(R.string.dbx_api_app_name));
        mAsyncExecutor.setPostAction(postExportAction);
        mAsyncExecutor.setErrorAction((args) -> Utils.showOneButtonDialogBox(this, getErrorMsgForAlertBox(synchronizer)));

        String msg = String.format(getString(R.string.msg_dbx_upload_format), synchronizer.getDbxAccount().getEmail());
        Utils.showTwoButtonDialogBox(this, msg, (arg1, arg2) -> mAsyncExecutor.execute());
    }

    private void performDbxImport() {
        String dbxAppName = getString(R.string.dbx_api_app_name);
        DropboxDBSynchronizer synchronizer = DropboxDBSynchronizer
                .getInstance(this, dbxAppName);

        if (!synchronizer.isAuthDone()) {
            String msg = getString(R.string.error_msg_dbx_auth_required) +
                    "\n\n" + getString(R.string.msg_dbx_login_required);
            Utils.showTwoButtonDialogBox(this, msg, (var1, var2) -> performDbxLogIn());
            return;
        }

        Predicate<String[]> mainImportAction = args -> synchronizer.performDbxImport(args[0]);

        Consumer<Void[]> postImportAction = args -> Utils
                .showToastMessage(getString(R.string.msg_dbx_import_success), this);

        mAsyncExecutor.setMainAction(mainImportAction, DBConstants.DB_NAME);
        mAsyncExecutor.setPostAction(postImportAction);
        mAsyncExecutor.setErrorAction((args) -> Utils.showOneButtonDialogBox(this, getErrorMsgForAlertBox(synchronizer)));

        String msg = String.format(getString(R.string.msg_dbx_download_format), synchronizer.getDbxAccount().getEmail());
        Utils.showTwoButtonDialogBox(this, msg, (arg1, arg2) -> mAsyncExecutor.execute());
    }

    private void performDbxReLog() {
        String dbxAppName = getString(R.string.dbx_api_app_name);
        DropboxDBSynchronizer synchronizer = DropboxDBSynchronizer
                .getInstance(this, dbxAppName);

        if (!synchronizer.isAuthDone()) {
            performDbxLogIn();
            return;
        }

        mAsyncExecutor.setMainAction(args -> synchronizer.resetAuth());
        mAsyncExecutor.setErrorAction((args) -> Utils.showOneButtonDialogBox(this, getErrorMsgForAlertBox(synchronizer)));
        mAsyncExecutor.setPostAction((args) -> performDbxLogIn());

        String msg = getReLogInMsgForAlertBox(synchronizer);
        Utils.showTwoButtonDialogBox(this, msg, (var1, var2) -> mAsyncExecutor.execute());
    }

    private String getReLogInMsgForAlertBox(DropboxDBSynchronizer synchronizer) {
        FullAccount account = synchronizer.getDbxAccount();
        String name = account.getName().getDisplayName();
        String email = account.getEmail();
        return String.format(getString(R.string.msg_dbx_login_cur_format), name, email) +
                "\n" + getString(R.string.msg_dbx_relogin_prompt);
    }

    private void refreshTransparentView() {
        String dbxAppName = getString(R.string.dbx_api_app_name);
        TextView tvDbxLogin = mTransparentView.findViewById(R.id.tv_dbx_login);

        if (DropboxDBSynchronizer.getInstance(this, dbxAppName).isAuthDone()) {
            tvDbxLogin.setText(getString(R.string.msg_dbx_press_to_change));
            tvDbxLogin.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_relogin,
                    0, 0, 0);
        } else {
            tvDbxLogin.setText(getString(R.string.msg_dbx_press_to_auth));
            tvDbxLogin.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_login,
                    0, 0, 0);
        }
    }

    public static class AsyncExecutor<MA, PA, EA> {
        private final Handler handler;
        private Consumer<EA[]> errorAction;
        private EA[] errorArgs;
        private String errorMsg;
        private Predicate<MA[]> mainAction;
        private MA[] mainArgs;
        private Consumer<PA[]> postAction;
        private PA[] postArgs;

        @SuppressLint("HandlerLeak")
        public AsyncExecutor() {
            handler = new Handler() {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    if (msg.what == 1) {
                        if (postAction != null) {
                            postAction.accept(postArgs);
                        }
                    } else if (msg.what == 0) {
                        if (errorAction != null) {
                            errorAction.accept(errorArgs);
                        }
                    }
                }
            };
        }

        public void execute() {
            errorMsg = null;
            new Thread() {
                @Override
                public void run() {
                    if (mainAction != null) {
                        try {
                            if (mainAction.test(mainArgs)) {
                                if (postAction != null) {
                                    handler.sendEmptyMessage(1);
                                }
                            } else {
                                if (errorAction != null) {
                                    handler.sendEmptyMessage(0);
                                }
                            }
                        } catch (Exception ex) {
                            if (errorAction != null) {
                                errorMsg = ex.getMessage();
                                handler.sendEmptyMessage(0);
                            }
                        }
                    }
                }
            }.start();
        }

        public String getErrorMsg() {
            return errorMsg;
        }

        public void setErrorAction(Consumer<EA[]> errorAction, EA... errorArgs) {
            this.errorAction = errorAction;
            this.errorArgs = errorArgs;
        }

        public void setMainAction(Predicate<MA[]> mainAction, MA... mainArgs) {
            this.mainAction = mainAction;
            this.mainArgs = mainArgs;
        }

        public void setPostAction(Consumer<PA[]> postAction, PA... postArgs) {
            this.postAction = postAction;
            this.postArgs = postArgs;
        }

    }

}