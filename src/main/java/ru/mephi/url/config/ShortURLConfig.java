package ru.mephi.url.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class ShortURLConfig {
    private Defaults defaults = new Defaults();

    @Getter
    @Setter
    public static class Defaults {
        private int useLimit;
        private int ttlHours;
    }
}
