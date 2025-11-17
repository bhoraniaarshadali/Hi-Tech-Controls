package com.example.hi_tech_controls.model;

public class DetailsModel {

    private int progress;
    private String uName;
    private int uId;

    // ----------------------------------------------------
    // Constructors
    // ----------------------------------------------------
    public DetailsModel() {
        // Required empty constructor for Firestore & parsing
    }

    public DetailsModel(int uId, String uName, int progress) {
        this.uId = uId;
        this.uName = uName;
        this.progress = progress;
    }

    // ----------------------------------------------------
    // Getters / Setters
    // ----------------------------------------------------
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
        return uId;
    }

    public void setUId(int uId) {
        this.uId = uId;
    }

    // ----------------------------------------------------
    // Debug helper
    // ----------------------------------------------------
    @Override
    public String toString() {
        return "DetailsModel{" +
                "uId=" + uId +
                ", uName='" + uName + '\'' +
                ", progress=" + progress +
                '}';
    }
}
