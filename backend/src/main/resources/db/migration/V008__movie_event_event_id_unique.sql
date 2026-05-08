-- Ported from origin/feature/ksg-event and renumbered to follow the current kmh Flyway chain.
-- Backfill legacy rows, then require stable event identifiers per cinema.

SET NAMES utf8mb4;

ALTER TABLE movie_events
  ADD COLUMN IF NOT EXISTS event_id VARCHAR(128) NULL AFTER cinema;

ALTER TABLE movie_events
  ADD COLUMN IF NOT EXISTS cinema VARCHAR(32) NULL AFTER source;

ALTER TABLE movie_events
  ADD COLUMN IF NOT EXISTS event_url VARCHAR(1024) NULL AFTER image_url;

ALTER TABLE movie_events
  ADD COLUMN IF NOT EXISTS updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) AFTER created_at;

UPDATE movie_events
SET cinema = source
WHERE cinema IS NULL OR cinema = '';

UPDATE movie_events
SET event_id = CONCAT('legacy:', id)
WHERE event_id IS NULL OR event_id = '';

ALTER TABLE movie_events
  MODIFY event_id VARCHAR(128) NOT NULL,
  MODIFY cinema VARCHAR(32) NOT NULL;

SET @stmt = IF(
  EXISTS(
    SELECT 1
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'movie_events'
      AND INDEX_NAME = 'uk_movie_events_event_id_cinema'
  ),
  'SELECT 1',
  'ALTER TABLE movie_events ADD CONSTRAINT uk_movie_events_event_id_cinema UNIQUE (event_id, cinema)'
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
