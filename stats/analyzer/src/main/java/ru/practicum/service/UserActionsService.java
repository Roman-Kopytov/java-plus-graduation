package ru.practicum.service;

import ru.practicum.ewm.stats.avro.UserActionAvro;

public interface UserActionsService {

    void saveAction(UserActionAvro userActionAvro);
}
