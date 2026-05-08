package kr.daboyeo.backend.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class MovieEvent {

    private Long id;
    private String source;
    private String cinema;
    private String eventId;
    private Category category;
    private String title;
    private String imageUrl;
    private String eventUrl;
    private LocalDate startDate;
    private LocalDate endDate;
    private String dDay;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public MovieEvent() {
    }

    public MovieEvent(
        Long id,
        String source,
        String cinema,
        String eventId,
        Category category,
        String title,
        String imageUrl,
        String eventUrl,
        LocalDate startDate,
        LocalDate endDate,
        String dDay,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        this.id = id;
        this.source = source;
        this.cinema = cinema;
        this.eventId = eventId;
        this.category = category;
        this.title = title;
        this.imageUrl = imageUrl;
        this.eventUrl = eventUrl;
        this.startDate = startDate;
        this.endDate = endDate;
        this.dDay = dDay;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public MovieEvent(
        String source,
        String cinema,
        String eventId,
        Category category,
        String title,
        String imageUrl,
        String eventUrl,
        LocalDate startDate,
        LocalDate endDate,
        String dDay
    ) {
        this(
            null,
            source,
            cinema,
            eventId,
            category,
            title,
            imageUrl,
            eventUrl,
            startDate,
            endDate,
            dDay,
            null,
            null
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getCinema() {
        return cinema;
    }

    public void setCinema(String cinema) {
        this.cinema = cinema;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getEventUrl() {
        return eventUrl;
    }

    public void setEventUrl(String eventUrl) {
        this.eventUrl = eventUrl;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getDDay() {
        return dDay;
    }

    public void setDDay(String dDay) {
        this.dDay = dDay;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
