package com.example.climapp.Utilites

import com.example.climapp.Models.WeatherModel
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiInterface {
    @GET("weather")
    fun getCurrentWeatherData(
        @Query("lat") lat:String,
        @Query("lon") lon:String,
        @Query("APPID") appid:String
    ):Call<WeatherModel>

    @GET("weather")
    fun getCityWeatherData(
        @Query("q") lon:String,
        @Query("APPID") appid:String
    ):Call<WeatherModel>

}