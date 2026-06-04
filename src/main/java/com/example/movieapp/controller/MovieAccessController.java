package com.example.movieapp.controller;

import com.example.movieapp.dto.SeriesDto;
import com.example.movieapp.dto.UserAccessUpdateRequest;
import com.example.movieapp.entities.MovieAccess;
import com.example.movieapp.entities.Series;
import com.example.movieapp.mapper.SeriesMapper;
import com.example.movieapp.service.MovieAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/access")
@RequiredArgsConstructor
public class MovieAccessController {

    private final MovieAccessService movieAccessService;
    private final SeriesMapper seriesMapper;


    @PostMapping("")
    public ResponseEntity<MovieAccess> giveAccessMovie(
            @RequestParam Long userId,
            @RequestParam Long seriesId,
            @RequestParam(defaultValue = "false") boolean paid
    ) {
        MovieAccess access = movieAccessService.giveAccess(userId, seriesId, paid);
        return ResponseEntity.ok(access);
    }

    @GetMapping()
    public ResponseEntity<?> getAllMovies() {
        return ResponseEntity.of(Optional.ofNullable(movieAccessService.getAllMovies()));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Series>> getUserAccessedSeries(@PathVariable Long userId) {
        List<Series> seriesIds = movieAccessService.getUserAccessedSeries(userId);
        return ResponseEntity.ok(seriesIds);
    }

    @DeleteMapping()
    public ResponseEntity<?> deleteAccessMovie(@RequestParam Long userId, @RequestParam Long movieId) {
        return movieAccessService.deleteAccess(userId, movieId);
    }

    @PutMapping("/user/{userId}")
    public ResponseEntity<?> updateUserAccess(
            @PathVariable Long userId,
            @RequestBody UserAccessUpdateRequest request
    ) {

        Map<Long, Integer> seriesAccessMap = request.getSeriesAccessMap() != null
                ? request.getSeriesAccessMap()
                : new HashMap<>();

        Boolean hasSubscription = request.getSubscription();
        Integer subscriptionDays = request.getSubscriptionDays() != null
                ? request.getSubscriptionDays()
                : 0;


        movieAccessService.updateUserAccessWithSubscription(
                userId,
                seriesAccessMap,
                hasSubscription != null ? hasSubscription : false,
                subscriptionDays
        );

        return ResponseEntity.ok("User access updated successfully");
    }

    @GetMapping("/all-with-series")
    public ResponseEntity<Map<Long, List<SeriesDto>>> getAllUsersWithSeriesAccess() {
        List<MovieAccess> all = movieAccessService.getAllMovies();
        Map<Long, List<SeriesDto>> map = all.stream()
                .filter(MovieAccess::isPaid)
                .collect(Collectors.groupingBy(
                        ma -> ma.getUser().getId(),
                        Collectors.mapping(ma -> seriesMapper.toDto(ma.getMovie()), Collectors.toList())
                ));
        return ResponseEntity.ok(map);
    }

}
