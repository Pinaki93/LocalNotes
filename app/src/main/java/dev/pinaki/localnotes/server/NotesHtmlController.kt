package dev.pinaki.localnotes.server

class NotesHtmlController : HtmlCrudController {
    override val path = "/notes"

    override suspend fun listing() = "/public/index.html"
    override suspend fun create() = "/public/create.html"
    override suspend fun update() = "/public/update.html"
}
