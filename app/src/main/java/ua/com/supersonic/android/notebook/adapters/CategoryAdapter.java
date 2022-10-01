package ua.com.supersonic.android.notebook.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ua.com.supersonic.android.notebook.MainActivity;
import ua.com.supersonic.android.notebook.NotebookCategory;
import ua.com.supersonic.android.notebook.NotebookRecord;
import ua.com.supersonic.android.notebook.R;
import ua.com.supersonic.android.notebook.db.DBManager;

public class CategoryAdapter extends ArrayAdapter<NotebookCategory> {

    public CategoryAdapter() {
        super(MainActivity.mainInstance.getApplicationContext(), 0, new ArrayList<>());

    }

    public CategoryAdapter(List<NotebookCategory> objects) {
        super(MainActivity.mainInstance.getApplicationContext(), 0, objects);
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
                textView.setText(String.format("%s %s", getContext().getString(R.string.tv_last_rec_ago_prefix), RecordAdapter.formatAgo(category.getLastRecordDate())));
            }

//            ViewGroup background = convertView.findViewById(R.id.text_container);
//            background.setBackgroundColor(mMiwokActivityCategory.getCategoryColor());
        }
        return convertView;
    }


}
