package com.example.movieapp.service;

import com.example.movieapp.entities.MovieAccess;
import com.example.movieapp.entities.Series;
import com.example.movieapp.entities.User;
import com.example.movieapp.repository.MovieAccessRepository;
import com.example.movieapp.repository.SeriesRepo;
import com.example.movieapp.repository.UserRepo;
import com.example.movieapp.exception.MovieAccessAlreadyExistsException;
import com.example.movieapp.exception.MovieAccessNotFoundException;
import com.example.movieapp.exception.SeriesNotFoundException;
import com.example.movieapp.exception.UserNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieAccessService {

    private final MovieAccessRepository movieAccessRepository;
    private final UserRepo userRepo;
    private final SeriesRepo seriesRepo;

    public MovieAccess giveAccess(Long userId, Long seriesId, boolean paid) {
        User user = userRepo.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        Series series = seriesRepo.findById(seriesId)
                .orElseThrow(SeriesNotFoundException::new);

        if (movieAccessRepository.existsByUserIdAndMovieId(userId, seriesId)) {
            log.error("Movie access already exists for user {} and series {}", userId, seriesId);
            throw new MovieAccessAlreadyExistsException();
        }
        MovieAccess access = new MovieAccess();
        access.setUser(user);
        access.setMovie(series);
        access.setPaid(paid);

        log.debug("Movie access has been given: User {} for Series {}", user.getUsername(), series.getTitle());
        return movieAccessRepository.save(access);
    }

    public List<Series> getUserAccessedSeries(Long userId) {
        log.debug("Getting paid series access for user {}", userId);
        LocalDate today = LocalDate.now();
        return movieAccessRepository.findByUserIdAndPaidTrue(userId)
                .stream()
                .filter(access -> access.getAccessEndDate() == null || !today.isAfter(access.getAccessEndDate()))
                .map(MovieAccess::getMovie)
                .toList();
    }

    public List<MovieAccess> getAllMovies() {
        return movieAccessRepository.findAll();
    }

    @Transactional
    public ResponseEntity<?> deleteAccess(Long userId, Long movieId) {
        Optional<MovieAccess> accessOpt = movieAccessRepository
                .findByUserIdAndMovieIdAndPaidTrue(userId, movieId);

        if (accessOpt.isEmpty()) {
            throw new MovieAccessNotFoundException();
        }

        log.debug("Movie access deleted: User {} for Movie {}", accessOpt.get().getUser().getUsername(), accessOpt.get().getMovie().getTitle());
        movieAccessRepository.delete(accessOpt.get());
        return ResponseEntity.ok("Movie access deleted for userId=" + userId + ", movieId=" + movieId);
    }

    @Transactional
    public void removeExpiredAccesses() {
        LocalDate today = LocalDate.now();
        List<MovieAccess> expired = movieAccessRepository.findByPaidTrueAndAccessEndDateIsNotNullAndAccessEndDateBefore(today);
        if (!expired.isEmpty()) {
            log.info("Removing {} expired movie accesses.", expired.size());
            movieAccessRepository.deleteAll(expired);
        }
    }

    @Transactional
    public void updateUserAccessWithSubscription(
            Long userId,
            Map<Long, Integer> seriesAccessMap,
            boolean hasSubscription,
            Integer subscriptionDays
    ) {
        User user = userRepo.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        user.setSubscription(hasSubscription);
        log.info("Updating access for user {}. Subscription: {}, Days: {}", userId, hasSubscription, subscriptionDays);

        if (hasSubscription && subscriptionDays != null && subscriptionDays > 0) {
            // Obuna Yoqilgan
            LocalDate accessEndDate = LocalDate.now().plusDays(subscriptionDays);
            user.setSubscriptionStartDate(LocalDate.now());
            user.setSubscriptionEndDate(accessEndDate);

            // Barcha seriallarga MovieAccess yozuvlarini qo'shish
            List<Series> allSeries = seriesRepo.findAll();

            for (Series series : allSeries) {
                Optional<MovieAccess> existingAccessOpt = movieAccessRepository
                        .findByUser_IdAndMovie_IdAndPaidIsTrue(userId, series.getId());

                MovieAccess access = existingAccessOpt.orElseGet(MovieAccess::new);

                access.setUser(user);
                access.setMovie(series);
                access.setPaid(true);
                access.setAccessEndDate(accessEndDate); // Obuna muddatini belgilash

                movieAccessRepository.save(access);
                log.debug("Added/Updated access for series {} (Subscription, End Date: {})", series.getTitle(), accessEndDate);
            }

        } else {
            // Obuna O'chirilgan
            user.setSubscriptionStartDate(null);
            user.setSubscriptionEndDate(null);

            List<MovieAccess> existingPaidAccesses = movieAccessRepository.findByUserAndPaidIsTrue(user);
            Set<Long> updatedSeriesIds = seriesAccessMap.keySet();
            LocalDate now = LocalDate.now();

            // A. Individual Access yaratish/yangilash
            for (Map.Entry<Long, Integer> entry : seriesAccessMap.entrySet()) {
                Long seriesId = entry.getKey();
                Integer days = entry.getValue();

                if (days == null || days <= 0) continue;

                Series series = seriesRepo.findById(seriesId)
                        .orElseThrow(SeriesNotFoundException::new);

                Optional<MovieAccess> existingAccess = existingPaidAccesses.stream()
                        .filter(ma -> ma.getMovie().getId().equals(seriesId))
                        .findFirst();

                MovieAccess access = existingAccess.orElseGet(MovieAccess::new);

                LocalDate newEndDate = now.plusDays(days);

                access.setUser(user);
                access.setMovie(series);
                access.setPaid(true);
                access.setAccessEndDate(newEndDate);

                movieAccessRepository.save(access);
                log.debug("Individual access updated for series {}. End Date: {}", series.getTitle(), newEndDate);
            }

            // B. O'chirish
            existingPaidAccesses.stream()
                    .filter(ma -> !updatedSeriesIds.contains(ma.getMovie().getId()))
                    .forEach(access -> {
                        movieAccessRepository.delete(access);
                        log.debug("Deleted expired/unselected individual access for series {}", access.getMovie().getTitle());
                    });
        }

        userRepo.save(user); // Obuna o'rnatilsa ham, o'chirilmasa ham User obyekti saqlanadi
    }


    @Transactional
    public void grantPaidAccess(Long userId, Long seriesId, int accessDays) {
        User user = userRepo.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        Series series = seriesRepo.findById(seriesId)
                .orElseThrow(SeriesNotFoundException::new);

        LocalDate endDate = LocalDate.now().plusDays(accessDays);

        MovieAccess access = movieAccessRepository
                .findByUser_IdAndMovie_IdAndPaidIsTrue(userId, seriesId)
                .orElseGet(MovieAccess::new);

        access.setUser(user);
        access.setMovie(series);
        access.setPaid(true);
        access.setAccessEndDate(endDate);
        movieAccessRepository.save(access);

        user.setSubscription(true);
        if (user.getSubscriptionStartDate() == null) {
            user.setSubscriptionStartDate(LocalDate.now());
        }
        if (user.getSubscriptionEndDate() == null || user.getSubscriptionEndDate().isBefore(endDate)) {
            user.setSubscriptionEndDate(endDate);
        }
        userRepo.save(user);

        log.info("Pullik kirish berildi: user={}, series={}, kunlar={}", userId, seriesId, accessDays);
    }

    public boolean canUserWatchMovie(Long userId, Long serialId) {
        Optional<MovieAccess> paidAccess = movieAccessRepository.findByUser_IdAndMovie_IdAndPaidIsTrue(userId, serialId);
        if (paidAccess.isPresent()) {
            LocalDate endDate = paidAccess.get().getAccessEndDate();
            if (endDate == null || !LocalDate.now().isAfter(endDate)) {
                log.debug("Access granted for user {} via paid access to serial {}.", userId, serialId);
                return true;
            }
        }

        Optional<MovieAccess> freeAccess = movieAccessRepository.findByUser_IdAndMovie_IdAndPaidIsFalse(userId, serialId);
        if (freeAccess.isPresent()) {
            log.debug("Access granted for user {} via free access to serial {}.", userId, serialId);
            return true;
        }

        log.debug("Access denied for user {} to serial {}.", userId, serialId);
        return false;
    }
}