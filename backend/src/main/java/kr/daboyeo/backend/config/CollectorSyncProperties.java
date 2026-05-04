package kr.daboyeo.backend.config;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "daboyeo.sync")
public class CollectorSyncProperties {

    private boolean enabled = false;
    private String timezone = "Asia/Seoul";
    private String pythonExecutable = "python";
    private int processTimeoutSeconds = 180;
    private final ShowtimeProperties showtimes = new ShowtimeProperties();
    private final SeatProperties seats = new SeatProperties();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getPythonExecutable() {
        return pythonExecutable;
    }

    public void setPythonExecutable(String pythonExecutable) {
        this.pythonExecutable = pythonExecutable;
    }

    public int getProcessTimeoutSeconds() {
        return processTimeoutSeconds;
    }

    public void setProcessTimeoutSeconds(int processTimeoutSeconds) {
        this.processTimeoutSeconds = processTimeoutSeconds;
    }

    public ShowtimeProperties getShowtimes() {
        return showtimes;
    }

    public SeatProperties getSeats() {
        return seats;
    }

    public static class ShowtimeProperties {

        private boolean enabled = false;
        private String cron = "0 0 3 * * *";
        private boolean startupEnabled = false;
        private List<Integer> dateOffsetDays = new ArrayList<>(List.of(0, 1, 2));
        private boolean autoDiscoveryEnabled = false;
        private boolean cleanupEnabled = true;
        private int retentionDays = 3;
        private int discoveryMovieLimit = 20;
        private int discoveryLotteCinemaLimit = 50;
        private int discoveryLotteMovieTargetLimit = 5;
        private int discoveryLotteTotalTargetLimit = 12;
        private int discoveryMegaboxBundleLimit = 20;
        private boolean nearbyRefreshEnabled = true;
        private int nearbyRefreshMaxTheatersPerProvider = 6;
        private BigDecimal nearbyRefreshRadiusKm = new BigDecimal("3");
        private long nearbyRefreshWaitMillis = 2500L;
        private int nearbyRefreshTodayTtlMinutes = 60;
        private int nearbyRefreshNextDayTtlMinutes = 360;
        private int nearbyRefreshFutureTtlMinutes = 1440;
        private int persistenceMaxRetries = 2;
        private long persistenceRetryBackoffMillis = 1000L;
        private List<CgvTarget> cgvTargets = new ArrayList<>();
        private List<LotteTarget> lotteTargets = new ArrayList<>();
        private List<MegaboxTarget> megaboxTargets = new ArrayList<>();
        private List<String> lottePreferredCinemaIds = new ArrayList<>();
        private List<String> lottePreferredCinemaNames = new ArrayList<>();
        private List<String> megaboxAreaCodes = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }

        public boolean isStartupEnabled() {
            return startupEnabled;
        }

        public void setStartupEnabled(boolean startupEnabled) {
            this.startupEnabled = startupEnabled;
        }

        public List<Integer> getDateOffsetDays() {
            return dateOffsetDays;
        }

        public void setDateOffsetDays(List<Integer> dateOffsetDays) {
            this.dateOffsetDays = dateOffsetDays == null ? new ArrayList<>(List.of(0, 1, 2)) : new ArrayList<>(dateOffsetDays);
        }

        public boolean isAutoDiscoveryEnabled() {
            return autoDiscoveryEnabled;
        }

        public void setAutoDiscoveryEnabled(boolean autoDiscoveryEnabled) {
            this.autoDiscoveryEnabled = autoDiscoveryEnabled;
        }

        public boolean isCleanupEnabled() {
            return cleanupEnabled;
        }

        public void setCleanupEnabled(boolean cleanupEnabled) {
            this.cleanupEnabled = cleanupEnabled;
        }

        public int getRetentionDays() {
            return retentionDays;
        }

        public void setRetentionDays(int retentionDays) {
            this.retentionDays = retentionDays;
        }

        public int getDiscoveryMovieLimit() {
            return discoveryMovieLimit;
        }

        public void setDiscoveryMovieLimit(int discoveryMovieLimit) {
            this.discoveryMovieLimit = discoveryMovieLimit;
        }

        public int getDiscoveryLotteCinemaLimit() {
            return discoveryLotteCinemaLimit;
        }

        public void setDiscoveryLotteCinemaLimit(int discoveryLotteCinemaLimit) {
            this.discoveryLotteCinemaLimit = discoveryLotteCinemaLimit;
        }

        public int getDiscoveryLotteMovieTargetLimit() {
            return discoveryLotteMovieTargetLimit;
        }

        public void setDiscoveryLotteMovieTargetLimit(int discoveryLotteMovieTargetLimit) {
            this.discoveryLotteMovieTargetLimit = discoveryLotteMovieTargetLimit;
        }

        public int getDiscoveryLotteTotalTargetLimit() {
            return discoveryLotteTotalTargetLimit;
        }

        public void setDiscoveryLotteTotalTargetLimit(int discoveryLotteTotalTargetLimit) {
            this.discoveryLotteTotalTargetLimit = discoveryLotteTotalTargetLimit;
        }

        public int getDiscoveryMegaboxBundleLimit() {
            return discoveryMegaboxBundleLimit;
        }

        public void setDiscoveryMegaboxBundleLimit(int discoveryMegaboxBundleLimit) {
            this.discoveryMegaboxBundleLimit = discoveryMegaboxBundleLimit;
        }

        public boolean isNearbyRefreshEnabled() {
            return nearbyRefreshEnabled;
        }

        public void setNearbyRefreshEnabled(boolean nearbyRefreshEnabled) {
            this.nearbyRefreshEnabled = nearbyRefreshEnabled;
        }

        public int getNearbyRefreshMaxTheatersPerProvider() {
            return nearbyRefreshMaxTheatersPerProvider;
        }

        public void setNearbyRefreshMaxTheatersPerProvider(int nearbyRefreshMaxTheatersPerProvider) {
            this.nearbyRefreshMaxTheatersPerProvider = nearbyRefreshMaxTheatersPerProvider;
        }

        public BigDecimal getNearbyRefreshRadiusKm() {
            return nearbyRefreshRadiusKm;
        }

        public void setNearbyRefreshRadiusKm(BigDecimal nearbyRefreshRadiusKm) {
            this.nearbyRefreshRadiusKm = nearbyRefreshRadiusKm == null ? new BigDecimal("3") : nearbyRefreshRadiusKm;
        }

        public long getNearbyRefreshWaitMillis() {
            return nearbyRefreshWaitMillis;
        }

        public void setNearbyRefreshWaitMillis(long nearbyRefreshWaitMillis) {
            this.nearbyRefreshWaitMillis = nearbyRefreshWaitMillis;
        }

        public int getNearbyRefreshTodayTtlMinutes() {
            return nearbyRefreshTodayTtlMinutes;
        }

        public void setNearbyRefreshTodayTtlMinutes(int nearbyRefreshTodayTtlMinutes) {
            this.nearbyRefreshTodayTtlMinutes = nearbyRefreshTodayTtlMinutes;
        }

        public int getNearbyRefreshNextDayTtlMinutes() {
            return nearbyRefreshNextDayTtlMinutes;
        }

        public void setNearbyRefreshNextDayTtlMinutes(int nearbyRefreshNextDayTtlMinutes) {
            this.nearbyRefreshNextDayTtlMinutes = nearbyRefreshNextDayTtlMinutes;
        }

        public int getNearbyRefreshFutureTtlMinutes() {
            return nearbyRefreshFutureTtlMinutes;
        }

        public void setNearbyRefreshFutureTtlMinutes(int nearbyRefreshFutureTtlMinutes) {
            this.nearbyRefreshFutureTtlMinutes = nearbyRefreshFutureTtlMinutes;
        }

        public int getPersistenceMaxRetries() {
            return persistenceMaxRetries;
        }

        public void setPersistenceMaxRetries(int persistenceMaxRetries) {
            this.persistenceMaxRetries = persistenceMaxRetries;
        }

        public long getPersistenceRetryBackoffMillis() {
            return persistenceRetryBackoffMillis;
        }

        public void setPersistenceRetryBackoffMillis(long persistenceRetryBackoffMillis) {
            this.persistenceRetryBackoffMillis = persistenceRetryBackoffMillis;
        }

        public List<CgvTarget> getCgvTargets() {
            return cgvTargets;
        }

        public void setCgvTargets(List<CgvTarget> cgvTargets) {
            this.cgvTargets = cgvTargets == null ? new ArrayList<>() : new ArrayList<>(cgvTargets);
        }

        public List<LotteTarget> getLotteTargets() {
            return lotteTargets;
        }

        public void setLotteTargets(List<LotteTarget> lotteTargets) {
            this.lotteTargets = lotteTargets == null ? new ArrayList<>() : new ArrayList<>(lotteTargets);
        }

        public List<MegaboxTarget> getMegaboxTargets() {
            return megaboxTargets;
        }

        public void setMegaboxTargets(List<MegaboxTarget> megaboxTargets) {
            this.megaboxTargets = megaboxTargets == null ? new ArrayList<>() : new ArrayList<>(megaboxTargets);
        }

        public List<String> getLottePreferredCinemaNames() {
            return lottePreferredCinemaNames;
        }

        public void setLottePreferredCinemaNames(List<String> lottePreferredCinemaNames) {
            this.lottePreferredCinemaNames = lottePreferredCinemaNames == null ? new ArrayList<>() : new ArrayList<>(lottePreferredCinemaNames);
        }

        public List<String> getLottePreferredCinemaIds() {
            return lottePreferredCinemaIds;
        }

        public void setLottePreferredCinemaIds(List<String> lottePreferredCinemaIds) {
            this.lottePreferredCinemaIds = lottePreferredCinemaIds == null ? new ArrayList<>() : new ArrayList<>(lottePreferredCinemaIds);
        }

        public List<String> getMegaboxAreaCodes() {
            return megaboxAreaCodes;
        }

        public void setMegaboxAreaCodes(List<String> megaboxAreaCodes) {
            this.megaboxAreaCodes = megaboxAreaCodes == null ? new ArrayList<>() : new ArrayList<>(megaboxAreaCodes);
        }
    }

    public static class SeatProperties {

        private boolean enabled = false;
        private String cron = "0 0/30 * * * *";
        private int lookaheadHours = 6;
        private int limit = 100;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }

        public int getLookaheadHours() {
            return lookaheadHours;
        }

        public void setLookaheadHours(int lookaheadHours) {
            this.lookaheadHours = lookaheadHours;
        }

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }
    }

    public static class CgvTarget {
        private String siteNo = "";
        private String movieNo = "";

        public String getSiteNo() {
            return siteNo;
        }

        public void setSiteNo(String siteNo) {
            this.siteNo = siteNo;
        }

        public String getMovieNo() {
            return movieNo;
        }

        public void setMovieNo(String movieNo) {
            this.movieNo = movieNo;
        }
    }

    public static class LotteTarget {
        private String cinemaSelector = "";
        private String representationMovieCode = "";

        public String getCinemaSelector() {
            return cinemaSelector;
        }

        public void setCinemaSelector(String cinemaSelector) {
            this.cinemaSelector = cinemaSelector;
        }

        public String getRepresentationMovieCode() {
            return representationMovieCode;
        }

        public void setRepresentationMovieCode(String representationMovieCode) {
            this.representationMovieCode = representationMovieCode;
        }
    }

    public static class MegaboxTarget {
        private String movieNo = "";
        private String areaCode = "";

        public String getMovieNo() {
            return movieNo;
        }

        public void setMovieNo(String movieNo) {
            this.movieNo = movieNo;
        }

        public String getAreaCode() {
            return areaCode;
        }

        public void setAreaCode(String areaCode) {
            this.areaCode = areaCode;
        }
    }
}
