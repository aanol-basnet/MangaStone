const ApiKey = require("./middleware/apiKeyMiddleware")
const app = require("express")()
const bodyParser = require("body-parser")
const mangaRouter = require("./routes/mangaRouter")
const mangaListRouter = require("./routes/mangaListRouter")
const mangaSearch = require("./routes/mangaSearch")
const mangaGenre = require("./routes/mangaGenre")

app.use(bodyParser.json())
require('dotenv').config()

app.use(ApiKey)
app.use("/api/manga", mangaRouter)
app.use("/api/mangaList", mangaListRouter)
app.use("/api/search", mangaSearch)
app.use("/api/genre", mangaGenre)

const PORT = process.env.PORT || 3000
app.listen(PORT, () => {
    console.log(`Server Start On Port ${PORT} 🎉✨ `)
})