const httpReq = require('request-promise')
const cheerio = require('cheerio')

const BASE = 'https://www.mangahere.cc'
const HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    'Referer': BASE + '/',
    'Cookie': 'isAdult=1'
}

const httpGet = (url, referer) => httpReq({
    uri: url,
    headers: referer ? { ...HEADERS, Referer: referer } : HEADERS,
    gzip: true
})

// Run the obfuscated inline script that sets guidkey into #dm5_key
function extractGuidkey(html) {
    const scripts = [...html.matchAll(/<script[^>]*>([\s\S]*?)<\/script>/gi)]
    for (const match of scripts) {
        const content = match[1]
        if (content.includes('dm5_key') && content.includes('guidkey')) {
            let guidkey = ''
            const $ = () => ({ val: (v) => { if (v !== undefined) guidkey = v } })
            try { eval(content) } catch (e) {}
            if (guidkey) return guidkey
        }
    }
    return ''
}

// Decode the packed JS from chapterfun.ashx and return the image URL array (d)
function decodeChapterFun(packedJs) {
    try {
        const cleaned = packedJs.replace(/\bvar\s+d\s*;/, '')
        const fn = new Function('var d; var currentimageid; ' + cleaned + '; return d;')
        const result = fn()
        return Array.isArray(result) ? result : []
    } catch (e) {
        return []
    }
}

function parseMangaItem($, li) {
    const a = $(li).find('a[href*="/manga/"]').first()
    const href = a.attr('href') || ''
    const slug = href.replace(/^\/manga\//, '').replace(/\/$/, '').trim()
    if (!slug) return null
    const img = $(li).find('img').first()
    const imgSrc = img.attr('src') || img.attr('data-src') || img.attr('data-original') || ''
    return {
        id: slug,
        image: imgSrc,
        title: a.attr('title') || a.text().trim() || slug,
        chapter: null,
        view: null,
        description: null
    }
}

function buildDirUrl(type, state, page) {
    const pageStr = page > 1 ? `${page}.htm` : ''
    if (state === 'completed') return `${BASE}/completed/${pageStr}`
    if (state === 'ongoing')   return `${BASE}/on_going/${pageStr}`

    const suffixMap = { newest: '?news', latest: '?latest', rating: '?rating' }
    const suffix = suffixMap[type] || ''
    return `${BASE}/directory/${pageStr}${suffix}`
}

async function getMangaList({ page = 1, type = 'topview', state = null } = {}) {
    const url = buildDirUrl(type, state, page)
    const html = await httpGet(url)
    const $ = cheerio.load(html)

    const mangaList = []
    $('ul[class*="manga-list"] li').each((i, li) => {
        const item = parseMangaItem($, li)
        if (item) mangaList.push(item)
    })

    let totalPages = 1
    $('.pager-list-left a[href]').each((i, el) => {
        const href = $(el).attr('href') || ''
        const m = href.match(/\/(\d+)\.htm/)
        if (m) {
            const n = parseInt(m[1])
            if (n > totalPages) totalPages = n
        }
    })

    return {
        mangaList,
        metaData: {
            totalStories: String(mangaList.length * totalPages),
            totalPages: String(totalPages),
            type: [
                { id: 'topview', name: 'Top Read' },
                { id: 'newest',  name: 'New Releases' },
                { id: 'latest',  name: 'Latest Updated' }
            ],
            state: [
                { id: 'all',       name: 'All' },
                { id: 'completed', name: 'Completed' },
                { id: 'ongoing',   name: 'Ongoing' }
            ],
            category: [{ id: '0', name: 'All' }]
        }
    }
}

async function searchManga({ query, page = 1 } = {}) {
    const q = encodeURIComponent(query).replace(/%20/g, '+')
    const url = `${BASE}/search?title=${q}&page=${page}`
    let html
    try {
        html = await httpReq({
            uri: url,
            headers: { ...HEADERS, Referer: BASE + '/search' },
            gzip: true,
            followAllRedirects: true,
            simple: false
        })
    } catch (e) {
        console.error('searchManga network error:', e.message?.slice(0, 200))
        return { mangaList: [], metaData: { totalPages: '1' } }
    }

    const $ = cheerio.load(html)
    const mangaList = []
    $('ul[class*="manga-list"] li').each((i, li) => {
        const item = parseMangaItem($, li)
        if (item) mangaList.push(item)
    })

    if (mangaList.length === 0) {
        console.log('searchManga: 0 results for query:', query, '— URL:', url)
    }

    return {
        mangaList,
        metaData: { totalPages: '1' }
    }
}

async function getMangaDetail(mangaSlug) {
    const url = `${BASE}/manga/${mangaSlug}/`
    const html = await httpGet(url)
    const $ = cheerio.load(html)

    const name = $('.detail-info-right-title-font').text().trim() || mangaSlug
    const imageUrl = $('img.detail-info-cover-img').attr('src') || ''
    const author = $('.detail-info-right-say a').first().text().trim() || null
    const status = $('.detail-info-right-title-tip').first().text().trim().toLowerCase() || null
    const description = $('#fullcontent').text().trim() || null
    const genres = $('.detail-info-right-tag-list a')
        .map((i, el) => $(el).text().trim()).get().filter(Boolean)

    const chapterList = []
    $('.detail-main-list li a').each((i, el) => {
        const href = $(el).attr('href') || ''
        // href like /manga/bleach/v58/c687/1.html or /manga/onepunch_man/c230/1.html
        // Extract everything between /{slug}/ and /1.html as the chapter path
        const parts = href.split('/').filter(Boolean)
        // parts: ['manga', slug, ...chapterSegments, '1.html']
        // chapterPath = segments between slug and page file
        const chapterPath = parts.slice(2, -1).join('/')
        if (!chapterPath) return

        const chName = $(el).find('.title3').text().trim() || chapterPath
        const chDate = $(el).find('.title2').text().trim() || null

        chapterList.push({
            id: chapterPath,
            path: href,
            name: chName,
            view: null,
            createdAt: chDate
        })
    })

    return {
        imageUrl,
        name,
        author,
        status,
        updated: chapterList.length > 0 ? chapterList[0].createdAt : null,
        view: null,
        genres,
        chapterList
    }
}

async function getChapterImages(mangaSlug, chapterPath) {
    const pageUrl = `${BASE}/manga/${mangaSlug}/${chapterPath}/1.html`
    const html = await httpGet(pageUrl)

    const chapterIdMatch = html.match(/var\s+chapterid\s*=\s*(\d+)/)
    const imageCountMatch = html.match(/var\s+imagecount\s*=\s*(\d+)/)
    if (!chapterIdMatch || !imageCountMatch) {
        throw new Error(`Could not parse chapter page for ${mangaSlug}/${chapterPath}`)
    }

    const chapterIntId = chapterIdMatch[1]
    const imageCount = parseInt(imageCountMatch[1])
    const guidkey = extractGuidkey(html)

    // Fetch pages in pairs (each call returns 2 consecutive URLs), in parallel
    const pagesToFetch = []
    for (let p = 1; p <= imageCount; p += 2) pagesToFetch.push(p)

    const results = await Promise.all(pagesToFetch.map(async (page) => {
        const apiUrl = `${BASE}/chapterfun.ashx?cid=${chapterIntId}&page=${page}&key=${guidkey}`
        const apiResp = await httpGet(apiUrl, pageUrl)
        return { page, urls: decodeChapterFun(apiResp) }
    }))

    const imageUrls = new Array(imageCount).fill(null)
    for (const { page, urls } of results) {
        for (let i = 0; i < urls.length; i++) {
            const idx = page - 1 + i
            if (idx < imageCount) {
                const url = urls[i]
                imageUrls[idx] = url.startsWith('//') ? 'https:' + url : url
            }
        }
    }

    const images = imageUrls
        .filter(Boolean)
        .map((image, i) => ({ title: `Page ${i + 1}`, image }))

    if (images.length === 0) {
        throw new Error(`Could not decode image URLs for ${mangaSlug}/${chapterPath}`)
    }

    return {
        title: null,
        currentChapter: chapterPath,
        images
    }
}

async function getMangaByGenre(genre, page = 1) {
    // MangaHere genre pages are at /{slug}/ e.g. /action/, /romance/
    const pageStr = page > 1 ? `${page}.htm` : ''
    const url = `${BASE}/${genre}/${pageStr}`
    let html
    try {
        html = await httpGet(url)
    } catch (e) {
        console.error('getMangaByGenre network error:', e.message?.slice(0, 200))
        return { mangaList: [], metaData: { totalPages: '1' } }
    }
    const $ = cheerio.load(html)

    const mangaList = []
    $('ul[class*="manga-list"] li').each((i, li) => {
        const item = parseMangaItem($, li)
        if (item) mangaList.push(item)
    })

    let totalPages = 1
    $('.pager-list-left a[href]').each((i, el) => {
        const href = $(el).attr('href') || ''
        const m = href.match(/\/(\d+)\.htm/)
        if (m) { const n = parseInt(m[1]); if (n > totalPages) totalPages = n }
    })

    return { mangaList, metaData: { totalPages: String(totalPages) } }
}

module.exports = { getMangaList, searchManga, getMangaDetail, getChapterImages, getMangaByGenre }
