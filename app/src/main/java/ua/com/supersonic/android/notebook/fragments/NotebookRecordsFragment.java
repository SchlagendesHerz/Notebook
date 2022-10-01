package ua.com.supersonic.android.notebook.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ua.com.supersonic.android.notebook.MainActivity;
import ua.com.supersonic.android.notebook.NotebookRecord;
import ua.com.supersonic.android.notebook.R;
import ua.com.supersonic.android.notebook.adapters.RecordAdapter;
import ua.com.supersonic.android.notebook.db.DBConstants;
import ua.com.supersonic.android.notebook.db.DBManager;
import ua.com.supersonic.android.notebook.widgets.NonSwipeableViewPager;

public class NotebookRecordsFragment extends Fragment implements View.OnClickListener,
        AdapterView.OnItemClickListener, AdapterView.OnItemSelectedListener, View.OnTouchListener {

    private static final int MIN_SWIPE_DISTANCE = 100;

    private RecordAdapter mRecordAdapter;
    private ArrayAdapter<String> mSpinnerAEFAdapter;

    private View mRootContainer;
    private ListView mLvRecords;
    private Spinner mSpinnerAEF;
    private EditText mEtAEFRecord;
    private View mWidgetsContainer;
    private View mCurItemButtons;

    private boolean mIsAddBtPressed;
    private boolean mIsFindBtPressed;
    private boolean mIsEditBtPressed;

    private String[] mSpinnerAEFMap;
    private int mPrevSpinnerAEFItem;

    private final List<Integer> mSelectedItems = new ArrayList<>();

    private int mCurCategoryId;

    private float mStartDragX;
    private float mStartDragY;

    public List<Integer> getSelectedItems() {
        return mSelectedItems;
    }

    public int getCurCategoryId() {
        return mCurCategoryId;
    }

    public void setCurCategoryId(int curCategoryId) {
        this.mCurCategoryId = curCategoryId;
    }

    public boolean isAddBtPressed() {
        return mIsAddBtPressed;
    }

    public boolean isEditBtPressed() {
        return mIsEditBtPressed;
    }

    public boolean isFindBtPressed() {
        return mIsFindBtPressed;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bt_list_all:
                resetSpinnerAEF();
                resetListView();
                resetBtAdd();
                mIsAddBtPressed = false;
                mIsEditBtPressed = false;
                mIsFindBtPressed = false;
//                DropboxDBSynchronizer.getInstance().performDropboxImportTask();
                showRecordList(DBManager.getInstance().readRecordsWhereKeyEquals(DBConstants.COLUMN_CATEGORY_ID, String.valueOf(mCurCategoryId)));
                MainActivity.hideKeyboard();
                mLvRecords.setVisibility(View.VISIBLE);
//                mWidgetsContainer.setVisibility(View.INVISIBLE);
                mWidgetsContainer.setVisibility(View.GONE);
                enableViewPagerSwipe();
                break;
            case R.id.bt_add:
                if (mIsEditBtPressed) {
                    mSpinnerAEFMap[mSpinnerAEF.getSelectedItemPosition()] = mEtAEFRecord.getText()
                            .toString().trim();
                    Date date = getDateFromSpinnerAEFMap();
                    if (date == null) break;
                    Double amount = getAmountFromSpinnerAEFMap();
                    if (amount == null) break;
                    NotebookRecord editRecord = mRecordAdapter.getItem(mSelectedItems.get(0));
                    editRecord.setDate(date);
                    editRecord.setAmount(amount);
                    editRecord.setDescription(mSpinnerAEFMap[2]);
                    mRecordAdapter.notifyDataSetChanged();
                    DBManager.getInstance().updateRecord(editRecord);
//                    DropboxDBSynchronizer.getInstance().performDropboxExportTask();

                    mIsEditBtPressed = false;
                    mWidgetsContainer.setVisibility(View.GONE);
                    mLvRecords.setVisibility(View.VISIBLE);
                    resetBtAdd();
                    resetListView();
                    MainActivity.hideKeyboard();
                    enableViewPagerSwipe();
                } else {
                    resetListView();
                    if (mIsAddBtPressed) {
                        mSpinnerAEFMap[mSpinnerAEF.getSelectedItemPosition()] = mEtAEFRecord.getText()
                                .toString().trim();
                        Double amount = getAmountFromSpinnerAEFMap();
                        if (amount == null) break;
                        NotebookRecord newRecord = new NotebookRecord();
                        newRecord.setCategoryId(mCurCategoryId);
                        newRecord.setDate(new Date());
                        newRecord.setAmount(amount);
                        newRecord.setDescription(mSpinnerAEFMap[1]);
                        DBManager.getInstance().addRecord(newRecord);
                        resetSpinnerAEF();
                        mIsAddBtPressed = false;
                        MainActivity.hideKeyboard();
//                    mSpinnerAdapter.notifyDataSetChanged();
//                        DropboxDBSynchronizer.getInstance().performDropboxExportTask();
                        mWidgetsContainer.setVisibility(View.GONE);
                        enableViewPagerSwipe();
                    } else {
                        resetBtAdd();
                        resetSpinnerAEF();
                        btAddPressedFirstTime();
                    }
                }
                break;
            case R.id.bt_find:
                /*try {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(DBManager.getNotebookRecordDateFormat().parse("1999-01-01 00:00:00 AM"));
                    calendar.set(Calendar.DAY_OF_YEAR, calendar.getActualMaximum(Calendar.DAY_OF_YEAR));
                    calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMaximum(Calendar.HOUR_OF_DAY));
                    calendar.set(Calendar.MINUTE, calendar.getActualMaximum(Calendar.MINUTE));
                    calendar.set(Calendar.SECOND, calendar.getActualMaximum(Calendar.SECOND));
                    String formattedDate = DBManager.getNotebookRecordDateFormat().format(
                            calendar.getTime()
                    );
                    Log.d("RECORD", "Minimal Day of Month = " + formattedDate);
                } catch (ParseException e) {
                    e.printStackTrace();
                }*/
                /*resetSpinner();
                resetListView();
                resetBtAdd();
                mAddBtPressed = false;
                mEditBtPressed = false;
                mFindBtPressed = true;

                try {
                    showRecordList(mDBManager.readRecordsWhereDateBetween(
                            mCurCategoryId,
                            DBManager.getNotebookRecordDateFormat().parse("1999-01-01 00:00:00 AM"),
                            DBManager.getNotebookRecordDateFormat().parse("1999-12-31 11:59:59 PM"))
                    );
                } catch (ParseException e) {
                    e.printStackTrace();
                }


                MainActivity.hideKeyboard();
                mLvRecords.setVisibility(View.VISIBLE);
//                mWidgetsContainer.setVisibility(View.INVISIBLE);
                mWidgetsContainer.setVisibility(View.GONE);*/
                break;
            case R.id.bt_clear:
                mEtAEFRecord.setText("");
                break;
            case R.id.bt_rem:
                if (!mSelectedItems.isEmpty()) {
                    List<NotebookRecord> listToDelete = new ArrayList<>();
                    for (Integer pos : mSelectedItems) {
                        listToDelete.add(mRecordAdapter.getItem(pos));
                    }
                    for (NotebookRecord recordToDelete : listToDelete) {
                        mRecordAdapter.remove(recordToDelete);
                    }
                    mRecordAdapter.notifyDataSetChanged();
                    DBManager.getInstance().deleteRecords(listToDelete);
//                    DropboxDBSynchronizer.getInstance().performDropboxExportTask();
                }
                resetListView();
                break;
            case R.id.bt_edit:
                resetSpinnerAEF();
//                resetListView();
                if (!mIsEditBtPressed) {
                    btEditPressedFirstTime();
                }
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        MainActivity.mainInstance.showToastMessage(String.valueOf(mCurCategoryId));
        mRootContainer = inflater.inflate(R.layout.fragment_categories_records, container, false);
        init();
        return mRootContainer;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//        MainActivity.mainInstance.showToastMessage("SELECTED :" + i);

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
                } else if (mSelectedItems.size() == 1) {
                    mCurItemButtons.findViewById(R.id.bt_edit).setVisibility(View.VISIBLE);

                } else {
                    mCurItemButtons.setVisibility(View.GONE);
                }
                MainActivity.hideKeyboard();
//                MainActivity.mainInstance.showToastMessage(mSelectedItems.toString());
                break;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
//        MainActivity.mainInstance.showToastMessage("SELECTED " + i + ", prev = " + mPrevSpinnerItem);
        mSpinnerAEFMap[mPrevSpinnerAEFItem] = mEtAEFRecord.getText().toString().trim();
        mPrevSpinnerAEFItem = i;
        mEtAEFRecord.setText(mSpinnerAEFMap[i]);
        mEtAEFRecord.requestFocus();
        mEtAEFRecord.setSelection(mEtAEFRecord.getText().length());
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
//        MainActivity.mainInstance.showToastMessage("NOTHING SELECTED");
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        float curDragX = motionEvent.getX();
        float curDragY = motionEvent.getY();

        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mStartDragX = curDragX;
                mStartDragY = curDragY;
                break;
            case MotionEvent.ACTION_UP:
//                if (Math.abs(motionEvent.getX() - mStartDragX) >= MIN_SWIPE_DISTANCE) {
                if (isHorisontalSwipe(curDragX, curDragY)) {
                    if (curDragX > mStartDragX) {
//                        mStartDragX = motionEvent.getX();
                        MainActivity.recordsFragment.showPrevField();

                    } else if (curDragX < mStartDragX) {
//                        mStartDragX = motionEvent.getX();
                        MainActivity.recordsFragment.showNextField();
                    }
                } else {
                    if (curDragY > mStartDragY) {
//                        mStartDragX = motionEvent.getX();
                        showRecordList();

                    }
                }
//                }
        }
        return false;
    }

    private boolean isHorisontalSwipe(float curDragX, float curDragY) {
        return Math.abs(curDragX - mStartDragX) > Math.abs(curDragY - mStartDragY);
    }

    public void resetRecords() {
        resetListView();
        resetSpinnerAEF();
        mRecordAdapter.clear();
        mRecordAdapter.notifyDataSetChanged();
        mWidgetsContainer.setVisibility(View.GONE);
        mLvRecords.setVisibility(View.GONE);
    }

    public void showNextField() {
        if (mIsAddBtPressed || mIsEditBtPressed) {
            int curSpinnerAEFPosition = mSpinnerAEF.getSelectedItemPosition();
            if (curSpinnerAEFPosition != (mSpinnerAEFAdapter.getCount() - 1)) {
                mSpinnerAEF.setSelection(curSpinnerAEFPosition + 1);
            }
        }
    }

    public void showPrevField() {
        if (mIsAddBtPressed || mIsEditBtPressed) {
            int curSpinnerAEFPosition = mSpinnerAEF.getSelectedItemPosition();
            if (curSpinnerAEFPosition != 0) {
                mSpinnerAEF.setSelection(curSpinnerAEFPosition - 1);
            }
        }
    }

    public void showRecordList() {
        onClick(mRootContainer.findViewById(R.id.bt_list_all));
    }

    private void btAddPressedFirstTime() {
//        mSpinnerAdapter.clear();
        disableViewPagerSwipe();
        mSpinnerAEFAdapter.addAll(getRecordFieldsOnBtAddPressed());
        mSpinnerAEFAdapter.notifyDataSetChanged();
        mSpinnerAEFMap = getSpinnerAEFMapOnBtAddPressed();
        mPrevSpinnerAEFItem = 0;
        mSpinnerAEF.setSelection(0);
        mEtAEFRecord.setText(mSpinnerAEFMap[0]);
        mEtAEFRecord.setSelection(mEtAEFRecord.getText().length());
//        mEtNewFind.setHint(getString(R.string.et_add_category_hint));
        mLvRecords.setVisibility(View.GONE);
        mWidgetsContainer.setVisibility(View.VISIBLE);
        mIsAddBtPressed = true;
        mIsFindBtPressed = false;
//        MainActivity.mainInstance.showToastMessage(Arrays.toString(mSpinnerMap));
        MainActivity.showKeyboardOnFocus(mEtAEFRecord);
    }

    private void btEditPressedFirstTime() {
//        mSpinnerAdapter.clear();
        disableViewPagerSwipe();
        mSpinnerAEFAdapter.addAll(getRecordFieldsOnBtEditPressed());
        mSpinnerAEFAdapter.notifyDataSetChanged();
        mSpinnerAEFMap = getSpinnerAEFMapOnBtEditPressed();
        mPrevSpinnerAEFItem = 0;
        mSpinnerAEF.setSelection(0);
        mEtAEFRecord.setText(mSpinnerAEFMap[0]);
        mEtAEFRecord.setSelection(mEtAEFRecord.getText().length());
//        mEtNewFind.setHint(getString(R.string.et_add_category_hint));
        mLvRecords.setVisibility(View.GONE);
        mWidgetsContainer.setVisibility(View.VISIBLE);
        mCurItemButtons.setVisibility(View.GONE);
        ((Button) mRootContainer.findViewById(R.id.bt_add)).setText(R.string.bt_edit);

        mIsEditBtPressed = true;
        mIsAddBtPressed = false;
        mIsFindBtPressed = false;
//        MainActivity.mainInstance.showToastMessage(Arrays.toString(mSpinnerMap));
        MainActivity.showKeyboardOnFocus(mEtAEFRecord);
    }

    private void disableViewPagerSwipe() {
        ((NonSwipeableViewPager) MainActivity.mainInstance.getViewPager()).setSwipeEnabled(false);
    }

    private void enableViewPagerSwipe() {
        ((NonSwipeableViewPager) MainActivity.mainInstance.getViewPager()).setSwipeEnabled(true);
    }

    private Double getAmountFromSpinnerAEFMap() {
        try {
            if (mIsAddBtPressed) return Double.parseDouble(mSpinnerAEFMap[0]);
            else if (mIsEditBtPressed) return Double.parseDouble(mSpinnerAEFMap[1]);
        } catch (NumberFormatException e) {
            MainActivity.mainInstance.showToastMessage(getString(R.string.invalid_amount_field_message));
            restoreSpinnerAEFAmount();
        }
        return null;
    }

    private Date getDateFromSpinnerAEFMap() {
        try {
            if (mIsEditBtPressed)
                return DBManager.getDBDateFormat().parse(mSpinnerAEFMap[0]);
        } catch (ParseException e) {
            MainActivity.mainInstance.showToastMessage(getString(R.string.invalid_date_field_message));
            restoreSpinnerAEFDate();
        }
        return null;
    }

    private List<String> getRecordFieldsOnBtAddPressed() {
        return new ArrayList<>() {
            {
                add(getString(R.string.record_field_amount));
                add(getString(R.string.record_field_descr));
            }
        };
    }

    private List<String> getRecordFieldsOnBtEditPressed() {
        return new ArrayList<>() {
            {
                add(getString(R.string.record_field_date));
                add(getString(R.string.record_field_amount));
                add(getString(R.string.record_field_descr));
            }
        };
    }

    private String[] getSpinnerAEFMapOnBtAddPressed() {
        String[] spinnerMap = new String[2];
        spinnerMap[0] = "1";
        spinnerMap[1] = "";
        return spinnerMap;
    }

    private String[] getSpinnerAEFMapOnBtEditPressed() {
        String[] spinnerMap = new String[3];
        NotebookRecord record = mRecordAdapter.getItem(mSelectedItems.get(0));
        spinnerMap[0] = DBManager.getDBDateFormat().format(record.getDate());
        spinnerMap[1] = String.valueOf(record.getAmount());
        spinnerMap[2] = record.getDescription();
        return spinnerMap;
    }

    private void init() {

        mRootContainer.findViewById(R.id.bt_list_all).setOnClickListener(this);
        mRootContainer.findViewById(R.id.bt_add).setOnClickListener(this);
        mRootContainer.findViewById(R.id.bt_find).setOnClickListener(this);
        mRootContainer.findViewById(R.id.bt_rem).setOnClickListener(this);
        mRootContainer.findViewById(R.id.bt_edit).setOnClickListener(this);
        mRootContainer.findViewById(R.id.bt_clear).setOnClickListener(this);
        mRootContainer.findViewById(R.id.bt_show_records).setVisibility(View.GONE);

        mSpinnerAEFAdapter = new ArrayAdapter<>(MainActivity.mainInstance, android.R.layout.simple_list_item_1, new ArrayList<>());
        mSpinnerAEF = mRootContainer.findViewById(R.id.spinner);
        mSpinnerAEF.setVisibility(View.VISIBLE);
        mSpinnerAEF.setAdapter(mSpinnerAEFAdapter);
        mSpinnerAEF.setOnItemSelectedListener(this);

        mEtAEFRecord = mRootContainer.findViewById(R.id.et_new_find);
        mWidgetsContainer = mRootContainer.findViewById(R.id.widgets_container);

        mLvRecords = mRootContainer.findViewById(R.id.lv);
        mRecordAdapter = new RecordAdapter();
        mLvRecords.setAdapter(mRecordAdapter);
        mLvRecords.setOnItemClickListener(this);
        mLvRecords.setVisibility(View.GONE);

        mCurItemButtons = mRootContainer.findViewById(R.id.bottom_bts);
        mCurItemButtons.setVisibility(View.GONE);

        mRootContainer.setOnTouchListener(this);
    }

    private void resetBtAdd() {
        ((Button) mRootContainer.findViewById(R.id.bt_add)).setText(R.string.bt_add);
    }

    private void resetListView() {
        if (!mSelectedItems.isEmpty()) {
            for (Integer pos : mSelectedItems) {
                MainActivity.getViewByPosition(pos, mLvRecords)
                        .setBackgroundColor(getResources().getColor(R.color.white));
            }
            mSelectedItems.clear();
        }
        mCurItemButtons.setVisibility(View.GONE);
        mRootContainer.findViewById(R.id.tv_message).setVisibility(View.GONE);
    }

    private void resetSpinnerAEF() {
        mEtAEFRecord.setText("");
        mSpinnerAEFAdapter.clear();
        mSpinnerAEFAdapter.notifyDataSetChanged();
//        mSpinner.setOnItemSelectedListener(this);
        mSpinnerAEFMap = null;
    }

    private void restoreSpinnerAEFAmount() {
        if (mIsAddBtPressed) {
            mSpinnerAEFMap[0] = getSpinnerAEFMapOnBtAddPressed()[0];
            setSpinnerAEFSelection(0);
        } else if (mIsEditBtPressed) {
            NotebookRecord editRecord = mRecordAdapter.getItem(mSelectedItems.get(0));
            mSpinnerAEFMap[1] = String.valueOf(editRecord.getAmount());
            setSpinnerAEFSelection(1);
        }
    }

    private void restoreSpinnerAEFDate() {
        if (mIsEditBtPressed) {
            NotebookRecord editRecord = mRecordAdapter.getItem(mSelectedItems.get(0));
            mSpinnerAEFMap[0] = DBManager.getDBDateFormat().format(editRecord.getDate());
            setSpinnerAEFSelection(0);
        }
    }

    private void setSpinnerAEFSelection(int position) {
        if (mSpinnerAEF.getSelectedItemPosition() == position) {
            mEtAEFRecord.setText(mSpinnerAEFMap[position]);
            mEtAEFRecord.setSelection(mEtAEFRecord.getText().length());
        } else mSpinnerAEF.setSelection(position);
    }

    private void showRecordList(List<NotebookRecord> input) {
        mRecordAdapter.clear();
        mRecordAdapter.addAll(input);
        mRecordAdapter.notifyDataSetChanged();
        if (mRecordAdapter.isEmpty()) {
            TextView tvMssage = mRootContainer.findViewById(R.id.tv_message);
            tvMssage.setVisibility(View.VISIBLE);
            tvMssage.setText(getResources().getString(R.string.tv_message_list_empty));
        }
    }
}
