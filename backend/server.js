const express = require('express')
const puppeteer = require('puppeteer')
const cheerio = require('cheerio')

const app = express()
const PORT = process.env.PORT || 3000
const BASE_URL = 'https://mangakakalot.com'

// Reuse a single browser instance across all requests
let browser = null

async function getBrowser() {
  if (!browser || !browser.isConnected()) {
    console.log('Launching headless browser...')
    browser = await puppeteer.launch({
      headless: true,
      args: [
        '--no-sandbox',
        '--disable-setuid-sandbox',
        '--disable-dev-shm-usage',
        '--disable-gpu'
      ]
    })
    console.log('Browser ready.')
  }
  return browser
}

async function fetchHtml(url) {
  const b = await getBrowser()
  const page = await b.newPage()
  await page.setUserAgent(
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36'
  )
  await page.setExtraHTTPHeaders({ Referer: BASE_URL })
  try {
    await page.goto(url, { waitUntil: 'networkidle2', timeout: 30000 })
    // Wait for manga list or detail to appear
    await page.waitForSelector(
      '.list-truyen-item-wrap, .story_item, .manga-info-text, .story-info-right, #panel-story-info',
      { timeout: 10000 }
    ).catch(() => {})
    return await page.content()
  } finally {
    await page.close()
  }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

function extractMangaId(url) {
  if (!url) return null
  const cleaned = url.split('?')[0].replace(/\/$/, '')
  if (cleaned.includes('/manga/')) {
    const slug = cleaned.split('/manga/').pop()
    return slug || null
  }
  const readMatch = cleaned.match(/\/(read-[^/]+)$/)
  if (readMatch) return readMatch[1]
  return null
}

function parseMangaList($) {
  const items = []
  $('.list-truyen-item-wrap, .story_item').each((_, el) => {
    const $el = $(el)
    const $link = $el.find('h3 > a, .story_name > a').first()
    const $img = $el.find('img').first()
    const href = $link.attr('href') || ''
    const title = $link.text().trim()
    const coverUrl = $img.attr('src') || $img.attr('data-src') || ''
    const id = extractMangaId(href)
    if (id && title) items.push({ id, title, coverUrl })
  })
  return items
}

function hasNextPage($) {
  return !!$('a.page_last, a[title="Last"]').length ||
    $('.list-truyen-item-wrap, .story_item').length >= 24
}

function mangaDetailUrl(id) {
  return id.startsWith('read-') ? `${BASE_URL}/${id}` : `${BASE_URL}/manga/${id}`
}

// ── Routes ────────────────────────────────────────────────────────────────────

app.use(express.static(__dirname))

app.get('/health', (_, res) => res.json({ status: 'ok' }))

app.get('/manga/popular', async (req, res) => {
  try {
    const page = parseInt(req.query.page) || 1
    const html = await fetchHtml(`${BASE_URL}/manga_list?type=topview&category=all&state=all&page=${page}`)
    const $ = cheerio.load(html)
    res.json({ manga: parseMangaList($), hasNextPage: hasNextPage($) })
  } catch (e) {
    console.error('/manga/popular', e.message)
    res.status(500).json({ error: e.message })
  }
})

app.get('/manga/newest', async (req, res) => {
  try {
    const page = parseInt(req.query.page) || 1
    const html = await fetchHtml(`${BASE_URL}/manga_list?type=newest&category=all&state=all&page=${page}`)
    const $ = cheerio.load(html)
    res.json({ manga: parseMangaList($), hasNextPage: hasNextPage($) })
  } catch (e) {
    console.error('/manga/newest', e.message)
    res.status(500).json({ error: e.message })
  }
})

app.get('/manga/completed', async (req, res) => {
  try {
    const page = parseInt(req.query.page) || 1
    const html = await fetchHtml(`${BASE_URL}/manga_list?type=topview&category=all&state=completed&page=${page}`)
    const $ = cheerio.load(html)
    res.json({ manga: parseMangaList($), hasNextPage: hasNextPage($) })
  } catch (e) {
    console.error('/manga/completed', e.message)
    res.status(500).json({ error: e.message })
  }
})

app.get('/manga/search', async (req, res) => {
  try {
    const q = (req.query.q || '').toLowerCase().trim().replace(/\s+/g, '_')
    const page = parseInt(req.query.page) || 1
    const url = page > 1
      ? `${BASE_URL}/search/story/${encodeURIComponent(q)}?page=${page}`
      : `${BASE_URL}/search/story/${encodeURIComponent(q)}`
    const html = await fetchHtml(url)
    const $ = cheerio.load(html)
    const items = parseMangaList($)
    res.json({ manga: items, hasNextPage: items.length >= 20 })
  } catch (e) {
    console.error('/manga/search', e.message)
    res.status(500).json({ error: e.message })
  }
})

app.get('/manga/:id/chapters', async (req, res) => {
  try {
    const { id } = req.params
    const html = await fetchHtml(mangaDetailUrl(id))
    const $ = cheerio.load(html)
    const chapters = []

    // Old layout
    const oldRows = $('.chapter-list .row')
    if (oldRows.length) {
      oldRows.each((_, row) => {
        const $row = $(row)
        const $a = $row.find('span:first-child > a').first()
        const chapterUrl = $a.attr('href') || ''
        if (!chapterUrl) return
        const rawTitle = $a.text().trim()
        const num = rawTitle.match(/[Cc]hapter[\s_]+([\d.]+)/)?.[1]
          || chapterUrl.match(/chapter[_-]([\d.]+)/i)?.[1]
          || ''
        chapters.push({
          id: chapterUrl,
          number: num,
          title: rawTitle,
          publishAt: $row.find('span:last-child').text().trim(),
          scanlationGroup: 'MangaKakalot'
        })
      })
    } else {
      // New layout
      $('ul.row-content-chapter li.a-h').each((_, li) => {
        const $li = $(li)
        const $a = $li.find('a.chapter-name').first()
        const chapterUrl = $a.attr('href') || ''
        if (!chapterUrl) return
        const rawTitle = $a.text().trim()
        const num = rawTitle.match(/[Cc]hapter[\s_]+([\d.]+)/)?.[1]
          || chapterUrl.match(/chapter[_-]([\d.]+)/i)?.[1]
          || ''
        chapters.push({
          id: chapterUrl,
          number: num,
          title: rawTitle,
          publishAt: $li.find('span.chapter-time').attr('title') || '',
          scanlationGroup: 'MangaKakalot'
        })
      })
    }

    res.json({ chapters })
  } catch (e) {
    console.error('/manga/:id/chapters', e.message)
    res.status(500).json({ error: e.message })
  }
})

app.get('/manga/:id', async (req, res) => {
  try {
    const { id } = req.params
    const html = await fetchHtml(mangaDetailUrl(id))
    const $ = cheerio.load(html)

    const title = $('.manga-info-text h1, .story-info-right h1').first().text().trim()
    const coverUrl = $('.manga-info-pic img, .story-info-left img').first().attr('src') || ''

    const $desc = $('#noidungm, #panel-story-info-description').first()
    $desc.find('h2, p.close-button').remove()
    const description = $desc.text().replace(/^Description\s*:/i, '').trim()

    let author = '', status = 'unknown'
    const tags = []

    // Old layout
    $('.manga-info-text li').each((_, li) => {
      const $li = $(li)
      const text = $li.text()
      if (/^Author/i.test(text)) {
        author = $li.find('a').map((_, a) => $(a).text()).get().join(', ')
          || text.split(':').slice(1).join(':').trim()
      } else if (/^Status/i.test(text)) {
        status = text.split(':').slice(1).join(':').trim().toLowerCase()
      } else if (/^Genre/i.test(text)) {
        $li.find('a').each((_, a) => {
          tags.push({
            id: ($(a).attr('href') || '').replace(/\/$/, '').split('/').pop(),
            name: $(a).text().trim(),
            group: 'genre'
          })
        })
      }
    })

    // New layout fallback
    if (!author) {
      $('table.variations-tableInfo tr').each((_, row) => {
        const label = $(row).find('td.table-label').text()
        const $val = $(row).find('td.table-value')
        if (/Author/i.test(label)) {
          author = $val.find('a').map((_, a) => $(a).text()).get().join(', ') || $val.text().trim()
        } else if (/Status/i.test(label)) {
          status = $val.text().trim().toLowerCase()
        } else if (/Genre/i.test(label)) {
          $val.find('a').each((_, a) => {
            tags.push({
              id: ($(a).attr('href') || '').replace(/\/$/, '').split('/').pop(),
              name: $(a).text().trim(),
              group: 'genre'
            })
          })
        }
      })
    }

    res.json({ id, title, coverUrl, description, author, status, tags })
  } catch (e) {
    console.error('/manga/:id', e.message)
    res.status(500).json({ error: e.message })
  }
})

app.get('/chapter/pages', async (req, res) => {
  try {
    const url = req.query.url
    if (!url) return res.status(400).json({ error: 'url parameter required' })
    const html = await fetchHtml(url)
    const $ = cheerio.load(html)
    const pages = []
    $('#vungdoc img, .container-chapter-reader img, .vung-doc img').each((_, img) => {
      const src = $(img).attr('src') || $(img).attr('data-src') || ''
      if (src.startsWith('http')) pages.push(src)
    })
    res.json({ pages })
  } catch (e) {
    console.error('/chapter/pages', e.message)
    res.status(500).json({ error: e.message })
  }
})

// ── Start ─────────────────────────────────────────────────────────────────────

app.listen(PORT, '0.0.0.0', () => {
  console.log(`MangaRock backend listening on http://0.0.0.0:${PORT}`)
  console.log(`Your phone/app should connect to http://<YOUR_PC_IP>:${PORT}`)
  console.log('To find your PC IP: run  ipconfig  in Command Prompt, look for IPv4 Address')
})

process.on('SIGINT', async () => {
  if (browser) await browser.close()
  process.exit(0)
})
