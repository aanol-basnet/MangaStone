// Page validation — just ensure page is a positive integer
// totalPages will be set by the MangaHere response in the controller
const pagesValidation = (req, res, next) => {
    req.query.page = Math.max(1, parseInt(req.query.page) || 1)
    next()
}

module.exports = pagesValidation