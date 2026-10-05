package com.example.movieapp.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.example.movieapp.entities.Series;

public interface SeriesRepo extends JpaRepository<Series, Long> {

    List<Series> findByGenres_Id(Long genreId);

    List<Series> findAllByOrderBySortOrderAscIdAsc();

    @Query("SELECT COALESCE(MAX(s.sortOrder), -1) FROM Series s")
    int findMaxSortOrder();

    List<Series> findAllBySubscriptionBasedTrue();

}
