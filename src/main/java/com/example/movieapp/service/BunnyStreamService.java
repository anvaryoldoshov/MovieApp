package com.example.movieapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class BunnyStreamService {

    // https://vz-xxxxx.b-cdn.net/{videoGuid}/playlist.m3u8
    private static final Pattern VIDEO_URL_PATTERN =
            Pattern.compile("(https?://[^/]+)/([0-9a-fA-F\\-]{36})/");

    private final RestTemplate restTemplate;

    @Value("${bunny.stream.library-id:}")
    private String libraryId;

    @Value("${bunny.stream.api-key:}")
    private String apiKey;

    @Value("${bunny.stream.api-url:https://video.bunnycdn.com/library}")
    private String apiUrl;

    public record BunnyVideoInfo(int durationSeconds, long sizeBytes, String downloadUrl) {
    }

    /**
     * Bunny Stream API orqali video haqida to'liq ma'lumot oladi: davomiylik (soniya),
     * hajm (bayt) va eng yuqori sifatdagi mp4 fayl uchun to'g'ridan-to'g'ri havola.
     * Sozlamalar yo'q yoki so'rov muvaffaqiyatsiz bo'lsa, bo'sh Optional qaytaradi.
     */
    public Optional<BunnyVideoInfo> fetchVideoInfo(String videoUrl) {
        if (libraryId.isBlank() || apiKey.isBlank()) {
            log.warn("Bunny Stream API sozlanmagan (bunny.stream.library-id / bunny.stream.api-key yo'q)");
            return Optional.empty();
        }

        Matcher matcher = videoUrl == null ? null : VIDEO_URL_PATTERN.matcher(videoUrl);
        if (matcher == null || !matcher.find()) {
            log.warn("Video URL'dan Bunny video ID topilmadi: {}", videoUrl);
            return Optional.empty();
        }
        String cdnHost = matcher.group(1);
        String videoId = matcher.group(2);

        String url = apiUrl + "/" + libraryId + "/videos/" + videoId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("AccessKey", apiKey);
        headers.set("accept", "application/json");

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            Map<?, ?> body = response.getBody();
            if (body == null || body.get("length") == null || body.get("storageSize") == null) {
                log.warn("Bunny API javobida 'length'/'storageSize' topilmadi: {}", body);
                return Optional.empty();
            }

            int durationSeconds = ((Number) body.get("length")).intValue();
            long sizeBytes = ((Number) body.get("storageSize")).longValue();
            String downloadUrl = buildDownloadUrl(cdnHost, videoId, (String) body.get("availableResolutions"));

            return Optional.of(new BunnyVideoInfo(durationSeconds, sizeBytes, downloadUrl));
        } catch (Exception e) {
            log.error("Bunny Stream API'ga murojaat xatosi (videoId={}): {}", videoId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Eng yuqori mavjud sifatdagi (masalan 1080p) to'g'ridan-to'g'ri mp4 fayl havolasini quradi.
     * Bunny Stream'da MP4 fallback yoqilgan bo'lishi kerak (play_{res}.mp4 fayllari yaratiladi).
     */
    private String buildDownloadUrl(String cdnHost, String videoId, String availableResolutions) {
        if (availableResolutions == null || availableResolutions.isBlank()) {
            return null;
        }
        String highestResolution = Arrays.stream(availableResolutions.split(","))
                .map(String::trim)
                .filter(res -> !res.isEmpty())
                .max(Comparator.comparingInt(res -> Integer.parseInt(res.replaceAll("[^0-9]", ""))))
                .orElse(null);
        if (highestResolution == null) {
            return null;
        }
        return cdnHost + "/" + videoId + "/play_" + highestResolution + ".mp4";
    }
}
