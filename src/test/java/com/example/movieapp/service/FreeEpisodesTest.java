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
    void qismlari_39_dan_boshlansa_ham_dastlabki_N_tasi_bepul() {
        // Oldingi qoida (raqam <= 5) bu serialda birorta qismni bepul qilmasdi.
        List<Episode> eps = List.of(ep(3, 3, 41), ep(1, 3, 39), ep(4, 3, 42), ep(2, 3, 40), ep(5, 3, 43), ep(6, 3, 44));
        assertEquals(Set.of(1L, 2L, 3L), FreeEpisodes.ids(eps, 3));
    }

    @Test
    void avval_fasl_keyin_qism_tartibi() {
        List<Episode> eps = List.of(ep(10, 2, 1), ep(20, 1, 30), ep(30, 1, 29));
        assertEquals(Set.of(30L, 20L), FreeEpisodes.ids(eps, 2));
    }

    @Test
    void bepul_soni_yoq_yoki_nol() {
        List<Episode> eps = List.of(ep(1, 1, 1));
        assertEquals(Set.of(), FreeEpisodes.ids(eps, null));
        assertEquals(Set.of(), FreeEpisodes.ids(eps, 0));
    }
}
