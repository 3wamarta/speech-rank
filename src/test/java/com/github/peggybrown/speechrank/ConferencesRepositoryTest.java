package com.github.peggybrown.speechrank;

import com.github.peggybrown.speechrank.dto.ConferenceDto;
import com.github.peggybrown.speechrank.dto.ConferenceImportDto;
import com.github.peggybrown.speechrank.dto.YearDto;
import com.github.peggybrown.speechrank.dto.YearsConferenceDto;
import com.github.peggybrown.speechrank.entity.Comment;
import com.github.peggybrown.speechrank.entity.Conference;
import com.github.peggybrown.speechrank.entity.Presentation;
import com.github.peggybrown.speechrank.entity.Rate;
import com.github.peggybrown.speechrank.gateway.Importer;
import javaslang.collection.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConferencesRepositoryTest {
    public static final String NOT_EXISTING = "not existing id";
    private static final String YEAR = "2019";
    private static final String CONF_NAME = "confName";
    private static final String PLAYLISTLINK = "playlistlink";
    private static final String TEST_VIDEO_TITLE = "test";
    private static final String TEST_VIDEO_TITLE_2 = "2";
    private Importer importer;
    private ConferencesRepository conferencesRepository;

    @BeforeEach
    void setUp() {
        importer = mock(Importer.class);
        conferencesRepository = new ConferencesRepository(importer);
    }

    @Test
    void shouldImportConferenceWithoutPresentations() {
        importConferenceWithPresentations(List.empty());
        assertThat(conferencesRepository.getConferences())
            .hasSize(1)
            .extracting(Conference::getPresentations).contains(List.empty());
    }

    @Test
    void shouldImportConferenceWithPresentations() {
        assertThat(conferencesRepository.getConferences()).isEmpty();

        importConferenceWithPresentations(createPresentationsWithTitles(TEST_VIDEO_TITLE, TEST_VIDEO_TITLE_2));

        Set<String> presentationsTitles = conferencesRepository.getConferences().stream()
            .flatMap(conf -> conf.getPresentations().toJavaStream())
            .map(Presentation::getTitle)
            .collect(Collectors.toSet());

        assertThat(presentationsTitles)
            .hasSize(2).contains(TEST_VIDEO_TITLE, TEST_VIDEO_TITLE_2);
    }

    @Test
    void shouldImportAllConferences() {
        mockImportingAllConferences();
        assertThat(conferencesRepository.getConferences()).isEmpty();

        conferencesRepository.importAllConferences();

        Set<String> presentationTitles = conferencesRepository.getConferences().stream()
            .flatMap(conf -> conf.getPresentations().toJavaStream())
            .map(Presentation::getTitle)
            .collect(Collectors.toSet());

        assertThat(presentationTitles)
            .hasSize(9).contains("1", "2", "3", "4", "5", "6", "7", "8", "9");
    }

    @Test
    void shouldNotAddCommentForNotFoundPresentation() {
        importConferenceWithPresentations(createPresentationsWithTitles(TEST_VIDEO_TITLE));
        Comment comment = new Comment();
        comment.setPresentationId(NOT_EXISTING);
        conferencesRepository.add(comment);
        assertThat(getFirstPresentation().getComments()).isEmpty();
    }

    @Test
    void shouldAddCommentForPresentation() {
        importConferenceWithPresentations(createPresentationsWithTitles(TEST_VIDEO_TITLE));
        Comment testComment = new Comment();
        testComment.setPresentationId(getFirstPresentation().getId());
        testComment.setComment("TEST_COMMENT");

        conferencesRepository.add(testComment);

        assertThat(getFirstPresentation().getComments())
            .hasSize(1)
            .contains(testComment);
    }

    @Test
    void shouldThrowExceptionWhenAddingRateForNotFoundPresentation() {
        importConferenceWithPresentations(createPresentationsWithTitles(TEST_VIDEO_TITLE));
        Rate rate = new Rate();
        rate.setPresentationId(NOT_EXISTING);
        assertThatThrownBy(() -> conferencesRepository.add(rate))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void shouldAddRateForPresentation() {
        importConferenceWithPresentations(createPresentationsWithTitles(TEST_VIDEO_TITLE));
        Rate rate = new Rate();
        rate.setPresentationId(getFirstPresentation().getId());
        conferencesRepository.add(rate);
        assertThat(getFirstPresentation().getRates()).hasSize(1).contains(rate);
    }

    @Test
    void shouldGetConferenceDto() {
        importConferenceWithPresentations(createPresentationsWithTitles(TEST_VIDEO_TITLE));
        Conference conference = conferencesRepository.getConferences().get(0);
        ConferenceDto conferenceDto = conferencesRepository.getConference(conference.getId());
        assertThat(conferenceDto.getPresentations()).hasSize(1);
    }

    @Test
    void shouldGetYearsWithoutConferencesIfNotImported() {
        java.util.List<YearDto> years = conferencesRepository.getYears();
        java.util.List<YearsConferenceDto> conferenceDtos = years.stream()
            .flatMap(yearDto -> yearDto.getConferences().stream()).collect(Collectors.toList());
        assertAll(
            () -> assertThat(years).hasSize(3),
            () -> assertThat(conferenceDtos).hasSize(0)
        );

    }

    @Test
    void shouldGetYearsWithConference() {
        importConferenceWithPresentations(createPresentationsWithTitles(TEST_VIDEO_TITLE));
        java.util.List<YearDto> years = conferencesRepository.getYears();

        java.util.List<YearsConferenceDto> conferenceDtos = years.stream()
            .flatMap(yearDto -> yearDto.getConferences().stream()).collect(Collectors.toList());
        assertAll(
            () -> assertThat(years).hasSize(3),
            () -> assertThat(conferenceDtos).hasSize(1),
            () -> assertThat(conferenceDtos.get(0).getName()).isEqualTo(CONF_NAME),
            () -> assertThat(conferenceDtos.get(0).getPresentations()).isEqualTo(1)
        );
    }

    private void importConferenceWithPresentations(List<Importer.VideoData> presentations) {
        when(importer.importFromYouTubePlaylist(PLAYLISTLINK)).thenReturn(presentations);
        assertThat(conferencesRepository.getConferences()).isEmpty();
        ConferenceImportDto conferenceDto = aConferenceImport();
        conferencesRepository.importConference(conferenceDto);
    }

    private Presentation getFirstPresentation() {
        return conferencesRepository.getConferences().stream()
            .map(Conference::getPresentations)
            .map(List::get)
            .findFirst().orElse(null);
    }


    private void mockImportingAllConferences() {
        when(importer.importFromYouTubePlaylist(anyString())).thenReturn(createPresentationsWithTitles("1"))
            .thenReturn(createPresentationsWithTitles("2"))
            .thenReturn(createPresentationsWithTitles("3"))
            .thenReturn(createPresentationsWithTitles("4"))
            .thenReturn(createPresentationsWithTitles("5"))
            .thenReturn(createPresentationsWithTitles("6", "8", "9"))
            .thenReturn(createPresentationsWithTitles("7"));
    }

    private List<Importer.VideoData> createPresentationsWithTitles(String... titles) {
        return List.ofAll(Arrays.asList(titles.clone()))
            .map(title -> new Importer.VideoData("1", title, "test description"));
    }

    private ConferenceImportDto aConferenceImport() {
        ConferenceImportDto conferenceImportDto = new ConferenceImportDto();
        conferenceImportDto.setYear(YEAR);
        conferenceImportDto.setName(CONF_NAME);
        conferenceImportDto.setPlaylistLink(PLAYLISTLINK);
        return conferenceImportDto;
    }
}
