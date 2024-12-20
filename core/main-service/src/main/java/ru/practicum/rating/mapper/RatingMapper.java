package ru.practicum.rating.mapper;

import org.mapstruct.*;
import ru.practicum.rating.dto.RatingDto;
import ru.practicum.rating.model.Rating;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface RatingMapper {

    @Mappings({
            @Mapping(source = "user.id", target = "userId"),
    })
    RatingDto toDto(Rating rating);
}
