package ru.mephi.url.validator;

import jakarta.persistence.EntityExistsException;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import ru.mephi.url.dto.ShortURLUpdateDto;
import ru.mephi.url.model.ShortURL;
import ru.mephi.url.model.User;
import ru.mephi.url.repository.ShortURLRepository;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShortURLValidatorTest {

    @Mock
    private ShortURLRepository repository;

    @InjectMocks
    private ShortURLValidator validator;

    @Test
    @DisplayName("Успешная проверка владения при совпадении UUID")
    void checkOwnership_Success() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        ShortURL url = new ShortURL();
        url.setCreator(user);
        assertDoesNotThrow(() -> validator.checkOwnership(url, userId));
    }

    @Test
    @DisplayName("Исключение AccessDenied, если UUID не совпадает с ID создателя")
    void checkOwnership_WrongUser_ThrowsException() {
        User owner = new User();
        owner.setId(UUID.randomUUID());

        ShortURL url = new ShortURL();
        url.setCreator(owner);
        UUID strangerId = UUID.randomUUID();
        assertThrows(AccessDeniedException.class, () -> validator.checkOwnership(url, strangerId));
    }

    @Test
    @DisplayName("Успешная валидация, если лимит еще не достигнут")
    void checkUseLimit_BelowLimit_Success() {
        ShortURL url = new ShortURL();
        url.setUseCount(5);
        url.setUseLimit(10);
        assertDoesNotThrow(() -> validator.checkUseLimit(url));
    }

    @Test
    @DisplayName("Исключение, если текущее количество переходов равно лимиту")
    void checkUseLimit_AtLimit_ThrowsException() {
        ShortURL url = new ShortURL();
        url.setUseCount(10);
        url.setUseLimit(10);
        ValidationException ex = assertThrows(ValidationException.class, () -> validator.checkUseLimit(url));
        assertTrue(ex.getMessage().contains("исчерпан"));
    }

    @Test
    @DisplayName("Успешная валидация, если TTL еще не истек")
    void checkExpiration_NotExpired_Success() {
        ShortURL url = new ShortURL();
        url.setCreatedAt(LocalDateTime.now().minusMinutes(30));
        url.setTtlHours(1);
        assertDoesNotThrow(() -> validator.checkExpiration(url));
    }

    @Test
    @DisplayName("Исключение, если время жизни ссылки подошло к концу")
    void checkExpiration_Expired_ThrowsException() {
        ShortURL url = new ShortURL();
        url.setCreatedAt(LocalDateTime.now().minusHours(28));
        url.setTtlHours(24);
        ValidationException ex = assertThrows(ValidationException.class, () -> validator.checkExpiration(url));
        assertTrue(ex.getMessage().contains("истекло"));
    }

    @Test
    @DisplayName("Исключение при попытке установить лимит меньше, чем уже совершено переходов")
    void validateUpdate_NewLimitTooLow_ThrowsException() {
        ShortURL url = new ShortURL();
        url.setUseCount(50);

        ShortURLUpdateDto dto = new ShortURLUpdateDto();
        dto.setUseLimit(40);
        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validateUpdate(url, dto));
        assertTrue(ex.getMessage().contains("не может быть меньше текущего количества переходов"));
    }

    @Test
    @DisplayName("Успешное обновление лимита, если он больше текущего количества переходов")
    void validateUpdate_NewLimitEqualsCount_Success() {
        ShortURL url = new ShortURL();
        url.setUseCount(50);

        ShortURLUpdateDto dto = new ShortURLUpdateDto();
        dto.setUseLimit(60);
        assertDoesNotThrow(() -> validator.validateUpdate(url, dto));
    }

    @Test
    @DisplayName("Исключение при попытке создать дубликат длинной ссылки для одного пользователя")
    void checkUniqueLongUrl_AlreadyExists_ThrowsException() {
        User user = new User();
        String longUrl = "https://unique.com";

        when(repository.existsByLongUrlForUser(user, longUrl)).thenReturn(true);
        EntityExistsException ex = assertThrows(EntityExistsException.class,
                () -> validator.checkUniqueLongUrlForUser(user, longUrl));
        assertTrue(ex.getMessage().contains("уже есть активная короткая ссылка"));
    }

    @Test
    @DisplayName("Успешная проверка, если пользователь еще не сокращал этот URL")
    void checkUniqueLongUrl_NotExists_Success() {
        User user = new User();
        String longUrl = "https://new-url.com";

        when(repository.existsByLongUrlForUser(user, longUrl)).thenReturn(false);
        assertDoesNotThrow(() -> validator.checkUniqueLongUrlForUser(user, longUrl));
    }
}
