package com.example.movieapp.service;

import com.example.movieapp.entities.Episode;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * "Dastlabki N ta qism bepul" qoidasi.
 *
 * Qism raqami bo'yicha emas, serialdagi TARTIB bo'yicha hisoblanadi: fasl raqami, keyin qism
 * raqami bo'yicha saralab, birinchi N tasi bepul. Oldin "qism raqami <= N" deb tekshirilardi —
 * qismlari 1 dan emas (masalan 39 dan) boshlanadigan seriallarda birorta qism bepul bo'lmay qolardi.
 */
public final class FreeEpisodes {

    private FreeEpisodes() {
    }

    private static final Comparator<Episode> ORDER = Comparator
            .comparing((Episode e) -> e.getSeason() != null && e.getSeason().getSeasonNumber() != null
                    ? e.getSeason().getSeasonNumber() : 0)
            .thenComparing(e -> e.getEpisodeNumber() != null ? e.getEpisodeNumber() : Integer.MAX_VALUE)
            .thenComparing(Episode::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    /** Serialning bepul qismlari id'lari. freeCount null yoki 0 bo'lsa — bo'sh to'plam. */
    public static Set<Long> ids(List<Episode> seriesEpisodes, Integer freeCount) {
        if (freeCount == null || freeCount <= 0 || seriesEpisodes == null) {
            return Set.of();
        }
        return seriesEpisodes.stream()
                .sorted(ORDER)
                .limit(freeCount)
                .map(Episode::getId)
                .collect(Collectors.toSet());
    }
}
