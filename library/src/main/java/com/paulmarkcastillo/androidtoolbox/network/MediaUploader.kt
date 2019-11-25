package com.paulmarkcastillo.androidtoolbox.network

import android.content.Context
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Region
import com.amazonaws.services.s3.AmazonS3Client
import com.paulmarkcastillo.androidlogger.PMCLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

class MediaUploader(
    private val context: Context? = null,
    private val preSignApiUrl: String = "",
    var accessToken: String = "",
    private val s3AccessKey: String = "",
    private val s3SecretKey: String = "",
    private val region: String = "",
    private val bucket: String = ""
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(0, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.MINUTES)
        .build()

    companion object {
        private const val OBJECT_URL = "https://%s.s3.%s.amazonaws.com/%s"
        private const val PUT = "PUT"
        private const val POST = "POST"
        private const val ACCEPT = "Accept"
        private const val ACCEPT_HEADER = "application/json"
        private const val AUTHORIZATION = "Authorization"
        private const val TOKEN_FORMAT = "Bearer %s"
        private const val DESTINATION_URL = "destinationUrl"
        private const val SIGNED_URL = "signedUrl"
        private const val LOG = "MediaUploader"
    }

    fun upload(file: File, listener: MediaUploadListener) {
        val basicAWSCredentials = BasicAWSCredentials(s3AccessKey, s3SecretKey)
        val s3Client = AmazonS3Client(basicAWSCredentials, Region.getRegion(region))
        val s3FileKey = UUID.randomUUID().toString()

        val transferUtility = TransferUtility.builder()
            .context(context)
            .awsConfiguration(AWSMobileClient.getInstance().configuration)
            .s3Client(s3Client)
            .build()

        val uploadObserver = transferUtility.upload(s3FileKey, file)

        uploadObserver?.setTransferListener(object : TransferListener {
            override fun onError(id: Int, exception: Exception?) {
                exception?.let { transferException ->
                    transferException.printStackTrace()
                    listener.onError(transferException)
                    PMCLogger.e(LOG, "onError: ${transferException.localizedMessage}")
                }
            }

            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                val progress = (((bytesCurrent.toDouble() / bytesTotal) * 100.0).toInt())
                listener.onProgressChanged(progress)
            }

            override fun onStateChanged(id: Int, state: TransferState?) {
                when (state) {
                    TransferState.COMPLETED -> {
                        listener.onTransferCompleted(OBJECT_URL.format(bucket, region, s3FileKey))
                    }
                    TransferState.CANCELED,
                    TransferState.FAILED -> listener.onError(Exception("Transfer ${state.name}"))
                    else -> {
                    }
                }
                PMCLogger.d(LOG, "onStateChanged: State - ${state?.name}")
            }
        })
    }

    fun uploadPreSigned(file: File, listener: MediaUploadListener) {
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    /**
                     * We first get our pre signed credentials and check if signedUrl is empty,
                     * if empty, then we no longer proceed to make the request for uploading
                     * our file.
                     */
                    val preSignedCredentials = getPreSignedCredentials()
                    if (preSignedCredentials.signedUrl.isEmpty()) {
                        listener.onError(Exception("No pre signed url provided."))
                        PMCLogger.e(LOG, "No pre signed credentials fetched")
                        cancel()
                    }

                    /**
                     * Here we make our request for uploading the file given
                     * a valid pre signed url, this is independent of any app
                     * server, we don't need to add headers.
                     */
                    val fileRequestBody = file.asRequestBody()
                    val countingRequestBody = CountingRequestBody(
                        fileRequestBody,
                        object : CountingRequestBody.CountingRequestBodyProgressListener {
                            override fun onProgressChanged(
                                bytesWritten: Long,
                                contentLength: Long
                            ) {
                                val percentage = 100f * bytesWritten / contentLength
                                listener.onProgressChanged(percentage.toInt())
                            }
                        })
                    val requestBuilder = Request.Builder()
                        .url(preSignedCredentials.signedUrl)
                        .method(PUT, countingRequestBody)

                    val response = client.newCall(requestBuilder.build()).execute()
                    if (response.isSuccessful) {
                        listener.onTransferCompleted(preSignedCredentials.objectUrl)
                        PMCLogger.d(LOG, "Upload with preSignedUrl - Successful")
                    } else {
                        response.run {
                            listener.onError(Exception(message))
                            PMCLogger.e(LOG, "Status Code: $code Message: $message")
                            cancel()
                        }
                    }
                } catch (exception: Exception) {
                    exception.printStackTrace()
                    listener.onError(exception)
                    PMCLogger.e(
                        LOG,
                        "Upload with preSignedUrl - Exception: ${exception.localizedMessage}"
                    )
                    cancel()
                }
            }
        }
    }

    @Throws(Exception::class)
    private fun getPreSignedCredentials(): PreSignedCredentials {
        val requestBuilder = preSignApiUrl.run {
            Request.Builder()
                .url(this)
                .addHeader(ACCEPT, ACCEPT_HEADER)
                .addHeader(AUTHORIZATION, TOKEN_FORMAT.format(accessToken))
                .method(POST, "".toRequestBody())
        }

        val response = client.newCall(requestBuilder.build()).execute()
        val jsonResponse = response.body?.string()?.let { JSONObject(it) }
        return PreSignedCredentials(
            jsonResponse!!.getString(DESTINATION_URL),
            jsonResponse.getString(SIGNED_URL)
        )
    }
}

interface MediaUploadListener {
    fun onProgressChanged(progress: Int)
    fun onTransferCompleted(objectUrl: String)
    fun onError(exception: Exception)
}

data class PreSignedCredentials(val objectUrl: String = "", val signedUrl: String = "")