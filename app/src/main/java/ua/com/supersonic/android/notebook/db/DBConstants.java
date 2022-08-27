package ua.com.supersonic.android.notebook.db;

public class DBConstants {
    public static final String DB_NAME = "notebook.db";
    public static final int DB_VERSION = 2;
    public static final String TABLE_CATEGORIES_NAME = "categories";
    public static final String COLUMN_NAME = "name";
    public static final String _ID = "_id";
    public static final String CREATE_ENTRIES =
            "CREATE TABLE IF NOT EXISTS " + TABLE_CATEGORIES_NAME + " (" +
                    _ID + " INTEGER PRIMARY KEY," +
                    COLUMN_NAME + " TEXT)";

    public static final String DELETE_ENTRIES = "DROP TABLE IF EXISTS " + TABLE_CATEGORIES_NAME;
    public static final String DELETE_ROW_SELECTION = COLUMN_NAME + " LIKE ?";
}
