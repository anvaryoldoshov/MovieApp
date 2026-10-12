package com.example.movieapp.service;

import com.example.movieapp.dto.BannerDto;
import com.example.movieapp.dto.BannerResponseDto;
import com.example.movieapp.entities.Banner;
import com.example.movieapp.entities.Series;
import com.example.movieapp.exception.BannerNotFoundException;
import com.example.movieapp.exception.SeriesNotFoundException;
import com.example.movieapp.mapper.BannerMapper;
import com.example.movieapp.mapper.SeriesMapper;
import com.example.movieapp.repository.BannerRepo;
import com.example.movieapp.repository.EpisodeRepo;
import com.example.movieapp.repository.SeriesRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BannerService {

    private final BannerRepo bannerRepo;
    private final SeriesRepo seriesRepo;
    private final EpisodeRepo episodeRepo;
    private final SeriesMapper seriesMapper;
    private final BannerMapper bannerMapper;

    public List<BannerResponseDto> getAllBannersDto() {
        return bannerRepo.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(banner -> new BannerResponseDto(
                        banner.getId(),
                        banner.getImage(),
                        banner.getSeries() != null ? banner.getSeries().getId() : null,
                        banner.getSeries() != null ? banner.getSeries().getTitle() : "N/A",
                        banner.getSeries() != null && episodeRepo.existsBySeriesId(banner.getSeries().getId())
                ))
                .collect(Collectors.toList());
    }

    public ResponseEntity<?> createBanner(BannerDto bannerDto, Long serisId) {
        Optional<Series> byId = seriesRepo.findById(serisId);
        if (!byId.isPresent())
            throw new SeriesNotFoundException();

        bannerDto.setMovie(seriesMapper.toDto(byId.get()));
        Banner banner = bannerMapper.toBanner(bannerDto);
        banner.setSortOrder(bannerRepo.findMaxSortOrder() + 1);
        bannerRepo.save(banner);
        return ResponseEntity.ok().build();
    }


    public ResponseEntity<?> updateBanner(Long id, BannerDto bannerDto, Long serisId) {
        Series series = seriesRepo.findById(serisId)
                .orElseThrow(() -> new RuntimeException("Series not found"));

        Banner banner = bannerRepo.findById(id)
                .orElseThrow(BannerNotFoundException::new);

        banner.setSeries(series);
        banner.setImage(bannerDto.getImage());
        bannerRepo.save(banner);

        return ResponseEntity.ok().build();
    }

    public ResponseEntity<?> deleteBanner(Long id, Long serisId) {
        Optional<Series> byId = seriesRepo.findById(serisId);
        if (byId.isEmpty())
            throw new SeriesNotFoundException();
        bannerRepo.deleteById(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Adminda bannerlarni qo'lda tartiblash. orderedIds ro'yxatidagi ketma-ketlik yangi tartibni belgilaydi.
     */
    @Transactional
    public ResponseEntity<?> reorderBanners(List<Long> orderedIds) {
        Map<Long, Banner> byId = bannerRepo.findAllById(orderedIds).stream()
                .collect(Collectors.toMap(Banner::getId, b -> b));

        for (int i = 0; i < orderedIds.size(); i++) {
            Banner banner = byId.get(orderedIds.get(i));
            if (banner != null) {
                banner.setSortOrder(i);
            }
        }

        bannerRepo.saveAll(byId.values());
        return ResponseEntity.ok().build();
    }

    public List<Banner> getAllBanners() {
        return bannerRepo.findAll();
    }
}
