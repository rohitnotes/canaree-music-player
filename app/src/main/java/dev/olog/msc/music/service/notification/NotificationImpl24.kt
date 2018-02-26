package dev.olog.msc.music.service.notification

import android.app.NotificationManager
import android.app.Service
import android.net.Uri
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.SpannableString
import dagger.Lazy
import dev.olog.msc.music.service.interfaces.INotification
import dev.olog.msc.utils.TextUtils
import dev.olog.msc.utils.img.ImageUtils
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.N)
open class NotificationImpl24 @Inject constructor(
        service: Service,
        token: MediaSessionCompat.Token,
        notificationManager: Lazy<NotificationManager>

) : NotificationImpl21(service, token, notificationManager) {

    override fun startChronometer(playbackState: PlaybackStateCompat) {
        super.startChronometer(playbackState)
        builder.setSubText(null)
    }

    override fun stopChronometer(playbackState: PlaybackStateCompat) {
        super.stopChronometer(playbackState)
        builder.setSubText(TextUtils.formatTimeMillisForNotification(playbackState.position))
    }

    override fun updateMetadataImpl(
            id: Long,
            title: SpannableString,
            artist: String,
            album: String,
            uri: Uri) {

        builder.setLargeIcon(ImageUtils.getBitmapFromUriWithPlaceholder(service, uri, id,
                                INotification.IMAGE_SIZE, INotification.IMAGE_SIZE))
                .setContentTitle(title)
                .setContentText(artist)
    }
}