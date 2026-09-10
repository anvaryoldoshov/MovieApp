package com.example.movieapp.repository;

import com.example.movieapp.entities.WatchProgress;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WatchProgressRepo extends JpaRepository<WatchProgress, Long> {

    Optional<WatchProgress> findByUser_IdAndEpisode_Id(Long userId, Long episodeId);

    List<WatchProgress> findByUser_IdAndEpisode_IdIn(Long userId, List<Long> episodeIds);

    List<WatchProgress> findByUser_IdOrderByUpdatedAtDesc(Long userId, Pageable pageable);
}
