package com.github.peggybrown.speechrank.gateway;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemContentDetails;
import com.google.api.services.youtube.model.PlaylistItemListResponse;
import com.google.api.services.youtube.model.PlaylistItemSnippet;
import com.typesafe.config.ConfigFactory;
import javaslang.collection.List;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImporterTest {

    private static final String PLAYLIST_ID = "abc";
    private static final String VIDEO_ID = "VIDEO_ID";
    private static final String TITLE = "TITLE";
    private static final String DESCRIPTION = "DESCRIPTION";
    String apiKey = ConfigFactory.parseFile(new File("api.conf"))
        .getString("youtube.apiKey");

    @Test
    void shouldImportConference() throws IOException {
        YouTube youTube = getMockedYouTube();

        Importer importer = new Importer(apiKey, youTube);

        List<Importer.VideoData> videos = importer.importFromYouTubePlaylist(PLAYLIST_ID);

        assertAll(
            () -> assertThat(videos).hasSize(1),
            () -> assertThat(videos.get(0).getVideoId()).isEqualTo(VIDEO_ID),
            () -> assertThat(videos.get(0).getDescription()).isEqualTo(DESCRIPTION),
            () -> assertThat(videos.get(0).getTitle()).isEqualTo(TITLE)
        );

    }

    @Test
    void shouldIgnoreIOExceptions() throws IOException {
        YouTube youTube = mock(YouTube.class);
        YouTube.PlaylistItems playlistItems = mock(YouTube.PlaylistItems.class);
        Importer importer = new Importer(apiKey, youTube);

        when(youTube.playlistItems()).thenReturn(playlistItems);
            when(playlistItems.list(anyString())).thenThrow(new IOException("message"));
        List<Importer.VideoData> videos = importer.importFromYouTubePlaylist(PLAYLIST_ID);
        assertThat(videos).isEmpty();
    }

    @Test
    void shouldIgnoreResponseExceptions() throws IOException {
        YouTube youTube = mock(YouTube.class);
        YouTube.PlaylistItems playlistItems = mock(YouTube.PlaylistItems.class);
        Importer importer = new Importer(apiKey, youTube);

        when(youTube.playlistItems()).thenReturn(playlistItems);
        when(playlistItems.list(anyString())).thenThrow(new GoogleJsonResponseException(
            new HttpResponseException.Builder(500, "", new HttpHeaders()),
            new GoogleJsonError()));
        List<Importer.VideoData> videos = importer.importFromYouTubePlaylist(PLAYLIST_ID);
        assertThat(videos).isEmpty();
    }
    private YouTube getMockedYouTube() throws IOException {
        YouTube youTube = mock(YouTube.class);
        YouTube.PlaylistItems playlistItems = mock(YouTube.PlaylistItems.class);
        YouTube.PlaylistItems.List playlistItemsList = mock(YouTube.PlaylistItems.List.class);
        PlaylistItemListResponse response = new PlaylistItemListResponse();

        when(youTube.playlistItems()).thenReturn(playlistItems);
        when(playlistItems.list(anyString())).thenReturn(playlistItemsList);
        when(playlistItemsList.execute()).thenReturn(response);

        ArrayList<PlaylistItem> responseItems = getPlaylistItems();
        response.setItems(responseItems);
        return youTube;
    }

    private ArrayList<PlaylistItem> getPlaylistItems() {
        PlaylistItem item = new PlaylistItem();
        PlaylistItemContentDetails details = new PlaylistItemContentDetails();
        details.setVideoId(VIDEO_ID);
        item.setContentDetails(details);
        PlaylistItemSnippet snippet = new PlaylistItemSnippet();
        snippet.setTitle(TITLE);
        snippet.setDescription(DESCRIPTION);
        item.setSnippet(snippet);
        ArrayList<PlaylistItem> responseItems = new ArrayList<>();
        responseItems.add(item);
        return responseItems;
    }
}
