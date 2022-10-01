package ua.com.supersonic.android.notebook.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.core.view.MotionEventCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;
import java.util.List;

import ua.com.supersonic.android.notebook.db.dropbox.DropboxDBSynchronizer;
import ua.com.supersonic.android.notebook.MainActivity;
import ua.com.supersonic.android.notebook.NotebookCategory;
import ua.com.supersonic.android.notebook.R;
import ua.com.supersonic.android.notebook.adapters.CategoryAdapter;
import ua.com.supersonic.android.notebook.db.DBConstants;
import ua.com.supersonic.android.notebook.db.DBManager;
import ua.com.supersonic.android.notebook.widgets.NonSwipeableViewPager;

public class NotebookCategoriesFragment extends Fragment implements View.OnClickListener,
        AdapterView.OnItemClickListener, ViewPager.OnPageChangeListener, View.OnTouchListener {
    private static final int MIN_SWIPE_DISTANCE = 100;

    private CategoryAdapter mCategoryAdapter;

    private ListView mLvCategories;
    private EditText mEtAEFCategory;
    private View mEtContainer;

    private final List<Integer> mSelectedItems = new ArrayList<>();
    private View mCurItemButtons;
    private View mRootContainer;

    private boolean mIsAddBtPressed;
    private boolean mIsEditBtPressed;
    private boolean mIsFindBtPressed;

    private float mStartDragX;
    private float mStartDragY;
    private boolean isUpSwipe;

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bt_list_all:
                resetListView();
                mIsAddBtPressed = false;
                mIsFindBtPressed = false;
                mIsEditBtPressed = false;
                mLvCategories.setVisibility(View.VISIBLE);
                mEtContainer.setVisibility(View.GONE);
                resetBtAdd();
//                DropboxDBSynchronizer.getInstance().performDropboxImportTask();
                showCategoryList(readAllCategories());
                MainActivity.hideKeyboard();
                break;
            case R.id.bt_add:
                if (mIsEditBtPressed) {

                    String newCategoryName = getCategoryNameFromEtAEF();
                    if (newCategoryName == null) break;
                    NotebookCategory curCategory = getCurrentCategory();
                    curCategory.setName(newCategoryName);

                    mCategoryAdapter.notifyDataSetChanged();
                    DBManager.getInstance().updateCategory(curCategory);

                    mIsEditBtPressed = false;
                    mEtContainer.setVisibility(View.GONE);
                    mLvCategories.setVisibility(View.VISIBLE);
                    resetListView();
                    resetBtAdd();
                    MainActivity.hideKeyboard();
                } else {
                    resetListView();
                    if (mIsAddBtPressed) {
                        String newCategoryName = getCategoryNameFromEtAEF();
                        if (newCategoryName == null) break;

                        NotebookCategory newCategory = new NotebookCategory();
                        newCategory.setName(newCategoryName);
                        DBManager.getInstance().addCategory(newCategory);
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
                resetListView();
                if (mIsFindBtPressed) {
                    String toFindString = mEtAEFCategory.getText().toString().trim();
                    if (!toFindString.isEmpty()) {
                        mLvCategories.setVisibility(View.VISIBLE);
                        mEtContainer.setVisibility(View.VISIBLE);
                        showCategoryList(findCategories(toFindString));
                        MainActivity.hideKeyboard();
                    } else mLvCategories.setVisibility(View.GONE);
                } else btFindPressedFirstTime();
                break;
            case R.id.bt_clear:
                if (mIsFindBtPressed) {
                    resetListView();
                    mLvCategories.setVisibility(View.GONE);
                }
                mEtAEFCategory.setText("");
                MainActivity.showKeyboardOnFocus(mEtAEFCategory);
//                mEtAEFCategory.requestFocus();
                break;
            case R.id.bt_rem:
                if (!mSelectedItems.isEmpty()) {
                    List<NotebookCategory> listToDelete = new ArrayList<>();
                    for (Integer pos : mSelectedItems) {
                        listToDelete.add(mCategoryAdapter.getItem(pos));
                    }
                    for (NotebookCategory catToDelete : listToDelete) {
                        mCategoryAdapter.remove(catToDelete);
                    }
                    mCategoryAdapter.notifyDataSetChanged();
                    DBManager.getInstance().deleteCategories(listToDelete);
//                    DropboxDBSynchronizer.getInstance().performDropboxExportTask();
                }
                resetListView();
                break;
            case R.id.bt_show_records:
//                mDBManager.insertToDB(DBConstants.TABLE_ITEMS, "35", "date", "1", "descr");

                int curCategoryId = getCurrentCategory().getId();
                MainActivity.recordsFragment.setCurCategoryId(curCategoryId);
                MainActivity.mainInstance.getViewPager().setCurrentItem(1);
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        mRootContainer = inflater.inflate(R.layout.fragment_categories_records, container, false);
        init();

        return mRootContainer;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
//        DBManager.getInstance().closeDB();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        switch (adapterView.getId()) {
            case R.id.lv:
//                resetListView();
//                mViewItemSelected = view;
                if (!mSelectedItems.contains(i)) {
                    mSelectedItems.add(i);
                    view.setBackgroundColor(getResources().getColor(R.color.list_item_selected));
                } else {
                    mSelectedItems.remove((Object) i);
                    view.setBackgroundColor(getResources().getColor(R.color.white));
                }
                mCurItemButtons.setVisibility(View.VISIBLE);
                if (mSelectedItems.size() > 1) {
                    mCurItemButtons.findViewById(R.id.bt_edit).setVisibility(View.INVISIBLE);
                    mCurItemButtons.findViewById(R.id.bt_show_records).setVisibility(View.INVISIBLE);
                    disableViewPagerSwipe();

                } else if (mSelectedItems.size() == 1) {
                    mCurItemButtons.findViewById(R.id.bt_edit).setVisibility(View.VISIBLE);
                    mCurItemButtons.findViewById(R.id.bt_show_records).setVisibility(View.VISIBLE);
                    enableViewPagerSwipe();
                } else {
                    mCurItemButtons.setVisibility(View.GONE);
                    disableViewPagerSwipe();
                }
                MainActivity.hideKeyboard();
//                MainActivity.mainInstance.showToastMessage(mSelectedItems.toString());
                break;
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        switch (position) {
            case 0:
                MainActivity.recordsFragment.resetRecords();
                refreshCurCategoryItem();
                break;
            case 1:
                int curCategoryId = getCurrentCategory().getId();
                NotebookRecordsFragment recordsFragment = MainActivity.recordsFragment;
                recordsFragment.setCurCategoryId(curCategoryId);
                recordsFragment.showRecordList();
                break;
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    public boolean onTouch(View view, MotionEvent motionEvent) {

        switch (MotionEventCompat.getActionMasked(motionEvent)) {
            case MotionEvent.ACTION_DOWN:
                mStartDragX = motionEvent.getX();
                mStartDragY = motionEvent.getY();
                break;
            case MotionEvent.ACTION_MOVE:


                break;
            case MotionEvent.ACTION_UP:
                if (isUpSwipe(mStartDragX, mStartDragY, motionEvent.getX(), motionEvent.getY())
                        && isSwipeDragEnough(mStartDragY, motionEvent.getY())) {
//                    Log.d("ON_TOUCH", "VERTICAL SWIPE DETECTED");
                    MainActivity.mainInstance.showDialogBox(R.string.dialog_box_msg_dropbox_upload,
                            (dialogInterface, i) -> {
                                dialogInterface.dismiss();
                                DropboxDBSynchronizer.getInstance().performDropboxExportTask();
                            });

                } else if (isDownSwipe(mStartDragX, mStartDragY, motionEvent.getX(), motionEvent.getY())
                        && isSwipeDragEnough(mStartDragY, motionEvent.getY())) {
                    MainActivity.mainInstance.showDialogBox(R.string.dialog_box_msg_dropbox_download,
                            (dialogInterface, i) -> {
                                dialogInterface.dismiss();
                                DropboxDBSynchronizer.getInstance().performDropboxImportTask();
                            });
                }
                break;
        }

        return false;
    }

    private boolean isSwipeDragEnough(float startCoord, float endCoord) {
        return Math.abs(startCoord - endCoord) >= MIN_SWIPE_DISTANCE;
    }

    private void btAddPressedFirstTime() {
        mEtAEFCategory.setText("");
        mEtAEFCategory.setHint(getResources().getString(R.string.et_add_category_hint));
        mLvCategories.setVisibility(View.GONE);
        mEtContainer.setVisibility(View.VISIBLE);
        mIsAddBtPressed = true;
        mIsEditBtPressed = false;
        mIsFindBtPressed = false;
        MainActivity.showKeyboardOnFocus(mEtAEFCategory);
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
        MainActivity.showKeyboardOnFocus(mEtAEFCategory);
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
        MainActivity.showKeyboardOnFocus(mEtAEFCategory);
    }

    private void disableViewPagerSwipe() {
        ((NonSwipeableViewPager) MainActivity.mainInstance.getViewPager()).setSwipeEnabled(false);
    }

    private void enableViewPagerSwipe() {
        ((NonSwipeableViewPager) MainActivity.mainInstance.getViewPager()).setSwipeEnabled(true);
    }

    private List<NotebookCategory> findCategories(String toFind) {
        return DBManager.getInstance().readCategoriesWhereKeyLike(DBConstants.COLUMN_CATEGORY_NAME, "%" + toFind + "%");
    }

    private String getCategoryNameFromEtAEF() {
        String readValue = mEtAEFCategory.getText()
                .toString().trim().toUpperCase();
        if (readValue.isEmpty()) {
            MainActivity.mainInstance.showToastMessage(getString(R.string.invalid_category_name_message));
            restoreEtAEFCategoryName();
            return null;
        }
        return readValue;
    }

    private NotebookCategory getCurrentCategory() {
        return mCategoryAdapter.getItem(mSelectedItems.get(0));
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

        mEtAEFCategory = mRootContainer.findViewById(R.id.et_new_find);
        mEtContainer = mRootContainer.findViewById(R.id.widgets_container);

        mLvCategories = mRootContainer.findViewById(R.id.lv);
        mCategoryAdapter = new CategoryAdapter();
        mLvCategories.setAdapter(mCategoryAdapter);
        mLvCategories.setOnItemClickListener(this);
        mLvCategories.setVisibility(View.GONE);

        mCurItemButtons = mRootContainer.findViewById(R.id.bottom_bts);
        mCurItemButtons.setVisibility(View.GONE);

        mRootContainer.setOnTouchListener(this);

        MainActivity.mainInstance.getViewPager().addOnPageChangeListener(this);
        disableViewPagerSwipe();
    }

    private boolean isDownSwipe(float startX, float startY, float endX, float endY) {
        return isVerticalSwipe(startX, startY, endX, endY) && startY < endY;
    }

    private boolean isUpSwipe(float startX, float startY, float endX, float endY) {
        return isVerticalSwipe(startX, startY, endX, endY) && startY > endY;
    }

    private boolean isVerticalSwipe(float startX, float startY, float endX, float endY) {
        return Math.abs(startX - endX) < Math.abs(startY - endY);
    }

    private List<NotebookCategory> readAllCategories() {
        return DBManager.getInstance().readAllCategories();
    }

    private void refreshCurCategoryItem() {
        NotebookCategory curCategoryFromAdapter = getCurrentCategory();
        NotebookCategory curCategoryFromDB = DBManager.getInstance()
                .readCategoriesWhereKeyEquals(DBConstants.COLUMN_CATEGORY_ID, String.valueOf(curCategoryFromAdapter.getId()))
                .get(0);
        curCategoryFromAdapter.setRecordQuantity(curCategoryFromDB.getRecordQuantity());
        curCategoryFromAdapter.setLastRecordDate(curCategoryFromDB.getLastRecordDate());
        mCategoryAdapter.notifyDataSetChanged();
    }

    private void resetBtAdd() {
        ((Button) mRootContainer.findViewById(R.id.bt_add)).setText(R.string.bt_add);
    }

    private void resetListView() {
        if (!mSelectedItems.isEmpty()) {
            disableViewPagerSwipe();
            for (Integer pos : mSelectedItems) {
                MainActivity.getViewByPosition(pos, mLvCategories)
                        .setBackgroundColor(getResources().getColor(R.color.white));

            }
            mSelectedItems.clear();
        }
        mCurItemButtons.setVisibility(View.GONE);
        mRootContainer.findViewById(R.id.tv_message).setVisibility(View.GONE);
    }

    private void restoreEtAEFCategoryName() {
        if (mIsAddBtPressed) {
            mEtAEFCategory.setText("");
        } else if (mIsEditBtPressed) {
            mEtAEFCategory.setText(getCurrentCategory().getName());
            mEtAEFCategory.setSelection(mEtAEFCategory.getText().length());
        }
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
}
