package ru.practicum.dto.event;

public enum EventAction {

    SEND_TO_REVIEW, // отправляется при юзером

    CANCEL_REVIEW, // отменяется юзером

    PUBLISH_EVENT, // публикуется админом

    REJECT_EVENT // отменяется админом
}
