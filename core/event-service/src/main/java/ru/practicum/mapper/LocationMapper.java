package ru.practicum.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import ru.practicum.dto.location.LocationDto;
import ru.practicum.model.Location;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface LocationMapper {

    Location toLocation(LocationDto locationDto);
}
