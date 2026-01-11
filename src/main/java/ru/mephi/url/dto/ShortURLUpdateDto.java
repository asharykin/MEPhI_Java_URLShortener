package ru.mephi.url.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;

@Getter
@Setter
public class ShortURLUpdateDto {

    @URL(message = "Указан некорректный формат URL")
    private String longUrl;

    @Min(value = 1, message = "Минимальный лимит переходов - 1 раз")
    @Max(value = 1000, message = "Максимальный лимит переходов - 1000 раз")
    private Integer useLimit;

    @Min(value = 1, message = "Минимальное TTL - 1 ч")
    @Max(value = 8760, message = "Максимальное TTL - 1 год (8760 ч)")
    private Integer ttlHours;
}
