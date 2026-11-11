package com.example.movieapp.service;

import com.example.movieapp.dto.TelegramConfirmRequest;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BotSubscriptionFetcher {

    private static final Logger log = LoggerFactory.getLogger(BotSubscriptionFetcher.class);

    private final BotPurchaseService botPurchaseService;
    private final RestTemplate http = buildClient();

    @Value("${telegram.bot-api.user-info-url:}")
    private String userInfoUrl;

    @Value("${telegram.bot-api.key:}")
    private String apiKey;

    public int sync(Long chatId) {
        if (chatId == null) return 0;
        if (userInfoUrl == null || userInfoUrl.isBlank() || apiKey == null || apiKey.isBlank()) {
            log.warn("telegram.bot-api.user-info-url / key sozlanmagan, bot obunalari olinmadi");
            return 0;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set("X-API-Key", apiKey);
            headers.set(HttpHeaders.ACCEPT_ENCODING, "identity");

            ResponseEntity<byte[]> res = http.exchange(
                    userInfoUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(Map.of("telegram_id", chatId), headers),
                    byte[].class);

            String raw = HttpBodies.decode(res.getBody());
            UserInfoResponse body;
            try {
                body = raw == null || raw.isBlank() ? null : MAPPER.readValue(raw.trim(), UserInfoResponse.class);
            } catch (Exception parseError) {
                log.warn("Bot obunalari: javob JSON emas, chatId={}, status={}, body={}",
                        chatId, res.getStatusCode().value(), snippet(raw));
                return 0;
            }
            if (body == null || !Boolean.TRUE.equals(body.getOk())) {
                log.info("Bot obunalari: ok=false, chatId={}, status={}, body={}",
                        chatId, res.getStatusCode().value(), snippet(raw));
                return 0;
            }
            if (body.getTelegramId() != null && !body.getTelegramId().equals(chatId)) {
                log.warn("Bot boshqa telegram_id qaytardi: so'ralgan={}, kelgan={}", chatId, body.getTelegramId());
                return 0;
            }

            int saved = botPurchaseService.receiveLoginSubscriptions(chatId, body.getSubscriptions());
            log.info("Bot obunalari olindi: chatId={}, kelgan={}, yangi={}",
                    chatId, body.getSubscriptions() == null ? 0 : body.getSubscriptions().size(), saved);
            return saved;
        } catch (Exception e) {
            log.warn("Bot obunalarini olishda xato: chatId={}, {}", chatId, e.getMessage());
            return 0;
        }
    }

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static String snippet(String raw) {
        return HttpBodies.printable(raw, 500);
    }

    private static RestTemplate buildClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(4_000);
        factory.setReadTimeout(6_000);
        RestTemplate rt = new RestTemplate(factory);
        rt.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }
        });
        return rt;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserInfoResponse {
        @JsonProperty("ok")
        private Boolean ok;

        @JsonProperty("Ok")
        private Boolean okCapital;

        @JsonProperty("telegram_id")
        private Long telegramId;

        private List<TelegramConfirmRequest.Subscription> subscriptions;

        public Boolean getOk() {
            return ok != null ? ok : okCapital;
        }
    }
}
