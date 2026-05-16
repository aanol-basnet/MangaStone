package com.personal.mangarock.data.scraper

import com.google.gson.Gson
import com.personal.mangarock.data.source.MangaSource
import com.personal.mangarock.data.source.SourcePage
import com.personal.mangarock.domain.models.Chapter
import com.personal.mangarock.domain.models.Manga
import com.personal.mangarock.domain.models.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangaHereScraper @Inject constructor(
    private val okHttpClient: OkHttpClient
) : MangaSource {

    override val id = "mangahere"
    override val name = "MangaHere"

    private companion object {
        const val BASE = "https://www.mangahere.cc"
        const val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

    // ── HTTP ──────────────────────────────────────────────────────────────────

    private suspend fun fetch(url: String, referer: String = "$BASE/"): String =
        withContext(Dispatchers.IO) {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", UA)
                .header("Referer", referer)
                .header("Cookie", "isAdult=1")
                .build()
            okHttpClient.newCall(req).execute().use { it.body?.string() ?: "" }
        }

    private fun String.toAbsoluteUrl(): String = when {
        startsWith("http") -> this
        startsWith("//")   -> "https:$this"
        else               -> this
    }

    // ── HTML helpers ──────────────────────────────────────────────────────────

    private fun parseMangaItem(el: Element): Manga? {
        val a = el.selectFirst("a[href*=/manga/]") ?: return null
        val slug = a.attr("href").removePrefix("/manga/").trimEnd('/')
        if (slug.isEmpty()) return null
        val img = el.selectFirst("img")
        val cover = (img?.attr("src") ?: img?.attr("data-src") ?: "").toAbsoluteUrl()
        return Manga(
            id = slug,
            title = a.attr("title").ifEmpty { a.text().ifEmpty { slug } },
            titleRomanji = null,
            coverUrl = cover,
            description = "",
            status = "",
            tags = emptyList(),
            author = "",
            year = null,
            lastChapter = null,
            latestChapterId = null,
            createdAt = "",
            updatedAt = ""
        )
    }

    private fun Document.totalPages(): Int {
        var max = 1
        select(".pager-list-left a[href]").forEach { el ->
            Regex("""/(\d+)\.htm""").find(el.attr("href"))
                ?.groupValues?.get(1)?.toIntOrNull()
                ?.let { if (it > max) max = it }
        }
        return max
    }

    private suspend fun fetchList(url: String, page: Int = 1): SourcePage {
        val doc = Jsoup.parse(fetch(url))
        val items = doc.select("ul[class*=manga-list] li").mapNotNull { parseMangaItem(it) }
        return SourcePage(items, page < doc.totalPages() || items.size >= 24)
    }

    // ── MangaSource ───────────────────────────────────────────────────────────

    override suspend fun getPopularManga(page: Int): SourcePage {
        val p = if (page > 1) "${page}.htm" else ""
        return fetchList("$BASE/directory/$p", page)
    }

    override suspend fun searchManga(query: String, page: Int): SourcePage {
        val q = query.trim().replace(" ", "+")
        val doc = Jsoup.parse(fetch("$BASE/search?title=$q&page=$page", "$BASE/search"))
        val items = doc.select("ul[class*=manga-list] li").mapNotNull { parseMangaItem(it) }
        return SourcePage(items, items.size >= 24)
    }

    override suspend fun getMangaDetail(mangaId: String): Manga {
        val doc = Jsoup.parse(fetch("$BASE/manga/$mangaId/"))
        val name   = doc.selectFirst(".detail-info-right-title-font")?.text()?.trim() ?: mangaId
        val cover  = (doc.selectFirst("img.detail-info-cover-img")?.attr("src") ?: "").toAbsoluteUrl()
        val author = doc.selectFirst(".detail-info-right-say a")?.text()?.trim() ?: ""
        val status = doc.selectFirst(".detail-info-right-title-tip")?.text()?.trim()?.lowercase() ?: ""
        val desc   = doc.selectFirst("#fullcontent")?.text()?.trim() ?: ""
        val tags   = doc.select(".detail-info-right-tag-list a")
            .map { it.text().trim() }.filter { it.isNotEmpty() }
            .map { Tag(it.lowercase().replace(" ", "-"), it, "genre") }
        val updated = doc.selectFirst(".detail-main-list li a .title2")?.text()?.trim() ?: ""
        return Manga(
            id = mangaId, title = name, titleRomanji = null,
            coverUrl = cover, description = desc, status = status,
            tags = tags, author = author, year = null,
            lastChapter = null, latestChapterId = null,
            createdAt = updated, updatedAt = updated
        )
    }

    override suspend fun getChapterList(mangaId: String): List<Chapter> {
        val doc = Jsoup.parse(fetch("$BASE/manga/$mangaId/"))
        return doc.select(".detail-main-list li a").mapNotNull { el ->
            val href  = el.attr("href")
            val parts = href.split("/").filter { it.isNotEmpty() }
            // parts: [manga, slug, ...chapSegments, 1.html]
            val chapPath = parts.drop(2).dropLast(1).joinToString("/")
            if (chapPath.isEmpty()) return@mapNotNull null
            val chName  = el.selectFirst(".title3")?.text()?.trim() ?: chapPath
            val chDate  = el.selectFirst(".title2")?.text()?.trim() ?: ""
            val chapNum = chapPath.substringAfterLast("/c")
                .takeIf { it.isNotBlank() && it.toFloatOrNull() != null }
                ?: chName.substringAfter("Ch.").substringBefore(" ").trim().ifBlank { null }
            Chapter(
                id = "$mangaId::$chapPath",
                mangaId = mangaId,
                title = chName.ifBlank { null },
                chapterNumber = chapNum,
                volume = null,
                pages = 0,
                publishAt = chDate,
                scanlationGroup = "MangaHere",
                externalUrl = null
            )
        }
    }

    override suspend fun getPageList(chapterId: String): List<String> {
        val (mangaId, chapPath) = chapterId.split("::", limit = 2)
        return fetchChapterImages(mangaId, chapPath)
    }

    // ── Extra (not in MangaSource interface) ─────────────────────────────────

    suspend fun getMangaByGenre(genre: String, page: Int): SourcePage {
        val p = if (page > 1) "${page}.htm" else ""
        return fetchList("$BASE/$genre/$p", page)
    }

    suspend fun getNewestManga(page: Int): SourcePage {
        val p = if (page > 1) "${page}.htm" else ""
        return fetchList("$BASE/directory/${p}?news", page)
    }

    suspend fun getCompletedManga(page: Int): SourcePage {
        val p = if (page > 1) "${page}.htm" else ""
        return fetchList("$BASE/completed/$p", page)
    }

    // ── Chapter image decoding ────────────────────────────────────────────────

    private suspend fun fetchChapterImages(mangaSlug: String, chapPath: String): List<String> =
        withContext(Dispatchers.IO) {
            val pageUrl = "$BASE/manga/$mangaSlug/$chapPath/1.html"
            val html    = fetch(pageUrl)

            val chapId = Regex("""var\s+chapterid\s*=\s*(\d+)""").find(html)?.groupValues?.get(1)
                ?: throw Exception("chapterId not found for $mangaSlug/$chapPath")
            val imgCount = Regex("""var\s+imagecount\s*=\s*(\d+)""").find(html)?.groupValues?.get(1)?.toInt()
                ?: throw Exception("imagecount not found for $mangaSlug/$chapPath")
            val guidkey = extractGuidkey(html)

            val slots = arrayOfNulls<String>(imgCount)

            coroutineScope {
                (1..imgCount step 2).map { page ->
                    async(Dispatchers.IO) {
                        // Send guidkey as both query param and dm5_key cookie, as MangaHere expects
                        val apiUrl = "$BASE/chapterfun.ashx?cid=$chapId&page=$page&key=$guidkey"
                        val packed = fetchWithCookie(
                            url = apiUrl,
                            referer = pageUrl,
                            extraCookie = if (guidkey.isNotEmpty()) "dm5_key=$guidkey" else null
                        )
                        val urls = decodeChapterFun(packed)
                        urls.forEachIndexed { i, url ->
                            val idx = page - 1 + i
                            if (idx < imgCount) slots[idx] = url.toAbsoluteUrl()
                        }
                    }
                }.awaitAll()
            }

            val result = slots.filterNotNull()
            if (result.isEmpty()) throw Exception("No images decoded for $mangaSlug/$chapPath (imgCount=$imgCount)")
            result
        }

    /** Like [fetch] but allows appending extra cookies. */
    private suspend fun fetchWithCookie(url: String, referer: String, extraCookie: String?): String =
        withContext(Dispatchers.IO) {
            val cookie = buildString {
                append("isAdult=1")
                if (!extraCookie.isNullOrEmpty()) {
                    append("; ")
                    append(extraCookie)
                }
            }
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", UA)
                .header("Referer", referer)
                .header("Cookie", cookie)
                .header("X-Requested-With", "XMLHttpRequest")
                .build()
            okHttpClient.newCall(req).execute().use { it.body?.string() ?: "" }
        }

    /** Extract the guidkey that MangaHere embeds in an inline script. */
    private fun extractGuidkey(html: String): String {
        val scriptRx = Regex("""<script[^>]*>([\s\S]*?)</script>""", RegexOption.IGNORE_CASE)
        for (m in scriptRx.findAll(html)) {
            val content = m.groupValues[1]
            if ("dm5_key" in content || "guidkey" in content) {
                Regex("""guidkey\s*=\s*["']([^"']*)["']""").find(content)
                    ?.groupValues?.get(1)?.let { return it }
                // Also check for direct dm5_key assignment
                Regex("""dm5_key\s*=\s*["']([^"']*)["']""").find(content)
                    ?.groupValues?.get(1)?.let { return it }
            }
        }
        return ""
    }

    /**
     * Decode a chapterfun.ashx response.
     *
     * After unpacking the p,a,c,k,e,d wrapper the result is a function like:
     *   function dm5imagefun(){
     *     var pix="//zjcdn.mangahere.org/store/manga/.../compressed";
     *     var pvalue=["/img1.jpg","/img2.jpg"];
     *     for(...){ pvalue[i]=pix+pvalue[i] }
     *     return pvalue
     *   }
     *   var d; d=dm5imagefun(); ...
     *
     * We extract pix + pvalue directly instead of trying to evaluate the JS.
     */
    private fun decodeChapterFun(packed: String): List<String> {
        val unpacked = unpackJs(packed)

        // Primary path: extract pix (base URL) and pvalue (relative paths array)
        val pixMatch   = Regex("""var\s+pix\s*=\s*"([^"]+)"""").find(unpacked)
        val pvalueMatch = Regex("""var\s+pvalue\s*=\s*(\[[\s\S]*?])""").find(unpacked)

        if (pixMatch != null && pvalueMatch != null) {
            val pix = pixMatch.groupValues[1]           // "//zjcdn.mangahere.org/.../compressed"
            val relPaths = try {
                Gson().fromJson(pvalueMatch.groupValues[1], Array<String>::class.java)
                    ?.toList() ?: emptyList()
            } catch (_: Exception) { emptyList() }
            if (relPaths.isNotEmpty()) {
                return relPaths.map { "$pix$it" }       // "//zjcdn.../compressed/imgN.jpg"
            }
        }

        // Fallback: older format where d is assigned a literal array  d=[...]
        val arrayMatch = Regex("""(?:var\s+)?d\s*=\s*(\[[\s\S]*?])""").find(unpacked)
            ?: return emptyList()
        return try {
            Gson().fromJson(arrayMatch.groupValues[1], Array<String>::class.java)
                ?.toList() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Decode a standard eval(function(p,a,c,k,e,d){...}('payload',base,count,'dict'.split('|'))) packer.
     * Supports both single-quoted and double-quoted variants (MangaHere uses double quotes).
     * Digit mapping: 0-9 → 0-9, a-z → 10-35, A-Z → 36-61.
     */
    private fun unpackJs(packed: String): String {
        // Try double-quote variant first (MangaHere's chapterfun.ashx uses double quotes)
        val doubleQ = Regex(
            """\}\s*\(\s*"([\s\S]*?)",\s*(\d+),\s*\d+,\s*"([\s\S]*?)"\.split\("\|"\)""",
            setOf(RegexOption.DOT_MATCHES_ALL)
        )
        // Fallback: single-quote variant
        val singleQ = Regex(
            """\}\s*\(\s*'([\s\S]*?)',\s*(\d+),\s*\d+,\s*'([\s\S]*?)'\.split\('\|'\)""",
            setOf(RegexOption.DOT_MATCHES_ALL)
        )

        val m = doubleQ.find(packed) ?: singleQ.find(packed) ?: return packed

        val payload = m.groupValues[1]
            .replace("\\'", "'")
            .replace("\\\"", "\"")
        val base = m.groupValues[2].toInt()
        val dict = m.groupValues[3].split("|")

        return Regex("""\b\w+\b""").replace(payload) { r ->
            val idx = fromBase(r.value, base)
            if (idx >= 0 && idx < dict.size && dict[idx].isNotEmpty()) dict[idx] else r.value
        }
    }

    private fun fromBase(s: String, base: Int): Int {
        var result = 0
        for (c in s) {
            val digit = when (c) {
                in '0'..'9' -> c - '0'
                in 'a'..'z' -> c - 'a' + 10
                in 'A'..'Z' -> c - 'A' + 36
                else        -> return -1
            }
            if (digit >= base) return -1
            result = result * base + digit
        }
        return result
    }
}
