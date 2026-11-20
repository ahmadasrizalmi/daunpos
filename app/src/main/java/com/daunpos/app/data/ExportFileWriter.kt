package com.daunpos.app.data

import java.io.InputStream
import java.io.OutputStream

object ExportFileWriter {
    fun copy(source: InputStream, destination: OutputStream) {
        source.copyTo(destination)
        destination.flush()
    }
}
