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

import java.util.List;

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

        return seasonMapper.toDto(seasonRepo.save(season));
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

        return seasonMapper.toDto(seasonRepo.save(season));
    }

    public void deleteSeason(Long id) {
        Season season = seasonRepo.findById(id).orElseThrow(SeasonNotFoundException::new);

        if (episodeRepo.existsBySeasonId(id)) {
            throw new SeasonHasEpisodesException();
        }

        seasonRepo.delete(season);
    }

}
