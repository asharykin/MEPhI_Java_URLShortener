package ru.mephi.url.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.mephi.url.dto.ShortURLCreateDto;
import ru.mephi.url.dto.ShortURLResponseDto;
import ru.mephi.url.dto.ShortURLUpdateDto;
import ru.mephi.url.service.ShortURLService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ShortURLController.class)
class ShortURLControllerTest {

    @MockBean
    private ShortURLService urlService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("POST /shorten - Успешное создание ссылки (201 Created)")
    void createShortUrl_Success() throws Exception {
        ShortURLCreateDto requestDto = new ShortURLCreateDto();
        requestDto.setLongUrl("https://google.com");

        ShortURLResponseDto responseDto = new ShortURLResponseDto();
        responseDto.setShortUrl("http://localhost/abc123");
        responseDto.setLongUrl("https://google.com");

        when(urlService.createShortUrl(eq(userId), any(ShortURLCreateDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/shorten")
                        .header("UUID", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortUrl").value("http://localhost/abc123"))
                .andExpect(jsonPath("$.longUrl").value("https://google.com"));
    }

    @Test
    @DisplayName("GET /{shortUrl} - Успешное перенаправление (302 Found)")
    void redirectToLongUrl_Success() throws Exception {
        String shortCode = "abc123";
        String targetUrl = "https://ya.ru";

        when(urlService.getLongUrl(shortCode)).thenReturn(targetUrl);

        mockMvc.perform(get("/{shortUrl}", shortCode))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", targetUrl));
    }

    @Test
    @DisplayName("PUT /{shortUrl} - Обновление ссылки владельцем (200 OK)")
    void updateShortUrl_Success() throws Exception {
        String shortCode = "abc123";
        ShortURLUpdateDto updateDto = new ShortURLUpdateDto();
        updateDto.setUseLimit(500);

        ShortURLResponseDto responseDto = new ShortURLResponseDto();
        responseDto.setUseLimit(500);

        when(urlService.updateShortUrl(eq(shortCode), eq(userId), any(ShortURLUpdateDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(put("/{shortUrl}", shortCode)
                        .header("UUID", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.useLimit").value(500));
    }

    @Test
    @DisplayName("DELETE /{shortUrl} - Удаление ссылки (204 No Content)")
    void deleteShortUrl_Success() throws Exception {
        String shortCode = "abc123";

        mockMvc.perform(delete("/{shortUrl}", shortCode)
                        .header("UUID", userId.toString()))
                .andExpect(status().isNoContent());

        verify(urlService).deleteShortUrl(shortCode, userId);
    }

    @Test
    @DisplayName("POST /shorten - Ошибка валидации (400 Bad Request)")
    void createShortUrl_ValidationError() throws Exception {
        ShortURLCreateDto invalidDto = new ShortURLCreateDto();
        invalidDto.setLongUrl("not-a-valid-url");

        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /{shortUrl} - Ошибка при отсутствии обязательного заголовка UUID (401 Unauthorized)")
    void updateShortUrl_MissingHeader() throws Exception {
        mockMvc.perform(put("/abc123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /{shortUrl} - Ошибка 404, если формат короткой ссылки неверный")
    void redirect_InvalidFormat_Returns404() throws Exception {
        mockMvc.perform(get("/tooLongCode7"))
                .andExpect(status().isNotFound());
    }
}
