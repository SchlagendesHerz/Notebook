package ua.com.supersonic.android.notebook.fragments;

import android.annotation.SuppressLint;
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
import androidx.core.view.MotionEventCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;

import org.joda.time.DateTimeZone;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
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
import ua.com.supersonic.android.notebook.custom_views.SwipeableConstraintLayout;
import ua.com.supersonic.android.notebook.custom_views.NonSwipeableViewPager;

public class NotebookRecordsFragment extends Fragment implements View.OnClickListener,
        AdapterView.OnItemClickListener, AdapterView.OnItemSelectedListener, View.OnTouchListener, View.OnFocusChangeListener {

    private static final int RANGE_SWIPE_OFF = -1;

    private static void updateCalendarHM(Calendar toUpdate, Date update) {
        Calendar calendarUpdate = Calendar.getInstance();
        calendarUpdate.setTime(update);

        updateCalendarHM(toUpdate, calendarUpdate.get(Calendar.HOUR), calendarUpdate.get(Calendar.MINUTE));
    }

    private static void updateCalendarHM(Calendar toUpdate, int hours, int minutes) {
        toUpdate.set(Calendar.HOUR, hours);
        toUpdate.set(Calendar.MINUTE, minutes);
    }

    private static void updateCalendarYMD(Calendar toUpdate, int year, int month, int day) {
        toUpdate.set(Calendar.YEAR, year);
        toUpdate.set(Calendar.MONTH, month);
        toUpdate.set(Calendar.DAY_OF_MONTH, day);
    }

    private RecordAdapter mRecordAdapter;
    private ArrayAdapter<String> mSpinnerAEFAdapter;

    private SwipeableConstraintLayout mRootContainer;
    private ListView mLvRecords;
    private Spinner mSpinnerAEF;
    private EditText mEtAEFRecord;
    private View mWidgetsContainer;
    private View mCurItemButtons;
    private View mDateSelector;

    private TextView mEtFindStartDate;
    private TextView mEtFindEndDate;

    private boolean mIsAddBtPressed;
    private int mFindBtMode;
    private boolean mIsEditBtPressed;

    private String[] mSpinnerAEFMap;
    private int[] mSwipeRangeMap;
    private int mCurSwipeRangeIdx;
    private int mPrevSpinnerAEFItem;
    private String mEtCache;
    private TextWatcher mTextWatcherCache;


    private int mCurCategoryId;

    private Calendar mFindStartCalendar;
    private Calendar mFindEndCalendar;

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
        return mFindBtMode != 0;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bt_list_all:
                resetSpinnerAEF();
                resetSelectedItems();
                resetBtAdd();
                resetFind();
                mIsAddBtPressed = false;
                mIsEditBtPressed = false;
//                mIsFindBtPressed = false;
//                DropboxDBSynchronizer.getInstance().performDropboxImportTask();
                showRecordList(DBManager.getInstance(getContext()).readRecordsWhereKeyEquals(DBConstants.COLUMN_CATEGORY_ID, String.valueOf(mCurCategoryId)));
                Utils.hideKeyboard(getAppMainActivity());
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
                    NotebookRecord editRecord = mRecordAdapter.getItem(mRecordAdapter.getSelectedItems().get(0));
                    editRecord.setDate(date);
                    editRecord.setAmount(amount);
                    editRecord.setDescription(mSpinnerAEFMap[2]);
                    mRecordAdapter.notifyDataSetChanged();
                    DBManager.getInstance(getContext()).updateRecord(editRecord);
//                    DropboxDBSynchronizer.getInstance().performDropboxExportTask();
                    resetEdit();

                    mLvRecords.setVisibility(View.VISIBLE);
                    resetSelectedItems();
                    Utils.hideKeyboard(getAppMainActivity());
                    if (mFindBtMode != 0) {
//                        mRootContainer.setInterceptOff();
                        if (mCurSwipeRangeIdx == 0) {
                            enableViewPagerSwipe();
                        }
                        mDateSelector.setVisibility(View.VISIBLE);
                        setTotals(calcRecordsTotals());
                    } else {
                        enableViewPagerSwipe();
                    }
                } else {
                    resetSelectedItems();
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
                        DBManager.getInstance(getContext()).addRecord(newRecord);
                        resetSpinnerAEF();
                        mIsAddBtPressed = false;
                        Utils.hideKeyboard(getAppMainActivity());
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
                mEtFindStartDate.clearFocus();
                mEtFindEndDate.clearFocus();
//                mEtFindStartDate.removeTextChangedListener(mTextWatcherCache);
//                mEtFindEndDate.removeTextChangedListener(mTextWatcherCache);
//                mTextWatcherCache = null;
//                mDateSelector.requestFocus();
                resetSelectedItems();
                Utils.hideKeyboard(getAppMainActivity());
                switch (mFindBtMode) {
                    case 0:
                        btFindPressedFirstTime();
                        enableViewPagerSwipe();
                        break;

//                  show totals first time
                    case 1:
                        if (mFindStartCalendar.compareTo(mFindEndCalendar) < 0) {
                            mRecordAdapter.clear();
                            mRecordAdapter.addAll(DBManager.getInstance(getContext())
                                    .readRecordsWhereDateBetween(mCurCategoryId, mFindStartCalendar.getTime(),
                                            mFindEndCalendar.getTime()));
                            if (mRecordAdapter.isEmpty()) {
                                hideTotals();
                                mLvRecords.setVisibility(View.GONE);
                                mRootContainer.findViewById(R.id.tv_message).setVisibility(View.VISIBLE);
                            } else {
                                mLvRecords.setVisibility(View.GONE);
                                showTotals(calcRecordsTotals());
                                mFindBtMode = 3;
                                mRootContainer.setInterceptOn();
                            }

                        } else {
                            Utils.showToastMessage(getString(R.string.error_msg_invalid_time_period_input), getContext());

                        }
                        break;

//                  show totals without DB fetch
                    case 2:
                        if (isEditBtPressed()) {
                            resetEdit();
                            mDateSelector.setVisibility(View.VISIBLE);
                            mLvRecords.setVisibility(View.VISIBLE);
                            break;
                        }
                        mLvRecords.setVisibility(View.GONE);
                        showTotals();
                        mFindBtMode = 3;
                        mRootContainer.setInterceptOn();
                        break;

//                  show list view
                    case 3:
                        hideTotals();
                        mRecordAdapter.notifyDataSetChanged();
                        mLvRecords.setVisibility(View.VISIBLE);
                        mFindBtMode = 2;
                        mRootContainer.setInterceptOff();
                        break;
                }
            /*    if (mFindBtPressedCount) {

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
                }*/
                break;
            case R.id.bt_clear:
                mEtAEFRecord.setText("");
                break;
            case R.id.bt_rem:
                if (!mRecordAdapter.getSelectedItems().isEmpty()) {
                    List<NotebookRecord> listToDelete = new ArrayList<>();
                    for (Integer pos : mRecordAdapter.getSelectedItems()) {
                        listToDelete.add(mRecordAdapter.getItem(pos));
                    }
                    for (NotebookRecord recordToDelete : listToDelete) {
                        mRecordAdapter.remove(recordToDelete);
                    }
                    DBManager.getInstance(getContext()).deleteRecords(listToDelete);
                    mRecordAdapter.notifyDataSetChanged();
                    if (mFindBtMode != 0) {
                        setTotals(calcRecordsTotals());
                    }
                }
                resetSelectedItems();
                break;
            case R.id.bt_edit:
                resetSpinnerAEF();
//                resetListView();
                if (!mIsEditBtPressed) {
                    btEditPressedFirstTime();
                }
                break;
            case R.id.bt_date_range_selector:
                mEtFindStartDate.clearFocus();
                mEtFindEndDate.clearFocus();
//                mEtFindStartDate.removeTextChangedListener(mTextWatcherCache);
//                mEtFindEndDate.removeTextChangedListener(mTextWatcherCache);
//                mTextWatcherCache = null;

//                onFocusChange(mEtFindStartDate, false);
//                onFocusChange(mEtFindEndDate, false);
                if (mFindStartCalendar.compareTo(mFindEndCalendar) > 0) {
                    Utils.showToastMessage(getString(R.string.error_msg_invalid_time_period_input), getContext());
                } else {
                    showDateRangePicker();
                    mFindBtMode = 1;
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

    private void resetEdit() {
        mIsEditBtPressed = false;
        mWidgetsContainer.setVisibility(View.GONE);
        resetBtAdd();
    }

    private MainActivity getAppMainActivity() {
        return ((MainActivity) requireActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        MainActivity.mainInstance.showToastMessage(String.valueOf(mCurCategoryId));
        mRootContainer = (SwipeableConstraintLayout) inflater.inflate(R.layout.fragment_categories_records, container, false);
        init();
        return mRootContainer;
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if (hasFocus) {
            mEtCache = ((EditText) view).getText().toString().trim();
//            if (mTextWatcherCache == null) mTextWatcherCache = new DateETWatcher();
            switch (view.getId()) {
                case R.id.et_start_date:
//                    mEtFindStartDate.addTextChangedListener(mTextWatcherCache);
                    break;
                case R.id.et_end_date:
//                    mEtFindEndDate.addTextChangedListener(mTextWatcherCache);
                    break;
            }
        } else {
//            MainActivity.hideKeyboard();
            DateFormat dateFormat;
            Date date;
            if (!((EditText) view).getText().toString().trim().equals(mEtCache)) {
                try {
                    switch (view.getId()) {
                        case R.id.et_start_date:
//                            mEtFindStartDate.removeTextChangedListener(mTextWatcherCache);
//                            mTextWatcherCache = null;
                            dateFormat = Utils.getDateFormatInstance(Utils.FormatType.RECORD_FIND_ET);
                            date = dateFormat.parse(String.valueOf(((EditText) view).getText()));
                            updateCalendarYMD(mFindStartCalendar, date);
                            resetFindResults();
                            break;
                        case R.id.et_end_date:
//                            mEtFindStartDate.removeTextChangedListener(mTextWatcherCache);
//                            mTextWatcherCache = null;
                            dateFormat = Utils.getDateFormatInstance(Utils.FormatType.RECORD_FIND_ET);
                            date = dateFormat.parse(String.valueOf(((EditText) view).getText()));
                            updateCalendarYMD(mFindEndCalendar, date);
                            resetFindResults();
                            break;
                        default:
                            mEtCache = null;
                    }
                } catch (ParseException ex) {
                    Utils.showToastMessage(getString(R.string.error_msg_invalid_date_input), getContext());
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
            case R.id.lv_items:
                Log.i("RECORD", "VIEW = " + view.getClass().getSimpleName());
//                resetListView();
//                mViewItemSelected = view;
                List<Integer> selectedItems = mRecordAdapter.getSelectedItems();
                if (!selectedItems.contains(i)) {
                    selectedItems.add(i);
                } else {
                    selectedItems.remove((Object) i);
                }
                mRecordAdapter.notifyDataSetChanged();
                mCurItemButtons.setVisibility(View.VISIBLE);
                if (selectedItems.size() > 1) {
                    mCurItemButtons.findViewById(R.id.bt_edit).setVisibility(View.INVISIBLE);
                } else if (selectedItems.size() == 1) {
                    mCurItemButtons.findViewById(R.id.bt_edit).setVisibility(View.VISIBLE);
                    int lastVisibleItemPosition = mLvRecords.getFirstVisiblePosition()
                            + mLvRecords.getChildCount() - 1;
                    if (i == lastVisibleItemPosition || i == lastVisibleItemPosition - 1) {
                        mLvRecords.smoothScrollToPosition(lastVisibleItemPosition);
                    }

                } else {
                    mCurItemButtons.setVisibility(View.GONE);
                }
                Utils.hideKeyboard(getAppMainActivity());
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

    @SuppressLint("NewApi")
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        final int action = MotionEventCompat.getActionMasked(motionEvent);

//        Log.d("RECORD", "===============");
//        Log.d("RECORD", "ACTION = " + MotionEvent.actionToString(action));

        float curX = motionEvent.getX();
        float curY = motionEvent.getY();

        switch (action) {
            case MotionEvent.ACTION_UP:
//                Log.d("RECORD", "ACTION = UP");

                if (isHorizontalSwipe(curX, curY)) {

//                  Button "Add" or "Edit" pressed
                    if (isAddBtPressed() || isEditBtPressed()) {

//                      Swipe right
                        if (curX > mRootContainer.getDownPointX()) {
                            showPrevField();

//                      Swipe left
                        } else if (curX < mRootContainer.getDownPointX()) {
//                          mStartDragX = motionEvent.getX();
                            showNextField();
                        }
                    } else if (isFindBtPressed() && mCurSwipeRangeIdx != 0) {
//                      Swipe right
                        if (curX > mRootContainer.getDownPointX()) {
                            handleHorizontalRangeSelectSwipe(false);

//                      Swipe left
                        } else if (curX < mRootContainer.getDownPointX()) {
                            handleHorizontalRangeSelectSwipe(true);
                        }
                    }

//              Vertical swipe
                } else {
//                  Button "Edit" pressed
                    if (isFindBtPressed()) {

//                      Swipe down
                        if (curY > mRootContainer.getDownPointY()) {
                            mCurSwipeRangeIdx++;
/*
                            mCurSwipeRangeIdx =
                                    mCurSwipeRangeIdx == 0
                                            ? 3
                                            : mCurSwipeRangeIdx + 1;
*/

//                      Swipe up
                        } else {
                            mCurSwipeRangeIdx--;
                        }
                        handleVerticalRangeSelectSwipe();
//                  Other buttons ("LIST ALL", "ADD") pressed
                    } else
//                      Swipe down
                        if (curY > mRootContainer.getDownPointY()) {
                            showRecordList();

                        }
                }
        }
        return false;
    }

    private void handleHorizontalRangeSelectSwipe(boolean isNext) {
        int incr = isNext ? 1 : -1;

        switch (mSwipeRangeMap[mCurSwipeRangeIdx]) {
            case Calendar.DAY_OF_MONTH:
                mFindStartCalendar.add(Calendar.DAY_OF_MONTH, incr);
                mFindEndCalendar.setTime(mFindStartCalendar.getTime());
                setDayStartOf(mFindStartCalendar);
                setDayEndOf(mFindEndCalendar);

                refreshFindEt();
                break;
            case Calendar.WEEK_OF_MONTH:
                mFindStartCalendar.add(Calendar.WEEK_OF_MONTH, incr);
                mFindEndCalendar.setTime(mFindStartCalendar.getTime());
                setWeekStartOf(mFindStartCalendar);
                setWeekEndOf(mFindEndCalendar);

//              Correction for week starting on Monday
                mFindStartCalendar.add(Calendar.DAY_OF_YEAR, 1);
                mFindEndCalendar.add(Calendar.DAY_OF_YEAR, 1);

                refreshFindEt();
                break;
            case Calendar.MONTH:
                mFindStartCalendar.add(Calendar.MONTH, incr);
                mFindEndCalendar.setTime(mFindStartCalendar.getTime());
                setMonthStartOf(mFindStartCalendar);
                setMonthEndOf(mFindEndCalendar);

                refreshFindEt();
                break;

            case Calendar.YEAR:
                mFindStartCalendar.add(Calendar.YEAR, incr);
                mFindEndCalendar.setTime(mFindStartCalendar.getTime());
                setYearStartOf(mFindStartCalendar);
                setYearEndOf(mFindEndCalendar);

                refreshFindEt();
                break;
        }
        resetFindResults();
    }

    private void resetFindResults() {
        hideTotals();
        mLvRecords.setVisibility(View.GONE);
        mRootContainer.findViewById(R.id.tv_message).setVisibility(View.GONE);
        mRecordAdapter.clear();
        mRecordAdapter.notifyDataSetChanged();
        mFindBtMode = 1;
    }

    private void handleVerticalRangeSelectSwipe() {
        if (mCurSwipeRangeIdx < 0) mCurSwipeRangeIdx = 0;
        else if (mCurSwipeRangeIdx >= mSwipeRangeMap.length)
            mCurSwipeRangeIdx = 0;

        switch (mSwipeRangeMap[mCurSwipeRangeIdx]) {
            case RANGE_SWIPE_OFF:
                setSwipeRangeOff();
                enableViewPagerSwipe();
                Log.d("RECORD", "SWIPE RANGE = OFF");
                break;
            case Calendar.DAY_OF_MONTH:
                disableViewPagerSwipe();
                setRangeSwipeToDay();
                Log.d("RECORD", "SWIPE RANGE = DAY");
                break;
            case Calendar.WEEK_OF_MONTH:
                disableViewPagerSwipe();
                setRangeSwipeToWeek();
                Log.d("RECORD", "SWIPE RANGE = WEEK");
                break;
            case Calendar.MONTH:
                disableViewPagerSwipe();
                setRangeSwipeToMonth();
                Log.d("RECORD", "SWIPE RANGE = MONTH");
                break;
            case Calendar.YEAR:
                disableViewPagerSwipe();
                setRangeSwipeToYear();
                Log.d("RECORD", "SWIPE RANGE = YEAR");
                break;
        }
    }

    private void setSwipeRangeOff() {
        mRootContainer.findViewById(R.id.prev_arrow_container).setVisibility(View.GONE);
        mRootContainer.findViewById(R.id.next_arrow_container).setVisibility(View.GONE);
    }

    private void setRangeSwipeToDay() {
        ((TextView) (mRootContainer.findViewById(R.id.tv_range_prev))).setText(R.string.tv_range_day);
        ((TextView) (mRootContainer.findViewById(R.id.tv_range_next))).setText(R.string.tv_range_day);
        mRootContainer.findViewById(R.id.prev_arrow_container).setVisibility(View.VISIBLE);
        mRootContainer.findViewById(R.id.next_arrow_container).setVisibility(View.VISIBLE);
        mRootContainer.findViewById(R.id.next_arrow_container).setVisibility(View.VISIBLE);
//        mRootContainer.findViewById(R.id.date_selector_container).requestLayout();

    }

    private void setRangeSwipeToWeek() {
        ((TextView) (mRootContainer.findViewById(R.id.tv_range_prev))).setText(R.string.tv_range_week);
        ((TextView) (mRootContainer.findViewById(R.id.tv_range_next))).setText(R.string.tv_range_week);
        mRootContainer.findViewById(R.id.prev_arrow_container).setVisibility(View.VISIBLE);
        mRootContainer.findViewById(R.id.next_arrow_container).setVisibility(View.VISIBLE);
    }

    private void setRangeSwipeToMonth() {
        ((TextView) (mRootContainer.findViewById(R.id.tv_range_prev))).setText(R.string.tv_range_month);
        ((TextView) (mRootContainer.findViewById(R.id.tv_range_next))).setText(R.string.tv_range_month);
        mRootContainer.findViewById(R.id.prev_arrow_container).setVisibility(View.VISIBLE);
        mRootContainer.findViewById(R.id.next_arrow_container).setVisibility(View.VISIBLE);
    }

    private void setRangeSwipeToYear() {
        ((TextView) (mRootContainer.findViewById(R.id.tv_range_prev))).setText(R.string.tv_range_year);
        ((TextView) (mRootContainer.findViewById(R.id.tv_range_next))).setText(R.string.tv_range_year);
        mRootContainer.findViewById(R.id.prev_arrow_container).setVisibility(View.VISIBLE);
        mRootContainer.findViewById(R.id.next_arrow_container).setVisibility(View.VISIBLE);
    }

    public void resetRecords() {
        resetSelectedItems();
        resetSpinnerAEF();
        resetFind();
        mRecordAdapter.clear();
        mRecordAdapter.notifyDataSetChanged();
        mWidgetsContainer.setVisibility(View.GONE);
        mLvRecords.setVisibility(View.GONE);
        mRootContainer.findViewById(R.id.totals_container).setVisibility(View.GONE);
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
//        MainActivity.mainInstance.showToastMessage(Arrays.toString(mSpinnerMap));
        Utils.showKeyboardOnFocus(mEtAEFRecord, requireContext());
    }

    private void btEditPressedFirstTime() {
//        mSpinnerAdapter.clear();
        Log.d("APP", "IS INTERCEPT ON = " + mRootContainer.isInterceptOn());

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
//        mFindBtMode = 0;
//        MainActivity.mainInstance.showToastMessage(Arrays.toString(mSpinnerMap));
        Utils.showKeyboardOnFocus(mEtAEFRecord, requireContext());
    }

    private void btFindPressedFirstTime() {
//        mSpinnerAdapter.clear();
        resetBtAdd();
//        disableViewPagerSwipe();
        mLvRecords.setVisibility(View.GONE);
        mWidgetsContainer.setVisibility(View.GONE);
        mCurItemButtons.setVisibility(View.GONE);
        mDateSelector.setVisibility(View.VISIBLE);
        hideTotals();

        mSwipeRangeMap = getSwipeRangeMap();
//        mFindStartCalendar = new GregorianCalendar();
//        mFindEndCalendar = new GregorianCalendar();

        mFindStartCalendar = Calendar.getInstance();
        mFindEndCalendar = Calendar.getInstance();

        setMonthStartOf(mFindStartCalendar);
        setDayEndOf(mFindEndCalendar);

        refreshFindEt();

        mIsAddBtPressed = false;
        mIsEditBtPressed = false;
        mFindBtMode = 1;
        Utils.hideKeyboard(getAppMainActivity());
        mRootContainer.setInterceptOn();
    }

    private double[] calcRecordsTotals() {
        int itemsCount = 0;
        double totalValue = 0, averageValue, maxValue, minValue;
        NotebookRecord curRecord;
        minValue = maxValue = mRecordAdapter.getItem(0).getAmount();

        for (int i = 0; i < mRecordAdapter.getCount(); i++) {
            curRecord = mRecordAdapter.getItem(i);
            itemsCount++;
            totalValue = Double.sum(totalValue, curRecord.getAmount());
            if (Double.compare(curRecord.getAmount(), minValue) < 0)
                minValue = curRecord.getAmount();
            if (Double.compare(curRecord.getAmount(), maxValue) > 0) {
                maxValue = curRecord.getAmount();
            }
        }
        averageValue = totalValue / itemsCount;
        return new double[]{itemsCount, minValue, maxValue, averageValue, totalValue};
    }

    private void disableViewPagerSwipe() {
        ((NonSwipeableViewPager) getAppMainActivity().getViewPager()).setSwipeEnabled(false);
    }

    private void enableViewPagerSwipe() {
        ((NonSwipeableViewPager) getAppMainActivity().getViewPager()).setSwipeEnabled(true);
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
            Utils.showToastMessage(getString(R.string.error_msg_invalid_amount_input), getContext());
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
            Utils.showToastMessage(getString(R.string.error_msg_invalid_date_input), getContext());
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
        NotebookRecord record = mRecordAdapter.getItem(mRecordAdapter.getSelectedItems().get(0));
        spinnerMap[0] = Utils.getDateFormatInstance(Utils.FormatType.DB_DATE_TIME)
                .format(record.getDate());
        spinnerMap[1] = String.valueOf(record.getAmount());
        spinnerMap[2] = record.getDescription();
        return spinnerMap;
    }

    private int[] getSwipeRangeMap() {
        return new int[]{RANGE_SWIPE_OFF, Calendar.DAY_OF_MONTH, Calendar.WEEK_OF_MONTH, Calendar.MONTH, Calendar.YEAR};
    }

    private void hideTotals() {
        View totalsContainer = mRootContainer.findViewById(R.id.totals_container);
        totalsContainer.setVisibility(View.GONE);
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

        mSpinnerAEFAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, new ArrayList<>());
        mSpinnerAEF = mRootContainer.findViewById(R.id.spinner);
        mSpinnerAEF.setVisibility(View.VISIBLE);
        mSpinnerAEF.setAdapter(mSpinnerAEFAdapter);
        mSpinnerAEF.setOnItemSelectedListener(this);

        mWidgetsContainer = mRootContainer.findViewById(R.id.widgets_container);
        mEtAEFRecord = mRootContainer.findViewById(R.id.et_add_edit_find);

        mDateSelector = mRootContainer.findViewById(R.id.date_selector_container);

        mEtFindStartDate = mRootContainer.findViewById(R.id.et_start_date);
        mEtFindStartDate.setOnFocusChangeListener(this);
//        mEtFindStartDate.addTextChangedListener(textWatcher);

        mEtFindEndDate = mRootContainer.findViewById(R.id.et_end_date);
        mEtFindEndDate.setOnFocusChangeListener(this);

        mLvRecords = mRootContainer.findViewById(R.id.lv_items);
        mRecordAdapter = new RecordAdapter(getContext());
        mLvRecords.setAdapter(mRecordAdapter);
        mLvRecords.setOnItemClickListener(this);
        mLvRecords.setVisibility(View.GONE);

        mCurItemButtons = mRootContainer.findViewById(R.id.bottom_bts);
        mCurItemButtons.setVisibility(View.GONE);

        mRootContainer.setInterceptOff();
        mRootContainer.setOnTouchListener(this);
    }

    private boolean isDateEqual(Calendar first, Calendar second) {
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
                && first.get(Calendar.MONTH) == second.get(Calendar.MONTH)
                && first.get(Calendar.DAY_OF_MONTH) == second.get(Calendar.DAY_OF_MONTH);
    }

    private boolean isHorizontalSwipe(float curDragX, float curDragY) {
        return Math.abs(curDragX - mRootContainer.getDownPointX()) > Math.abs(curDragY - mRootContainer.getDownPointY());
    }

    private boolean isVerticalSwipe(float curDragX, float curDragY) {
        return Math.abs(curDragX - mRootContainer.getDownPointX()) < Math.abs(curDragY - mRootContainer.getDownPointY());
    }

    private void resetBtAdd() {
        ((Button) mRootContainer.findViewById(R.id.bt_add)).setText(R.string.bt_add);
    }

    private void resetFind() {
        mFindBtMode = 0;
        mFindEndCalendar = null;
        mFindStartCalendar = null;
        mSwipeRangeMap = null;
        mCurSwipeRangeIdx = 0;
        setSwipeRangeOff();
        mDateSelector.setVisibility(View.GONE);
        hideTotals();
        mRootContainer.setInterceptOff();
    }

    private void resetSelectedItems() {
//        List<Integer> selectedItems = mRecordAdapter.getSelectedItems();
//        if (!selectedItems.isEmpty()) {
//            View curListItem;
//            for (Integer pos : selectedItems) {
//                curListItem = Utils.getViewByPosition(pos, mLvRecords);
//                curListItem.setBackgroundColor(getResources().getColor(R.color.white));
//                ((TextView) (curListItem.findViewById(R.id.tv_date))).setTextColor(getResources().getColor(R.color.tv_time_date_text_color));
//                ((TextView) (curListItem.findViewById(R.id.tv_time))).setTextColor(getResources().getColor(R.color.tv_time_date_text_color));
//                ((TextView) (curListItem.findViewById(R.id.tv_ago))).setTextColor(getResources().getColor(R.color.tv_time_date_text_color));
//                ((TextView) (curListItem.findViewById(R.id.tv_amount))).setTextColor(getResources().getColor(R.color.tv_time_date_text_color));
//            }
//            selectedItems.clear();
//        }
        mRecordAdapter.getSelectedItems().clear();
        mRecordAdapter.notifyDataSetChanged();
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
            NotebookRecord editRecord = mRecordAdapter.getItem(mRecordAdapter.getSelectedItems().get(0));
            mSpinnerAEFMap[1] = String.valueOf(editRecord.getAmount());
            setSpinnerAEFSelection(1);
        }
    }

    private void restoreSpinnerAEFDate() {
        if (mIsEditBtPressed) {
            NotebookRecord editRecord = mRecordAdapter.getItem(mRecordAdapter.getSelectedItems().get(0));
            mSpinnerAEFMap[0] = Utils.getDateFormatInstance(Utils.FormatType.DB_DATE_TIME)
                    .format(editRecord.getDate());
            setSpinnerAEFSelection(0);
        }
    }

    private void setDayStartOf(Calendar inputCalendar) {
        inputCalendar.set(Calendar.HOUR_OF_DAY, inputCalendar.getActualMinimum(Calendar.HOUR_OF_DAY));
        inputCalendar.set(Calendar.MINUTE, inputCalendar.getActualMinimum(Calendar.MINUTE));
        inputCalendar.set(Calendar.SECOND, inputCalendar.getActualMinimum(Calendar.SECOND));
        inputCalendar.set(Calendar.MILLISECOND, inputCalendar.getActualMinimum(Calendar.MILLISECOND));
    }

    private void setWeekStartOf(Calendar inputCalendar) {
        inputCalendar.set(Calendar.DAY_OF_WEEK, inputCalendar.getActualMinimum(Calendar.DAY_OF_WEEK));
        setDayStartOf(inputCalendar);
    }

    private void setMonthStartOf(Calendar inputCalendar) {
        inputCalendar.set(Calendar.DAY_OF_MONTH, inputCalendar.getActualMinimum(Calendar.DAY_OF_MONTH));
        setDayStartOf(inputCalendar);
//        Log.d("RECORD", "MILLISECONDS MIN = " + inputCalendar.getActualMinimum(Calendar.MILLISECOND));

    }

    private void setYearStartOf(Calendar inputCalendar) {
        inputCalendar.set(Calendar.MONTH, inputCalendar.getActualMinimum(Calendar.MONTH));
        setMonthStartOf(inputCalendar);
    }

    private void setDayEndOf(Calendar inputCalendar) {
//        inputCalendar.set(Calendar.DAY_OF_MONTH, inputCalendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        inputCalendar.set(Calendar.HOUR_OF_DAY, inputCalendar.getActualMaximum(Calendar.HOUR_OF_DAY));
        inputCalendar.set(Calendar.MINUTE, inputCalendar.getActualMaximum(Calendar.MINUTE));
        inputCalendar.set(Calendar.SECOND, inputCalendar.getActualMaximum(Calendar.SECOND));
        inputCalendar.set(Calendar.MILLISECOND, inputCalendar.getActualMaximum(Calendar.MILLISECOND));
//        Log.d("RECORD", "MILLISECONDS MAX = " + inputCalendar.getActualMaximum(Calendar.MILLISECOND));

    }

    private void setWeekEndOf(Calendar inputCalendar) {
        inputCalendar.set(Calendar.DAY_OF_WEEK, inputCalendar.getActualMaximum(Calendar.DAY_OF_WEEK));
        setDayEndOf(inputCalendar);
    }

    private void setMonthEndOf(Calendar inputCalendar) {
        inputCalendar.set(Calendar.DAY_OF_MONTH, inputCalendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        setDayEndOf(inputCalendar);
    }

    private void setYearEndOf(Calendar inputCalendar) {
        inputCalendar.set(Calendar.MONTH, inputCalendar.getActualMaximum(Calendar.MONTH));
        setMonthEndOf(inputCalendar);
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

    private Pair<Long, Long> genPair() {
        long offsetStart = calcZoneOffsetForTime(mFindStartCalendar.getTime().getTime());
        long offsetEnd = calcZoneOffsetForTime(mFindEndCalendar.getTime().getTime());

        return new Pair<>(mFindStartCalendar.getTime().getTime() + offsetStart,
                mFindEndCalendar.getTime().getTime() + offsetEnd);
    }

    private long calcZoneOffsetForTime(long curTime) {
        return DateTimeZone.forTimeZone(TimeZone.getDefault()).getOffset(curTime);
    }

    @SuppressLint("RestrictedApi")
    private void showDateRangePicker() {
        MaterialDatePicker.Builder<Pair<Long, Long>> rangePickerBuilder = MaterialDatePicker.Builder.dateRangePicker();

        CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder();
        constraintsBuilder.setFirstDayOfWeek(Calendar.MONDAY);
        rangePickerBuilder.setCalendarConstraints(constraintsBuilder.build());

        Pair<Long, Long> pair = genPair();
        rangePickerBuilder.setSelection(pair);
        MaterialDatePicker<Pair<Long, Long>> rangePicker = rangePickerBuilder.build();

        rangePicker.show(requireActivity().getSupportFragmentManager(), "");
        rangePicker.addOnPositiveButtonClickListener(
                selection -> {

                    Date startDate = new Date(selection.first - calcZoneOffsetForTime(selection.first));
                    Date endDate = new Date(selection.second - calcZoneOffsetForTime(selection.second));

                    updateCalendarYMD(mFindStartCalendar, startDate);
                    updateCalendarYMD(mFindEndCalendar, endDate);

                    refreshFindEt();
                    resetFindResults();
/*
                    dateFormat = Utils.getDateFormatInstance(Utils.FormatType.DB_DATE_TIME);

                    DurationFieldType[] durFields = new DurationFieldType[2];
                    durFields[0] = DurationFieldType.hours();
                    durFields[1] = DurationFieldType.minutes();
                    Period period = new Period(timeZoneOffset, PeriodType.forFields(durFields));
                    int hours = period.getHours();
                    int minutes = period.getMinutes();
                    Log.d("RECORD", "TIME ZONE OFFSET: " + String.format(Locale.US, Utils.AGO_FORMAT_PATTERN_HM, hours, minutes));
                    Log.d("RECORD", "TimeZone   "+TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT)+" Timezone id :: " + TimeZone.getDefault().getID());
*/

//                    Log.d("RECORD", "-----------------------------------");
//                    Log.d("RECORD", "INPUT FIRST: " + dateFormat.format(pair.first));
//                    Log.d("RECORD", "INPUT SECOND: " + dateFormat.format(pair.second));
//                    Log.d("RECORD", "OUTPUT START: " + dateFormat.format(startDate));
//                    Log.d("RECORD", "OUTPUT END: " + dateFormat.format(endDate));
//                    Log.d("RECORD", "CALENDAR START: " + dateFormat.format(mFindStartCalendar.getTime()));
//                    Log.d("RECORD", "CALENDAR END: " + dateFormat.format(mFindEndCalendar.getTime()));
                }
        );
    }

    private void refreshFindEt() {
        DateFormat dateFormat = Utils.getDateFormatInstance(Utils.FormatType.RECORD_FIND_ET);
        mEtFindStartDate.setText(dateFormat.format(mFindStartCalendar.getTime()));
        mEtFindEndDate.setText(dateFormat.format(mFindEndCalendar.getTime()));
    }

    private void showRecordList(List<NotebookRecord> input) {
        mRecordAdapter.clear();
        mRecordAdapter.addAll(input);
        if (mRecordAdapter.isEmpty()) {
            TextView tvMssage = mRootContainer.findViewById(R.id.tv_message);
            tvMssage.setVisibility(View.VISIBLE);
            tvMssage.setText(getResources().getString(R.string.tv_message_list_empty));
        } else {
            mRecordAdapter.notifyDataSetChanged();
        }
    }

    private void showTimePickerDialog(int titleStringId, Calendar initCalendar, Consumer<Calendar> onTimeSetAction) {
        TimePickerFragment timePickerDialog = new TimePickerFragment(initCalendar, titleStringId);
        timePickerDialog.setOnTimeSetAction(onTimeSetAction);
        timePickerDialog.show(getAppMainActivity().getSupportFragmentManager(), getString(titleStringId));
    }

    private void showTotals() {
        View totalsContainer = mRootContainer.findViewById(R.id.totals_container);
        totalsContainer.setVisibility(View.VISIBLE);
    }

    private void setTotals(double[] totals) {
        TextView tvItemCount = mRootContainer.findViewById(R.id.tv_items_count);
        TextView tvMinValue = mRootContainer.findViewById(R.id.tv_min_value);
        TextView tvMaxValue = mRootContainer.findViewById(R.id.tv_max_value);
        TextView tvAverageValue = mRootContainer.findViewById(R.id.tv_average_value);
        TextView tvTotalValue = mRootContainer.findViewById(R.id.tv_total_value);

        tvItemCount.setText(Utils.formatDouble(totals[0], 0));
        tvMinValue.setText(Utils.formatDouble(totals[1], 2));
        tvMaxValue.setText(Utils.formatDouble(totals[2], 2));
        tvAverageValue.setText(Utils.formatDouble(totals[3], 2));
        tvTotalValue.setText(Utils.formatDouble(totals[4], 2));
    }

    private void showTotals(double[] totals) {
        setTotals(totals);
        mRootContainer.findViewById(R.id.totals_container).setVisibility(View.VISIBLE);
    }

    private void updateCalendarYMD(Calendar toUpdate, Date update) {
        Calendar calendarUpdate = Calendar.getInstance();
        calendarUpdate.setTime(update);

        updateCalendarYMD(toUpdate, calendarUpdate.get(Calendar.YEAR),
                calendarUpdate.get(Calendar.MONTH),
                calendarUpdate.get(Calendar.DAY_OF_MONTH));
//        hideTotals();
//        mRecordAdapter.clear();
//        mRecordAdapter.notifyDataSetChanged();
    }

    private static class DateETWatcher implements TextWatcher {

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

            static boolean isAtLeastOneTrue(TextWatcherParamHolder paramHolder) {
                return Arrays.stream(BeforeTextChangedEvents.values())
                        .anyMatch(beforeTextChangedEvent -> beforeTextChangedEvent.isTrue(paramHolder));
            }

            private final Predicate<TextWatcherParamHolder> eventCondition;

            BeforeTextChangedEvents(Predicate<TextWatcherParamHolder> eventCondition) {
                this.eventCondition = eventCondition;
            }

            boolean isTrue(TextWatcherParamHolder paramHolder) {
                return this.eventCondition.test(paramHolder);
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

            static boolean isAtLeastOneTrue(TextWatcherParamHolder paramHolder) {
                return Arrays.stream(OnTextChangedEvents.values())
                        .anyMatch(onTextChangedEvent -> onTextChangedEvent.isTrue(paramHolder));
            }

            private final Predicate<TextWatcherParamHolder> eventCondition;

            OnTextChangedEvents(Predicate<TextWatcherParamHolder> eventCondition) {
                this.eventCondition = eventCondition;
            }

            boolean isTrue(TextWatcherParamHolder paramHolder) {
                return this.eventCondition.test(paramHolder);
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

        private static class TextWatcherParamHolder {
            private static TextWatcherParamHolder instance;

            private static TextWatcherParamHolder getInstance() {
                if (instance == null) {
                    instance = new TextWatcherParamHolder();
                }
                return instance;
            }

            private CharSequence charSequence;
            private int nextCharQty;
            private int prevCharQty;
            private int start;

            public void setParams(CharSequence charSequence, int start, int prevCharQty, int nextCharQty) {
                this.charSequence = charSequence;
                this.start = start;
                this.prevCharQty = prevCharQty;
                this.nextCharQty = nextCharQty;
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

            DatePickerDialog dialog = new DatePickerDialog(getContext(), this, year, month, day);
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

            TimePickerDialog dialog = new TimePickerDialog(getContext(), this, hour, minute, false);
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
