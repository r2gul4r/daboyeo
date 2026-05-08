package kr.daboyeo.backend.repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import kr.daboyeo.backend.domain.Category;
import kr.daboyeo.backend.domain.MovieEvent;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class MovieEventRepository {

    private static final String BASE_SELECT = """
        SELECT
          id,
          source,
          cinema,
          event_id,
          category,
          title,
          image_url,
          event_url,
          start_date,
          end_date,
          d_day,
          created_at,
          updated_at
        FROM movie_events
        """;

    private final JdbcTemplate jdbcTemplate;

    public MovieEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean existsByEventIdAndCinema(String eventId, String cinema) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM movie_events WHERE event_id = ? AND cinema = ?",
            Integer.class,
            eventId,
            cinema
        );
        return count != null && count > 0;
    }

    public void saveAll(List<MovieEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        String sql = """
            INSERT INTO movie_events (
              source,
              cinema,
              event_id,
              category,
              title,
              image_url,
              event_url,
              start_date,
              end_date,
              d_day
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
              source = VALUES(source),
              category = VALUES(category),
              title = VALUES(title),
              image_url = VALUES(image_url),
              event_url = VALUES(event_url),
              start_date = VALUES(start_date),
              end_date = VALUES(end_date),
              d_day = VALUES(d_day)
            """;

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(java.sql.PreparedStatement ps, int index) throws SQLException {
                MovieEvent event = events.get(index);
                ps.setString(1, event.getSource());
                ps.setString(2, event.getCinema());
                ps.setString(3, event.getEventId());
                ps.setString(4, event.getCategory().name());
                ps.setString(5, event.getTitle());
                ps.setString(6, event.getImageUrl());
                ps.setString(7, event.getEventUrl());
                if (event.getStartDate() == null) {
                    ps.setNull(8, Types.DATE);
                } else {
                    ps.setDate(8, Date.valueOf(event.getStartDate()));
                }
                if (event.getEndDate() == null) {
                    ps.setNull(9, Types.DATE);
                } else {
                    ps.setDate(9, Date.valueOf(event.getEndDate()));
                }
                ps.setString(10, event.getDDay());
            }

            @Override
            public int getBatchSize() {
                return events.size();
            }
        });
    }

    public List<MovieEvent> findAllOrderByCreatedAtDesc() {
        return jdbcTemplate.query(BASE_SELECT + " ORDER BY created_at DESC", rowMapper());
    }

    public List<MovieEvent> findByCategory(Category category) {
        return jdbcTemplate.query(
            BASE_SELECT + " WHERE category = ? ORDER BY created_at DESC",
            rowMapper(),
            category.name()
        );
    }

    public List<MovieEvent> findBySource(String source) {
        return jdbcTemplate.query(
            BASE_SELECT + " WHERE source = ? OR cinema = ? ORDER BY created_at DESC",
            rowMapper(),
            source,
            source
        );
    }

    public List<MovieEvent> findBySourceAndCategory(String source, Category category) {
        return jdbcTemplate.query(
            BASE_SELECT + " WHERE (source = ? OR cinema = ?) AND category = ? ORDER BY created_at DESC",
            rowMapper(),
            source,
            source,
            category.name()
        );
    }

    public List<MovieEvent> findActiveEventsOnDate(LocalDate date) {
        return jdbcTemplate.query(
            BASE_SELECT + " WHERE start_date <= ? AND (end_date IS NULL OR end_date >= ?) ORDER BY created_at DESC",
            rowMapper(),
            Date.valueOf(date),
            Date.valueOf(date)
        );
    }

    private RowMapper<MovieEvent> rowMapper() {
        return (resultSet, rowNum) -> mapRow(resultSet);
    }

    private MovieEvent mapRow(ResultSet rs) throws SQLException {
        return new MovieEvent(
            rs.getLong("id"),
            rs.getString("source"),
            rs.getString("cinema"),
            rs.getString("event_id"),
            Category.valueOf(rs.getString("category")),
            rs.getString("title"),
            rs.getString("image_url"),
            rs.getString("event_url"),
            rs.getObject("start_date", LocalDate.class),
            rs.getObject("end_date", LocalDate.class),
            rs.getString("d_day"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime()
        );
    }
}
