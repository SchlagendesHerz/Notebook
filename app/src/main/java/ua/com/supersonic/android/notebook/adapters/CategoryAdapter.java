package ua.com.supersonic.android.notebook.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ua.com.supersonic.android.notebook.NotebookCategory;
import ua.com.supersonic.android.notebook.R;
import ua.com.supersonic.android.notebook.utils.Utils;

public class CategoryAdapter extends ArrayAdapter<NotebookCategory> {

    private final List<Integer> mSelectedItems = new ArrayList<>();

    public List<Integer> getSelectedItems() {
        return mSelectedItems;
    }

    public CategoryAdapter(Context appContext) {
        super(appContext, 0, new ArrayList<>());
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.list_item_category, parent, false);
        }
        NotebookCategory category = getItem(position);
        if (category != null) {
            TextView textView = convertView.findViewById(R.id.tv_ordinal);
            textView.setText((position + 1) + ".");
            textView = convertView.findViewById(R.id.tv_title);
            textView.setText(category.getName());
            textView = convertView.findViewById(R.id.tv_rec_quant);
            textView.setText(String.format(getContext().getString(R.string.tv_rec_quant_message), category.getRecordQuantity()));

            textView = convertView.findViewById(R.id.tv_last_rec_ago);
            if (category.getLastRecordDate() == null) {
                textView.setVisibility(View.GONE);
            } else {
                textView.setVisibility(View.VISIBLE);
                textView.setText(String.format("%s %s", getContext().getString(R.string.tv_last_rec_ago_prefix), Utils.formatAgoDate(category.getLastRecordDate())));
            }

            if (mSelectedItems.contains(position)) {
                convertView.setBackgroundColor(getContext().getColor(R.color.list_item_selected));
                ((TextView) (convertView.findViewById(R.id.tv_rec_quant))).setTextColor(getContext().getColor(R.color.white));
                ((TextView) (convertView.findViewById(R.id.tv_last_rec_ago))).setTextColor(getContext().getColor(R.color.white));
            } else {
                convertView.setBackgroundColor(getContext().getColor(R.color.white));
                ((TextView) (convertView.findViewById(R.id.tv_rec_quant))).setTextColor(getContext().getColor(R.color.tv_rec_quant_color));
                ((TextView) (convertView.findViewById(R.id.tv_last_rec_ago))).setTextColor(getContext().getColor(R.color.tv_last_rec_ago_color));
            }

//            ViewGroup background = convertView.findViewById(R.id.text_container);
//            background.setBackgroundColor(mMiwokActivityCategory.getCategoryColor());
        }
        return convertView;
    }


}
