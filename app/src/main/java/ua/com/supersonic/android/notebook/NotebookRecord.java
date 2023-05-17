package ua.com.supersonic.android.notebook;

import java.util.Date;

public class NotebookRecord {
    private int id;
    private int categoryId;
    private Date date;

    @Override
    public String toString() {
        return "NotebookRecord{" +
                "id=" + id +
                ", categoryId=" + categoryId +
                ", date=" + date +
                ", amount=" + amount +
                ", description='" + description + '\'' +
                '}';
    }

    private double amount;
    private String description;

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public int getCategoryId() {
        return categoryId;
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

    public void setCategoryId(int category) {
        this.categoryId = category;
    }
}
