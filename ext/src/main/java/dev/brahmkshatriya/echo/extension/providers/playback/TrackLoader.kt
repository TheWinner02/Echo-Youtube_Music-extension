package dev.brahmkshatriya.echo.extension.providers.playback

import dev.brahmkshatriya.echo.extension.ExtensionText

import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extension.auth.YouTubeAuthManager
import dev.brahmkshatriya.echo.extension.endpoints.EchoEnhancedSongEndpoint
import dev.brahmkshatriya.echo.extension.streaming.YouTubeStreamResolver
import dev.toastbits.ytmkt.model.external.ThumbnailProvider


class TrackLoader(
    private val authManager: YouTubeAuthManager,
    private val enhancedSongEndpoint: EchoEnhancedSongEndpoint,
    private val streamResolver: YouTubeStreamResolver
) {
    suspend fun loadTrackDetails(
        track: Track,
        thumbnailQuality: ThumbnailProvider.Quality
    ): Track {

        try {
            authManager.ensureVisitorId().getOrNull()
        } catch (e: Exception) {
            println("Failed to ensure visitor ID in loadTrack: ${e.message}")
        }

        return enhancedSongEndpoint.loadEnhancedTrack(track.id, track, thumbnailQuality)
    }

    suspend fun loadStreamableMedia(
        streamable: Streamable,
        preferVideos: Boolean
    ): Streamable.Media {
        when (streamable.type) {
            Streamable.MediaType.Server -> {
                val videoId = streamable.extras["videoId"]
                    ?: throw Exception(ExtensionText.get("providers.no_video_id_found_this_track_may_not_be_playable_83575d", "No video ID found. This track may not be playable."))

                return streamResolver.resolveStreamable(videoId, preferVideos)
            }
            Streamable.MediaType.Background -> {
                throw Exception(ExtensionText.get("providers.background_streamables_not_supported_3bd1af", "Background streamables not supported"))
            }
            Streamable.MediaType.Subtitle -> {
                throw Exception(ExtensionText.get("providers.subtitles_not_supported_98a8c0", "Subtitles not supported"))
            }
        }
    }
}
