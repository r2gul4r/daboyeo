package kr.daboyeo.backend.ingest;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class TheaterLocationEnricherTests {

    @Test
    void enrichesMegaboxTheaterCoordinatesFromTheaterMapCode() {
        TheaterLocationEnricher enricher = new TheaterLocationEnricher(List.of(
            new TheaterLocationEnricher.TheaterLocation(
                "MEGABOX",
                "4631",
                "분당",
                new BigDecimal("37.385000"),
                new BigDecimal("127.123000"),
                "경기도 성남시 분당구 황새울로 332"
            )
        ));

        CollectorBundleIngestCommand.TheaterRow theater = new CollectorBundleIngestCommand.TheaterRow(
            "MEGABOX",
            "4631",
            "분당",
            "30",
            "경기",
            null,
            null,
            null,
            null
        );

        CollectorBundleIngestCommand.NormalizedBundle bundle = new CollectorBundleIngestCommand.NormalizedBundle(
            List.of(),
            List.of(theater),
            List.of(),
            List.of()
        );

        CollectorBundleIngestCommand.TheaterRow enriched = enricher.enrich(bundle).theaters().get(0);
        assertThat(enriched.latitude()).isEqualByComparingTo("37.385000");
        assertThat(enriched.longitude()).isEqualByComparingTo("127.123000");
        assertThat(enriched.address()).isEqualTo("경기도 성남시 분당구 황새울로 332");
    }

    @Test
    void enrichesLotteRegionNameFromExistingAddressEvenWhenCoordinatesExist() {
        TheaterLocationEnricher enricher = new TheaterLocationEnricher(List.of());

        CollectorBundleIngestCommand.TheaterRow theater = new CollectorBundleIngestCommand.TheaterRow(
            "LOTTE_CINEMA",
            "1|101|0001",
            "건대입구",
            null,
            null,
            "서울특별시 광진구 아차산로 262",
            new BigDecimal("37.540000"),
            new BigDecimal("127.070000"),
            null
        );

        CollectorBundleIngestCommand.NormalizedBundle bundle = new CollectorBundleIngestCommand.NormalizedBundle(
            List.of(),
            List.of(theater),
            List.of(),
            List.of()
        );

        CollectorBundleIngestCommand.TheaterRow enriched = enricher.enrich(bundle).theaters().get(0);
        assertThat(enriched.regionName()).isEqualTo("서울");
        assertThat(enriched.latitude()).isEqualByComparingTo("37.540000");
        assertThat(enriched.longitude()).isEqualByComparingTo("127.070000");
    }

    @Test
    void enrichesLotteRegionNameFromTheaterMapAddressWhenOriginalAddressIsMissing() {
        TheaterLocationEnricher enricher = new TheaterLocationEnricher(List.of(
            new TheaterLocationEnricher.TheaterLocation(
                "LOTTE_CINEMA",
                "1|101|0001",
                "건대입구",
                new BigDecimal("37.540000"),
                new BigDecimal("127.070000"),
                "서울특별시 광진구 아차산로 262"
            )
        ));

        CollectorBundleIngestCommand.TheaterRow theater = new CollectorBundleIngestCommand.TheaterRow(
            "LOTTE_CINEMA",
            "1|101|0001",
            "건대입구",
            null,
            null,
            null,
            new BigDecimal("37.540000"),
            new BigDecimal("127.070000"),
            null
        );

        CollectorBundleIngestCommand.NormalizedBundle bundle = new CollectorBundleIngestCommand.NormalizedBundle(
            List.of(),
            List.of(theater),
            List.of(),
            List.of()
        );

        CollectorBundleIngestCommand.TheaterRow enriched = enricher.enrich(bundle).theaters().get(0);
        assertThat(enriched.address()).isEqualTo("서울특별시 광진구 아차산로 262");
        assertThat(enriched.regionName()).isEqualTo("서울");
    }

    @Test
    void collectorBundleHelpersHandleProviderSpecificDateAndTimeFormats() {
        assertThat(CollectorBundleIngestCommand.parseDate("2026-02-04 오전 12:00:00"))
            .isEqualTo(LocalDate.of(2026, 2, 4));
        assertThat(CollectorBundleIngestCommand.parseTime("2419"))
            .isEqualTo(LocalTime.of(0, 19));
    }
}
