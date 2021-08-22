package io.arunbuilds.libraryapp.network

import javax.inject.Inject

class RemoteRepository @Inject constructor(
    private val api: LibraryApi
) {
    fun submitSessionAsFields(
        locationId: String,
        timeSpentInMins: Int,
        endTimeStamp: Long
    ) = api.submitSessionAsFields(locationId, timeSpentInMins, endTimeStamp)

    fun submitSessionAsBody(
        postBody: PostBody
    ) = api.submitSessionAsBody(postBody)
}