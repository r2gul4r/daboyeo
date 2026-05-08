package kr.daboyeo.backend.crawler.lotte;

import kr.daboyeo.backend.domain.Category;

public enum LotteCinemaEventCategory {
    HOT("10", Category.HOT, 1),
    MOVIE("20", Category.MOVIE, 2),
    PREMIERE("40", Category.PREMIERE, 3),
    DISCOUNT("50", Category.DISCOUNT, 4);

    private final String code;
    private final Category domainCategory;
    private final int priority;

    LotteCinemaEventCategory(String code, Category domainCategory, int priority) {
        this.code = code;
        this.domainCategory = domainCategory;
        this.priority = priority;
    }

    public String getCode() {
        return code;
    }

    public Category getDomainCategory() {
        return domainCategory;
    }

    public int getPriority() {
        return priority;
    }
}
