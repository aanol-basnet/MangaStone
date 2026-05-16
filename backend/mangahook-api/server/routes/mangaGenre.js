const { getMangaByGenre } = require('../utils/mangahere')
const router = require('express').Router()

router.get('/:genre', async (req, res) => {
    try {
        const page = parseInt(req.query.page) || 1
        const result = await getMangaByGenre(req.params.genre, page)
        res.json(result)
    } catch (e) {
        console.error('getMangaByGenre error:', e.message)
        res.status(500).json({ error: e.message })
    }
})

module.exports = router
