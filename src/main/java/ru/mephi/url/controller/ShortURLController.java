package ru.mephi.url.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.mephi.url.dto.ShortURLCreateDto;
import ru.mephi.url.dto.ShortURLResponseDto;
import ru.mephi.url.dto.ShortURLUpdateDto;
import ru.mephi.url.service.ShortURLService;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ShortURLController {
    private final ShortURLService urlService;

    @PostMapping("/shorten")
    public ResponseEntity<ShortURLResponseDto> createShortUrl(@RequestHeader(name = "UUID", required = false) UUID uuid,
                                                              @RequestBody @Valid ShortURLCreateDto requestDto) {
        ShortURLResponseDto responseDto = urlService.createShortUrl(uuid, requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @GetMapping("/{shortUrl:[a-zA-Z0-9]{6}}")
    public ResponseEntity<?> redirectToLongUrl(@PathVariable String shortUrl) {
        String longUrl = urlService.getLongUrl(shortUrl);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(longUrl));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @PutMapping("/{shortUrl:[a-zA-Z0-9]{6}}")
    public ResponseEntity<ShortURLResponseDto> updateShortUrl(@PathVariable String shortUrl,
                                                              @RequestHeader("UUID") UUID uuid,
                                                              @RequestBody @Valid ShortURLUpdateDto requestDto) {
        ShortURLResponseDto responseDto = urlService.updateShortUrl(shortUrl, uuid, requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.OK);
    }

    @DeleteMapping("/{shortUrl:[a-zA-Z0-9]{6}}")
    public ResponseEntity<?> deleteShortUrl(@PathVariable String shortUrl, @RequestHeader("UUID") UUID uuid) {
        urlService.deleteShortUrl(shortUrl, uuid);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
