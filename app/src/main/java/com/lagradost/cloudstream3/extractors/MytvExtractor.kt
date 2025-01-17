package com.lagradost.cloudstream3.extractors

import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.app


open class MytvExtractor : ExtractorApi() {
    override val name: String = "MyVi_tv"
    override val mainUrl: String = "https://www.myvi.tv/"
    private val srcRegex = Regex("""PlayerLoader\.CreatePlayer\(\"v\=(.*)\\u0026tp""")  // would be possible to use the parse and find src attribute
    override val requiresReferer = false


    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink>? {
        val cleaned_url = url
        val html =app.get(cleaned_url)
        with(html) {  // raised error ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED (3003) is due to the response: "error_nofile"
            srcRegex.find(this.text)?.groupValues?.get(1)?.let { link ->
                var lien = link
              lien= lien.replace("%2f","/").replace("%3a",":").replace("%3f","?").replace("%3d","=").replace("%26","&")

                return listOf(
                    ExtractorLink(
                        name,
                        name ,
                        lien,
                        cleaned_url, // voir si site demande le referer à mettre ici
                        Qualities.Unknown.value,
                    )
                )
            }
        }

        return null

    }
}
