package ru.mephi.url.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "short_urls")
@Getter
@Setter
public class ShortURL {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String shortUrl;
    private String longUrl;
    private Integer useCount;
    private Integer useLimit;
    private LocalDateTime createdAt;
    private Integer ttlHours;
    private Boolean deleted;

    @ManyToOne
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;
}
