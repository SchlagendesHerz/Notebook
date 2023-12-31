package ua.com.supersonic.android.notebook.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

public class DBHelper extends SQLiteOpenHelper {

    public DBHelper(@Nullable Context context) {
        super(context, DBConstants.DB_NAME, null, DBConstants.DB_VERSION);

    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        Log.d("SQL", "onCreate INVOKED!");
        sqLiteDatabase.execSQL(DBConstants.CREATE_CATEGORY_TABLE);
        sqLiteDatabase.execSQL(DBConstants.CREATE_RECORDS_TABLE);

        sqLiteDatabase.execSQL(DBConstants.CREATE_TRIGGER_INC_AFTER_RECORD_INS);
        sqLiteDatabase.execSQL(DBConstants.CREATE_TRIGGER_DEC_AFTER_RECORD_DEL);
        sqLiteDatabase.execSQL(DBConstants.CREATE_TRIGGER_UPD_LAST_DATE_AFTER_RECORD_INS);
        sqLiteDatabase.execSQL(DBConstants.CREATE_TRIGGER_UPD_LAST_DATE_AFTER_RECORD_UPD);
        sqLiteDatabase.execSQL(DBConstants.CREATE_TRIGGER_UPD_LAST_DATE_AFTER_RECORD_DEL);

        sqLiteDatabase.execSQL(DBConstants.CREATE_TRIGGER_SET_PREV_RECORD_AFTER_RECORD_INS);
        sqLiteDatabase.execSQL(DBConstants.CREATE_TRIGGER_UPD_PREV_RECORD_AFTER_RECORD_UPD);
        sqLiteDatabase.execSQL(DBConstants.CREATE_TRIGGER_UPD_PREV_RECORD_AFTER_RECORD_DEL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        Log.d("SQL", "onUpgrade INVOKED!");

        sqLiteDatabase.execSQL(DBConstants.DELETE_RECORDS_TABLE);
        sqLiteDatabase.execSQL(DBConstants.DELETE_CATEGORY_TABLE);
        onCreate(sqLiteDatabase);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d("SQL", "onDowngrade INVOKED!");

        db.execSQL(DBConstants.DELETE_RECORDS_TABLE);
        db.execSQL(DBConstants.DELETE_CATEGORY_TABLE);
        onCreate(db);
    }
}
