package ua.com.supersonic.android.notebook.db;

public class DBConstants {
    public static final String DB_NAME = "notebook.db";
    public static final int DB_VERSION = 4;

    public static final String TABLE_CATEGORIES = "categories";
    public static final String COLUMN_CATEGORY_ID = "category_id";
    public static final String COLUMN_NAME = "category_name";
    public static final String CREATE_CATEGORY_TABLE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_CATEGORIES + " (" +
                    COLUMN_CATEGORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_NAME + " TEXT)";

    public static final String DELETE_ENTRIES = "DROP TABLE IF EXISTS " + TABLE_CATEGORIES;
    public static final String DELETE_ROW_SELECTION = COLUMN_NAME + " LIKE ?";
}
