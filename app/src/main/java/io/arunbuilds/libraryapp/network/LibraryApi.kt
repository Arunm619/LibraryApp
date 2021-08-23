package io.arunbuilds.libraryapp.network

import com.google.gson.annotations.SerializedName
import io.reactivex.rxjava3.core.Single
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/**
 * Unsure of the type that server is expecting for submit-session POST API. It returns 200 for everything :)
 * */
interface LibraryApi {
    @FormUrlEncoded
    @POST("submit-session")
    fun submitSessionAsFields(
        @Field("location_id") locationId: String,
        @Field("time_spent") timeSpentInMins: Int,
        @Field("end_time") endTimeStamp: Long
    ): Single<Response<APIResult>>

    @POST("submit-session")
    fun submitSessionAsBody(
        @Body postBody: PostBody
    ): Single<Response<APIResult>>
}

/**
 * Response given by the submit-session post API
 * */
data class APIResult(val success: Boolean)

/**
 * Body part for the submit-session Post API
 * */
data class PostBody(
    @SerializedName("location_id") val locationId: String,
    @SerializedName("time_spent") val timeSpentInMins: Int,
    @SerializedName("end_time") val endTimeStamp: Long
)
