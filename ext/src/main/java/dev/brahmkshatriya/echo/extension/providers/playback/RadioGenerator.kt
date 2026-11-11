package dev.brahmkshatriya.echo.extension.providers.playback

import dev.brahmkshatriya.echo.extension.ExtensionText

import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.loadAll
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Radio
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.extension.ModelTypeHelper
import dev.brahmkshatriya.echo.extension.toTrack
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiApi
import dev.toastbits.ytmkt.model.external.ThumbnailProvider
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import dev.brahmkshatriya.echo.common.helpers.PagedData


class RadioGenerator(
    private val api: YoutubeiApi,
    private val json: Json,
    private val thumbnailQuality: ThumbnailProvider.Quality,
    private val trackCache: MutableMap<String, PagedData<Track>>,
    private val playlistEndpoint: dev.brahmkshatriya.echo.extension.endpoints.EchoPlaylistEndpoint? = null
) {
    suspend fun generateRadio(item: EchoMediaItem, context: EchoMediaItem? = null): Radio {
        return when (item) {
            is Track -> generateFromTrack(item, context)
            is Album -> generateFromAlbum(item)
            is Artist -> generateFromArtist(item)
            is Playlist -> generateFromPlaylist(item)
            else -> throw Exception("Radio not supported for ${item::class.simpleName}")
        }
    }

    private suspend fun generateFromTrack(track: Track, context: EchoMediaItem?): Radio {
        val id = "radio_${track.id}"
        val cont = context?.extras?.get("cont")
        val result = api.SongRadio.getSongRadio(track.id, cont).getOrThrow()
        val tracks = result.items.map { song: dev.toastbits.ytmkt.model.external.mediaitem.YtmSong -> 
            song.toTrack(thumbnailQuality)
        }
        
        return Radio(
            id = id,
            title = ExtensionText.get("providers.radio_1_s_736db9", "Radio: %1\$s", track.title),
            extras = mutableMapOf<String, String>().apply {
                put("tracks", json.encodeToString(tracks))
                result.continuation?.let { put("cont", it) }
            }
        )
    }

    private suspend fun generateFromAlbum(album: Album): Radio {
        val cachedTrack = trackCache[album.id]?.toFeed()?.loadAll()?.lastOrNull()
        if (cachedTrack != null) {
            return generateFromTrack(cachedTrack, null)
        }
        val firstArtist = album.artists.firstOrNull()
        if (firstArtist != null) {
            return generateFromArtist(firstArtist)
        }
        val loaded = runCatching { playlistEndpoint?.loadFromPlaylist(album.id, quality = thumbnailQuality) }.getOrNull()
        val loadedTrack = loaded?.third?.toFeed()?.loadAll()?.lastOrNull()
            ?: throw Exception("No tracks found for album ${album.title}")
        return generateFromTrack(loadedTrack, null)
    }

    private suspend fun generateFromArtist(artist: Artist): Radio {
        val id = "radio_${artist.id}"
        val result = api.ArtistRadio.getArtistRadio(artist.id, null).getOrThrow()
        val tracks = result.items.map { song: dev.toastbits.ytmkt.model.external.mediaitem.YtmSong -> 
            song.toTrack(thumbnailQuality)
        }
        
        return Radio(
            id = id,
            title = ExtensionText.get("providers.radio_1_s_736db9", "Radio: %1\$s", artist.name),
            extras = mutableMapOf<String, String>().apply {
                put("tracks", json.encodeToString(tracks))
            }
        )
    }

    private suspend fun generateFromPlaylist(playlist: Playlist): Radio {
        val cachedTrack = trackCache[playlist.id]?.toFeed()?.loadAll()?.lastOrNull()
        if (cachedTrack != null) {
            return generateFromTrack(cachedTrack, null)
        }
        val loaded = runCatching { playlistEndpoint?.loadFromPlaylist(playlist.id, quality = thumbnailQuality) }.getOrNull()
        val loadedTrack = loaded?.third?.toFeed()?.loadAll()?.lastOrNull()
            ?: throw Exception("No tracks found for playlist ${playlist.title}")
        return generateFromTrack(loadedTrack, null)
    }

    private suspend fun generateFromUser(user: User): Radio {
        val artist = ModelTypeHelper.userToArtist(user)
        return generateFromArtist(artist)
    }

    fun loadRadioTracks(radio: Radio): Feed<Track> {
        return PagedData.Single { 
            val tracksJson = radio.extras["tracks"] 
                ?: throw Exception(ExtensionText.get("providers.no_tracks_found_in_radio_279868", "No tracks found in radio"))
            json.decodeFromString<List<Track>>(tracksJson)
        }.toFeed()
    }
}
