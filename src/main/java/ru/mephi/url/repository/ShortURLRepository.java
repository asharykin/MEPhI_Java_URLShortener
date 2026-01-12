package ru.mephi.url.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.mephi.url.model.ShortURL;
import ru.mephi.url.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ShortURLRepository extends JpaRepository<ShortURL, String> {

    @Query("SELECT s FROM ShortURL s JOIN FETCH s.creator WHERE s.shortUrl = :shortUrl")
    Optional<ShortURL> findByShortUrl(String shortUrl);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM ShortURL s WHERE s.shortUrl = :shortUrl")
    boolean existsByShortUrl(String shortUrl);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
            "FROM ShortURL s " +
            "WHERE s.creator = :creator " +
            "AND s.longUrl = :longUrl " +
            "AND s.deleted = false")
    boolean existsByLongUrlForUser(User creator, String longUrl);

    @Query(value = "SELECT * FROM short_urls " +
            "WHERE deleted = false " +
            "AND DATEADD('HOUR', ttl_hours, created_at) < :now",
            nativeQuery = true)
    List<ShortURL> findExpiredUrls(LocalDateTime now);
}
