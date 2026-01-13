package ru.mephi.url.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import ru.mephi.url.config.ShortURLConfig;
import ru.mephi.url.dto.ShortURLCreateDto;
import ru.mephi.url.dto.ShortURLResponseDto;
import ru.mephi.url.dto.ShortURLUpdateDto;
import ru.mephi.url.model.ShortURL;
import ru.mephi.url.model.User;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ShortURLMapperTest {

    @Mock
    private ShortURLConfig config;

    @Mock
    private ShortURLConfig.Defaults defaults;

    private ShortURLMapper mapper;

    @BeforeEach
    void setUp() {
        lenient().when(config.getDefaults()).thenReturn(defaults);
        lenient().when(defaults.getUseLimit()).thenReturn(100);
        lenient().when(defaults.getTtlHours()).thenReturn(24);

        mapper = new ShortURLMapper(config);
    }

    @Test
    @DisplayName("Маппинг сущности в Response DTO: Проверка формирования полей")
    void entityToResponseDto_ShouldMapAllFieldsCorrectly() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        User user = new User();
        user.setId(UUID.randomUUID());

        ShortURL url = new ShortURL();
        url.setShortUrl("abc123");
        url.setLongUrl("https://example.com");
        url.setCreator(user);
        url.setUseCount(10);
        url.setUseLimit(50);
        url.setCreatedAt(LocalDateTime.now());
        url.setTtlHours(12);

        ShortURLResponseDto result = mapper.entityToResponseDto(url);

        assertAll(
                () -> assertTrue(result.getShortUrl().endsWith("/abc123")),
                () -> assertEquals(url.getLongUrl(), result.getLongUrl()),
                () -> assertEquals(user.getId(), result.getCreatorId()),
                () -> assertEquals(10, result.getUseCount()),
                () -> assertEquals(50, result.getUseLimit()),
                () -> assertEquals(12, result.getTtlHours())
        );
    }

    @Test
    @DisplayName("Создание сущности из Request DTO: Использование предоставленных значений")
    void requestDtoToEntity_ShouldUseDtoValues_WhenProvided() {
        ShortURLCreateDto dto = new ShortURLCreateDto();
        dto.setLongUrl("https://target.com");
        dto.setUseLimit(500);
        dto.setTtlHours(72);

        User user = new User();
        user.setId(UUID.randomUUID());

        String shortUrl = "xyz789";
        ShortURL url = mapper.requestDtoToEntity(dto, user, shortUrl);

        assertAll(
                () -> assertEquals("https://target.com", url.getLongUrl()),
                () -> assertEquals(500, url.getUseLimit()),
                () -> assertEquals(72, url.getTtlHours()),
                () -> assertEquals(user, url.getCreator()),
                () -> assertEquals(0, url.getUseCount()),
                () -> assertFalse(url.getDeleted())
        );
    }

    @Test
    @DisplayName("Создание сущности из Request DTO: Использование дефолтных значений из конфига")
    void requestDtoToEntity_ShouldUseDefaults_WhenDtoValuesAreNull() {
        ShortURLCreateDto dto = new ShortURLCreateDto();
        dto.setLongUrl("https://target.com");

        ShortURL url = mapper.requestDtoToEntity(dto, new User(), "code12");
        assertAll(
                () -> assertEquals(100, url.getUseLimit(), "Должно взяться из app.defaults"),
                () -> assertEquals(24, url.getTtlHours(), "Должно взяться из app.defaults")
        );
    }

    @Test
    @DisplayName("Обновление сущности: Полное обновление всех полей")
    void updateEntityFromRequestDto_ShouldUpdateAllFields() {
        ShortURL entity = new ShortURL();
        entity.setLongUrl("https://old.com");
        entity.setUseLimit(10);
        entity.setTtlHours(5);

        ShortURLUpdateDto dto = new ShortURLUpdateDto();
        dto.setLongUrl("https://new.com");
        dto.setUseLimit(20);
        dto.setTtlHours(10);

        mapper.updateEntityFromRequestDto(entity, dto);

        assertAll(
                () -> assertEquals("https://new.com", entity.getLongUrl()),
                () -> assertEquals(20, entity.getUseLimit()),
                () -> assertEquals(10, entity.getTtlHours())
        );
    }

    @Test
    @DisplayName("Обновление сущности: Игнорирование null полей в DTO")
    void updateEntityFromRequestDto_ShouldNotUpdate_WhenFieldsAreNull() {
        ShortURL url = new ShortURL();
        url.setLongUrl("https://stay.com");
        url.setUseLimit(100);

        ShortURLUpdateDto dto = new ShortURLUpdateDto();

        mapper.updateEntityFromRequestDto(url, dto);

        assertAll(
                () -> assertEquals("https://stay.com", url.getLongUrl()),
                () -> assertEquals(100, url.getUseLimit())
        );
    }
}
