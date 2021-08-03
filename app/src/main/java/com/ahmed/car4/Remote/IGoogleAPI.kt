package com.ahmed.car4.Remote

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.annotations.NonNull
import io.reactivex.rxjava3.disposables.Disposable
import retrofit2.http.GET
import retrofit2.http.Query

interface IGoogleAPI : @NonNull Disposable {
    @GET("maps/api/direction/json")
    fun getDirections(
        //Billing
        @Query("mode") mode: String?,
        @Query("transit_route_preference") transit_routing: String?,
        @Query("origin") from: String,
        @Query("destination") to: String,
        @Query("key") key: String?,

        ):Observable<String>?

}