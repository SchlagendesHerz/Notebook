package ua.com.supersonic.android.notebook.fragments;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.function.Consumer;
import java.util.function.Predicate;

import ua.com.supersonic.android.notebook.MainActivity;
import ua.com.supersonic.android.notebook.NotebookRecord;
import ua.com.supersonic.android.notebook.R;
import ua.com.supersonic.android.notebook.adapters.RecordAdapter;
import ua.com.supersonic.android.notebook.db.DBConstants;
import ua.com.supersonic.android.notebook.db.DBManager;
import ua.com.supersonic.android.notebook.utils.Utils;
import ua.com.supersonic.android.notebook.widgets.NonSwipeableViewPager;

public class NotebookRecordsFragment extends Fragment implements View.OnClickListener,
        AdapterView.OnItemClickListener, AdapterView.OnItemSelectedListener, View.OnTouchListener, View.OnFocusChangeListener {

    private static final int MIN_SWIPE_DISTANCE = 100;

    private static void updateCalendarHM(Calendar toUpdate, Date update) {
        Calendar calendarUpdate = Calendar.getInstance();
        calendarUpdate.setTime(update);

        updateCalendarHM(toUpdate, calendarUpdate.get(Calendar.HOUR), calendarUpdate.get(Calendar.MINUTE));
    }

    private static void updateCalendarHM(Calendar toUpdate, int hours, int minutes) {
        toUpdate.set(Calendar.HOUR, hours);
        toUpdate.set(Calendar.MINUTE, minutes);
    }

    private static void updateCalendarYMD(Calendar toUpdate, Date update) {
        Calendar calendarUpdate = Calendar.getInstance();
        calendarUpdate.setTime(update);

        updateCalendarYMD(toUpdate, calendarUpdate.get(Calendar.YEAR),
                calendarUpdate.get(Calendar.MONTH),
                calendarUpdate.get(Calendar.DAY_OF_MONTH));
    }

    private static void updateCalendarYMD(Calendar toUpdate, int year, int month, int day) {
        toUpdate.set(Calendar.YEAR, year);
        toUpdate.set(Calendar.MONTH, month);
        toUpdate.set(Calendar.DAY_OF_MONTH, day);
    }

    private RecordAdapter mRecordAdapter;
    private ArrayAdapter<String> mSpinnerAEFAdapter;

    private View mRootContainer;
    private ListView mLvRecords;
    private Spinner mSpinnerAEF;
    private EditText mEtAEFRecord;
    private View mWidgetsContainer;
    private View mCurItemButtons;
    private View mDateSelector;

    private TextView mEtFindStartDate;
    private TextView mEtFindEndDate;

    private boolean mIsAddBtPressed;
    private boolean mIsFindBtPressed;
    private boolean mIsEditBtPressed;

    private String[] mSpinnerAEFMap;
    private int mPrevSpinnerAEFItem;
    private String mEtCache;
    private TextWatcher mTextWatcherCache;

    private final List<Integer> mSelectedItems = new ArrayList<>();

    private int mCurCategoryId;

    private float mStartDragX;
    private float mStartDragY;

    private Calendar mFindStartCalendar;
    private Calendar mFindEndCalendar;

    public int getCurCategoryId() {
        return mCurCategoryId;
    }

    public void setCurCategoryId(int curCategoryId) {
        this.mCurCategoryId = curCategoryId;
    }

    public List<Integer> getSelectedItems() {
        return mSelectedItems;
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
                resetFind();
                mIsAddBtPressed = false;
                mIsEditBtPressed = false;
//                mIsFindBtPressed = false;
//                DropboxDBSynchronizer.getInstance().performDropboxImportTask();
                showRecordList(DBManager.getInstance().readRecordsWhereKeyEquals(DBConstants.COLUMN_CATEGORY_ID, String.valueOf(mCurCategoryId)));
                MainActivity.hideKeyboard();
                mLvRecords.setVisibility(View.VISIBLE);
//                mWidgetsContainer.setVisibility(View.INVISIBLE);
                mWidgetsContainer.setVisibility(View.GONE);
                mDateSelector.setVisibility(View.GONE);
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
                        btAddPressedFirstTime();
                    }
                }
                break;
            case R.id.bt_find:
                resetListView();
                if (mIsFindBtPressed) {
                    if (mFindStartCalendar.compareTo(mFindEndCalendar) < 0) {
                        mLvRecords.setVisibility(View.VISIBLE);
                        showRecordList(DBManager.getInstance()
                                .readRecordsWhereDateBetween(mCurCategoryId, mFindStartCalendar.getTime(),
                                        mFindEndCalendar.getTime()));
                    } else {
                        MainActivity.mainInstance.showToastMessage(getString(R.string.invalid_time_period_message));
                    }
                } else {
                    btFindPressedFirstTime();
                    enableViewPagerSwipe();
                }
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
            case R.id.bt_date_range_selector:
                onFocusChange(mEtFindStartDate, false);
                onFocusChange(mEtFindEndDate, false);
                if (mFindStartCalendar.compareTo(mFindEndCalendar) > 0) {
                    MainActivity.mainInstance.showToastMessage(getString(R.string.invalid_time_period_message));
                } else {
                    showDateRangePicker();
                }
//                Log.d("RECORD", "BEFORE: " + mFindStartCalendar.getTime().getTime());

//                Consumer<Calendar> onDateSetAction = inputCalendar -> {
//                    DateFormat dateFormat = Utils.getDateFormatInstance(Utils.FormatType.RECORD_FIND_DATE);
//                    mEtFindStartDate.setText(dateFormat.format(inputCalendar.getTime()));
//                };
//                showDatePickerDialog(R.string.start_date_picker_dialog_title, mFindStartCalendar, onDateSetAction);
                break;
        }
    }

    private void showDateRangePicker() {
        MaterialDatePicker.Builder<Pair<Long, Long>> rangePickerBuilder = MaterialDatePicker.Builder.dateRangePicker();
        long timeZoneOffset = TimeZone.getDefault().getRawOffset();
        rangePickerBuilder.setSelection(new Pair<>(mFindStartCalendar.getTimeInMillis() + timeZoneOffset,
                mFindEndCalendar.getTimeInMillis() + timeZoneOffset));

        MaterialDatePicker<Pair<Long, Long>> rangePicker = rangePickerBuilder.build();
        rangePicker.addOnPositiveButtonClickListener(
                selection -> {
                    DateFormat dateFormat = Utils.getDateFormatInstance(Utils.FormatType.RECORD_FIND_DATE);
                    Date startDate = new Date(selection.first - timeZoneOffset);
                    Date endDate = new Date(selection.second - timeZoneOffset);
                    mEtFindStartDate.setText(dateFormat.format(startDate));
                    mEtFindEndDate.setText(dateFormat.format(endDate));
                    updateCalendarYMD(mFindStartCalendar, startDate);
                    updateCalendarYMD(mFindEndCalendar, endDate);

//                    DateFormat dateFormat1 = Utils.getDateFormatInstance(Utils.FormatType.DB_DATE_TIME);
//                    MainActivity.mainInstance.showToastMessage(dateFormat1.format(new Date(selection.first)));
//                    MainActivity.mainInstance.showToastMessage(dateFormat1.getTimeZone().toString());
                    dateFormat = Utils.getDateFormatInstance(Utils.FormatType.DB_DATE_TIME);
                    Log.d("RECORD", "START: " + dateFormat.format(new Date(selection.first - timeZoneOffset)));
                    Log.d("RECORD", "END: " + dateFormat.format(new Date(selection.second - timeZoneOffset)));
                    Log.d("RECORD", "CALENDAR START: " + dateFormat.format(mFindStartCalendar.getTime()));
                    Log.d("RECORD", "CALENDAR END: " + dateFormat.format(mFindEndCalendar.getTime()));
                }
        );
        rangePicker.show(requireActivity().getSupportFragmentManager(), "");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        MainActivity.mainInstance.showToastMessage(String.valueOf(mCurCategoryId));
        mRootContainer = inflater.inflate(R.layout.fragment_categories_records, container, false);
        init();
        return mRootContainer;
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if (hasFocus) {
            mEtCache = ((EditText) view).getText().toString();
            switch (view.getId()) {
                case R.id.et_start_date:
                    mEtFindStartDate.addTextChangedListener(mTextWatcherCache = new DateETWatcher());
                    break;
                case R.id.et_end_date:
                    mEtFindEndDate.addTextChangedListener(mTextWatcherCache = new DateETWatcher());
                    break;
            }
        } else {
            DateFormat dateFormat;
            Date date;
            if (!((EditText) view).getText().toString().trim().equals(mEtCache)) {
                try {
                    switch (view.getId()) {
                        case R.id.et_start_date:
                            mEtFindStartDate.removeTextChangedListener(mTextWatcherCache);
                            mTextWatcherCache = null;
                            dateFormat = Utils.getDateFormatInstance(Utils.FormatType.RECORD_FIND_DATE);
                            date = dateFormat.parse(String.valueOf(((EditText) view).getText()));
                            updateCalendarYMD(mFindStartCalendar, date);
                            break;
                        case R.id.et_end_date:
                            mEtFindStartDate.removeTextChangedListener(mTextWatcherCache);
                            mTextWatcherCache = null;
                            dateFormat = Utils.getDateFormatInstance(Utils.FormatType.RECORD_FIND_DATE);
                            date = dateFormat.parse(String.valueOf(((EditText) view).getText()));
                            updateCalendarYMD(mFindEndCalendar, date);
                            break;
                        default:
                            mEtCache = null;
                    }
                } catch (ParseException ex) {
                    MainActivity.mainInstance.showToastMessage(getString(R.string.invalid_date_field_message));
                    ((EditText) view).setText(mEtCache);
                    mEtCache = null;
                }
            }
        }
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
                if (isHorizontalSwipe(curDragX, curDragY)) {
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

    public void resetRecords() {
        resetListView();
        resetSpinnerAEF();
        resetFind();
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
        resetBtAdd();
        resetSpinnerAEF();
        resetFind();
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
        mDateSelector.setVisibility(View.GONE);
        ((Button) mRootContainer.findViewById(R.id.bt_add)).setText(R.string.bt_edit);

        mIsEditBtPressed = true;
        mIsAddBtPressed = false;
        mIsFindBtPressed = false;
//        MainActivity.mainInstance.showToastMessage(Arrays.toString(mSpinnerMap));
        MainActivity.showKeyboardOnFocus(mEtAEFRecord);
    }

    private void btFindPressedFirstTime() {
//        mSpinnerAdapter.clear();
        resetBtAdd();
//        disableViewPagerSwipe();
        mLvRecords.setVisibility(View.GONE);
        mWidgetsContainer.setVisibility(View.GONE);
        mCurItemButtons.setVisibility(View.GONE);
        mDateSelector.setVisibility(View.VISIBLE);

        mFindStartCalendar = new GregorianCalendar();
        mFindEndCalendar = new GregorianCalendar();
        setMonthStartOf(mFindStartCalendar);
        setDayEndOf(mFindEndCalendar);

        DateFormat dateFormat = Utils.getDateFormatInstance(Utils.FormatType.RECORD_FIND_DATE);
        mEtFindStartDate.setText(dateFormat.format(mFindStartCalendar.getTime()));
        mEtFindEndDate.setText(dateFormat.format(mFindEndCalendar.getTime()));

        mIsAddBtPressed = false;
        mIsEditBtPressed = false;
        mIsFindBtPressed = true;
        MainActivity.hideKeyboard();
    }

    private void disableViewPagerSwipe() {
        ((NonSwipeableViewPager) MainActivity.mainInstance.getViewPager()).setSwipeEnabled(false);
    }

    private void enableViewPagerSwipe() {
        ((NonSwipeableViewPager) MainActivity.mainInstance.getViewPager()).setSwipeEnabled(true);
    }

    private InputFilter[] genTimeETFilters() {
        InputFilter[] resultAr = new InputFilter[1];
        resultAr[0] = (charSequence, i, i1, spanned, i2, i3) -> {
            Log.d("RECORD", "charSequence = " + charSequence);
            return null;
        };
        return resultAr;
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
                return Utils.getDateFormatInstance(Utils.FormatType.DB_DATE_TIME)
                        .parse(mSpinnerAEFMap[0]);
        } catch (ParseException e) {
            MainActivity.mainInstance.showToastMessage(getString(R.string.invalid_date_field_message));
            restoreSpinnerAEFDate();
        }
        return null;
    }

    private void setMonthStartOf(Calendar inputCalendar) {
        inputCalendar.set(Calendar.DAY_OF_MONTH, inputCalendar.getActualMinimum(Calendar.DAY_OF_MONTH));
        inputCalendar.set(Calendar.HOUR_OF_DAY, inputCalendar.getActualMinimum(Calendar.HOUR_OF_DAY));
        inputCalendar.set(Calendar.MINUTE, inputCalendar.getActualMinimum(Calendar.MINUTE));
        inputCalendar.set(Calendar.SECOND, inputCalendar.getActualMinimum(Calendar.SECOND));
        inputCalendar.set(Calendar.MILLISECOND, inputCalendar.getActualMinimum(Calendar.MILLISECOND));
    }

    private void setDayEndOf(Calendar inputCalendar) {
//        inputCalendar.set(Calendar.DAY_OF_MONTH, inputCalendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        inputCalendar.set(Calendar.HOUR_OF_DAY, inputCalendar.getActualMaximum(Calendar.HOUR_OF_DAY));
        inputCalendar.set(Calendar.MINUTE, inputCalendar.getActualMaximum(Calendar.MINUTE));
        inputCalendar.set(Calendar.SECOND, inputCalendar.getActualMaximum(Calendar.SECOND));
        inputCalendar.set(Calendar.MILLISECOND, inputCalendar.getActualMaximum(Calendar.MILLISECOND));
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
        spinnerMap[0] = Utils.getDateFormatInstance(Utils.FormatType.DB_DATE_TIME)
                .format(record.getDate());
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
        mRootContainer.findViewById(R.id.bt_date_range_selector).setOnClickListener(this);
        mRootContainer.findViewById(R.id.bt_show_records).setVisibility(View.GONE);

        mSpinnerAEFAdapter = new ArrayAdapter<>(MainActivity.mainInstance, android.R.layout.simple_list_item_1, new ArrayList<>());
        mSpinnerAEF = mRootContainer.findViewById(R.id.spinner);
        mSpinnerAEF.setVisibility(View.VISIBLE);
        mSpinnerAEF.setAdapter(mSpinnerAEFAdapter);
        mSpinnerAEF.setOnItemSelectedListener(this);

        mWidgetsContainer = mRootContainer.findViewById(R.id.widgets_container);
        mEtAEFRecord = mRootContainer.findViewById(R.id.et_add_edit_find);

        mDateSelector = mRootContainer.findViewById(R.id.date_selector);

        mEtFindStartDate = mRootContainer.findViewById(R.id.et_start_date);
        mEtFindStartDate.setOnFocusChangeListener(this);
//        mEtFindStartDate.addTextChangedListener(textWatcher);

        mEtFindEndDate = mRootContainer.findViewById(R.id.et_end_date);
        mEtFindEndDate.setOnFocusChangeListener(this);

        mLvRecords = mRootContainer.findViewById(R.id.lv);
        mRecordAdapter = new RecordAdapter();
        mLvRecords.setAdapter(mRecordAdapter);
        mLvRecords.setOnItemClickListener(this);
        mLvRecords.setVisibility(View.GONE);

        mCurItemButtons = mRootContainer.findViewById(R.id.bottom_bts);
        mCurItemButtons.setVisibility(View.GONE);

        mRootContainer.setOnTouchListener(this);
    }

    private boolean isHorizontalSwipe(float curDragX, float curDragY) {
        return Math.abs(curDragX - mStartDragX) > Math.abs(curDragY - mStartDragY);
    }

    private void resetBtAdd() {
        ((Button) mRootContainer.findViewById(R.id.bt_add)).setText(R.string.bt_add);
    }

    private void resetFind() {
        mIsFindBtPressed = false;
        mFindEndCalendar = null;
        mFindStartCalendar = null;
        mDateSelector.setVisibility(View.GONE);
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
            mSpinnerAEFMap[0] = Utils.getDateFormatInstance(Utils.FormatType.DB_DATE_TIME)
                    .format(editRecord.getDate());
            setSpinnerAEFSelection(0);
        }
    }

    private void setSpinnerAEFSelection(int position) {
        if (mSpinnerAEF.getSelectedItemPosition() == position) {
            mEtAEFRecord.setText(mSpinnerAEFMap[position]);
            mEtAEFRecord.setSelection(mEtAEFRecord.getText().length());
        } else mSpinnerAEF.setSelection(position);
    }

    private void showDatePickerDialog(int titleStringId, Calendar initCalendar, Consumer<Calendar> onDateSetAction) {
        DatePickerFragment datePickerDialog = new DatePickerFragment(initCalendar, titleStringId);
        datePickerDialog.setOnDateSetAction(onDateSetAction);
        datePickerDialog.show(requireActivity().getSupportFragmentManager(), getString(titleStringId));
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

    private void showTimePickerDialog(int titleStringId, Calendar initCalendar, Consumer<Calendar> onTimeSetAction) {
        TimePickerFragment timePickerDialog = new TimePickerFragment(initCalendar, titleStringId);
        timePickerDialog.setOnTimeSetAction(onTimeSetAction);
        timePickerDialog.show(MainActivity.mainInstance.getSupportFragmentManager(), getString(titleStringId));
    }

    private static class DateETWatcher implements TextWatcher {

        private static class TextWatcherParamHolder {
            private static TextWatcherParamHolder instance;

            private static TextWatcherParamHolder getInstance() {
                if (instance == null) {
                    instance = new TextWatcherParamHolder();
                }
                return instance;
            }

            private CharSequence charSequence;
            private int start;
            private int prevCharQty;
            private int nextCharQty;

            public void setParams(CharSequence charSequence, int start, int prevCharQty, int nextCharQty) {
                this.charSequence = charSequence;
                this.start = start;
                this.prevCharQty = prevCharQty;
                this.nextCharQty = nextCharQty;
            }
        }

        private enum BeforeTextChangedEvents {
            MULTIPLE_CHARS_EDITED(inputParamHolder -> {
                if (inputParamHolder.prevCharQty > 1 || inputParamHolder.nextCharQty > 1) {
                    Log.d("RECORD", "MULTIPLE CHARS EDIT");
                    return true;
                }
                return false;
            }),
            DASH_DELETED(inputParamHolder -> {
                if (inputParamHolder.prevCharQty == 1
                        && inputParamHolder.nextCharQty == 0
                        && inputParamHolder.charSequence.charAt(inputParamHolder.start) == '-') {
                    Log.d("RECORD", "DASH DELETION");
                    return true;
                }
                return false;

            }),
            ;
            private final Predicate<TextWatcherParamHolder> eventCondition;

            BeforeTextChangedEvents(Predicate<TextWatcherParamHolder> eventCondition) {
                this.eventCondition = eventCondition;
            }

            boolean isTrue(TextWatcherParamHolder paramHolder) {
                return this.eventCondition.test(paramHolder);
            }

            static boolean isAtLeastOneTrue(TextWatcherParamHolder paramHolder) {
                return Arrays.stream(BeforeTextChangedEvents.values())
                        .anyMatch(beforeTextChangedEvent -> beforeTextChangedEvent.isTrue(paramHolder));
            }
        }

        private enum OnTextChangedEvents {
            NON_NUMBER_ENTERED(inputParamHolder -> {
                if (!Character.isDigit(inputParamHolder.charSequence.charAt(inputParamHolder.start))) {
                    Log.d("RECORD", "NON NUMBER ENTERED");
                    return true;
                }
                return false;
            }),
            MORE_THAN_4_YEAR_DIGITS_ENTERED(inputParamHolder -> {
                if (inputParamHolder.charSequence.toString().indexOf('-') > 4) {
                    Log.d("RECORD", "MORE THAN 4 YEAR DIGITS ENTERED");
                    return true;
                }
                return false;
            }),
            MORE_THAN_2_MONTHS_DIGITS_ENTERED(inputParamHolder -> {
                if (inputParamHolder.charSequence.toString().lastIndexOf('-') - inputParamHolder.charSequence.toString().indexOf('-') > 3) {
                    Log.d("RECORD", "MORE THAN 2 MONTHS DIGITS ENTERED");
                    return true;
                }
                return false;
            }),
            MORE_THAN_2_DATE_DIGITS_ENTERED(inputParamHolder -> {
                if (inputParamHolder.charSequence.length() - inputParamHolder.charSequence.toString().lastIndexOf('-') > 3) {
                    Log.d("RECORD", "MORE THAN 2 DATE DIGITS ENTERED");
                    return true;
                }
                return false;
            }),
            ;
            private final Predicate<TextWatcherParamHolder> eventCondition;

            OnTextChangedEvents(Predicate<TextWatcherParamHolder> eventCondition) {
                this.eventCondition = eventCondition;
            }

            boolean isTrue(TextWatcherParamHolder paramHolder) {
                return this.eventCondition.test(paramHolder);
            }

            static boolean isAtLeastOneTrue(TextWatcherParamHolder paramHolder) {
                return Arrays.stream(OnTextChangedEvents.values())
                        .anyMatch(onTextChangedEvent -> onTextChangedEvent.isTrue(paramHolder));
            }
        }

        private int[] editIdx;
        private boolean isCallbackSkip;
        private CharSequence toInsert;

        @Override
        public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
            if (!isCallbackSkip) {
                TextWatcherParamHolder paramHolder = TextWatcherParamHolder.getInstance();
                paramHolder.setParams(charSequence, start, count, after);
                if (BeforeTextChangedEvents.isAtLeastOneTrue(paramHolder)) {
                    editIdx = new int[]{start, start + after};
                    toInsert = charSequence.subSequence(start, start + count);
                }
            }

            Log.d("RECORD",
                    String.format("%s: charSequence=%s; start=%d; count=%d; after=%d", "beforeTextChanged", charSequence, start, count, after)
            );
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
            // One char added
            if (!isCallbackSkip && before == 0 && count == 1) {
                TextWatcherParamHolder paramHolder = TextWatcherParamHolder.getInstance();
                paramHolder.setParams(charSequence, start, before, count);
                if (OnTextChangedEvents.isAtLeastOneTrue(paramHolder)) {
                    editIdx = new int[]{start, start + count};
                    toInsert = charSequence.subSequence(start, start + before);
                }
  /*              if (!Character.isDigit(charSequence.charAt(start))
                        // More than 4 year digits entered
                        || charSequence.toString().indexOf('-') > 4
                        // More than 2 months digits entered
                        || (charSequence.toString().lastIndexOf('-') - charSequence.toString().indexOf('-') > 3)
                        // More than 2 date digits entered
                        || (charSequence.length() - charSequence.toString().lastIndexOf('-') > 3)
                ) {
                    editIdx = new int[]{start, start + count};
                    toInsert = charSequence.subSequence(start, start + before);
                }*/
            }
         /*   Log.d("RECORD",
                    String.format("%s: charSequence=%s; start=%d; before=%d; count=%d", "onTextChanged", charSequence, i, i1, i2)
            );*/
        }

        @Override
        public void afterTextChanged(Editable editable) {
            Log.d("RECORD",
                    String.format("%s: charSequence=%s;", "afterTextChanged", editable.toString())
            );
            if (!isCallbackSkip && editIdx != null) {
                Log.d("RECORD", "toInsert = " + toInsert);
                Log.d("RECORD", "editIdx = " + Arrays.toString(editIdx));
                isCallbackSkip = true;
                editable.replace(editIdx[0], editIdx[1], toInsert);
                toInsert = null;
                editIdx = null;
            } else if (isCallbackSkip) {
                isCallbackSkip = false;
            }
        }
    }

    public static class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

        private final Calendar calendar;
        private final int titleId;
        private Consumer<Calendar> consumer = input -> {
        };

        public DatePickerFragment(Calendar initCalendar, int titleId) {
            this.calendar = initCalendar;
            this.titleId = titleId;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dialog = new DatePickerDialog(MainActivity.mainInstance, this, year, month, day);
            dialog.setTitle(this.titleId);

            return dialog;
        }

        @Override
        public void onDateSet(DatePicker datePicker, int year, int month, int day) {
            updateCalendarYMD(calendar, year, month, day);
            consumer.accept(calendar);
        }

        public void setOnDateSetAction(Consumer<Calendar> consumer) {
            this.consumer = consumer;
        }
    }


    public static class TimePickerFragment extends DialogFragment implements android.app.TimePickerDialog.OnTimeSetListener {
        private final Calendar calendar;
        private final int titleId;
        private Consumer<Calendar> consumer = input -> {
        };

        public TimePickerFragment(Calendar calendar, int titleStringId) {
            this.calendar = calendar;
            this.titleId = titleStringId;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            TimePickerDialog dialog = new TimePickerDialog(MainActivity.mainInstance, this, hour, minute, false);
            dialog.setTitle(this.titleId);
            return dialog;
        }

        @Override
        public void onTimeSet(TimePicker timePicker, int hourOfDay, int minute) {
            updateCalendarHM(calendar, hourOfDay, minute);
            consumer.accept(calendar);
        }

        public void setOnTimeSetAction(Consumer<Calendar> consumer) {
            this.consumer = consumer;
        }
    }
}
