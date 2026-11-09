package com.example.movieapp.repository;

import com.example.movieapp.entities.BotSeriesMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BotSeriesMappingRepository extends JpaRepository<BotSeriesMapping, Long> {
    Optional<BotSeriesMapping> findByBotSeriesId(Long botSeriesId);

    List<BotSeriesMapping> findAllByOrderByBotSeriesIdAsc();
}
