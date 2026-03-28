# Feature workflows

This document describes how the main behaviors work end to end, with pointers into the backend (and one frontend touchpoint). Paths are relative to the repository root.

---

## 1. Primary key (`id`) and short code (`short_code`)

### Database primary key

The row identifier is a PostgreSQL `BIGSERIAL`: the database assigns a monotonically increasing 64-bit integer on insert.

| Location | Role |
|----------|------|
| `backend/src/main/resources/db/migration/V1__create_short_links.sql` **L2** | `id BIGSERIAL PRIMARY KEY` |
| `backend/src/main/java/com/urlshortener/app/models/entity/ShortLink.java` **L15–L17** | JPA maps `id` with `@GeneratedValue(strategy = GenerationType.IDENTITY)` so the DB owns the sequence |
| `backend/src/main/java/com/urlshortener/app/models/entity/ShortLink.java` **L22–L23** | `short_code` column; set by the service on insert (not computed from `id`) |

Uniqueness is enforced at the DB level (`V1__create_short_links.sql` **L7**). New codes are **7 characters** from the same 62-character set as Base62 (`0-9`, `A-Z`, `a-z`).

### How `short_code` is generated (unpredictable, fixed length)

Each new link gets a **cryptographically random** 7-character code from `ShortCodeGenerator` (`SecureRandom` picks indices into [`Base62Alphabet.CHARS`](backend/src/main/java/com/urlshortener/app/utils/Base62Alphabet.java)). That is **not** derived from the primary key, so codes are not enumerable from neighboring rows.

| Step | Location |
|------|----------|
| Generate candidate code | `ShortCodeGenerator.java` **L13–L20** (`SHORT_CODE_LENGTH` **L9**) |
| Shared alphabet | `Base62Alphabet.java` **L8–L9** |
| Build entity with `long_url`, `created_at`, `short_code`; single `save` | `ShortLinkService.java` **L58–L63** |
| Populate Redis cache | `ShortLinkService.java` **L64** (`cachePut`) |
| Retry loop on unique violation | `ShortLinkService.java` **L55–L79** (`MAX_SHORT_CODE_ATTEMPTS` **L20**) |

**Collisions:** A duplicate `short_code` is extremely unlikely but possible; on `DataIntegrityViolationException`, if `findByLongUrl` finds a row (concurrent create for the same URL), that row is returned and cached (**L67–L75**). Otherwise the service assumes a code collision and retries with a new random code. After **20** failed attempts it throws `IllegalStateException` (**L78–L79**).

**Legacy rows:** Older deployments may still have shorter codes (e.g. sequential Base62 of `id`); redirects and lookups remain valid. The [`Base62.encode`](backend/src/main/java/com/urlshortener/app/utils/Base62.java) helper is unchanged for other uses.

**Concurrency / idempotency:** The same canonical long URL is unique (`V1__create_short_links.sql` **L6**). Parallel creates for the same URL can hit a unique violation; the handler above re-reads by `long_url` and returns the existing row when present.

---

## 2. Create short link (API)

| Step | Location |
|------|----------|
| HTTP `POST /api/v1/urls` | `UrlController.java` **L39–L41** |
| Normalize and validate URL (scheme, host, length, no userinfo, canonical form) | `UrlNormalizer.java` **L14–L60**; invoked from `ShortLinkService.create` **L43** |
| Return existing link if `long_url` already exists | `ShortLinkService.java` **L44–L51** |
| Otherwise random code + insert + cache | `ShortLinkService.java` **L55–L79** (`insertNew`) |
| Build public short URL | `ShortLinkService.java` **L82–L85** (`publicShortLinkBase` + `/r/` + code) |

Invalid URLs become `400` via `UrlValidationException` handled in `GlobalExceptionHandler.java` **L14–L16**.

---

## 3. Redirect (`GET /r/{shortCode}`)

| Step | Location |
|------|----------|
| Entry | `RedirectController.java` **L19–L24** |
| Reject invalid code characters | `RedirectService.java` **L32–L35** (pattern **L17**) |
| Try Redis, then DB; on DB hit, backfill cache | `RedirectService.java` **L39–L51** |
| Re-check scheme is `http` or `https` before building `Location` | `RedirectService.java` **L54–L65** |
| `302 Found` with `Location`, or `404` | `RedirectController.java` **L21–L24** |

---

## 4. Lookup by short code (`GET /api/v1/urls/{shortCode}`)

| Step | Location |
|------|----------|
| Validate path segment | `UrlController.java` **L44–L47** |
| Load from DB (no redirect) | `UrlController.java` **L49–L53** |
| Response shape | `UrlController.java` **L56–L62** |

---

## 5. Correlation ID

| Step | Location |
|------|----------|
| Read `X-Correlation-Id` or generate UUID | `CorrelationIdFilter.java` **L26–L28** |
| Echo on response | **L30** |
| Store in MDC for logging | **L31–L32** |

---

## 6. Frontend create flow (reference)

The UI posts JSON to the same API path the backend exposes.

| Step | Location |
|------|----------|
| `POST ${API_BASE}/api/v1/urls` with `{ longUrl }` | `frontend/src/app/page.tsx` **L27–L30** |
| Surface validation / network errors | **L38–L50** |

`API_BASE` defaults to `http://localhost:8080` (**L6**), overridable with `NEXT_PUBLIC_API_URL`.
