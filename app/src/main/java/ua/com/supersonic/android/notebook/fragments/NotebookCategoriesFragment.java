package ua.com.supersonic.android.notebook.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.MotionEventCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.dropbox.core.android.Auth;
import com.dropbox.core.v2.users.FullAccount;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import ua.com.supersonic.android.notebook.db.dropbox.DropboxDBSynchronizer;
import ua.com.supersonic.android.notebook.MainActivity;
import ua.com.supersonic.android.notebook.NotebookCategory;
import ua.com.supersonic.android.notebook.R;
import ua.com.supersonic.android.notebook.adapters.CategoryAdapter;
import ua.com.supersonic.android.notebook.db.DBConstants;
import ua.com.supersonic.android.notebook.db.DBManager;
import ua.com.supersonic.android.notebook.custom_views.InterceptConstraintLayout;
import ua.com.supersonic.android.notebook.custom_views.NonSwipeableViewPager;
import ua.com.supersonic.android.notebook.utils.Utils;

public class NotebookCategoriesFragment extends Fragment implements View.OnClickListener,
        AdapterView.OnItemClickListener, ViewPager.OnPageChangeListener {

    private CategoryAdapter mCategoryAdapter;
    private View mCurItemButtons;
    private EditText mEtAEFCategory;
    private View mEtContainer;
    private ListView mLvCategories;
    private InterceptConstraintLayout mRootContainer;

    private boolean mIsAddBtPressed;
    private boolean mIsEditBtPressed;
    private boolean mIsFindBtPressed;
    private boolean mIsListBtPressed;

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bt_list_all:
                btListPressedFirstTime();
                showCategoryList(readAllCategories());
                Utils.hideKeyboard(getAppMainActivity());
                break;
            case R.id.bt_add:
                if (mIsEditBtPressed) {

                    String newCategoryName = getCategoryNameFromEtAEF();
                    if (newCategoryName == null) break;
                    NotebookCategory curCategory = getCurrentCategory();
                    curCategory.setName(newCategoryName);

                    mCategoryAdapter.notifyDataSetChanged();
                    DBManager.getInstance(getContext()).updateCategory(curCategory);

                    mIsEditBtPressed = false;
                    mEtContainer.setVisibility(View.GONE);
                    mLvCategories.setVisibility(View.VISIBLE);
                    resetSelectedItems();
                    resetBtAdd();
                    Utils.hideKeyboard(getAppMainActivity());
                } else {
                    resetSelectedItems();
                    if (mIsAddBtPressed) {
                        String newCategoryName = getCategoryNameFromEtAEF();
                        if (newCategoryName == null) break;

                        NotebookCategory newCategory = new NotebookCategory();
                        newCategory.setName(newCategoryName);
                        DBManager.getInstance(getContext()).addCategory(newCategory);
//                        DropboxDBSynchronizer.getInstance().performDropboxExportTask();
                        mEtAEFCategory.setText("");
                    } else btAddPressedFirstTime();
                }
                break;
            case R.id.bt_edit:
                if (!mIsEditBtPressed) {
                    btEditPressedFirstTime();
                }
                break;
            case R.id.bt_find:
                resetSelectedItems();
                if (mIsFindBtPressed) {
                    String toFindString = mEtAEFCategory.getText().toString().trim();
                    if (!toFindString.isEmpty()) {
                        mLvCategories.setVisibility(View.VISIBLE);
                        mEtContainer.setVisibility(View.VISIBLE);
                        showCategoryList(findCategories(toFindString));
                        Utils.hideKeyboard(getAppMainActivity());
                    } else mLvCategories.setVisibility(View.GONE);
                } else btFindPressedFirstTime();
                break;
            case R.id.bt_clear:
                if (mIsFindBtPressed) {
                    resetSelectedItems();
                    mLvCategories.setVisibility(View.GONE);
                }
                mEtAEFCategory.setText("");
                Utils.showKeyboardOnFocus(mEtAEFCategory, requireContext());
//                mEtAEFCategory.requestFocus();
                break;
            case R.id.bt_rem:
                if (!mCategoryAdapter.getSelectedItems().isEmpty()) {
                    List<NotebookCategory> listToDelete = new ArrayList<>();
                    for (Integer pos : mCategoryAdapter.getSelectedItems()) {
                        listToDelete.add(mCategoryAdapter.getItem(pos));
                    }
                    for (NotebookCategory catToDelete : listToDelete) {
                        mCategoryAdapter.remove(catToDelete);
                    }
                    mCategoryAdapter.notifyDataSetChanged();
                    DBManager.getInstance(getContext()).deleteCategories(listToDelete);
//                    DropboxDBSynchronizer.getInstance().performDropboxExportTask();
                }
                resetSelectedItems();
                break;
            case R.id.bt_show_records:
//                mDBManager.insertToDB(DBConstants.TABLE_ITEMS, "35", "date", "1", "descr");

                int curCategoryId = getCurrentCategory().getId();
                getRecordsFragment().setCurCategoryId(curCategoryId);
                getAppMainActivity().getViewPager().setCurrentItem(1);
                break;
        }
    }

    private void btListPressedFirstTime() {
        resetSelectedItems();
        mIsAddBtPressed = false;
        mIsFindBtPressed = false;
        mIsEditBtPressed = false;
        mIsListBtPressed = true;
        mLvCategories.setVisibility(View.VISIBLE);
        mEtContainer.setVisibility(View.GONE);
        resetBtAdd();
    }

    private void showCategoryList(List<NotebookCategory> input) {
        mCategoryAdapter.clear();
        mCategoryAdapter.addAll(input);
        mCategoryAdapter.notifyDataSetChanged();
        if (mCategoryAdapter.isEmpty()) {
            TextView tvMssage = mRootContainer.findViewById(R.id.tv_message);
            tvMssage.setVisibility(View.VISIBLE);
            tvMssage.setText(getResources().getString(R.string.tv_message_list_empty));
        }
    }

    private List<NotebookCategory> readAllCategories() {
        return DBManager.getInstance(getContext()).readAllCategories();
    }

    private MainActivity getAppMainActivity() {
        return ((MainActivity) requireActivity());
    }

    private String getCategoryNameFromEtAEF() {
        String readValue = mEtAEFCategory.getText()
                .toString().trim().toUpperCase();
        if (readValue.isEmpty()) {
            Utils.showToastMessage(getString(R.string.error_msg_invalid_category_name_input), getContext());
            restoreEtAEFCategoryName();
            return null;
        }
        return readValue;
    }

    private NotebookCategory getCurrentCategory() {
        return mCategoryAdapter.getItem(mCategoryAdapter.getSelectedItems().get(0));
    }

    private void resetSelectedItems() {
        List<Integer> selectedItems = mCategoryAdapter.getSelectedItems();
        if (!selectedItems.isEmpty()) {
//            disableViewPagerSwipe();
            View curListItem;
            for (Integer pos : selectedItems) {
                curListItem = Utils.getViewByPosition(pos, mLvCategories);
                curListItem.setBackgroundColor(getResources().getColor(R.color.white));
                ((TextView) (curListItem.findViewById(R.id.tv_rec_quant))).setTextColor(getResources().getColor(R.color.tv_rec_quant_color));
                ((TextView) (curListItem.findViewById(R.id.tv_last_rec_ago))).setTextColor(getResources().getColor(R.color.tv_last_rec_ago_color));
            }
            selectedItems.clear();
        }
        mCurItemButtons.setVisibility(View.GONE);
        mRootContainer.findViewById(R.id.tv_message).setVisibility(View.GONE);
    }

    private void resetBtAdd() {
        ((Button) mRootContainer.findViewById(R.id.bt_add)).setText(R.string.bt_add);
    }

    private void btAddPressedFirstTime() {
        mEtAEFCategory.setText("");
        mEtAEFCategory.setHint(getResources().getString(R.string.et_add_category_hint));
        mLvCategories.setVisibility(View.GONE);
        mEtContainer.setVisibility(View.VISIBLE);
        mIsAddBtPressed = true;
        mIsEditBtPressed = false;
        mIsFindBtPressed = false;
        mIsListBtPressed = false;
        Utils.showKeyboardOnFocus(mEtAEFCategory, requireContext());
    }

    private void btEditPressedFirstTime() {
        disableViewPagerSwipe();
        mEtAEFCategory.setText(getCurrentCategory().getName());
        mLvCategories.setVisibility(View.GONE);
        mEtContainer.setVisibility(View.VISIBLE);
        ((Button) mRootContainer.findViewById(R.id.bt_add)).setText(R.string.bt_edit);
        mEtAEFCategory.setSelection(mEtAEFCategory.getText().length());

        mIsAddBtPressed = false;
        mIsEditBtPressed = true;
        mIsFindBtPressed = false;
        mIsListBtPressed = false;
        Utils.showKeyboardOnFocus(mEtAEFCategory, requireContext());
    }

    private List<NotebookCategory> findCategories(String toFind) {
        return DBManager.getInstance(getContext()).readCategoriesWhereKeyLike(DBConstants.COLUMN_CATEGORY_NAME, "%" + toFind + "%");
    }

    private void btFindPressedFirstTime() {
        resetBtAdd();
        mEtAEFCategory.setText("");
        mEtAEFCategory.setHint(getResources().getString(R.string.et_find_category_hint));
        mLvCategories.setVisibility(View.GONE);
        mEtContainer.setVisibility(View.VISIBLE);
        mIsAddBtPressed = false;
        mIsEditBtPressed = false;
        mIsFindBtPressed = true;
        mIsListBtPressed = false;
        Utils.showKeyboardOnFocus(mEtAEFCategory, requireContext());
    }

    private NotebookRecordsFragment getRecordsFragment() {
        Fragment page = getAppMainActivity().getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.view_pager + ":" + 1);
        return (NotebookRecordsFragment) page;
    }

    private void restoreEtAEFCategoryName() {
        if (mIsAddBtPressed) {
            mEtAEFCategory.setText("");
        } else if (mIsEditBtPressed) {
            mEtAEFCategory.setText(getCurrentCategory().getName());
            mEtAEFCategory.setSelection(mEtAEFCategory.getText().length());
        }
    }

    private void disableViewPagerSwipe() {
        ((NonSwipeableViewPager) getAppMainActivity().getViewPager()).setSwipeEnabled(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        mRootContainer = (InterceptConstraintLayout) inflater.inflate(R.layout.fragment_categories_records, container, false);
        init();

        return mRootContainer;
    }

    @Override
    public void onResume() {
        super.onResume();

/*        if (isDownloadPending) {
            isDownloadPending = false;

            DropboxDBSynchronizer
                    .getInstance(getContext(), getString(R.string.dbx_api_app_name))
                    .updateDbxCredential(Auth.getDbxCredential());

            Log.d("DROPBOX", "ON RESUME DOWNLOAD");
            Utils.logDbxCredentials(DropboxDBSynchronizer
                    .getInstance(getContext(), getString(R.string.dbx_api_app_name)).getDbxCredential());

            performDbxImport();
            return;
        }

        if (isUploadPending) {
            isUploadPending = false;

            DropboxDBSynchronizer
                    .getInstance(getContext(), getString(R.string.dbx_api_app_name))
                    .updateDbxCredential(Auth.getDbxCredential());

            Log.d("DROPBOX", "ON RESUME UPLOAD");
            Utils.logDbxCredentials(DropboxDBSynchronizer
                    .getInstance(getContext(), getString(R.string.dbx_api_app_name)).getDbxCredential());


            performDbxExport();
        }*/
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
//        DBManager.getInstance().closeDB();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void init() {

        mRootContainer.findViewById(R.id.bt_list_all).setOnClickListener(this);
        mRootContainer.findViewById(R.id.bt_add).setOnClickListener(this);
        mRootContainer.findViewById(R.id.bt_find).setOnClickListener(this);
        mRootContainer.findViewById(R.id.bt_rem).setOnClickListener(this);
        mRootContainer.findViewById(R.id.bt_clear).setOnClickListener(this);
        mRootContainer.findViewById(R.id.bt_edit).setOnClickListener(this);
        mRootContainer.findViewById(R.id.bt_show_records).setOnClickListener(this);

        mEtAEFCategory = mRootContainer.findViewById(R.id.et_add_edit_find);
        mEtContainer = mRootContainer.findViewById(R.id.widgets_container);

        mLvCategories = mRootContainer.findViewById(R.id.lv_items);
        mCategoryAdapter = new CategoryAdapter(getContext());
        mLvCategories.setAdapter(mCategoryAdapter);
        mLvCategories.setOnItemClickListener(this);
        mLvCategories.setVisibility(View.GONE);

        mCurItemButtons = mRootContainer.findViewById(R.id.bottom_bts);
        mCurItemButtons.setVisibility(View.GONE);

        getAppMainActivity().getViewPager().addOnPageChangeListener(this);
        disableViewPagerSwipe();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        switch (adapterView.getId()) {
            case R.id.lv_items:
//                resetListView();
//                mViewItemSelected = view;
                List<Integer> selectedItems = mCategoryAdapter.getSelectedItems();
                if (!selectedItems.contains(i)) {
                    selectedItems.add(i);
                } else {
                    selectedItems.remove((Object) i);
                }
                mCategoryAdapter.notifyDataSetChanged();
                mCurItemButtons.setVisibility(View.VISIBLE);
                if (selectedItems.size() > 1) {
                    mCurItemButtons.findViewById(R.id.bt_edit).setVisibility(View.INVISIBLE);
                    mCurItemButtons.findViewById(R.id.bt_show_records).setVisibility(View.INVISIBLE);
                    disableViewPagerSwipe();
                } else if (selectedItems.size() == 1) {
                    mCurItemButtons.findViewById(R.id.bt_edit).setVisibility(View.VISIBLE);
                    mCurItemButtons.findViewById(R.id.bt_show_records).setVisibility(View.VISIBLE);
                    enableViewPagerSwipe();

                    int lastVisibleItemPosition = mLvCategories.getFirstVisiblePosition() + mLvCategories.getChildCount() - 1;
                    if (i == lastVisibleItemPosition || i == lastVisibleItemPosition - 1) {
                        mLvCategories.smoothScrollToPosition(lastVisibleItemPosition);
                    }
                } else {
                    mCurItemButtons.setVisibility(View.GONE);
                    disableViewPagerSwipe();
                }
                Utils.hideKeyboard(getAppMainActivity());
//                MainActivity.mainInstance.showToastMessage(mSelectedItems.toString());
                break;
        }
    }

    private void enableViewPagerSwipe() {
        ((NonSwipeableViewPager) getAppMainActivity().getViewPager()).setSwipeEnabled(true);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        TabLayout tabLayout = getAppMainActivity().findViewById(R.id.tab_layout);
        switch (position) {
            case 0:
                getRecordsFragment().resetRecords();
                refreshCurCategoryItem();
                tabLayout.getTabAt(0).setText(getString(R.string.tab_categories));
                break;
            case 1:
                int curCategoryId = getCurrentCategory().getId();
                getRecordsFragment().setCurCategoryId(curCategoryId);
                tabLayout.getTabAt(0).setText(getCurrentCategory().getName().toUpperCase());
//                recordsFragment.showRecordList();
//                MainActivity.mainInstance.getPagerAdapter().setCurrentCategory(getCurrentCategory().getName());
                break;
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    private void refreshCurCategoryItem() {
        NotebookCategory curCategoryFromAdapter = getCurrentCategory();
        NotebookCategory curCategoryFromDB = DBManager.getInstance(getContext())
                .readCategoriesWhereKeyEquals(DBConstants.COLUMN_CATEGORY_ID, String.valueOf(curCategoryFromAdapter.getId()))
                .get(0);
        curCategoryFromAdapter.setRecordQuantity(curCategoryFromDB.getRecordQuantity());
        curCategoryFromAdapter.setLastRecordDate(curCategoryFromDB.getLastRecordDate());
        mCategoryAdapter.notifyDataSetChanged();
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
