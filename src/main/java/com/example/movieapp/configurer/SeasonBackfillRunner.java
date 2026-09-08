package com.example.movieapp.configurer;

import com.example.movieapp.entities.Episode;
import com.example.movieapp.entities.Season;
import com.example.movieapp.entities.Series;
import com.example.movieapp.repository.EpisodeRepo;
import com.example.movieapp.repository.SeasonRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fasl (Season) funksiyasi qo'shilishidan oldin yaratilgan epizodlarni har bir serial uchun
 * avtomatik "1-fasl"ga biriktiradi, shunda mavjud kontent yo'qolmaydi.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeasonBackfillRunner implements ApplicationRunner {

    private final EpisodeRepo episodeRepo;
    private final SeasonRepo seasonRepo;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Episode> orphanEpisodes = episodeRepo.findBySeasonIsNull();
        if (orphanEpisodes.isEmpty()) {
            return;
        }

        Map<Long, Season> firstSeasonBySeriesId = new HashMap<>();

        for (Episode episode : orphanEpisodes) {
            Series series = episode.getSeries();
            if (series == null) {
                continue;
            }

            Season season = firstSeasonBySeriesId.computeIfAbsent(series.getId(), seriesId ->
                    seasonRepo.findBySeries_IdAndSeasonNumber(seriesId, 1).orElseGet(() -> {
                        Season newSeason = new Season();
                        newSeason.setSeries(series);
                        newSeason.setSeasonNumber(1);
                        newSeason.setTitle("1-fasl");
                        return seasonRepo.save(newSeason);
                    }));

            episode.setSeason(season);
        }

        episodeRepo.saveAll(orphanEpisodes);
        log.info("Fasllarga ega bo'lmagan {} ta epizod standart faslga biriktirildi.", orphanEpisodes.size());
    }
}
