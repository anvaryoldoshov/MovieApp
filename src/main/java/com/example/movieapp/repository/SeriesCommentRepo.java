package com.example.movieapp.repository;

import com.example.movieapp.entities.SeriesComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SeriesCommentRepo extends JpaRepository<SeriesComment, Long> {

    @EntityGraph(attributePaths = {"user"})
    Page<SeriesComment> findBySeries_IdAndHiddenFalseOrderByCreatedAtDesc(Long seriesId, Pageable pageable);

    long countBySeries_IdAndHiddenFalse(Long seriesId);

    Optional<SeriesComment> findTopByUser_IdOrderByCreatedAtDesc(Long userId);

    // Admin: shikoyat qilinganlar tepada, keyin yangilari.
    @EntityGraph(attributePaths = {"user", "series"})
    @Query("select c from SeriesComment c where (:seriesId is null or c.series.id = :seriesId) "
            + "order by c.reportCount desc, c.createdAt desc")
    Page<SeriesComment> findForAdmin(@Param("seriesId") Long seriesId, Pageable pageable);

    List<SeriesComment> findBySeries_Id(Long seriesId);

    List<SeriesComment> findByUser_Id(Long userId);
}
