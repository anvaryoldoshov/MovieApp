package com.example.movieapp.controller;

import com.example.movieapp.dto.EpisodeDto;
import com.example.movieapp.entities.Episode;
import com.example.movieapp.mapper.EpisodeMapper;
import com.example.movieapp.service.EpisodeService;
import com.example.movieapp.service.FileStorageService;
import com.example.movieapp.service.SeriesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/admin/series")
@RequiredArgsConstructor
public class AdminEpisodeController {

    private final EpisodeService episodeService;
    private final EpisodeMapper episodeMapper;
    private final SeriesService seriesService;
    private final FileStorageService fileStorageService;


    @GetMapping("/{seriesId}/episodes")
    public ResponseEntity<List<EpisodeDto>> getEpisodesBySeries(@PathVariable Long seriesId) {
        List<EpisodeDto> episodes = episodeService.getEpisodesBySeries(seriesId);
        return ResponseEntity.ok(episodes);
    }

    @PostMapping("/{seriesId}/episodes")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addEpisodeToSeries(
            @PathVariable Long seriesId,
            @RequestParam("title") String title,
            @RequestParam("videoUrl") String videoUrl,
            @RequestParam("episodeNumber") Integer episodeNumber,
            @RequestParam(value = "durationHours", required = false) Integer durationHours,
            @RequestParam(value = "durationMinutes", required = false) Integer durationMinutes,
            @RequestParam(value = "durationSeconds", required = false) Integer durationSeconds,
            @RequestParam("image") MultipartFile image
    ) {
        // Simple validation
        if (title == null || title.isBlank() || videoUrl == null || videoUrl.isBlank() || image.isEmpty()) {
            return ResponseEntity.badRequest().body("Missing required fields");
        }

        // Save image
        String imagePath = fileStorageService.saveImage("episodes", image);
        String savedFileName = Paths.get(imagePath).getFileName().toString();


        // Create DTO
        EpisodeDto episodeDto = new EpisodeDto();
        episodeDto.setSeriesId(seriesId);
        episodeDto.setTitle(title);
        episodeDto.setEpisodeNumber(episodeNumber);
        episodeDto.setDurationHours(durationHours);
        episodeDto.setDurationMinutes(durationMinutes);
        episodeDto.setDurationSeconds(durationSeconds);
        episodeDto.setThumbnail(imagePath);
        episodeDto.setVideoUrl(videoUrl);
        episodeDto.setFileName(title);

        // Save and return
        Episode saved = episodeService.addEpisode(seriesId, episodeDto);
        return ResponseEntity.ok(episodeMapper.toEpisodeDto(saved));
    }



//    @PostMapping("/add-episode")
//    public ResponseEntity<EpisodeDto> addEpisode(@RequestBody EpisodeDto episodeDto) {
//
//        Episode episodeEntity = episodeMapper.toEpisodeEntity(episodeDto);
//
//        episodeService.addEpisode(episodeDto.getSeriesId(),episodeDto);
//
//        return ResponseEntity.ok(episodeMapper.toEpisodeDto(episodeEntity));
//    }

//    @PostMapping("/add-series")
//    public ResponseEntity<Map<String, Object>> addSeries(@RequestBody SeriesDto seriesDto) {
//        return seriesService.saveSeries(seriesDto);
//    }

    @PutMapping(value = "/episodes/{episodeId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateEpisode(
            @PathVariable Long episodeId,
            @RequestParam("title") String title,
            @RequestParam("episodeNumber") Integer episodeNumber,
            @RequestParam("videoUrl") String videoUrl,
            @RequestParam(value = "durationHours", required = false) Integer durationHours,
            @RequestParam(value = "durationMinutes", required = false) Integer durationMinutes,
            @RequestParam(value = "durationSeconds", required = false) Integer durationSeconds,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) {
        String imagePath = null;
        if (image != null && !image.isEmpty()) {
            imagePath = fileStorageService.saveImage("episodes", image);
        }

        EpisodeDto episodeDto = new EpisodeDto();
        episodeDto.setTitle(title);
        episodeDto.setEpisodeNumber(episodeNumber);
        episodeDto.setVideoUrl(videoUrl);
        episodeDto.setDurationHours(durationHours);
        episodeDto.setDurationMinutes(durationMinutes);
        episodeDto.setDurationSeconds(durationSeconds);

        if (imagePath != null) {
            episodeDto.setThumbnail(imagePath);
        }
        return episodeService.updateEpisode(episodeId, episodeDto);
    }


    @DeleteMapping("/episodes/{episodeId}")
    public ResponseEntity<?> deleteEpisode(@PathVariable Long episodeId) {
        return episodeService.deleteEpisode(episodeId);
    }



}
