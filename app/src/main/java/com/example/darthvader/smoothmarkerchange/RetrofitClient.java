package com.example.darthvader.smoothmarkerchange;

import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;


public class RetrofitClient {
    private static Retrofit retrofit=null;

    public static Retrofit getClient(String BASE_URL){
        if(retrofit==null){
            retrofit=new Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(ScalarsConverterFactory.create()).build();
        }
        return retrofit;
    }
}
