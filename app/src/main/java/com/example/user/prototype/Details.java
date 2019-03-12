package com.example.user.prototype;

import java.util.Date;

public class Details {
    private String UID;
    private Float total;
    private String descriptions;
    private Date date;

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Details(){
        // Default constructor required for calls to DataSnapshot.getValue(Details.class)
    }

    public Details(String UID, Float total, String descriptions, Date date) {
        this.UID = UID;
        this.total = total;
        this.descriptions = descriptions;
        this.date = date;
    }

    public String getUID() {
        return UID;
    }

    public void setUID(String UID) {
        this.UID = UID;
    }

    public Float getTotal() {
        return total;
    }

    public void setTotal(Float total) {
        this.total = total;
    }

    public String getDescriptions() {
        return descriptions;
    }

    public void setDescriptions(String descriptions) {
        this.descriptions = descriptions;
    }
}
