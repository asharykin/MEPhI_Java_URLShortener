package ru.mephi.url.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mephi.url.dto.ShortURLCreateDto;
import ru.mephi.url.dto.ShortURLResponseDto;
import ru.mephi.url.dto.ShortURLUpdateDto;
import ru.mephi.url.mapper.ShortURLMapper;
import ru.mephi.url.model.ShortURL;
import ru.mephi.url.model.User;
import ru.mephi.url.repository.ShortURLRepository;
import ru.mephi.url.validator.ShortURLValidator;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShortURLService {
    private final ShortURLRepository urlRepository;
    private final ShortURLMapper urlMapper;
    private final ShortURLValidator urlValidator;
    private final UserService userService;

    @Transactional
    public ShortURLResponseDto createShortUrl(UUID userId, ShortURLCreateDto requestDto) {
        User user = (userId != null) ? userService.getUserById(userId) : userService.createUser();

        urlValidator.checkUniqueLongUrlForUser(user, requestDto.getLongUrl());

        String shortUrl = generateUniqueShortUrl();
        ShortURL url = urlMapper.requestDtoToEntity(requestDto, user, shortUrl);
        urlRepository.save(url);

        return urlMapper.entityToResponseDto(url);
    }

    @Transactional
    public String getLongUrl(String shortUrl) {
        ShortURL url = urlRepository.findByShortUrl(shortUrl)
                .orElseThrow(() -> new EntityNotFoundException("Короткая ссылка '" + shortUrl + "' не найдена"));

        urlValidator.validateAccess(url);

        url.setUseCount(url.getUseCount() + 1);
        urlRepository.save(url);

        if (url.getUseCount().equals(url.getUseLimit())) {
            sendLimitReachedNotification(url);
        }

        return url.getLongUrl();
    }

    @Transactional
    public ShortURLResponseDto updateShortUrl(String shortUrl, UUID userId, ShortURLUpdateDto requestDto) {
        ShortURL url = urlRepository.findByShortUrl(shortUrl)
                .orElseThrow(() -> new EntityNotFoundException("Короткая ссылка '" + shortUrl + "' не найдена"));

        urlValidator.checkOwnership(url, userId);
        urlValidator.validateUpdate(url, requestDto);

        urlMapper.updateEntityFromRequestDto(url, requestDto);
        urlRepository.save(url);

        return urlMapper.entityToResponseDto(url);
    }

    @Transactional
    public void deleteShortUrl(String shortUrl, UUID userId) {
        ShortURL url = urlRepository.findByShortUrl(shortUrl)
                .orElseThrow(() -> new EntityNotFoundException("Короткая ссылка '" + shortUrl + "' не найдена"));

        urlValidator.checkOwnership(url, userId);

        urlRepository.delete(url);
    }

    private String generateUniqueShortUrl() {
        String code; // Классический вариант для коротких ссылок, 6 случайных символов из кодировки Base62
        do {
            code = RandomStringUtils.randomAlphanumeric(6);
        } while (urlRepository.existsByShortUrl(code)); // На случай коллизии (очень маловероятно)
        return code;
    }

    private void sendLimitReachedNotification(ShortURL url) {
        log.info("Пользователь с ID {} - лимит переходов ({}) по ссылке '{}' (перенаправление на {}) был исчерпан. " +
                "Следующие попытки перехода будут заблокированы.",
                url.getCreator().getId(),
                url.getUseLimit(),
                url.getShortUrl(),
                url.getLongUrl());
    }
}
