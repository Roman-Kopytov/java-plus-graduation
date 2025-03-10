package ru.practicum.category.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import ru.practicum.category.model.Category;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface CategoryMapper {

    @Mapping(target = "id", ignore = true)
    Category toCategory(final NewCategoryDto newCategoryDto);

    CategoryDto toCategoryDto(final Category category);

}
