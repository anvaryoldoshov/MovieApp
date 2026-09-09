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
import java.util.List;
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

    private static final Pattern UUID_PATTERN =
            Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    // Video nomining oxiridagi raqamni ajratib oladi (masalan "ayyubiy32" -> 32).
    // Nom qismidagi imlo xatolariga (harflarga) e'tibor bermaydi - faqat oxirgi raqam muhim.
    private static final Pattern TRAILING_NUMBER_PATTERN = Pattern.compile("(\\d+)\\s*$");

    // Bunny'ga yuklashda video nomi ko'pincha original fayl nomi (kengaytmasi bilan) bo'lib
    // qoladi (masalan "Ayyubiy 32.mp4") - raqamni izlashdan oldin shu kengaytmani olib tashlaymiz,
    // aks holda "32.mp4" oxirida raqam emas, nuqta+harflar turgani uchun mos kelmay qoladi.
    private static final Pattern TRAILING_FILE_EXTENSION_PATTERN =
            Pattern.compile("(?i)\\.(mp4|mkv|mov|avi|wmv|flv|webm|m4v|ts|m3u8|mpg|mpeg)$");

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

    public record BunnyVideoInfo(int durationSeconds, long sizeBytes, String thumbnailUrl) {
    }

    public record BunnyLibraryVideo(String guid, String title) {
    }

    /**
     * Bunny Stream kutubxonasidagi (yoki collectionId berilsa, faqat shu Collection ichidagi)
     * barcha videolarni yuklangan sana bo'yicha (eskisi birinchi) ro'yxatini qaytaradi.
     * Epizodga hali biriktirilmagan videoni topish uchun ishlatiladi.
     */
    public List<BunnyLibraryVideo> listLibraryVideos(String collectionId) {
        if (libraryId.isBlank() || apiKey.isBlank()) {
            return List.of();
        }

        String url = apiUrl + "/" + libraryId + "/videos?page=1&itemsPerPage=1000&orderBy=date";
        if (collectionId != null && !collectionId.isBlank()) {
            url += "&collection=" + collectionId;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set("AccessKey", apiKey);
        headers.set("accept", "application/json");

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            Map<?, ?> body = response.getBody();
            Object items = body == null ? null : body.get("items");
            if (!(items instanceof List<?> itemList)) {
                return List.of();
            }

            return itemList.stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .map(item -> new BunnyLibraryVideo((String) item.get("guid"), (String) item.get("title")))
                    .filter(v -> v.guid() != null)
                    .toList();
        } catch (Exception e) {
            log.error("Bunny kutubxonasidagi videolar ro'yxatini olishda xatolik: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Admin Bunny Collection'ning ID'sini yoki uning dashboard havolasini kiritishi mumkin -
     * ikkalasidan ham GUID'ni ajratib oladi. GUID formatiga mos kelmasa, kiritilgan qiymatni
     * o'zgarishsiz qaytaradi (Bunny kelajakda boshqa ID formatidan foydalansa ham ishlashi uchun).
     */
    public String extractCollectionId(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String trimmed = input.trim();
        Matcher matcher = UUID_PATTERN.matcher(trimmed);
        return matcher.find() ? matcher.group() : trimmed;
    }

    /**
     * Video nomining oxiridagi raqamni epizod raqami sifatida ajratib oladi
     * (masalan "Ayyubiy 32" yoki "ayubiy32" -> 32). Nomning matn qismida imlo xatosi
     * bo'lishi mumkinligi uchun faqat raqamga tayaniladi.
     */
    public Integer extractEpisodeNumberFromTitle(String title) {
        if (title == null) {
            return null;
        }
        String cleaned = TRAILING_FILE_EXTENSION_PATTERN.matcher(title.trim()).replaceFirst("");
        Matcher matcher = TRAILING_NUMBER_PATTERN.matcher(cleaned.trim());
        if (!matcher.find()) {
            return null;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Mavjud playback URL'dan Bunny CDN bazaviy manzilini (masalan https://vz-xxxxx.b-cdn.net)
     * ajratib oladi - yangi topilgan video GUID uchun to'liq URL yasashda ishlatiladi.
     */
    public Optional<String> extractBaseUrl(String videoUrl) {
        if (videoUrl == null) {
            return Optional.empty();
        }
        Matcher matcher = VIDEO_URL_PATTERN.matcher(videoUrl);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    public String buildPlaybackUrl(String baseUrl, String videoGuid) {
        return baseUrl + "/" + videoGuid + "/playlist.m3u8";
    }

    /**
     * Video URL'dan Bunny video GUID'ini ajratib oladi (masalan mavjud epizodlar orasida
     * qaysi Bunny videolari allaqachon ishlatilganini aniqlash uchun).
     */
    public String extractVideoGuid(String videoUrl) {
        return extractVideoId(videoUrl);
    }

    /**
     * Bunny Stream API orqali video haqida ma'lumot oladi: davomiylik (soniya), hajm (bayt)
     * va Bunny avtomatik yaratgan thumbnail (muddati cheklangan token bilan imzolangan,
     * faqat bir martalik yuklab olish uchun - o'zimizning serverga saqlab qo'yiladi).
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

            String thumbnailFileName = (String) body.get("thumbnailFileName");
            String thumbnailUrl = buildThumbnailUrl(videoUrl, videoId, thumbnailFileName);

            return Optional.of(new BunnyVideoInfo(durationSeconds, sizeBytes, thumbnailUrl));
        } catch (Exception e) {
            log.error("Bunny Stream API'ga murojaat xatosi (videoId={}): {}", videoId, e.getMessage());
            return Optional.empty();
        }
    }

    private String buildThumbnailUrl(String videoUrl, String videoId, String thumbnailFileName) {
        Matcher matcher = VIDEO_URL_PATTERN.matcher(videoUrl);
        if (!matcher.find()) {
            return null;
        }
        String baseUrl = matcher.group(1);
        String fileName = (thumbnailFileName == null || thumbnailFileName.isBlank()) ? "thumbnail.jpg" : thumbnailFileName;
        return signPlaybackUrl(baseUrl + "/" + videoId + "/" + fileName);
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
