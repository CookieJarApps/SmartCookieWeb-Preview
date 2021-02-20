package com.cookiejarapps.android.smartcookieweb.utils

import android.app.Application
import android.os.Bundle
import android.os.Parcel
import android.util.Log
import io.reactivex.Completable
import java.io.*

class FileUtils {
    companion object {
        @JvmStatic
        fun writeBundleToStorage(app: Application, bundle: Bundle?, name: String): Completable {
            return Completable.fromAction {
                val outputFile = File(app.filesDir, name)
                var outputStream: FileOutputStream? = null
                try {
                    outputStream = FileOutputStream(outputFile)
                    val parcel = Parcel.obtain()
                    parcel.writeBundle(bundle)
                    outputStream.write(parcel.marshall())
                    outputStream.flush()
                    parcel.recycle()
                } catch (e: IOException) {
                    Log.e(
                        "FileUtils",
                        "Unable to write bundle to storage"
                    )
                } finally {
                    outputStream?.close()
                }
            }
        }

        @JvmStatic
        fun readBundleFromStorage(app: Application, name: String): Bundle? {
            val inputFile = File(app.filesDir, name)
            var inputStream: FileInputStream? = null
            try {
                inputStream = FileInputStream(inputFile)
                val parcel = Parcel.obtain()
                val data = ByteArray(inputStream.channel.size().toInt())
                inputStream.read(data, 0, data.size)
                parcel.unmarshall(data, 0, data.size)
                parcel.setDataPosition(0)
                val out = parcel.readBundle(ClassLoader.getSystemClassLoader())
                out!!.putAll(out)
                parcel.recycle()
                return out
            } catch (e: FileNotFoundException) {
                Log.e(
                    "FileUtils",
                    "Unable to read bundle from storage"
                )
            } catch (e: IOException) {
                Log.e(
                    "FileUtils",
                    "Unable to read bundle from storage",
                    e
                )
            } finally {
                inputStream?.close()
            }
            return null
        }
    }
}