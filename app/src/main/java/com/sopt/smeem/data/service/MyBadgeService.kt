package com.sopt.smeem.data.service

import com.sopt.smeem.data.model.response.ApiResponse
import com.sopt.smeem.data.model.response.MyBadgesResponse
import retrofit2.Response
import retrofit2.http.GET

interface MyBadgeService {
    @GET("/api/v3/members/badges")
    suspend fun getBadges() : Response<ApiResponse<MyBadgesResponse>>
}