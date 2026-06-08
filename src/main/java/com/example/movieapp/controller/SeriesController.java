package com.example.movieapp.controller;

import com.example.movieapp.dto.SeriesDto;
import com.example.movieapp.entities.Series;
import com.example.movieapp.entities.User;
import com.example.movieapp.repository.BannerRepo;
import com.example.movieapp.repository.SeriesRepo;
import com.example.movieapp.repository.UserRepo;
import com.example.movieapp.service.EpisodeService;
import com.example.movieapp.service.FileStorageService;
import com.example.movieapp.service.MovieAccessService;
import com.example.movieapp.service.SeriesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/series")

public class SeriesController {
    private final SeriesService seriesService;
    private final FileStorageService fileStorageService;
    private final BannerRepo bannerRepo;
    private final SeriesRepo seriesRepo;
    private final UserRepo userRepo;
    private final MovieAccessService movieAccessService;
    private final EpisodeService episodeService;

    @GetMapping("/all")
    public ResponseEntity<List<SeriesDto>> series() {
        return seriesService.findAll();
    }

    @GetMapping("/{serialId}/episode/{episodeId}") // EpisodeController dagi getEpisode metodi SeriesControllerga ko'chirildi
    public ResponseEntity<?> getEpisode(@PathVariable Long serialId, @PathVariable Long episodeId, Authentication authentication) {
        String email = authentication.getName();
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean canWatch = movieAccessService.canUserWatchMovie(user.getId(), serialId);

        if (!canWatch) {
            // notificationService.sendNotification(user.getUserId(), "Serial ko'rish huquqi yo'q", "Sizning obunangiz tugagan yoki bu serial uchun ruxsat berilmagan.");

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Siz bu serialni ko'ra olmaysiz. Obuna yoki individual kirish huquqi tugagan/berilmagan."));
        }

        return ResponseEntity.ok(episodeService.getEpisodeById(serialId, episodeId));
//        return ResponseEntity.ok("Episode content will be here (After successful access check)");
    }

    @GetMapping("/{serialId}")
    public ResponseEntity<?> getDetails(@PathVariable Long serialId, Authentication authentication) {
        String email = authentication.getName();
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean canWatch = movieAccessService.canUserWatchMovie(user.getId(), serialId);

        if (!canWatch) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Siz bu serial ni ko'ra olmaysiz");
        }
        return ResponseEntity.ok(seriesService.getDetails(serialId));
    }


    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/add")
    public ResponseEntity<?> createSeries(@RequestParam("title") String title,
                                          @RequestParam("status") String status,
                                          @RequestParam("image") MultipartFile image) {
        String imagePath = fileStorageService.saveImage("series", image);

        SeriesDto dto = new SeriesDto();
        dto.setTitle(title);
        dto.setStatus(status);
        dto.setImagePath(imagePath);

        return seriesService.saveSeries(dto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateSeries(@PathVariable Long id,
                                          @RequestParam("title") String title,
                                          @RequestParam("status") String status,
                                          @RequestParam(value = "image", required = false) MultipartFile image) {

        Optional<Series> existing = seriesRepo.findById(id);
        if (existing.isEmpty()) return ResponseEntity.notFound().build();

        SeriesDto dto = new SeriesDto();
        dto.setTitle(title);
        dto.setStatus(status);

        if (image != null && !image.isEmpty()) {
            String imagePath = fileStorageService.saveImage("series", image);
            dto.setImagePath(imagePath);
        } else {

            dto.setImagePath(existing.get().getImagePath());
        }

        return seriesService.updateSeries(id, dto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteSeries(@PathVariable Long id) {
        return seriesService.deleteSeries(id);
    }

}
