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

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class BunnyStreamService {

    // https://vz-xxxxx.b-cdn.net/{videoGuid}/playlist.m3u8
    private static final Pattern VIDEO_ID_PATTERN =
            Pattern.compile("b-cdn\\.net/([0-9a-fA-F\\-]{36})/");

    private final RestTemplate restTemplate;

    @Value("${bunny.stream.library-id:}")
    private String libraryId;

    @Value("${bunny.stream.api-key:}")
    private String apiKey;

    @Value("${bunny.stream.api-url:https://video.bunnycdn.com/library}")
    private String apiUrl;

    /**
     * Bunny Stream API orqali video davomiyligini (soniyalarda) oladi.
     * Sozlamalar yo'q yoki so'rov muvaffaqiyatsiz bo'lsa, bo'sh Optional qaytaradi.
     */
    public Optional<Integer> fetchDurationSeconds(String videoUrl) {
        if (libraryId.isBlank() || apiKey.isBlank()) {
            log.warn("Bunny Stream API sozlanmagan (bunny.stream.library-id / bunny.stream.api-key yo'q)");
            return Optional.empty();
        }

        String videoId = extractVideoId(videoUrl);
        if (videoId == null) {
            log.warn("Video URL'dan Bunny video ID topilmadi: {}", videoUrl);
            return Optional.empty();
        }

        String url = apiUrl + "/" + libraryId + "/videos/" + videoId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("AccessKey", apiKey);
        headers.set("accept", "application/json");

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            Map<?, ?> body = response.getBody();
            if (body == null || body.get("length") == null) {
                log.warn("Bunny API javobida 'length' topilmadi: {}", body);
                return Optional.empty();
            }
            return Optional.of(((Number) body.get("length")).intValue());
        } catch (Exception e) {
            log.error("Bunny Stream API'ga murojaat xatosi (videoId={}): {}", videoId, e.getMessage());
            return Optional.empty();
        }
    }

    private String extractVideoId(String videoUrl) {
        if (videoUrl == null) {
            return null;
        }
        Matcher matcher = VIDEO_ID_PATTERN.matcher(videoUrl);
        return matcher.find() ? matcher.group(1) : null;
    }
}
