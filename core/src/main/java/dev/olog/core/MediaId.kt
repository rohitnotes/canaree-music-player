package dev.olog.core

import dev.olog.core.MediaIdCategory.*

private val podcastCategories = listOf(
    PODCASTS_AUTHORS, PODCASTS, PODCASTS_PLAYLIST
)

sealed class MediaId(
    open val category: MediaIdCategory,
    open val categoryId: Long
) {

    data class Category(
        override val category: MediaIdCategory,
        override val categoryId: Long
    ) : MediaId(category, categoryId) {

        fun playableItem(id: Long): Track {
            return Track(
                category = this.category,
                categoryId = this.categoryId,
                id = id
            )
        }

    }

    data class Track(
        override val category: MediaIdCategory,
        override val categoryId: Long,
        val id: Long
    ) : MediaId(category, categoryId) {

        val parentId: Category
            get() = Category(category, categoryId)

    }

    companion object {
        private const val CATEGORY_SEPARATOR = '/'
        private const val LEAF_SEPARATOR = '|'

        @JvmStatic
        val SHUFFLE_ID: Category = Category(SONGS, -100)

        @JvmStatic
        val SONGS_CATEGORY: Category = Category(SONGS, -1)

        @JvmStatic
        val PODCAST_CATEGORY: Category = Category(SONGS, -2)

        @JvmStatic
        fun fromString(mediaId: String): MediaId {
            val categoryFinish = mediaId.indexOf(CATEGORY_SEPARATOR)
            val categoryValueFinish = mediaId.indexOf(LEAF_SEPARATOR)

            if (categoryValueFinish == -1) {
                val category = mediaId.substring(0, categoryFinish)
                return Category(
                    valueOf(category),
                    mediaId.substring(categoryFinish + 1).toLong()
                )
            }
            return Track(
                valueOf(mediaId.substring(0, categoryFinish)),
                mediaId.substring(categoryFinish + 1, categoryValueFinish).toLong(),
                mediaId.substring(categoryValueFinish + 1).toLong()
            )
        }
    }

    override fun toString(): String {
        return when (this) {
            is Category -> "$category$CATEGORY_SEPARATOR$categoryId"
            is Track -> "$category$CATEGORY_SEPARATOR$categoryId$LEAF_SEPARATOR$id"
        }
    }

    val isAnyPodcast: Boolean
        get() = category in podcastCategories

}