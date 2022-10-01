package ua.com.supersonic.android.notebook.adapters;

import android.icu.util.Calendar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.joda.time.DateTime;
import org.joda.time.DurationFieldType;
import org.joda.time.Period;
import org.joda.time.PeriodType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import ua.com.supersonic.android.notebook.MainActivity;
import ua.com.supersonic.android.notebook.NotebookRecord;
import ua.com.supersonic.android.notebook.R;

public class RecordAdapter extends ArrayAdapter<NotebookRecord> {
    private static final String AGO_FORMAT_PATTERN_D = "%dd ago";
    private static final String AGO_FORMAT_PATTERN_HM = "%dh; %dm ago";
    private static final String AGO_FORMAT_PATTERN_MOD = "%dm; %dd ago";
    private static final String AGO_FORMAT_PATTERN_YMD = "%dy;%dm;%dd ago";
    private static final String DATE_FORMAT_PATTERN = "MMM d, yyyy";
    private static final String TIME_FORMAT_PATTERN = "E h:mm a";

    private static SimpleDateFormat recordItemFormat;

    public static String formatAgo(Date input) {
        DateTime start = new DateTime(input);
        DateTime end = new DateTime(Calendar.getInstance().getTime());

//        Period period = new Period(start, end, PeriodType.yearMonthDay());
        DurationFieldType[] periodType = new DurationFieldType[5];
        periodType[0] = DurationFieldType.years();
        periodType[1] = DurationFieldType.months();
        periodType[2] = DurationFieldType.days();
        periodType[3] = DurationFieldType.hours();
        periodType[4] = DurationFieldType.minutes();

        Period period = new Period(start, end, PeriodType.forFields(periodType));

        int years = period.getYears();
        int months = period.getMonths();
        int days = period.getDays();
        int hours = 0;
        int minutes = 0;

        if (days == 0) {
            hours = period.getHours();
            minutes = period.getMinutes();
        }

//        return String.format(Locale.ROOT, AGO_FORMAT_PATTERN_FULL, years, months, days);
/*        return years == 0
                ? months == 0
                    ? String.format(Locale.US, AGO_FORMAT_PATTERN_D, days)
                    : String.format(Locale.US, AGO_FORMAT_PATTERN_MD, months, days)
                : String.format(Locale.US, AGO_FORMAT_PATTERN_YMD, years, months, days); */

        return years == 0
                ? months == 0
                ? days == 0
                ? String.format(Locale.US, AGO_FORMAT_PATTERN_HM, hours, minutes)
                : String.format(Locale.US, AGO_FORMAT_PATTERN_D, days)
                : String.format(Locale.US, AGO_FORMAT_PATTERN_MOD, months, days)
                : String.format(Locale.US, AGO_FORMAT_PATTERN_YMD, years, months, days);
    }

    private static SimpleDateFormat getRecordItemDateFormat() {
        if (recordItemFormat == null) {
            recordItemFormat = new SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.US);
        } else {
            recordItemFormat.applyPattern(DATE_FORMAT_PATTERN);
        }
        return recordItemFormat;
    }

    private static SimpleDateFormat getRecordItemTimeFormat() {
        if (recordItemFormat == null) {
            recordItemFormat = new SimpleDateFormat(TIME_FORMAT_PATTERN, Locale.US);
        } else {
            recordItemFormat.applyPattern(TIME_FORMAT_PATTERN);
        }
        return recordItemFormat;
    }

    public RecordAdapter() {
        super(MainActivity.mainInstance.getApplicationContext(), 0, new ArrayList<>());
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.list_item_record, parent, false);
        }
        NotebookRecord item = getItem(position);
        if (item != null) {
            TextView textView = convertView.findViewById(R.id.tv_ordinal);
            textView.setText((position + 1) + ".");

            textView = convertView.findViewById(R.id.tv_amount);
            textView.setText(formatAmount(item.getAmount()));

            textView = convertView.findViewById(R.id.tv_desc);
//            String desc = item.getDescription() == null ? "--" : item.getDescription();
            textView.setText(item.getDescription());

            textView = convertView.findViewById(R.id.tv_date);
            textView.setText(formatDate(item.getDate()));

            textView = convertView.findViewById(R.id.tv_time);
            textView.setText(formatTime(item.getDate()));

            textView = convertView.findViewById(R.id.tv_ago);
            textView.setText(formatAgo(item.getDate()));

            if (MainActivity.recordsFragment.getSelectedItems().contains(position)) {
                convertView.setBackgroundColor(MainActivity.mainInstance.getResources().getColor(R.color.list_item_selected));
            } else {
                convertView.setBackgroundColor(MainActivity.mainInstance.getResources().getColor(R.color.white));
            }
        }
        return convertView;
    }

    private String formatAmount(double amount) {
        return amount - ((int) amount) < 0.01
                ? String.format(Locale.US, "%d", (int) amount)
                : String.format(Locale.US, "%.2f", amount);
    }

    private String formatDate(Date date) {
        return getRecordItemDateFormat().format(date);
    }

    private String formatTime(Date date) {
//        Locale curLocale = MainActivity.mainInstance.getResources().getConfiguration().locale;

        return getRecordItemTimeFormat().format(date);
    }
}
