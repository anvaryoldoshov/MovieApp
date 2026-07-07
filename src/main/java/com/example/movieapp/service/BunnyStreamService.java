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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
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
            Pattern.compile("(https?://[^/]+)(/[0-9a-fA-F\\-]{36}/)");

    // Havola bir necha soatlik pleer sessiyasi davomida ishlashi uchun yetarli, lekin
    // taqsimlab yuborilgan holda uzoq muddat ishlamasligi uchun qisqa muddatga cheklangan.
    private static final long TOKEN_TTL_SECONDS = 4 * 60 * 60;

    private final RestTemplate restTemplate;

    @Value("${bunny.stream.library-id:}")
    private String libraryId;

    @Value("${bunny.stream.api-key:}")
    private String apiKey;

    @Value("${bunny.stream.api-url:https://video.bunnycdn.com/library}")
    private String apiUrl;

    // Bunny dashboard > Pull Zone/Stream > Security > Token Authentication'dagi maxfiy kalit.
    @Value("${bunny.stream.token-auth-key:}")
    private String tokenAuthKey;

    public record BunnyVideoInfo(int durationSeconds, long sizeBytes) {
    }

    /**
     * Bunny Stream API orqali video haqida ma'lumot oladi: davomiylik (soniya) va hajm (bayt).
     * Sozlamalar yo'q yoki so'rov muvaffaqiyatsiz bo'lsa, bo'sh Optional qaytaradi.
     */
    public Optional<BunnyVideoInfo> fetchVideoInfo(String videoUrl) {
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
            if (body == null || body.get("length") == null || body.get("storageSize") == null) {
                log.warn("Bunny API javobida 'length'/'storageSize' topilmadi: {}", body);
                return Optional.empty();
            }

            int durationSeconds = ((Number) body.get("length")).intValue();
            long sizeBytes = ((Number) body.get("storageSize")).longValue();

            if (durationSeconds <= 0 || sizeBytes <= 0) {
                log.warn("Bunny'da video hali qayta ishlanmoqda (encoding tugamagan), keyinroq backfill orqali qayta urinib ko'riladi: videoId={}", videoId);
                return Optional.empty();
            }

            return Optional.of(new BunnyVideoInfo(durationSeconds, sizeBytes));
        } catch (Exception e) {
            log.error("Bunny Stream API'ga murojaat xatosi (videoId={}): {}", videoId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * HLS playback URL'ga (m3u8) muddati cheklangan token qo'shadi, shunda foydalanuvchiga
     * berilgan havola faqat cheklangan vaqt davomida ishlaydi va taqsimlab yuborilsa ham
     * tez orada yaroqsiz bo'lib qoladi. Token butun video papkasi (guid) uchun imzolanadi,
     * shu bois playlist ichidagi .ts segmentlar ham qo'shimcha so'rovsiz ishlaydi.
     *
     * Ishlashi uchun Bunny'da (Stream kutubxonasi bog'langan Pull Zone > Security) Token
     * Authentication yoqilgan va bu yerdagi kalit bilan bir xil bo'lishi shart. Kalit
     * sozlanmagan bo'lsa, havola imzolanmasdan qaytariladi (mavjud xulq-atvor saqlanadi).
     */
    public String signPlaybackUrl(String videoUrl) {
        if (tokenAuthKey.isBlank() || videoUrl == null) {
            return videoUrl;
        }

        Matcher matcher = VIDEO_URL_PATTERN.matcher(videoUrl);
        if (!matcher.find()) {
            log.warn("Video URL formati kutilganidek emas, imzolanmadi: {}", videoUrl);
            return videoUrl;
        }
        String directoryPath = matcher.group(2); // masalan: /1742ee4f-.../

        long expires = Instant.now().getEpochSecond() + TOKEN_TTL_SECONDS;
        String hashableBase = tokenAuthKey + directoryPath + expires;
        String token = sha256Base64Url(hashableBase);

        String separator = videoUrl.contains("?") ? "&" : "?";
        return videoUrl + separator + "token=" + token + "&expires=" + expires;
    }

    private String extractVideoId(String videoUrl) {
        if (videoUrl == null) {
            return null;
        }
        Matcher matcher = VIDEO_URL_PATTERN.matcher(videoUrl);
        return matcher.find() ? matcher.group(2).replace("/", "") : null;
    }

    private String sha256Base64Url(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash)
                    .replace("+", "-")
                    .replace("/", "_")
                    .replace("=", "");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 mavjud emas", e);
        }
    }
}
