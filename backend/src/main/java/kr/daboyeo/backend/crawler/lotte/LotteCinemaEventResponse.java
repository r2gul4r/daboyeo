package kr.daboyeo.backend.crawler.lotte;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LotteCinemaEventResponse {

    @JsonProperty("Items")
    private List<Item> items = new ArrayList<>();

    @JsonProperty("TotalCount")
    private Integer totalCount;

    public List<Item> getItems() {
        return items == null ? Collections.emptyList() : items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {

        @JsonProperty("EventID")
        private String eventId;

        @JsonProperty("EventName")
        private String eventName;

        @JsonProperty("ImageUrl")
        private String imageUrl;

        @JsonProperty("ProgressStartDate")
        private String progressStartDate;

        @JsonProperty("ProgressEndDate")
        private String progressEndDate;

        @JsonProperty("ImageAlt")
        private String imageAlt;

        @JsonProperty("CloseNearYN")
        private Object closeNearYn;

        @JsonProperty("RemainsDayCount")
        private Integer remainsDayCount;

        public String getEventId() {
            return eventId;
        }

        public void setEventId(String eventId) {
            this.eventId = eventId;
        }

        public String getEventName() {
            return eventName;
        }

        public void setEventName(String eventName) {
            this.eventName = eventName;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        public String getProgressStartDate() {
            return progressStartDate;
        }

        public void setProgressStartDate(String progressStartDate) {
            this.progressStartDate = progressStartDate;
        }

        public String getProgressEndDate() {
            return progressEndDate;
        }

        public void setProgressEndDate(String progressEndDate) {
            this.progressEndDate = progressEndDate;
        }

        public String getImageAlt() {
            return imageAlt;
        }

        public void setImageAlt(String imageAlt) {
            this.imageAlt = imageAlt;
        }

        public Object getCloseNearYn() {
            return closeNearYn;
        }

        public void setCloseNearYn(Object closeNearYn) {
            this.closeNearYn = closeNearYn;
        }

        public Integer getRemainsDayCount() {
            return remainsDayCount;
        }

        public void setRemainsDayCount(Integer remainsDayCount) {
            this.remainsDayCount = remainsDayCount;
        }
    }
}
