package ru.practicum.category.sevice;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;
import ru.practicum.exeption.ConflictException;
import ru.practicum.exeption.NotFoundException;
import ru.practicum.repository.EventRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    private final EventRepository eventRepository;

    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryDto createCategory(NewCategoryDto dto) {
        Category category = categoryMapper.toCategory(dto);

        return categoryMapper.toCategoryDto(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void delete(long catId) {
        categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id= " + catId + " was not found"));

        if (eventRepository.existsByCategoryId(catId)) {
            throw new ConflictException("The category is not empty");
        }
        categoryRepository.deleteById(catId);
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(CategoryDto dto) {
        Category category = categoryRepository.findById(dto.getId())
                .orElseThrow(() -> new NotFoundException("Category with id= " + dto.getId() + " was not found"));
        category.setName(dto.getName());

        return categoryMapper.toCategoryDto(categoryRepository.save(category));
    }

    @Override
    public List<CategoryDto> getAll(int from, int size) {
        Pageable pageable = PageRequest.of(from > 0 ? from / size : 0, size);

        return categoryRepository
                .findAll(pageable)
                .stream()
                .map(categoryMapper::toCategoryDto)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDto getById(long catId) {
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id= " + catId + " was not found"));

        return categoryMapper.toCategoryDto(category);
    }
}
