package com.paulmarkcastillo.androidtoolbox.network

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.Sink
import okio.ForwardingSink
import okio.buffer
import okio.Buffer
import java.io.IOException

class CountingRequestBody(
    private val requestBody: RequestBody,
    private val listener: CountingRequestBodyProgressListener
) : RequestBody() {
    private lateinit var countingSink: CountingSink

    override fun contentType(): MediaType? {
        return requestBody.contentType()
    }

    override fun writeTo(sink: BufferedSink) {
        countingSink = CountingSink(sink, listener)
        val bufferedSink = countingSink.buffer()
        requestBody.writeTo(bufferedSink)
        bufferedSink.flush()
    }

    override fun contentLength(): Long {
        return requestBody.contentLength()
    }

    class CountingSink(
        sink: Sink,
        private val listener: CountingRequestBodyProgressListener
    ) : ForwardingSink(sink) {
        private var bytesWritten = 0L

        @Throws(IOException::class)
        override fun write(source: Buffer, byteCount: Long) {
            super.write(source, byteCount)
            bytesWritten += byteCount
            listener.onProgressChanged(bytesWritten, source.size)
        }
    }

    interface CountingRequestBodyProgressListener {
        fun onProgressChanged(bytesWritten: Long, contentLength: Long)
    }
}