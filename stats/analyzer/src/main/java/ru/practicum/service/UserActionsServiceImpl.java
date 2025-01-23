package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.model.ActionType;
import ru.practicum.model.UserAction;
import ru.practicum.repository.UserActionsRepository;

@Service
@RequiredArgsConstructor
public class UserActionsServiceImpl implements UserActionsService {
    private final UserActionsRepository userActionsRepository;

    @Override
    public void saveAction(UserActionAvro userActionAvro) {
        UserAction userAction = UserAction.builder()
                .userId(userActionAvro.getUserId())
                .eventId(userActionAvro.getEventId())
                .maxWeight(ActionType.valueOf(userActionAvro.getActionType().name()).getWeight())
                .actionType(ActionType.valueOf(userActionAvro.getActionType().name()))
                .timestamp(userActionAvro.getTimestamp())
                .build();
        userActionsRepository.save(userAction);
    }
}
