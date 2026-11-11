package com.example.movieapp.service;

import com.example.movieapp.entities.MovieAccess;
import com.example.movieapp.entities.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AccountMergeService {

    private static final Logger log = LoggerFactory.getLogger(AccountMergeService.class);

    private static final String[][] UNIQUE_CHILDREN = {
            {"SeriesLike", "series"},
            {"SeriesReminder", "series"},
            {"EpisodeReminder", "episode"},
            {"CommentReport", "comment"},
            {"WatchProgress", "episode"},
    };

    private static final String[] PLAIN_CHILDREN = {
            "Payment", "BotPurchase", "SeriesComment", "UserNotification",
    };

    @PersistenceContext
    private EntityManager em;

    public static boolean isTelegramOnly(User user) {
        return user != null && user.getEmail() != null
                && user.getEmail().endsWith(TelegramLoginService.FAKE_EMAIL_DOMAIN);
    }

    @Transactional
    public User merge(Long sourceId, Long targetId) {
        if (sourceId.equals(targetId)) {
            return em.find(User.class, targetId);
        }
        em.flush();
        User source = em.find(User.class, sourceId);
        User target = em.find(User.class, targetId);
        if (source == null || target == null) {
            throw new IllegalArgumentException("Foydalanuvchi topilmadi");
        }

        for (String[] child : UNIQUE_CHILDREN) {
            String entity = child[0];
            String key = child[1];
            em.createQuery("delete from " + entity + " x where x.user = :s and exists "
                            + "(select 1 from " + entity + " y where y.user = :t and y." + key + " = x." + key + ")")
                    .setParameter("s", source).setParameter("t", target).executeUpdate();
            em.createQuery("update " + entity + " x set x.user = :t where x.user = :s")
                    .setParameter("s", source).setParameter("t", target).executeUpdate();
        }
        for (String entity : PLAIN_CHILDREN) {
            em.createQuery("update " + entity + " x set x.user = :t where x.user = :s")
                    .setParameter("s", source).setParameter("t", target).executeUpdate();
        }

        mergeAccess(source, target);

        em.createQuery("delete from RefreshToken x where x.user = :s").setParameter("s", source).executeUpdate();
        em.createQuery("delete from UserDevice x where x.user = :s").setParameter("s", source).executeUpdate();

        em.flush();
        em.clear();
        source = em.find(User.class, sourceId);
        target = em.find(User.class, targetId);

        if (isLater(source.getSubscriptionEndDate(), target.getSubscriptionEndDate())) {
            target.setSubscription(source.getSubscription());
            target.setSubscriptionStartDate(source.getSubscriptionStartDate());
            target.setSubscriptionEndDate(source.getSubscriptionEndDate());
        }

        Long chatId = source.getTelegramChatId();
        String tgUsername = source.getTelegramUsername();
        if (chatId != null && target.getTelegramChatId() == null) {
            source.setTelegramChatId(null);
            em.flush();
            target.setTelegramChatId(chatId);
            target.setTelegramUsername(tgUsername);
        }

        em.remove(source);
        em.flush();
        log.info("Akkauntlar birlashtirildi: source={} ({}) -> target={} ({})",
                sourceId, source.getEmail(), targetId, target.getEmail());
        return target;
    }

    private void mergeAccess(User source, User target) {
        List<MovieAccess> sourceAccess = em.createQuery(
                        "select a from MovieAccess a where a.user = :s", MovieAccess.class)
                .setParameter("s", source).getResultList();
        for (MovieAccess a : sourceAccess) {
            List<MovieAccess> existing = em.createQuery(
                            "select a from MovieAccess a where a.user = :t and a.movie = :m and a.paid = :p", MovieAccess.class)
                    .setParameter("t", target).setParameter("m", a.getMovie()).setParameter("p", a.isPaid())
                    .getResultList();
            if (existing.isEmpty()) {
                a.setUser(target);
                continue;
            }
            MovieAccess keep = existing.get(0);
            if (isLater(a.getAccessEndDate(), keep.getAccessEndDate())) {
                keep.setAccessEndDate(a.getAccessEndDate());
                keep.setReminderSent(false);
            }
            em.remove(a);
        }
        em.flush();
    }

    private static boolean isLater(LocalDate a, LocalDate b) {
        return a != null && (b == null || a.isAfter(b));
    }
}
