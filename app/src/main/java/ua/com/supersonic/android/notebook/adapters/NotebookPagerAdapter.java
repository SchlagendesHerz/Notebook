package ua.com.supersonic.android.notebook.adapters;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import ua.com.supersonic.android.notebook.MainActivity;
import ua.com.supersonic.android.notebook.fragments.NotebookCategoriesFragment;
import ua.com.supersonic.android.notebook.fragments.NotebookRecordsFragment;
import ua.com.supersonic.android.notebook.R;

public class NotebookPagerAdapter extends FragmentPagerAdapter {
    private static final int NOTEBOOK_APP_TAB_NUMBER = 2;

    public NotebookPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public int getCount() {
        return NOTEBOOK_APP_TAB_NUMBER;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return position == 0
                ? MainActivity.mainInstance.getString(R.string.tab_categories)
                : MainActivity.mainInstance.getString(R.string.tab_items);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return position == 0
                ? MainActivity.categoriesFragment
                : MainActivity.recordsFragment;
    }
}
