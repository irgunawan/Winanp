package id.irgunawan.winanp

import android.content.ContentUris
import android.content.Context
import android.media.audiofx.Equalizer
import android.net.Uri
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

data class LocalAudioTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationSec: Int,
    val format: String,
    val uri: Uri
)

class AudioEngineController(private val context: Context) {

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()
    private var equalizer: Equalizer? = null

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                setupHardwareEqualizer(audioSessionId)
            }
        })
    }

    private fun setupHardwareEqualizer(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        try {
            equalizer?.release()
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = true
            }
        } catch (_: Exception) {
        }
    }

    fun setBandLevelDb(bandIndex: Int, gainDb: Int) {
        val eq = equalizer ?: return
        val numBands = eq.numberOfBands.toInt()
        if (numBands <= 0) return
        val mappedBand = ((bandIndex.toFloat() / 10f) * numBands).toInt().coerceIn(0, numBands - 1).toShort()
        val minLevel = eq.bandLevelRange[0]
        val maxLevel = eq.bandLevelRange[1]
        val targetMillibels = (gainDb * 100).coerceIn(minLevel.toInt(), maxLevel.toInt()).toShort()
        eq.setBandLevel(mappedBand, targetMillibels)
    }

    fun scanDeviceMp3AndFlac(): List<LocalAudioTrack> {
        val list = mutableListOf<LocalAudioTrack>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DISPLAY_NAME
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val title = cursor.getString(titleCol) ?: "Unknown Track"
                val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                val album = cursor.getString(albumCol) ?: "Unknown Album"
                val durMs = cursor.getLong(durCol)
                val mime = cursor.getString(mimeCol) ?: ""
                val displayName = cursor.getString(nameCol) ?: ""

                val isFlac = mime.contains("flac", true) || displayName.endsWith(".flac", true)
                val isMp3 = mime.contains("mpeg", true) || displayName.endsWith(".mp3", true)

                if (isFlac || isMp3) {
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    list.add(
                        LocalAudioTrack(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            durationSec = (durMs / 1000L).toInt(),
                            format = if (isFlac) "FLAC" else "MP3",
                            uri = contentUri
                        )
                    )
                }
            }
        }
        return list
    }

    fun playUri(uri: Uri) {
        exoPlayer.setMediaItem(MediaItem.fromUri(uri))
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun release() {
        equalizer?.release()
        exoPlayer.release()
    }
}
