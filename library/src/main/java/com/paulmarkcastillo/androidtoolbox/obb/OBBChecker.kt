package com.paulmarkcastillo.androidtoolbox.obb

import android.content.Context
import com.paulmarkcastillo.androidlogger.PMCLogger
import java.io.File
import java.io.FileFilter
import java.util.regex.Pattern

class OBBChecker {

    companion object {
        private const val TAG = "OBBChecker"
    }

    fun hasOBB(context: Context, versionCode: Int, packageName: String): Boolean {
        val obbDir = context.obbDir
        val file = "main.$versionCode.$packageName.obb"
        PMCLogger.d(TAG, "Checking OBB Directory: " + obbDir.absolutePath)
        PMCLogger.d(TAG, "Checking for File (like): $file")
        if (obbDir.exists()) {
            if (obbDir.isDirectory) {
                val files = obbDir.listFiles(OBBFilter(packageName))
                if (files != null && files.isNotEmpty()) {
                    return true
                } else {
                    PMCLogger.e(TAG, "OBB file not found.")
                }
            } else {
                PMCLogger.e(TAG, "OBB path isn't a directory.")
            }
        } else {
            PMCLogger.e(TAG, "OBB directory doesn't exists.")
        }
        return false
    }

    class OBBFilter(packageName: String) : FileFilter {
        private val pattern: Pattern

        init {
            val escapedPackage = packageName.replace(".", "\\.")
            val regex = "main\\.\\d+\\.$escapedPackage\\.obb"
            pattern = Pattern.compile(regex)
            PMCLogger.v(TAG, "Regular Expression: $regex")
        }

        override fun accept(pathname: File): Boolean {
            val filepath = pathname.absolutePath
            PMCLogger.v(TAG, "Verifying File: $filepath")

            return if (pattern.matcher(filepath).find()) {
                PMCLogger.i(TAG, "OBB File Found: $filepath")
                true
            } else {
                false
            }
        }
    }
}