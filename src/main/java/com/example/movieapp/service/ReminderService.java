package com.example.movieapp.service;

import com.example.movieapp.entities.Episode;
import com.example.movieapp.entities.EpisodeReminder;
import com.example.movieapp.entities.Series;
import com.example.movieapp.entities.SeriesReminder;
import com.example.movieapp.entities.User;
import com.example.movieapp.exception.EpisodeNotFoundException;
import com.example.movieapp.exception.SeriesNotFoundException;
import com.example.movieapp.repository.EpisodeReminderRepo;
import com.example.movieapp.repository.EpisodeRepo;
import com.example.movieapp.repository.SeriesReminderRepo;
import com.example.movieapp.repository.SeriesRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderService {

    public static final String COMING_SOON = "COMING_SOON";
    public static final String PUBLISHED = "PUBLISHED";

    private final SeriesReminderRepo reminderRepo;
    private final SeriesRepo seriesRepo;
    private final UserNotificationService userNotificationService;
    private final EpisodeReminderRepo episodeReminderRepo;
    private final EpisodeRepo episodeRepo;

    public boolean isReminded(Long seriesId, Long userId) {
        return userId != null && reminderRepo.existsBySeries_IdAndUser_Id(seriesId, userId);
    }

    public Set<Long> remindedEpisodeIds(Long userId, Collection<Long> episodeIds) {
        if (userId == null || episodeIds == null || episodeIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(episodeReminderRepo.findRemindedEpisodeIds(userId, episodeIds));
    }

    // Faqat hali chiqmagan (rejalashtirilgan) qism uchun eslatma qo'yish mumkin
    @Transactional
    public boolean toggleEpisode(Long seriesId, Long episodeId, User user) {
        var existing = episodeReminderRepo.findByEpisode_IdAndUser_Id(episodeId, user.getId());
        if (existing.isPresent()) {
            episodeReminderRepo.delete(existing.get());
            return false;
        }
        Episode episode = episodeRepo.findById(episodeId).orElseThrow(EpisodeNotFoundException::new);
        if (!episode.getSeries().getId().equals(seriesId) || !episode.isScheduled()) {
            return false;
        }
        episodeReminderRepo.save(EpisodeReminder.builder().episode(episode).user(user).createdAt(Instant.now()).build());
        return true;
    }

    @Transactional
    public boolean toggle(Long seriesId, User user) {
        var existing = reminderRepo.findBySeries_IdAndUser_Id(seriesId, user.getId());
        if (existing.isPresent()) {
            reminderRepo.delete(existing.get());
            return false;
        }
        Series series = seriesRepo.findById(seriesId).orElseThrow(SeriesNotFoundException::new);
        reminderRepo.save(SeriesReminder.builder().series(series).user(user).createdAt(Instant.now()).build());
        return true;
    }

    // "Tez kunda"dan "Efirda"ga o'tganda eslatma so'raganlarning hammasiga push yuboriladi va eslatmalar tozalanadi.
    @Transactional
    public int notifyIfReleased(Series series, String previousStatus) {
        if (!COMING_SOON.equals(previousStatus) || !PUBLISHED.equals(series.getStatus())) {
            return 0;
        }
        List<SeriesReminder> reminders = reminderRepo.findBySeries_Id(series.getId());
        String title = "Efirga chiqdi!";
        String body = "«" + series.getTitle().replace("\"", "").strip() + "» serialini endi tomosha qilishingiz mumkin.";
        int sent = 0;
        for (SeriesReminder reminder : reminders) {
            try {
                userNotificationService.notifyUser(reminder.getUser(), "SERIES_RELEASED", title, body, null, series.getId());
                sent++;
            } catch (Exception e) {
                log.error("Eslatma yuborishda xatolik (user {}): {}", reminder.getUser().getId(), e.getMessage());
            }
        }
        reminderRepo.deleteBySeriesId(series.getId());
        log.info("Serial {} efirga chiqdi, {} ta foydalanuvchiga eslatma yuborildi", series.getId(), sent);
        return sent;
    }

    @Transactional
    public void deleteBySeries(Long seriesId) {
        reminderRepo.deleteBySeriesId(seriesId);
        episodeReminderRepo.deleteBySeriesId(seriesId);
    }

    @Transactional
    public void deleteByUser(Long userId) {
        reminderRepo.deleteByUserId(userId);
        episodeReminderRepo.deleteByUserId(userId);
    }
}
