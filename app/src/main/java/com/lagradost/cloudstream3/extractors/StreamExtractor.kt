package com.lagradost.cloudstream3.extractors

import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.app

import com.lagradost.cloudstream3.network.WebViewResolver

import com.lagradost.nicehttp.requestCreator

open class StreamExtractor : ExtractorApi() {
    override val name: String = "Myvi_ru"
    override val mainUrl: String = "https://myvi.ru/"

    private val srcRegex =
        Regex("""reatePlayer\(\"v\=(.*)\\u0026tp""")  // would be possible to use the parse and find src attribute
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink>? {
        var header = mapOf(
            "Accept-Encoding" to "gzip, deflate, br",
            "Cookie" to "UniversalUserID=cae65adefe2f4014bdae2dcf6001522c; vp=0.33"
        )
        val html = app.get(
            url
        )
        with(html) {  // raised error ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED (3003) is due to the response: "error_nofile"
            srcRegex.find(this.text)?.groupValues?.get(1)?.let { link ->
                var lien = link.replace("%2f", "/").replace("%3a", ":").replace("%3f", "?")
                    .replace("%3d", "=").replace("%26", "&")

                val redirectLink = app.get(lien, headers = header, allowRedirects = false).headers.get("location").toString()
                header = mapOf(
                    "Accept-Encoding" to "gzip, deflate, br",
                    "Referer" to redirectLink,
                    "Cookie" to "UniversalUserID=cae65adefe2f4014bdae2dcf6001522c; vp=0.33"
                )
                return listOf(
                    ExtractorLink(
                        name,
                        name,
                        redirectLink,
                        redirectLink, // voir si site demande le referer à mettre ici
                        Qualities.Unknown.value,
                        false,
                        headers = header

                    )
                )

            }
        }

        return null

    }
}

