package com.futmanager.service;

import com.futmanager.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class RateLimiterService {

    private record RateEntry(Instant lastRequestTime, int requestCount, Instant windowStart) {}

    private final ConcurrentHashMap<String, RateEntry> rateMap = new ConcurrentHashMap<>();

    /**
     * Verifica cooldown mínimo entre chamadas (ex: 60 segundos entre reenvios)
     */
    public void checkCooldown(String key, int cooldownSeconds, String errorMessage) {
        Instant now = Instant.now();
        RateEntry entry = rateMap.get(key);

        if (entry != null && entry.lastRequestTime() != null) {
            long secondsPassed = java.time.Duration.between(entry.lastRequestTime(), now).getSeconds();
            if (secondsPassed < cooldownSeconds) {
                long waitSeconds = cooldownSeconds - secondsPassed;
                log.warn("Rate limit excedido para a chave {}. Aguarde {}s.", maskKey(key), waitSeconds);
                throw new BusinessException(errorMessage != null ? errorMessage :
                        String.format("Muitas solicitações em sequência. Por favor, aguarde %d segundos antes de tentar novamente.", waitSeconds));
            }
        }

        rateMap.compute(key, (k, current) -> {
            int count = (current == null) ? 1 : current.requestCount() + 1;
            Instant window = (current == null) ? now : current.windowStart();
            return new RateEntry(now, count, window);
        });
    }

    /**
     * Verifica limite de tentativas dentro de uma janela (ex: max 5 tentativas em 15 minutos)
     */
    public void checkRateLimit(String key, int maxRequests, int windowMinutes, String errorMessage) {
        Instant now = Instant.now();
        rateMap.compute(key, (k, current) -> {
            if (current == null || java.time.Duration.between(current.windowStart(), now).toMinutes() >= windowMinutes) {
                return new RateEntry(now, 1, now);
            }

            if (current.requestCount() >= maxRequests) {
                log.warn("Limite de requisições excedido para a chave {}. {} requisições na janela de {} min.",
                        maskKey(key), current.requestCount(), windowMinutes);
                throw new BusinessException(errorMessage != null ? errorMessage :
                        "Limite de solicitações excedido. Por favor, tente novamente mais tarde.");
            }

            return new RateEntry(now, current.requestCount() + 1, current.windowStart());
        });
    }

    private String maskKey(String key) {
        if (key == null) return "";
        if (key.contains("@")) {
            int atIndex = key.indexOf('@');
            if (atIndex > 2) {
                return key.substring(0, 2) + "***" + key.substring(atIndex);
            }
        }
        return key.length() > 4 ? key.substring(0, 3) + "***" : "***";
    }

    public void resetAll() {
        rateMap.clear();
    }
}
