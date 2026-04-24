package kr.daboyeo.backend.sync;

import java.util.List;
import java.util.Map;

public record SeatCollectionResult(
    Map<String, Object> summary,
    List<Map<String, Object>> seats
) {
}
