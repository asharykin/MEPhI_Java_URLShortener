package ru.mephi.url.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class ShortURLResponseDto {
    private String shortUrl;
    private String longUrl;
    private UUID creatorId;
    private Integer useCount;
    private Integer useLimit;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    private Integer ttlHours;
}