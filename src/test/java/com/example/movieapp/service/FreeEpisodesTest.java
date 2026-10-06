package com.example.movieapp.service;

import com.example.movieapp.entities.Episode;
import com.example.movieapp.entities.Season;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FreeEpisodesTest {

    private static Episode ep(long id, int season, int number) {
        Season s = new Season();
        s.setSeasonNumber(season);
        Episode e = new Episode();
        e.setId(id);
        e.setSeason(s);
        e.setEpisodeNumber(number);
        return e;
    }

    @Test
    void qismlar_39_dan_boshlansa_39_qism_bepul() {
        List<Episode> eps = List.of(ep(3, 3, 41), ep(1, 3, 39), ep(2, 3, 40));
        assertEquals(Set.of(1L), FreeEpisodes.ids(eps));
    }

    @Test
    void avval_fasl_keyin_qism_tartibi() {
        List<Episode> eps = List.of(ep(10, 2, 1), ep(20, 1, 30), ep(30, 1, 29));
        assertEquals(Set.of(30L), FreeEpisodes.ids(eps));
    }

    @Test
    void bosh_serial() {
        assertEquals(Set.of(), FreeEpisodes.ids(List.of()));
    }
}
