package com.urlshortener.app.models.repository;

import com.urlshortener.app.models.entity.ShortLink;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShortLinkRepository extends JpaRepository<ShortLink, Long> {

	Optional<ShortLink> findByLongUrl(String longUrl);

	Optional<ShortLink> findByShortCode(String shortCode);
}
