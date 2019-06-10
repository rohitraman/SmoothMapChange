package com.example.darthvader.smoothmarkerchange;

public class Common {
    public static final String baseURL="https://googleapis.com";
    public static IGoogleAPI getGoogleAPI(){
        return RetrofitClient.getClient(baseURL).create(IGoogleAPI.class);
    }
}
