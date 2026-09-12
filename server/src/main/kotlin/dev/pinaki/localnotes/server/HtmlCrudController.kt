package dev.pinaki.localnotes.server

/**
 * Exposes optional HTML pages for a CRUD resource.
 *
 * Returning null (the default) keeps that route unavailable. A returned path must point to an
 * HTML classpath resource, for example `/public/notes.html`.
 */
interface HtmlCrudController {
    val path: String
    val servesRoot: Boolean get() = false

    suspend fun listing(): String? = null
    suspend fun create(): String? = null
    suspend fun update(): String? = null
    suspend fun delete(): String? = null
}
