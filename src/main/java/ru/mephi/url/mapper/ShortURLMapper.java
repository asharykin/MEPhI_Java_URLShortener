package ru.mephi.url.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.mephi.url.config.ShortURLConfig;
import ru.mephi.url.dto.ShortURLCreateDto;
import ru.mephi.url.dto.ShortURLResponseDto;
import ru.mephi.url.dto.ShortURLUpdateDto;
import ru.mephi.url.model.ShortURL;
import ru.mephi.url.model.User;

import java.time.LocalDateTime;

@Component
public class ShortURLMapper {
    private final ShortURLConfig config;

    @Autowired
    public ShortURLMapper(ShortURLConfig config) {
        this.config = config;
    }

    public ShortURLResponseDto entityToResponseDto(ShortURL url) {
        ShortURLResponseDto dto = new ShortURLResponseDto();
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        dto.setShortUrl(baseUrl + "/" + url.getShortUrl());

        dto.setLongUrl(url.getLongUrl());
        dto.setCreatorId(url.getCreator().getId());
        dto.setUseCount(url.getUseCount());
        dto.setUseLimit(url.getUseLimit());
        dto.setCreatedAt(url.getCreatedAt());
        dto.setTtlHours(url.getTtlHours());
        return dto;
    }

    public ShortURL requestDtoToEntity(ShortURLCreateDto dto, User creator, String shortUrl) {
        ShortURL entity = new ShortURL();
        entity.setShortUrl(shortUrl);
        entity.setLongUrl(dto.getLongUrl());
        entity.setCreator(creator);

        entity.setUseCount(0);
        Integer useLimit = (dto.getUseLimit() != null) ? dto.getUseLimit() : config.getDefaults().getUseLimit();
        entity.setUseLimit(useLimit);

        entity.setCreatedAt(LocalDateTime.now());
        Integer ttlHours = (dto.getTtlHours() != null) ? dto.getTtlHours() : config.getDefaults().getTtlHours();
        entity.setTtlHours(ttlHours);

        entity.setDeleted(false);
        return entity;
    }

    public void updateEntityFromRequestDto(ShortURL url, ShortURLUpdateDto dto) {
        if (dto.getLongUrl() != null) {
            url.setLongUrl(dto.getLongUrl());
        }

        if (dto.getUseLimit() != null) {
            url.setUseLimit(dto.getUseLimit());
        }

        if (dto.getTtlHours() != null) {
            url.setTtlHours(dto.getTtlHours());
        }
    }
}
