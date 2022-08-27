package ua.com.supersonic.android.notebook.db;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class DBManager {
    private Context mContext;
    private DBHelper mDBHelper;
    private SQLiteDatabase mDB;

    public DBManager(Context context) {
        this.mContext = context;
        this.mDBHelper = new DBHelper(context);
    }

    public void openDB() {
        this.mDB = mDBHelper.getWritableDatabase();
    }

    public void insertToDB(String category) {
        ContentValues values = new ContentValues();
        values.put(DBConstants.COLUMN_NAME, category);
        mDB.insert(DBConstants.TABLE_CATEGORIES_NAME, null, values);
    }

    public List<String> readFromDB() {
        List<String> read = new ArrayList<>();
        Cursor cursor = mDB.query(
                DBConstants.TABLE_CATEGORIES_NAME,   // The table to query
                null,             // The array of columns to return (pass null to get all)
                null,              // The columns for the WHERE clause
                null,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                null               // The sort order
        );
        while (cursor.moveToNext()) {
            @SuppressLint("Range")
            String categoryName = cursor.getString(cursor.getColumnIndex(DBConstants.COLUMN_NAME));
            read.add(categoryName);
        }
        cursor.close();
        return read;
    }

    public void deleteFromDB(String... toDelete) {
        String selection = DBConstants.DELETE_ROW_SELECTION;
        mDB.delete(DBConstants.TABLE_CATEGORIES_NAME, selection, toDelete);
    }

    public void closeDB() {
        mDBHelper.close();
        mDB = null;
    }

    public boolean isOpened() {
        return mDB != null && mDB.isOpen();
    }

    public void dropTable() {
        mDB.execSQL(DBConstants.DELETE_ENTRIES);
    }

}
