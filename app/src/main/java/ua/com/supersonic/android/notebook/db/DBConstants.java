package ua.com.supersonic.android.notebook.db;

public class DBConstants {
    public static final String DB_NAME = "notebook.db";
    public static final int DB_VERSION = 3;
    public static final String DB_FOREIGN_KEYS_ON = "PRAGMA foreign_keys=ON";

    public static final String TABLE_CATEGORIES = "categories";
    public static final String COLUMN_CATEGORY_ID = "category_id";
    public static final String COLUMN_CATEGORY_NAME = "category_name";
    public static final String COLUMN_RECORDS_QUANTITY = "records_quantity";
    public static final String COLUMN_LAST_RECORD_DATE = "last_record_date";

    public static final String CREATE_CATEGORY_TABLE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_CATEGORIES + " (" +
                    COLUMN_CATEGORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_CATEGORY_NAME + " TEXT UNIQUE NOT NULL, " +
                    COLUMN_RECORDS_QUANTITY + " INTEGER NOT NULL, " +
                    COLUMN_LAST_RECORD_DATE + ")";

    public static final String DELETE_CATEGORY_TABLE = "DROP TABLE IF EXISTS " + TABLE_CATEGORIES;
    public static final String SELECTION_BY_CATEGORY_NAME_KEY = COLUMN_CATEGORY_NAME + " = ?";
    public static final String SELECTION_BY_CATEGORY_ID_KEY = COLUMN_CATEGORY_ID + " = ?";

    public static final String TABLE_RECORDS = "records";
    public static final String COLUMN_RECORD_ID = "record_id";
    public static final String COLUMN_RECORD_DATE = "record_date";
    public static final String COLUMN_RECORD_AMOUNT = "record_amount";
    public static final String COLUMN_RECORD_DESCR = "record_descr";
    public static final String CREATE_RECORDS_TABLE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_RECORDS
                    + " (" + COLUMN_RECORD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_CATEGORY_ID + " INTEGER NOT NULL, "
                    + COLUMN_RECORD_DATE + " TEXT NOT NULL, "
                    + COLUMN_RECORD_AMOUNT + " REAL, "
                    + COLUMN_RECORD_DESCR + " TEXT, "
                    + "CONSTRAINT fk_category "
                    + "FOREIGN KEY (" + COLUMN_CATEGORY_ID + ") "
                    + "REFERENCES " + TABLE_CATEGORIES + "(" + COLUMN_CATEGORY_ID + ") ON DELETE CASCADE)";
    public static final String DELETE_RECORDS_TABLE = "DROP TABLE IF EXISTS " + TABLE_RECORDS;
    public static final String SELECTION_BY_RECORD_ID_KEY = COLUMN_RECORD_ID + " = ?";
    public static final String SELECTION_BY_RECORD_DATE_BETWEEN_KEY = SELECTION_BY_CATEGORY_ID_KEY + " AND "
            + COLUMN_RECORD_DATE + " BETWEEN ? AND ?";
    public static final String ORDER_BY_RECORD_DATE_DESC = COLUMN_RECORD_DATE + " DESC";

    public static final String INC_RECORDS_AFTER_RECORD_INS_TRIGGER_NAME = "inc_records_quantity_after_record_ins";
    public static final String DEC_RECORDS_AFTER_RECORD_DEL_TRIGGER_NAME = "dec_records_quantity_after_record_del";
    public static final String UPD_LAST_RECORD_DATE_AFTER_RECORD_INS_TRIGGER_NAME = "upd_last_record_date_after_record_ins";
    public static final String UPD_LAST_RECORD_DATE_AFTER_RECORD_DEL_TRIGGER_NAME = "upd_last_record_date_after_record_del";
    public static final String UPD_LAST_RECORD_DATE_AFTER_RECORD_UPD_TRIGGER_NAME = "upd_last_record_date_after_record_upd";

    public static final String CREATE_TRIGGER_INC_AFTER_RECORD_INS = "CREATE TRIGGER IF NOT EXISTS " +
            INC_RECORDS_AFTER_RECORD_INS_TRIGGER_NAME +
            " AFTER INSERT ON " + TABLE_RECORDS + " BEGIN UPDATE " + TABLE_CATEGORIES + " SET " +
            COLUMN_RECORDS_QUANTITY + " = " + COLUMN_RECORDS_QUANTITY + " + 1 WHERE " + COLUMN_CATEGORY_ID + " = " +
            "new." + COLUMN_CATEGORY_ID + "; END;";

    public static final String CREATE_TRIGGER_DEC_AFTER_RECORD_DEL = "CREATE TRIGGER IF NOT EXISTS " +
            DEC_RECORDS_AFTER_RECORD_DEL_TRIGGER_NAME +
            " AFTER DELETE ON " + TABLE_RECORDS + " BEGIN UPDATE " + TABLE_CATEGORIES + " SET " +
            COLUMN_RECORDS_QUANTITY + " = " + COLUMN_RECORDS_QUANTITY + " - 1 WHERE " + COLUMN_CATEGORY_ID + " = " +
            "old." + COLUMN_CATEGORY_ID + "; END;";

    public static final String CREATE_TRIGGER_UPD_DATE_AFTER_RECORD_INS = "CREATE TRIGGER IF NOT EXISTS " +
            UPD_LAST_RECORD_DATE_AFTER_RECORD_INS_TRIGGER_NAME +
            " AFTER INSERT ON " + TABLE_RECORDS + " BEGIN UPDATE " + TABLE_CATEGORIES + " SET " +
            COLUMN_LAST_RECORD_DATE + " = " + "NEW." + COLUMN_RECORD_DATE +
            " WHERE " + COLUMN_CATEGORY_ID + " = " + "NEW." + COLUMN_CATEGORY_ID + "; END;";

    public static final String CREATE_TRIGGER_UPD_DATE_AFTER_RECORD_DEL = "CREATE TRIGGER IF NOT EXISTS " +
            UPD_LAST_RECORD_DATE_AFTER_RECORD_DEL_TRIGGER_NAME +
            " AFTER DELETE ON " + TABLE_RECORDS +
            " WHEN OLD." + COLUMN_RECORD_DATE + " = (SELECT " + COLUMN_LAST_RECORD_DATE + " FROM " + TABLE_CATEGORIES +
            " WHERE " + COLUMN_CATEGORY_ID + " = OLD." + COLUMN_CATEGORY_ID + ") " +
            "BEGIN UPDATE " + TABLE_CATEGORIES + " SET " + COLUMN_LAST_RECORD_DATE + " = (SELECT " + COLUMN_RECORD_DATE +
            " FROM " + TABLE_RECORDS + " WHERE " + COLUMN_CATEGORY_ID + " = OLD." + COLUMN_CATEGORY_ID +
            " ORDER BY " + COLUMN_RECORD_DATE + " DESC LIMIT 1)" +
            " WHERE " + COLUMN_CATEGORY_ID + " = OLD." + COLUMN_CATEGORY_ID + "; END;";

    public static final String CREATE_TRIGGER_UPD_DATE_AFTER_RECORD_UPD = "CREATE TRIGGER IF NOT EXISTS " +
            UPD_LAST_RECORD_DATE_AFTER_RECORD_UPD_TRIGGER_NAME +
            " AFTER UPDATE ON " + TABLE_RECORDS +
            " BEGIN UPDATE " + TABLE_CATEGORIES + " SET " + COLUMN_LAST_RECORD_DATE + " = (SELECT " + COLUMN_RECORD_DATE +
            " FROM " + TABLE_RECORDS + " WHERE " + COLUMN_CATEGORY_ID + " = NEW." + COLUMN_CATEGORY_ID +
            " ORDER BY " + COLUMN_RECORD_DATE + " DESC LIMIT 1)" +
            " WHERE " + COLUMN_CATEGORY_ID + " = NEW." + COLUMN_CATEGORY_ID + "; END;";

}
