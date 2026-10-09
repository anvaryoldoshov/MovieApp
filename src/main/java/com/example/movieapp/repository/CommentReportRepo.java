package com.example.movieapp.repository;

import com.example.movieapp.entities.CommentReport;
import com.example.movieapp.entities.SeriesComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface CommentReportRepo extends JpaRepository<CommentReport, Long> {

    boolean existsByComment_IdAndUser_Id(Long commentId, Long userId);

    @Modifying
    @Query("delete from CommentReport r where r.comment in :comments")
    void deleteByComments(@Param("comments") Collection<SeriesComment> comments);

    @Modifying
    @Query("delete from CommentReport r where r.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
