package com.clue2crew.app.data.remote

import com.clue2crew.app.data.remote.dto.FamilyDto
import com.clue2crew.app.data.remote.dto.FamilyMemberDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {
    @POST("api/families")
    suspend fun createFamily(@Body family: FamilyDto): Response<Unit>

    @PUT("api/families/{id}")
    suspend fun updateFamily(@Path("id") id: String, @Body family: FamilyDto): Response<Unit>

    @POST("api/members")
    suspend fun addMember(@Body member: FamilyMemberDto): Response<Unit>

    @PUT("api/members/{id}")
    suspend fun updateMember(@Path("id") id: String, @Body member: FamilyMemberDto): Response<Unit>
}
