package com.example.movieapp.service;

import com.example.movieapp.entities.Episode;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Bepul qism qoidasi: har serialning BIRINCHI qismi doim bepul, qolganlari pullik.
 *
 * "Birinchi" — qism raqami 1 emas, serialdagi TARTIB bo'yicha birinchisi: fasl raqami, keyin qism
 * raqami bo'yicha saralanadi (qismlar 39 dan boshlansa — 39-qism bepul).
 * Qo'shimcha bepul qismlar platformada emas, Telegram kanalda (Series.telegramFreeUrl).
 */
public final class FreeEpisodes {

    private FreeEpisodes() {
    }

    private static final Comparator<Episode> ORDER = Comparator
            .comparing((Episode e) -> e.getSeason() != null && e.getSeason().getSeasonNumber() != null
                    ? e.getSeason().getSeasonNumber() : 0)
            .thenComparing(e -> e.getEpisodeNumber() != null ? e.getEpisodeNumber() : Integer.MAX_VALUE)
            .thenComparing(Episode::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    public static final int FREE_COUNT = 1;

    /** Serialning bepul qismi id'si (bo'sh serialda — bo'sh to'plam). */
    public static Set<Long> ids(List<Episode> seriesEpisodes) {
        if (seriesEpisodes == null) {
            return Set.of();
        }
        return seriesEpisodes.stream()
                .sorted(ORDER)
                .limit(FREE_COUNT)
                .map(Episode::getId)
                .collect(Collectors.toSet());
    }
}
