package ua.com.supersonic.android.notebook.db;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ua.com.supersonic.android.notebook.MainActivity;
import ua.com.supersonic.android.notebook.NotebookCategory;
import ua.com.supersonic.android.notebook.NotebookRecord;
import ua.com.supersonic.android.notebook.utils.Utils;

public class DBManager {
    private static final String DB_DATE_TIME_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static DBManager sDBManager;

    public static DBManager getInstance() {
        return sDBManager == null
                ? sDBManager = new DBManager()
                : sDBManager;
    }

    private SQLiteDatabase mDB;
    private DBHelper mDBHelper;

    private DBManager() {
        this.mDBHelper = new DBHelper(MainActivity.mainInstance.getApplicationContext());
    }

    public void addCategory(NotebookCategory... catsToAdd) {
        ContentValues values = new ContentValues();
        for (NotebookCategory cat : catsToAdd) {
            values.put(DBConstants.COLUMN_CATEGORY_NAME, cat.getName());
            values.put(DBConstants.COLUMN_RECORDS_QUANTITY, 0);
            mDB.insert(DBConstants.TABLE_CATEGORIES, null, values);
            values.clear();
        }
    }

    public void addRecord(NotebookRecord... recordsToAdd) {
        ContentValues values = new ContentValues();
        for (NotebookRecord record : recordsToAdd) {
            values.put(DBConstants.COLUMN_CATEGORY_ID, record.getCategoryId());
            values.put(DBConstants.COLUMN_RECORD_DATE,
                    Utils.getDateFormatInstance(Utils.FormatType.DB_DATE_TIME).format(record.getDate()));
            values.put(DBConstants.COLUMN_RECORD_AMOUNT, record.getAmount());
            values.put(DBConstants.COLUMN_RECORD_DESCR, record.getDescription());
            mDB.insert(DBConstants.TABLE_RECORDS, null, values);
            values.clear();
        }
    }

    public void openDB() {
        this.mDB = mDBHelper.getWritableDatabase();
//        Log.d("SQL", "getWritableDatabase INVOKED!");
    }

    public void closeDB() {
        mDBHelper.close();
        mDB = null;
    }

    public void deleteCategories(List<NotebookCategory> catsToDelete) {
        String[] iDToDelete = new String[1];
        for (NotebookCategory curCat : catsToDelete) {
            iDToDelete[0] = curCat.getName();
            mDB.delete(DBConstants.TABLE_CATEGORIES, DBConstants.SELECTION_BY_CATEGORY_NAME_KEY, iDToDelete);
        }
    }

    public void deleteRecords(List<NotebookRecord> recordsToDelete) {
        String[] iDToDelete = new String[1];
        for (NotebookRecord curRecord : recordsToDelete) {
            iDToDelete[0] = String.valueOf(curRecord.getId());
            mDB.delete(DBConstants.TABLE_RECORDS, DBConstants.SELECTION_BY_RECORD_ID_KEY, iDToDelete);
        }
    }

    public void dropTable() {
        mDB.execSQL(DBConstants.DELETE_CATEGORY_TABLE);
    }

    public boolean isOpened() {
        return mDB != null && mDB.isOpen();
    }

    @SuppressLint("Range")
    public List<NotebookCategory> readAllCategories() {
        Cursor cursor = mDB.query(
                DBConstants.TABLE_CATEGORIES,   // The table to query
                null,             // The array of columns to return (pass null to get all)
                null,              // The columns for the WHERE clause
                null,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                null               // The sort order
        );
        return cursorToCategories(cursor);
    }

    public NotebookRecord readLastRecordInCategory(int categoryId) {
        Cursor cursor = mDB.query(
                DBConstants.TABLE_RECORDS,   // The table to query
                null,             // The array of columns to return (pass null to get all)
                DBConstants.COLUMN_CATEGORY_ID + " LIKE ?",              // The columns for the WHERE clause
                new String[]{String.valueOf(categoryId)},          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                DBConstants.ORDER_BY_RECORD_DATE_DESC,               // The sort order
                "1"
        );
        List<NotebookRecord> lastRecordList = cursorToRecords(cursor);
        return lastRecordList.isEmpty() ? null : lastRecordList.get(0);
    }

    @SuppressLint("Range")
    public List<NotebookRecord> readAllRecords() {
        Cursor cursor = mDB.query(
                DBConstants.TABLE_RECORDS,   // The table to query
                null,             // The array of columns to return (pass null to get all)
                null,              // The columns for the WHERE clause
                null,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                null               // The sort order
        );
        return cursorToRecords(cursor);
    }

    public List<NotebookCategory> readCategoriesWhereKeyLike(String keyName, String likeString) {
        Cursor cursor = mDB.query(
                DBConstants.TABLE_CATEGORIES,   // The table to query
                null,             // The array of columns to return (pass null to get all)
                keyName + " LIKE ?",              // The columns for the WHERE clause
                new String[]{likeString},          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                null               // The sort order
        );
        return cursorToCategories(cursor);
    }

    public List<NotebookCategory> readCategoriesWhereKeyEquals(String keyName, String equalsString) {
        Cursor cursor = mDB.query(
                DBConstants.TABLE_CATEGORIES,   // The table to query
                null,             // The array of columns to return (pass null to get all)
                keyName + " = ?",              // The columns for the WHERE clause
                new String[]{equalsString},          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                null               // The sort order
        );
        return cursorToCategories(cursor);
    }

    public List<NotebookRecord> readRecordsWhereDateBetween(int categoryId, Date start, Date end) {
        DateFormat dateFormat = Utils.getDateFormatInstance(Utils.FormatType.DB_DATE_TIME);
        Cursor cursor = mDB.query(
                DBConstants.TABLE_RECORDS,   // The table to query
                null,             // The array of columns to return (pass null to get all)
                DBConstants.SELECTION_BY_RECORD_DATE_BETWEEN_KEY,              // The columns for the WHERE clause
                new String[]{String.valueOf(categoryId), dateFormat.format(start), dateFormat.format(end)},          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                DBConstants.ORDER_BY_RECORD_DATE_DESC              // The sort order
        );
        return cursorToRecords(cursor);
    }

    public List<NotebookRecord> readRecordsWhereKeyEquals(String keyName, String equalsString) {
        Cursor cursor = mDB.query(
                DBConstants.TABLE_RECORDS,   // The table to query
                null,             // The array of columns to return (pass null to get all)
                keyName + " = ?",              // The columns for the WHERE clause
                new String[]{equalsString},          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                null               // The sort order
        );
        return cursorToRecords(cursor);
    }

    public void updateCategory(NotebookCategory category) {
        ContentValues cv = new ContentValues();
        String catIdString = String.valueOf(category.getId());
        cv.put(DBConstants.COLUMN_CATEGORY_NAME, category.getName());
        mDB.update(DBConstants.TABLE_CATEGORIES, cv, DBConstants.SELECTION_BY_CATEGORY_ID_KEY, new String[]{catIdString});
    }

    public void updateRecord(NotebookRecord record) {
        ContentValues cv = new ContentValues();
        String recordIdString = String.valueOf(record.getId());
        cv.put(DBConstants.COLUMN_RECORD_DATE,
                Utils.getDateFormatInstance(Utils.FormatType.DB_DATE_TIME).format(record.getDate()));
        cv.put(DBConstants.COLUMN_RECORD_AMOUNT, record.getAmount());
        cv.put(DBConstants.COLUMN_RECORD_DESCR, record.getDescription());
        mDB.update(DBConstants.TABLE_RECORDS, cv, DBConstants.SELECTION_BY_RECORD_ID_KEY, new String[]{recordIdString});
    }

    @SuppressLint("Range")
    private List<NotebookCategory> cursorToCategories(Cursor cursor) {
        List<NotebookCategory> result = new ArrayList<>();
        NotebookCategory newCategory;
        while (cursor.moveToNext()) {
            newCategory = new NotebookCategory();
            newCategory.setId(Integer.parseInt(cursor.getString(cursor.getColumnIndex(DBConstants.COLUMN_CATEGORY_ID))));
            newCategory.setName(cursor.getString(cursor.getColumnIndex(DBConstants.COLUMN_CATEGORY_NAME)));
            newCategory.setRecordQuantity(cursor.getInt(cursor.getColumnIndex(DBConstants.COLUMN_RECORDS_QUANTITY)));
            String lastRecordDateString = cursor.getString(cursor.getColumnIndex(DBConstants.COLUMN_LAST_RECORD_DATE));
            try {
                newCategory.setLastRecordDate(lastRecordDateString == null
                        ? null
                        : Utils.getDateFormatInstance(Utils.FormatType.DB_DATE_TIME)
                        .parse(lastRecordDateString));
            } catch (ParseException e) {
                newCategory.setLastRecordDate(null);
            }
            result.add(newCategory);
        }
        cursor.close();
        return result;
    }

    @SuppressLint("Range")
    private List<NotebookRecord> cursorToRecords(Cursor cursor) {
        List<NotebookRecord> result = new ArrayList<>();
        NotebookRecord newRecord;
        while (cursor.moveToNext()) {
            newRecord = new NotebookRecord();
            newRecord.setId(cursor.getInt(cursor.getColumnIndex(DBConstants.COLUMN_RECORD_ID)));
            newRecord.setCategoryId(cursor.getInt(cursor.getColumnIndex(DBConstants.COLUMN_CATEGORY_ID)));
            try {
                Date date = Utils.getDateFormatInstance(Utils.FormatType.DB_DATE_TIME)
                        .parse(cursor.getString(cursor.getColumnIndex(DBConstants.COLUMN_RECORD_DATE)));
                newRecord.setDate(date);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            newRecord.setAmount(cursor.getDouble(cursor.getColumnIndex(DBConstants.COLUMN_RECORD_AMOUNT)));
            newRecord.setDescription(cursor.getString(cursor.getColumnIndex(DBConstants.COLUMN_RECORD_DESCR)));
            result.add(newRecord);
        }
        cursor.close();
        return result;
    }

}
