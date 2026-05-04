package kr.daboyeo.backend.sync.nearby;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import kr.daboyeo.backend.config.CollectorSyncProperties;
import kr.daboyeo.backend.domain.LiveMovieSearchCriteria;
import kr.daboyeo.backend.domain.SeatState;
import kr.daboyeo.backend.sync.bridge.CollectorProvider;
import org.junit.jupiter.api.Test;

class NearbyTheaterTargetResolverTests {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-04-30T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Test
    void resolveCapsRefreshTargetsToConfiguredRadiusWhenSearchRadiusIsLarger() {
        CollectorSyncProperties properties = new CollectorSyncProperties();
        properties.getShowtimes().setNearbyRefreshMaxTheatersPerProvider(6);
        properties.getShowtimes().setNearbyRefreshRadiusKm(new BigDecimal("3"));
        NearbyTheaterTargetResolver resolver = new NearbyTheaterTargetResolver(
            properties,
            List.of(
                new NearbyTheaterTargetResolver.TheaterMapEntry(CollectorProvider.MEGABOX, "near", "Near", 37.4979d, 127.0276d, 0d),
                new NearbyTheaterTargetResolver.TheaterMapEntry(CollectorProvider.MEGABOX, "far", "Far", 37.5339d, 127.0276d, 0d)
            )
        );

        NearbyTheaterTargetResolver.Resolution resolution = resolver.resolve(sampleCriteria(new BigDecimal("8")));

        assertThat(resolution.megaboxEntries())
            .extracting(NearbyTheaterTargetResolver.TheaterMapEntry::externalTheaterId)
            .containsExactly("near");
    }

    private static LiveMovieSearchCriteria sampleCriteria(BigDecimal radiusKm) {
        return LiveMovieSearchCriteria.of(
            new BigDecimal("37.4979"),
            new BigDecimal("127.0276"),
            LocalDate.of(2026, 4, 30),
            LocalTime.of(6, 0),
            LocalTime.of(23, 59),
            radiusKm,
            List.of(),
            List.of(),
            List.of(),
            SeatState.ALL,
            "",
            300,
            FIXED_CLOCK
        );
    }
}
