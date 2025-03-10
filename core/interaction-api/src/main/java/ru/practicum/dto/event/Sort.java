package ru.practicum.dto.event;

public enum Sort {
    EVENT_DATE,
    VIEWS,
    TOP_RATING;


    public static Sort from(String sortString) {
        for (Sort sort : Sort.values()) {
            if (sort.name().equalsIgnoreCase(sortString)) {
                return sort;
            }
        }
        throw new IllegalArgumentException("Wrong sort enum [" + sortString + "]");
    }
}
