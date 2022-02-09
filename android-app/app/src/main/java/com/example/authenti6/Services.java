package com.example.authenti6;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Services {
    private final ArrayList<Service> servicesList = new ArrayList<>();

    // For testing purposes only
    int lastServiceId = 0;

    public ArrayList<Service> getServicesList() {
        return servicesList;
    }

    public int getNumServices() {
        return servicesList.size();
    }

    public void load(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);
            JSONArray array = json.getJSONArray("services");

            servicesList.clear();
            for (int i = 0; i < array.length(); i++) {
                JSONObject element = array.getJSONObject(i);
                Service s = new Service(element.getString("name"), element.getString("url"));
                servicesList.add(s);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public Services() {}
}
