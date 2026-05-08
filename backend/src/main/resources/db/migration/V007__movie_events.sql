-- Ported from origin/feature/ksg-event and renumbered to follow the current kmh Flyway chain.
-- Create a movie-events table first, then tighten keys in V008.

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS movie_events (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  source VARCHAR(32) NOT NULL,
  cinema VARCHAR(32) NULL,
  event_id VARCHAR(128) NULL,
  category VARCHAR(32) NOT NULL,
  title VARCHAR(500) NOT NULL,
  image_url VARCHAR(1024) NULL,
  event_url VARCHAR(1024) NULL,
  start_date DATE NULL,
  end_date DATE NULL,
  d_day VARCHAR(64) NULL,
  raw_json JSON NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_movie_events_category (category, start_date),
  KEY idx_movie_events_source (source, created_at),
  KEY idx_movie_events_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
