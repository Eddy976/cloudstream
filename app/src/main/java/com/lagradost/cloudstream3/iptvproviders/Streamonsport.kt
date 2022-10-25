package com.lagradost.cloudstream3.iptvproviders

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import org.jsoup.nodes.Element

import me.xdrop.fuzzywuzzy.FuzzySearch
import java.util.*
import kotlin.collections.ArrayList

class StreamonsportProvider : MainAPI() {
    override var mainUrl = "https://www.streamonsport22.buzz"
    override var name = "Streamonsport22"
    override val hasQuickSearch = false // recherche rapide (optionel, pas vraimet utile)
    override val hasMainPage = true // page d'accueil (optionel mais encoragé)
    override var lang = "fr" // fournisseur est en francais
    override val supportedTypes =
        setOf(TvType.Live) // live

    /**
    Cherche le site pour un titre spécifique

    La recherche retourne une SearchResponse, qui peut être des classes suivants: AnimeSearchResponse, MovieSearchResponse, TorrentSearchResponse, TvSeriesSearchResponse
    Chaque classes nécessite des données différentes, mais a en commun le nom, le poster et l'url
     **/
    override suspend fun search(query: String): List<SearchResponse> {
        val link =
            "$mainUrl/1-football-streaming-regarder-le-foot-en-streaming.html" // search'
        val document =
            app.post(link).document // app.get() permet de télécharger la page html avec une requete HTTP (get)
        val results = document.select("div.container-fluid > div.row > div.row")

        val allresultshome =
            results.mapNotNull { article ->  // avec mapnotnull si un élément est null, il sera automatiquement enlevé de la liste
                article.toSearchResponse()
            }
        return allresultshome

    }

    /**
     * charge la page d'informations, il ya toutes les donées, les épisodes, le résumé etc ...
     * Il faut retourner soit: AnimeLoadResponse, MovieLoadResponse, TorrentLoadResponse, TvSeriesLoadResponse.
     */
    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document //

        val infos = document.select("div.container-fluid > div.row")

        val posterUrl =
            infos.select("div.logos > img")
                .attr("src")
        var title = infos.select("div.details > span").text() //
        val description = document.selectFirst("div.container-fluid > div.lematch")?.text()
        val takeDate = infos.select("div.details > p.date > time").attr("data-time")
            .replace("T", " à ")
            .split("+")[0]
        var year = if (!takeDate.isNullOrBlank()
        ) {
            "Le $takeDate :\n"
        } else {
            ""
        }
        return LiveStreamLoadResponse(
            name = title,
            url = url,
            apiName = this.name,
            dataUrl = url,
            posterUrl = posterUrl,
            year = null,
            plot = year + description
        )

    }


    /** récupere les liens .mp4 ou m3u8 directement à partir du paramètre data généré avec la fonction load()**/
    override suspend fun loadLinks(
        data: String, // fournit par load()
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ): Boolean {
        val srcRegex = Regex("""var src=\"(.*)\";if""")
        val getMainUrlRegex = Regex("""(http[s]{0,1}:\/\/.[^\/\s]*)""")
        // val referer = "https://leet365.cc"
        val linktoDocforEmbed = app.get(data).document.select("iframe").attr("src")
        val referer =
            getMainUrlRegex.find(linktoDocforEmbed)?.groupValues?.get(1).toString()  //"https://leet365.cc"
        val linkEmbed = app.get(linktoDocforEmbed).document.select("iframe").attr("src")
        val refererEmbed =
            getMainUrlRegex.find(linkEmbed)?.groupValues?.get(1).toString()  //"https://voraciousglove.net/"
        val headers = mapOf(
            "Accept" to "*/*",
            "Referer" to refererEmbed,
            "Accept-Language" to "en-US,en;q=0.5",
        )
        with(
            app.get(
                linkEmbed,
                referer = referer
            )
        ) {
            getAndUnpack(this.text).let { unpackedText ->
                srcRegex.find(unpackedText)?.groupValues?.get(1).let { url ->
                    callback.invoke(
                        ExtractorLink(
                            name,
                            name,
                            url.toString(),
                            "",
                            Qualities.Unknown.value,
                            isM3u8 = true,
                            headers = headers
                        )
                    )
                }
            }
        }
        return true


    }

    private fun Element.toSearchResponse(): SearchResponse {
        var posterUrl = select("div.logos > img").attr("src")
        val title = select(" div.details > a > span").text()
        val link = select("div.details > a").attr("href")
        val time = select("div.details > p.date > time").text()

        return LiveSearchResponse(
            title + "\n $time",
            link,
            title,
            TvType.Live,
            posterUrl,
        )
    }


    override val mainPage = mainPageOf(
        Pair("$mainUrl/1-football-streaming-regarder-le-foot-en-streaming.html", "Football"),
        Pair(
            "$mainUrl/31-sport-tv-fr-bein-canal-eurosport-prime-video-en-streaming.html",
            "Chaines de sport"
        ),

        )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val categoryName = request.name
        var url = request.data
        var cssSelector = ""
        if (page <= 1) {
            cssSelector = "div.container-fluid > div.row > div.row"
        }
        val document = app.get(url).document

        val home = when (!categoryName.isNullOrBlank()) {
            categoryName.contains("Football") -> document.select(cssSelector)
                .mapNotNull { article -> article.toSearchResponse() }

            else -> document.select(cssSelector)
                .map { article -> article.toSearchResponse() }
        }

        return newHomePageResponse(categoryName, home)
    }


}