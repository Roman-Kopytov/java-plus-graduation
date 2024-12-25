package ru.practicum.mapper;

import org.mapstruct.*;
import ru.practicum.dto.rating.RatingDto;
import ru.practicum.model.Rating;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface RatingMapper {

    @Mappings({
            @Mapping(source = "userId", target = "userId"),
    })
    RatingDto toDto(Rating rating);
}
