package ua.com.supersonic.android.notebook;

import java.util.Date;

public class NotebookCategory {

    private int id;
    private Date lastRecordDate;
    private int recordQuantity;
    private String title;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getLastRecordDate() {
        return lastRecordDate;
    }

    public void setLastRecordDate(Date lastRecordDate) {
        this.lastRecordDate = lastRecordDate;
    }

    public String getName() {
        return title;
    }

    public void setName(String title) {
        this.title = title;
    }

    public int getRecordQuantity() {
        return recordQuantity;
    }

    public void setRecordQuantity(int recordQuantity) {
        this.recordQuantity = recordQuantity;
    }
}
