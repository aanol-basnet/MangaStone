const pagesValidation = (req, res, next) => {
    req.query.page = Number(req.query.page) || 1
    next()
}

module.exports = pagesValidation