package com.example.movieapp.controller;

import com.example.movieapp.dto.BannerDto;
import com.example.movieapp.dto.BannerResponseDto;
import com.example.movieapp.entities.Banner;
import com.example.movieapp.repository.BannerRepo;
import com.example.movieapp.service.BannerService;
import com.example.movieapp.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/banners")
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;
    private final FileStorageService fileStorageService;
    private final BannerRepo bannerRepo;

    @GetMapping("/all")
    public ResponseEntity<?> getAllBanners() {
        List<Banner> banners = bannerRepo.findAll();
        log.debug("Banner count: {}", banners.size());
        banners.forEach(b -> System.out.println("ID: " + b.getId() + ", Image: " + b.getImage()));
        return ResponseEntity.ok(banners);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{seriesId}")
    public ResponseEntity<?> createBanner(@RequestParam("seriesId") Long seriesId,
                                          @RequestParam("image") MultipartFile image) {
        try {
            String imagePath = fileStorageService.saveImage("banners", image);
            BannerDto bannerDto = new BannerDto();
            bannerDto.setImage(imagePath);
            return bannerService.createBanner(bannerDto, seriesId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/{seriesId}")
    public ResponseEntity<?> updateBanner(@PathVariable Long id,
                                          @RequestParam("seriesId") Long seriesId,
                                          @RequestParam(value = "image", required = false) MultipartFile image,
                                          @RequestParam(value = "imageUrl", required = false) String imageUrl) {
        try {
            BannerDto bannerDto = new BannerDto();
            if (image != null && !image.isEmpty()) {
                String imagePath = fileStorageService.saveImage("banners", image);
                bannerDto.setImage(imagePath);
            } else {
                bannerDto.setImage(imageUrl);
            }
            return bannerService.updateBanner(id, bannerDto, seriesId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/all-banners")
    public List<BannerResponseDto> getAllBannersDto() {
        return bannerService.getAllBannersDto();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}/{seriesId}")
    public ResponseEntity<?> deleteBanner(@PathVariable Long id, @PathVariable Long seriesId) {
        try {
            return bannerService.deleteBanner(id, seriesId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
