package com.daunpos.app

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Builds navigation routes with opaque identifiers safely encoded as one URI path segment.
 * Invoice numbers currently start with '#', which URI parsers interpret as a fragment delimiter.
 */
object AppRoutes {
    fun success(transactionId: String): String {
        require(transactionId.isNotBlank()) { "ID transaksi wajib diisi." }
        val encodedId = URLEncoder
            .encode(transactionId, StandardCharsets.UTF_8.name())
            .replace("+", "%20")
        return "success/$encodedId"
    }

    fun detail(transactionId: String): String {
        require(transactionId.isNotBlank()) { "ID transaksi wajib diisi." }
        val encodedId = URLEncoder
            .encode(transactionId, StandardCharsets.UTF_8.name())
            .replace("+", "%20")
        return "detail/$encodedId"
    }
}
