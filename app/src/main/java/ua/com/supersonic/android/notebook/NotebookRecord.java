package ua.com.supersonic.android.notebook;

import java.util.Date;

public class NotebookRecord {
    private int id;
    private int categoryId;
    private Date date;
    private double amount;
    private String description;
    private Date prevDate;

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int category) {
        this.categoryId = category;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getPrevDate() {
        return prevDate;
    }

    public void setPrevDate(Date prevDate) {
        this.prevDate = prevDate;
    }
}
