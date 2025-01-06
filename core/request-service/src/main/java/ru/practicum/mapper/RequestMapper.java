package ru.practicum.mapper;

import org.mapstruct.*;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.model.Request;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface RequestMapper {

    @Mappings({
            @Mapping(source = "eventId", target = "event"),
            @Mapping(source = "requesterId", target = "requester")
    })
    ParticipationRequestDto toParticipationRequestDto(Request request);

}