package com.example.movieapp.service;

import com.example.movieapp.entities.Episode;
import com.example.movieapp.enums.NotificationAudience;
import com.example.movieapp.repository.EpisodeReminderRepo;
import com.example.movieapp.repository.EpisodeRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rejalashtirilgan qism chiqish vaqti kelganda: eslatma so'raganlarga va (notifyBuyers bo'lsa)
 * serialning amaldagi xaridorlariga push + inbox yuboradi. Qismning ko'rinishi publishAt bo'yicha
 * so'rovning o'zida hal qilinadi, bu yerda faqat xabar yuboriladi.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EpisodeReleaseService {

    private static final String TYPE = "EPISODE_RELEASED";

    private final EpisodeRepo episodeRepo;
    private final EpisodeReminderRepo episodeReminderRepo;
    private final NotificationAudienceService notificationAudienceService;
    private final UserNotificationService userNotificationService;
    private final NotificationService notificationService;
    private final TransactionTemplate transactionTemplate;

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void releaseDueEpisodes() {
        List<Episode> due = episodeRepo.findReleasedNotNotified(Instant.now());
        for (Episode episode : due) {
            try {
                release(episode.getId());
            } catch (Exception e) {
                log.error("Qism {} chiqishi haqida xabar yuborishda xatolik: {}", episode.getId(), e.getMessage());
            }
        }
    }

    private void release(Long episodeId) {
        // Avval belgilab qo'yamiz — yuborish xato bersa ham takroriy push ketmasligi uchun
        Recipients recipients = transactionTemplate.execute(status -> {
            Episode episode = episodeRepo.findById(episodeId).orElse(null);
            if (episode == null || Boolean.TRUE.equals(episode.getReleaseNotified()) || episode.isScheduled()) {
                return null;
            }
            Long seriesId = episode.getSeries().getId();
            Set<Long> userIds = new HashSet<>(episodeReminderRepo.findUserIdsByEpisodeId(episodeId));
            if (Boolean.TRUE.equals(episode.getNotifyBuyers())) {
                userIds.addAll(notificationAudienceService.resolveUserIds(List.of(seriesId), NotificationAudience.ACTIVE));
            }
            episode.setReleaseNotified(true);
            episodeRepo.save(episode);
            episodeReminderRepo.deleteByEpisodeId(episodeId);
            return new Recipients(userIds, seriesId, title(episode), body(episode));
        });
        if (recipients == null || recipients.userIds().isEmpty()) {
            return;
        }

        userNotificationService.saveToInbox(recipients.userIds(), TYPE, recipients.title(), recipients.body(),
                null, recipients.seriesId());
        int[] result = notificationService.sendToTokens(
                notificationAudienceService.findTokens(recipients.userIds()), TYPE,
                recipients.title(), recipients.body(), null, null,
                Map.of("seriesId", recipients.seriesId().toString(), "episodeId", episodeId.toString()));
        log.info("Qism {} chiqdi: {} ta foydalanuvchi, push {} muvaffaqiyatli / {} xato",
                episodeId, recipients.userIds().size(), result[0], result[1]);
    }

    private static String title(Episode episode) {
        String series = episode.getSeries().getTitle() == null ? "" : episode.getSeries().getTitle().replace("\"", "").strip();
        return episode.getEpisodeNumber() == null
                ? "«" + series + "» — yangi qism chiqdi!"
                : "«" + series + "» — " + episode.getEpisodeNumber() + "-qism chiqdi!";
    }

    private static String body(Episode episode) {
        return episode.getTitle() == null || episode.getTitle().isBlank()
                ? "Hoziroq tomosha qiling."
                : episode.getTitle().strip() + " — hoziroq tomosha qiling.";
    }

    private record Recipients(Set<Long> userIds, Long seriesId, String title, String body) {
    }
}
