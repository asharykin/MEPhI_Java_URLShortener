package ru.mephi.url;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.mephi.url.dto.ShortURLCreateDto;
import ru.mephi.url.dto.ShortURLResponseDto;
import ru.mephi.url.model.ShortURL;
import ru.mephi.url.model.User;
import ru.mephi.url.repository.ShortURLRepository;
import ru.mephi.url.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@AutoConfigureMockMvc
class ShortURLIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ShortURLRepository urlRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Сценарий: Полный жизненный цикл ссылки (Создание -> Переход -> Удаление)")
    void fullCycle_Success() throws Exception {
        // 1. Создание ссылки
        ShortURLCreateDto createDto = new ShortURLCreateDto();
        createDto.setLongUrl("https://spring.io");
        createDto.setUseLimit(5);

        String responseJson = mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        ShortURLResponseDto responseDto = objectMapper.readValue(responseJson, ShortURLResponseDto.class);
        String shortCode = responseDto.getShortUrl().substring(responseDto.getShortUrl().lastIndexOf("/") + 1);
        UUID creatorId = responseDto.getCreatorId();

        // 2. Проверка, что пользователь и ссылка созданы в БД
        assertTrue(userRepository.existsById(creatorId));
        assertTrue(urlRepository.findByShortUrl(shortCode).isPresent());

        // 3. Переход по ссылке
        mockMvc.perform(get("/{shortUrl}", shortCode))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://spring.io"));

        ShortURL updatedUrl = urlRepository.findByShortUrl(shortCode).get();
        assertEquals(1, updatedUrl.getUseCount());

        // 4. Удаление ссылки владельцем
        mockMvc.perform(delete("/{shortUrl}", shortCode)
                        .header("UUID", creatorId.toString()))
                .andExpect(status().isNoContent());

        assertFalse(urlRepository.findByShortUrl(shortCode).isPresent());
    }

    @Test
    @DisplayName("Сценарий: Попытка перехода по просроченной ссылке")
    void redirect_ExpiredLink_ReturnsBadRequest() throws Exception {
        // Создаем пользователя
        User user = new User();
        user.setId(UUID.randomUUID());
        userRepository.save(user);

        // Создаем ссылку, которая "протухла" (создана 24 часа назад с TTL 1 час)
        ShortURL expiredUrl = new ShortURL();
        expiredUrl.setShortUrl("oldone");
        expiredUrl.setLongUrl("https://expired.com");
        expiredUrl.setCreator(user);
        expiredUrl.setCreatedAt(LocalDateTime.now().minusHours(24));
        expiredUrl.setTtlHours(1);
        expiredUrl.setUseCount(0);
        expiredUrl.setUseLimit(10);
        expiredUrl.setDeleted(false);
        urlRepository.save(expiredUrl);

        mockMvc.perform(get("/oldone"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("истекло")));
    }

    @Test
    @DisplayName("Сценарий: Превышение лимита переходов")
    void limitReached_ReturnsBadRequest() throws Exception {
        User user = new User();
        user.setId(UUID.randomUUID());
        userRepository.save(user);

        ShortURL limitUrl = new ShortURL();
        limitUrl.setShortUrl("limits");
        limitUrl.setLongUrl("https://test.com");
        limitUrl.setCreator(user);
        limitUrl.setUseCount(1);
        limitUrl.setUseLimit(2);
        limitUrl.setTtlHours(24);
        limitUrl.setCreatedAt(LocalDateTime.now());
        limitUrl.setDeleted(false);
        urlRepository.save(limitUrl);

        // Первый переход будет ок (счетчик станет 2), но второй должен упасть
        mockMvc.perform(get("/limits")).andExpect(status().isFound());

        // Теперь лимит исчерпан
        mockMvc.perform(get("/limits"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("исчерпан")));
    }
}
