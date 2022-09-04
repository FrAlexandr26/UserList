package com.hugo.userlist

import retrofit2.Call
import retrofit2.http.*


public interface UsersApi {

    @GET("users")
    fun getUsers() :   Call<ArrayList<User>>


}