package ua.com.supersonic.android.notebook.adapters;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.Objects;

import ua.com.supersonic.android.notebook.MainActivity;
import ua.com.supersonic.android.notebook.R;
import ua.com.supersonic.android.notebook.fragments.NotebookCategoriesFragment;
import ua.com.supersonic.android.notebook.fragments.NotebookRecordsFragment;

public class NotebookPagerAdapter extends FragmentPagerAdapter {
    private static final int NOTEBOOK_APP_TAB_NUMBER = 2;

    private String curCategory;
    private final Context appContext;

    public NotebookPagerAdapter(FragmentManager fm, Context appContext) {
        super(fm);
        this.appContext = appContext;
    }

    public void setCurrentCategory(String categoryToSet) {
        this.curCategory = categoryToSet;
    }

    @Override
    public int getCount() {
        return NOTEBOOK_APP_TAB_NUMBER;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {

        return position == 0
                ? appContext.getString(R.string.tab_categories)
                : appContext.getString(R.string.tab_records);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return position == 0
                ? new NotebookCategoriesFragment()
                : new NotebookRecordsFragment();
    }
}
