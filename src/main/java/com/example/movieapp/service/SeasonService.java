package com.example.movieapp.service;

import com.example.movieapp.dto.SeasonDto;
import com.example.movieapp.entities.Season;
import com.example.movieapp.entities.Series;
import com.example.movieapp.exception.SeasonAlreadyExistsException;
import com.example.movieapp.exception.SeasonHasEpisodesException;
import com.example.movieapp.exception.SeasonNotFoundException;
import com.example.movieapp.exception.SeriesNotFoundException;
import com.example.movieapp.mapper.SeasonMapper;
import com.example.movieapp.repository.EpisodeRepo;
import com.example.movieapp.repository.SeasonRepo;
import com.example.movieapp.repository.SeriesRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.example.movieapp.entities.Episode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeasonService {

    private final SeasonRepo seasonRepo;
    private final SeasonMapper seasonMapper;
    private final SeriesRepo seriesRepo;
    private final EpisodeRepo episodeRepo;

    public List<SeasonDto> getSeasonsBySeries(Long seriesId) {
        return seasonMapper.toDtoList(seasonRepo.findBySeries_IdOrderBySeasonNumberAsc(seriesId));
    }

    public SeasonDto createSeason(Long seriesId, SeasonDto dto) {
        Series series = seriesRepo.findById(seriesId).orElseThrow(SeriesNotFoundException::new);

        if (seasonRepo.existsBySeries_IdAndSeasonNumber(seriesId, dto.getSeasonNumber())) {
            throw new SeasonAlreadyExistsException();
        }

        Season season = new Season();
        season.setSeries(series);
        season.setSeasonNumber(dto.getSeasonNumber());
        season.setTitle(dto.getTitle());
        season.setEpisodeCount(dto.getEpisodeCount());

        SeasonDto saved = seasonMapper.toDto(seasonRepo.save(season));
        reassignEpisodes(seriesId);
        return saved;
    }

    @Transactional
    public int reassignEpisodes(Long seriesId) {
        List<Season> seasons = seasonRepo.findBySeries_IdOrderBySeasonNumberAsc(seriesId);
        if (seasons.isEmpty()) return 0;
        List<Episode> changed = new ArrayList<>();
        for (Episode episode : episodeRepo.findBySeriesId(seriesId)) {
            Season target = seasonFor(seasons, episode.getEpisodeNumber());
            if (episode.getSeason() == null || !episode.getSeason().getId().equals(target.getId())) {
                episode.setSeason(target);
                changed.add(episode);
            }
        }
        if (!changed.isEmpty()) {
            episodeRepo.saveAll(changed);
            log.info("Fasllar qayta taqsimlandi: seriesId={}, ko'chirilgan epizodlar={}", seriesId, changed.size());
        }
        return changed.size();
    }

    @Transactional
    public int reassignAll() {
        int total = 0;
        for (Series series : seriesRepo.findAll()) {
            total += reassignEpisodes(series.getId());
        }
        return total;
    }

    static Season seasonFor(List<Season> seasons, Integer episodeNumber) {
        if (episodeNumber == null) return seasons.get(0);
        int cumulative = 0;
        for (Season season : seasons) {
            Integer capacity = season.getEpisodeCount();
            if (capacity == null || capacity <= 0) return season;
            cumulative += capacity;
            if (episodeNumber <= cumulative) return season;
        }
        return seasons.get(seasons.size() - 1);
    }

    public SeasonDto updateSeason(Long id, SeasonDto dto) {
        Season season = seasonRepo.findById(id).orElseThrow(SeasonNotFoundException::new);

        if (!season.getSeasonNumber().equals(dto.getSeasonNumber())
                && seasonRepo.existsBySeries_IdAndSeasonNumber(season.getSeries().getId(), dto.getSeasonNumber())) {
            throw new SeasonAlreadyExistsException();
        }

        season.setSeasonNumber(dto.getSeasonNumber());
        season.setTitle(dto.getTitle());
        season.setEpisodeCount(dto.getEpisodeCount());

        SeasonDto saved = seasonMapper.toDto(seasonRepo.save(season));
        reassignEpisodes(season.getSeries().getId());
        return saved;
    }

    public void deleteSeason(Long id) {
        Season season = seasonRepo.findById(id).orElseThrow(SeasonNotFoundException::new);

        if (episodeRepo.existsBySeasonId(id)) {
            throw new SeasonHasEpisodesException();
        }

        Long seriesId = season.getSeries().getId();
        seasonRepo.delete(season);
        reassignEpisodes(seriesId);
    }

}
