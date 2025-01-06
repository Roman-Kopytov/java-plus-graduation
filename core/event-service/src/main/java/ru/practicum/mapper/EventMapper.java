package ru.practicum.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import ru.practicum.category.model.Category;
import ru.practicum.dto.event.*;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.model.Event;
import ru.practicum.model.Location;

import java.time.LocalDateTime;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface EventMapper {
    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "rating", source = "rating")
    @Mapping(target = "views", source = "views")
    @Mapping(target = "initiator", source = "userShortDto")
    EventFullDto toEventFullDto(final Event event, final UserShortDto userShortDto, final long rating, final long views);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "category", source = "category")
    @Mapping(target = "location", source = "location")
    @Mapping(target = "state", source = "state")
    @Mapping(target = "createdOn", source = "createdOn")
    Event toEvent(final NewEventDto newEventDto, final Category category, final Location location,
                  final EventState state, LocalDateTime createdOn);

    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "rating", source = "rating")
    @Mapping(target = "views", source = "views")
    @Mapping(target = "initiator", source = "userShortDto")
    EventShortDto toEventShortDto(final Event event, final UserShortDto userShortDto, final long rating, final long views);

    EventRequestDto toEventRequestDto(final Event event);
}
