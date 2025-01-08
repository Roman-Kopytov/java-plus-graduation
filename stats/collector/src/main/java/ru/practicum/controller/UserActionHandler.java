package ru.practicum.controller;

import ru.yandex.practicum.grpc.stats.action.UserActionProto;

public interface UserActionHandler {
    void handle(UserActionProto userAction);
}
