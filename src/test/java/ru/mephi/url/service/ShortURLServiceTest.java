package ru.mephi.url.service;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.mephi.url.dto.ShortURLCreateDto;
import ru.mephi.url.dto.ShortURLUpdateDto;
import ru.mephi.url.mapper.ShortURLMapper;
import ru.mephi.url.model.ShortURL;
import ru.mephi.url.model.User;
import ru.mephi.url.repository.ShortURLRepository;
import ru.mephi.url.validator.ShortURLValidator;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShortURLServiceTest {

    @Mock
    private ShortURLRepository urlRepository;

    @Mock
    private ShortURLMapper urlMapper;

    @Mock
    private ShortURLValidator urlValidator;

    @Mock
    private UserService userService;

    @InjectMocks
    private ShortURLService shortURLService;

    @Test
    @DisplayName("Создание короткой ссылки для нового пользователя")
    void createShortUrl_ShouldCreateNewUser_WhenIdIsNull() {
        ShortURLCreateDto dto = new ShortURLCreateDto();
        dto.setLongUrl("https://google.com");

        User user = new User();
        user.setId(UUID.randomUUID());

        when(userService.createUser()).thenReturn(user);
        when(urlRepository.existsByShortUrl(anyString())).thenReturn(false);
        when(urlMapper.requestDtoToEntity(any(), any(), any())).thenReturn(new ShortURL());
        shortURLService.createShortUrl(null, dto);

        verify(userService).createUser();
        verify(urlRepository).save(any(ShortURL.class));
    }

    @Test
    @DisplayName("Обработка коллизии при генерации короткого кода")
    void createShortUrl_WithCodeCollision() {
        ShortURLCreateDto requestDto = new ShortURLCreateDto();

        User user = new User();
        user.setId(UUID.randomUUID());

        when(userService.createUser()).thenReturn(user);
        when(urlRepository.existsByShortUrl(anyString())).thenReturn(true).thenReturn(false);
        when(urlMapper.requestDtoToEntity(any(), any(), any())).thenReturn(new ShortURL());
        shortURLService.createShortUrl(null, requestDto);

        verify(urlRepository, atLeast(2)).existsByShortUrl(anyString());
    }

    @Test
    @DisplayName("Получение длинной ссылки и инкремент счетчика")
    void getLongUrl_ShouldIncrementCount_WhenExists() {
        String shortUrl = "abc123";
        ShortURL url = new ShortURL();
        url.setShortUrl(shortUrl);
        url.setLongUrl("https://ya.ru");
        url.setUseCount(5);
        url.setUseLimit(10);

        when(urlRepository.findByShortUrl(shortUrl)).thenReturn(Optional.of(url));
        String longUrl = shortURLService.getLongUrl(shortUrl);

        assertEquals("https://ya.ru", longUrl);
        assertEquals(6, url.getUseCount());
        verify(urlValidator).validateAccess(url);
    }

    @Test
    @DisplayName("Ошибка при получении несуществующей ссылки")
    void getLongUrl_ShouldThrowException_WhenNotFound() {
        String shortUrl = "missin";

        when(urlRepository.findByShortUrl(shortUrl)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> shortURLService.getLongUrl(shortUrl));
    }

    @Test
    @DisplayName("Обновление ссылки")
    void updateShortUrl_Success() {
        String shortUrl = "update";
        ShortURLUpdateDto requestDto = new ShortURLUpdateDto();

        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        ShortURL url = new ShortURL();
        url.setCreator(user);
        url.setShortUrl(shortUrl);

        when(urlRepository.findByShortUrl(shortUrl)).thenReturn(Optional.of(url));
        shortURLService.updateShortUrl(shortUrl, userId, requestDto);

        verify(urlValidator).checkOwnership(url, userId);
        verify(urlValidator).validateUpdate(url, requestDto);
        verify(urlMapper).updateEntityFromRequestDto(url, requestDto);
        verify(urlRepository).save(url);
    }

    @Test
    @DisplayName("Удаление ссылки")
    void deleteShortUrl_Success() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        String shortUrl = "delete";
        ShortURL url = new ShortURL();
        url.setShortUrl(shortUrl);
        url.setCreator(user);

        when(urlRepository.findByShortUrl(shortUrl)).thenReturn(Optional.of(url));
        shortURLService.deleteShortUrl(shortUrl, userId);

        verify(urlValidator).checkOwnership(url, userId);
        verify(urlRepository).delete(url);
    }
}
