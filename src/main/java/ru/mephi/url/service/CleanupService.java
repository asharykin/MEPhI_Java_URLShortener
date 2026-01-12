package ru.mephi.url.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mephi.url.model.ShortURL;
import ru.mephi.url.repository.ShortURLRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CleanupService {
    private final ShortURLRepository urlRepository;

    @Scheduled(cron = "0 0 * * * *") // В начале каждого часа
    @Transactional
    public void cleanupExpiredUrls() {
        LocalDateTime now = LocalDateTime.now();
        List<ShortURL> expiredUrls = urlRepository.findExpiredUrls(now);

        for (ShortURL url : expiredUrls) {
            url.setDeleted(true);
            sendExpirationNotification(url);
        }
    }

    private void sendExpirationNotification(ShortURL url) {
        log.info("Пользователь с ID {} - время жизни ({} ч) ссылки '{}' (перенаправление на {}) истекло.",
                url.getCreator().getId(),
                url.getTtlHours(),
                url.getShortUrl(),
                url.getLongUrl());
    }
}
