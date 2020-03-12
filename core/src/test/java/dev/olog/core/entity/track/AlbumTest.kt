package dev.olog.core.entity.track

import dev.olog.core.MediaId.Category
import dev.olog.core.MediaIdCategory.ALBUMS
import dev.olog.core.MediaIdCategory.ARTISTS
import dev.olog.core.Mocks
import org.junit.Assert.assertEquals
import org.junit.Test

class AlbumTest {

    @Test
    fun testTrackGetMediaId() {
        val id = 1L
        val album = Mocks.album.copy(id = id)
        assertEquals(
            Category(ALBUMS, id),
            album.getMediaId()
        )
    }

    @Test
    fun testTrackGetArtistMediaId() {
        val id = 1L
        val album = Mocks.album.copy(artistId = id)
        assertEquals(
            Category(ARTISTS, id),
            album.getArtistMediaId()
        )
    }

}