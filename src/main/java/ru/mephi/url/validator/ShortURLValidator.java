package ru.mephi.url.validator;

import jakarta.persistence.EntityExistsException;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import ru.mephi.url.dto.ShortURLUpdateDto;
import ru.mephi.url.model.ShortURL;
import ru.mephi.url.model.User;
import ru.mephi.url.repository.ShortURLRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShortURLValidator {
    private final ShortURLRepository urlRepository;

    public void validateAccess(ShortURL url) {
        checkExpiration(url);
        checkUseLimit(url);
    }

    public void validateUpdate(ShortURL url, ShortURLUpdateDto requestDto) {
        if (requestDto.getTtlHours() != null) {
            checkNewExpiration(url, requestDto.getTtlHours());
        }

        if (requestDto.getUseLimit() != null) {
            checkNewUseLimit(url, requestDto.getUseLimit());
        }
    }

    public void checkOwnership(ShortURL url, UUID userId) {
        if (!url.getCreator().getId().equals(userId)) {
            throw new AccessDeniedException("У пользователя с ID " + userId + " нет прав на управление этой ссылкой");
        }
    }

    public void checkExpiration(ShortURL url) {
        LocalDateTime expirationTime = url.getCreatedAt().plusHours(url.getTtlHours());
        if (LocalDateTime.now().isAfter(expirationTime)) {
            throw new ValidationException("Время жизни ссылки '" + url.getShortUrl() + "' истекло");
        }
    }

    public void checkUseLimit(ShortURL url) {
        if (url.getUseCount() >= url.getUseLimit()) {
            throw new ValidationException("Лимит переходов по ссылке '" + url.getShortUrl() + "' исчерпан");
        }
    }

    public void checkNewUseLimit(ShortURL url, Integer newUseLimit) {
        if (newUseLimit < url.getUseCount()) {
            throw new ValidationException(
                    "Новый лимит не может быть меньше текущего количества переходов (" + url.getUseCount() + ")"
            );
        }
    }

    public void checkNewExpiration(ShortURL url, Integer newTtlHours) {
        long hoursPassed = ChronoUnit.HOURS.between(url.getCreatedAt(), LocalDateTime.now());
        if (newTtlHours < hoursPassed) {
            throw new ValidationException(
                    "Новое TTL не может быть меньше времени, уже прошедшего с момента создания (" + hoursPassed + " ч)"
            );
        }
    }

    public void checkUniqueLongUrlForUser(User user, String longUrl) {
        if (urlRepository.existsByLongUrlForUser(user, longUrl)) {
            throw new EntityExistsException(
                    "У пользователя с ID " + user.getId() + " уже есть активная короткая ссылка, " +
                            "перенаправляющая на " + longUrl
            );
        }
    }
}
