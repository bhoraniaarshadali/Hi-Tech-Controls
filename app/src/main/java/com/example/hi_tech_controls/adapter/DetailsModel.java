package com.example.hi_tech_controls.adapter;

public class DetailsModel {

    int progress;
    String uName;
    int UId;

    public DetailsModel() {
    }

    public DetailsModel(int UId, String uName, int progress) {
        this.UId = UId;
        this.uName = uName;
        this.progress = progress;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getuName() {
        return uName;
    }

    public void setuName(String uName) {
        this.uName = uName;
    }

    public int getUId() {
        return UId;
    }

    public void setUId(int UId) {
        this.UId = UId;
    }

    @Override
    public String toString() {
        return "DetailsModel{" +
                "UId=" + UId +
                ", uName='" + uName + '\'' +
                ", progress=" + progress +
                '}';
    }
}
