package kr.daboyeo.backend.sync;

import java.time.LocalDateTime;
import java.util.Map;

public record SeatSnapshotTarget(
    long showtimeId,
    CollectorProvider provider,
    String externalShowtimeKey,
    LocalDateTime startsAt,
    Integer totalSeatCount,
    Integer remainingSeatCount,
    Map<String, Object> bookingKey
) {
}
