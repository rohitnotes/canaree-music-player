package dev.olog.lib.repository

import dev.olog.domain.entity.AutoPlaylist
import dev.olog.domain.entity.favorite.FavoriteTrackType
import dev.olog.domain.gateway.FavoriteGateway
import dev.olog.domain.gateway.track.PlaylistOperations
import dev.olog.domain.schedulers.Schedulers
import dev.olog.lib.db.HistoryDao
import dev.olog.lib.db.PlaylistDao
import dev.olog.lib.model.db.PlaylistEntity
import dev.olog.lib.model.db.PlaylistTrackEntity
import dev.olog.shared.android.utils.assertBackgroundThread
import dev.olog.shared.swap
import dev.olog.shared.throwNotHandled
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class PlaylistRepositoryHelper @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val historyDao: HistoryDao,
    private val favoriteGateway: FavoriteGateway,
    private val schedulers: Schedulers

) : PlaylistOperations {


    override suspend fun createPlaylist(playlistName: String): Long {
        assertBackgroundThread()

        return playlistDao.createPlaylist(
            PlaylistEntity(name = playlistName, size = 0)
        )
    }

    override suspend fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>) {
        assertBackgroundThread()

        var maxIdInPlaylist = (playlistDao.getPlaylistMaxId(playlistId) ?: 1).toLong()
        val tracks = songIds.map {
            PlaylistTrackEntity(
                playlistId = playlistId, idInPlaylist = ++maxIdInPlaylist,
                trackId = it
            )
        }
        playlistDao.insertTracks(tracks)
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        return playlistDao.deletePlaylist(playlistId)
    }

    override suspend fun clearPlaylist(playlistId: Long) {
        require(AutoPlaylist.isAutoPlaylist(playlistId))
        return when (playlistId) {
            AutoPlaylist.FAVORITE.id -> return favoriteGateway.deleteAll(FavoriteTrackType.TRACK)
            AutoPlaylist.HISTORY.id -> return historyDao.deleteAll()
            AutoPlaylist.LAST_ADDED.id -> {}
            else -> throwNotHandled("not an autoplaylist $playlistId")
        }
    }

    override suspend fun removeFromPlaylist(playlistId: Long, idInPlaylist: Long) {
        assertBackgroundThread()

        if (AutoPlaylist.isAutoPlaylist(playlistId)) {
            removeFromAutoPlaylist(playlistId, idInPlaylist)
        } else {
            return playlistDao.deleteTrack(playlistId, idInPlaylist)
        }
    }

    private suspend fun removeFromAutoPlaylist(playlistId: Long, songId: Long) {
        return when (playlistId) {
            AutoPlaylist.FAVORITE.id -> favoriteGateway.deleteSingle(FavoriteTrackType.TRACK, songId)
            AutoPlaylist.HISTORY.id -> historyDao.deleteSingle(songId)
            else -> throw IllegalArgumentException("invalid auto playlist id: $playlistId")
        }
    }

    override suspend fun renamePlaylist(playlistId: Long, newTitle: String) {
        return playlistDao.renamePlaylist(playlistId, newTitle)
    }

    override suspend fun moveItem(playlistId: Long, moveList: List<Pair<Int, Int>>) =
        withContext(schedulers.io) {
            var trackList = playlistDao.getPlaylistTracksImpl(playlistId)
            for ((from, to) in moveList) {
                trackList.swap(from, to)
            }
            trackList =
                trackList.mapIndexed { index, entity -> entity.copy(idInPlaylist = index.toLong()) }
            playlistDao.updateTrackList(trackList)
        }

    override suspend fun removeDuplicated(playlistId: Long) {
        val notDuplicate = playlistDao.getPlaylistTracksImpl(playlistId)
            .asSequence()
            .groupBy { it.trackId }
            .map { it.value[0] }
            .toList()
        playlistDao.deletePlaylistTracks(playlistId)
        playlistDao.insertTracks(notDuplicate)
    }

    override suspend fun insertSongToHistory(songId: Long) {
        return historyDao.insert(songId)
    }

}