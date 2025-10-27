package com.example.hi_tech_controls.adapter;

public class ClientModel {
    public String name, clientId, gpDate, makeName;

    public ClientModel() {}

    public ClientModel(String name, String clientId, String gpDate, String makeName) {
        this.name = name;
        this.clientId = clientId;
        this.gpDate = gpDate;
        this.makeName = makeName;
    }
}