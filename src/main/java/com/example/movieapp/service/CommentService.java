package com.example.movieapp.service;

import com.example.movieapp.dto.CommentDto;
import com.example.movieapp.dto.CommentPageDto;
import com.example.movieapp.entities.CommentReport;
import com.example.movieapp.entities.Series;
import com.example.movieapp.entities.SeriesComment;
import com.example.movieapp.entities.User;
import com.example.movieapp.enums.Role;
import com.example.movieapp.exception.CommentInvalidException;
import com.example.movieapp.exception.CommentNotFoundException;
import com.example.movieapp.exception.CommentTooFrequentException;
import com.example.movieapp.exception.SeriesNotFoundException;
import com.example.movieapp.repository.CommentReportRepo;
import com.example.movieapp.repository.SeriesCommentRepo;
import com.example.movieapp.repository.SeriesRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    public static final int MAX_LENGTH = 1000;
    private static final Duration MIN_INTERVAL = Duration.ofSeconds(20);
    // Shuncha foydalanuvchi shikoyat qilsa, izoh admin ko'rib chiqquncha avtomatik yashiriladi.
    private static final int AUTO_HIDE_REPORTS = 5;

    private final SeriesCommentRepo commentRepo;
    private final CommentReportRepo reportRepo;
    private final SeriesRepo seriesRepo;

    @Transactional(readOnly = true)
    public CommentPageDto list(Long seriesId, Long userId, int page, int size) {
        Page<SeriesComment> result = commentRepo.findBySeries_IdAndHiddenFalseOrderByCreatedAtDesc(
                seriesId, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50)));
        List<CommentDto> items = result.getContent().stream().map(c -> toDto(c, userId, false)).toList();
        return new CommentPageDto(items, result.getNumber(), result.hasNext(), result.getTotalElements());
    }

    public long count(Long seriesId) {
        return commentRepo.countBySeries_IdAndHiddenFalse(seriesId);
    }

    @Transactional
    public CommentDto add(Long seriesId, User user, String rawText) {
        String text = normalize(rawText);
        if (text.isEmpty() || text.length() > MAX_LENGTH) {
            throw new CommentInvalidException();
        }
        commentRepo.findTopByUser_IdOrderByCreatedAtDesc(user.getId())
                .filter(last -> last.getCreatedAt().plus(MIN_INTERVAL).isAfter(Instant.now()))
                .ifPresent(last -> {
                    throw new CommentTooFrequentException();
                });
        Series series = seriesRepo.findById(seriesId).orElseThrow(SeriesNotFoundException::new);
        SeriesComment saved = commentRepo.save(SeriesComment.builder()
                .series(series)
                .user(user)
                .text(text)
                .createdAt(Instant.now())
                .build());
        return toDto(saved, user.getId(), false);
    }

    @Transactional
    public void delete(Long commentId, User user) {
        SeriesComment comment = commentRepo.findById(commentId).orElseThrow(CommentNotFoundException::new);
        boolean owner = comment.getUser().getId().equals(user.getId());
        if (!owner && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("not owner");
        }
        remove(List.of(comment));
    }

    @Transactional
    public void report(Long commentId, User user) {
        SeriesComment comment = commentRepo.findById(commentId).orElseThrow(CommentNotFoundException::new);
        if (comment.getUser().getId().equals(user.getId())
                || reportRepo.existsByComment_IdAndUser_Id(commentId, user.getId())) {
            return;
        }
        reportRepo.save(CommentReport.builder().comment(comment).user(user).createdAt(Instant.now()).build());
        comment.setReportCount(comment.getReportCount() + 1);
        if (comment.getReportCount() >= AUTO_HIDE_REPORTS) {
            comment.setHidden(true);
        }
    }

    @Transactional(readOnly = true)
    public CommentPageDto adminList(Long seriesId, int page, int size) {
        Page<SeriesComment> result = commentRepo.findForAdmin(
                seriesId, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
        List<CommentDto> items = result.getContent().stream().map(c -> toDto(c, null, true)).toList();
        return new CommentPageDto(items, result.getNumber(), result.hasNext(), result.getTotalElements());
    }

    @Transactional
    public void setHidden(Long commentId, boolean hidden) {
        SeriesComment comment = commentRepo.findById(commentId).orElseThrow(CommentNotFoundException::new);
        comment.setHidden(hidden);
        if (!hidden) {
            comment.setReportCount(0);
        }
    }

    @Transactional
    public void adminDelete(Long commentId) {
        remove(List.of(commentRepo.findById(commentId).orElseThrow(CommentNotFoundException::new)));
    }

    @Transactional
    public void deleteBySeries(Long seriesId) {
        remove(commentRepo.findBySeries_Id(seriesId));
    }

    @Transactional
    public void deleteByUser(Long userId) {
        reportRepo.deleteByUserId(userId);
        remove(commentRepo.findByUser_Id(userId));
    }

    private void remove(List<SeriesComment> comments) {
        if (comments.isEmpty()) return;
        reportRepo.deleteByComments(comments);
        commentRepo.deleteAll(comments);
    }

    private static String normalize(String raw) {
        if (raw == null) return "";
        return raw.replace("\r\n", "\n").replaceAll("\n{3,}", "\n\n").strip();
    }

    private static String displayName(User u) {
        String name = u.getUsername() == null ? "" : u.getUsername().strip();
        if (name.isEmpty() || name.contains("@")) return "Foydalanuvchi";
        return name;
    }

    private static CommentDto toDto(SeriesComment c, Long userId, boolean admin) {
        return CommentDto.builder()
                .id(c.getId())
                .seriesId(c.getSeries().getId())
                .seriesTitle(admin ? c.getSeries().getTitle() : null)
                .authorName(displayName(c.getUser()))
                .authorEmail(admin ? c.getUser().getEmail() : null)
                .text(c.getText())
                .createdAt(c.getCreatedAt())
                .mine(userId != null && c.getUser().getId().equals(userId))
                .hidden(c.isHidden())
                .reportCount(c.getReportCount())
                .build();
    }
}
