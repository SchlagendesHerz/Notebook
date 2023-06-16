package ua.com.supersonic.android.notebook.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ua.com.supersonic.android.notebook.NotebookRecord;
import ua.com.supersonic.android.notebook.R;
import ua.com.supersonic.android.notebook.utils.Utils;

public class RecordAdapter extends ArrayAdapter<NotebookRecord> {

    private final List<Integer> mSelectedItems = new ArrayList<>();

    public List<Integer> getSelectedItems() {
        return mSelectedItems;
    }

    public RecordAdapter(Context appContext) {
        super(appContext, 0, new ArrayList<>());
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
            textView.setText(Utils.formatDouble(item.getAmount(), 2));

            textView = convertView.findViewById(R.id.tv_desc);
//            String desc = item.getDescription() == null ? "--" : item.getDescription();
            textView.setText(item.getDescription());

            textView = convertView.findViewById(R.id.tv_date);
            textView.setText(formatDate(item.getDate()));

            textView = convertView.findViewById(R.id.tv_time);
            textView.setText(formatTime(item.getDate()));

            textView = convertView.findViewById(R.id.tv_ago);
            textView.setText(Utils.formatAgoDate(item.getDate()));

            textView = convertView.findViewById(R.id.tv_prev_rec_ago);
            textView.setText(item.getPrevDate() == null
                    ? "--"
                    : Utils.formatAgoDate(item.getDate(), item.getPrevDate()));

            if (mSelectedItems.contains(position)) {
                convertView.setBackgroundColor(getContext().getResources().getColor(R.color.list_item_selected));
                ((TextView) (convertView.findViewById(R.id.tv_date))).setTextColor(getContext().getColor(R.color.white));
                ((TextView) (convertView.findViewById(R.id.tv_time))).setTextColor(getContext().getColor(R.color.white));
                ((TextView) (convertView.findViewById(R.id.tv_ago))).setTextColor(getContext().getColor(R.color.white));
                ((TextView) (convertView.findViewById(R.id.tv_amount))).setTextColor(getContext().getColor(R.color.white));
                ((TextView) (convertView.findViewById(R.id.tv_prev_rec_ago))).setTextColor(getContext().getColor(R.color.white));

            } else {
                convertView.setBackgroundColor(getContext().getColor(R.color.white));
                ((TextView) (convertView.findViewById(R.id.tv_date))).setTextColor(getContext().getColor(R.color.tv_time_date_text_color));
                ((TextView) (convertView.findViewById(R.id.tv_time))).setTextColor(getContext().getColor(R.color.tv_time_date_text_color));
                ((TextView) (convertView.findViewById(R.id.tv_ago))).setTextColor(getContext().getColor(R.color.tv_time_date_text_color));
                ((TextView) (convertView.findViewById(R.id.tv_amount))).setTextColor(getContext().getColor(R.color.tv_time_date_text_color));
                ((TextView) (convertView.findViewById(R.id.tv_prev_rec_ago))).setTextColor(getContext().getColor(R.color.tv_time_date_text_color));
            }
        }
        return convertView;
    }

    private String formatDate(Date date) {
        return Utils.getDateFormatInstance(Utils.FormatType.RECORD_ITEM_DATE).format(date);
    }

    private String formatTime(Date date) {
        return Utils.getDateFormatInstance(Utils.FormatType.RECORD_ITEM_TIME).format(date);
    }
}
