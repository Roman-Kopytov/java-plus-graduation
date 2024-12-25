package ru.practicum.service.compilation;

import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.dto.compilation.PublicCompilationParams;
import ru.practicum.dto.compilation.UpdateCompilationRequest;

import java.util.List;

public interface CompilationService {
    CompilationDto create(NewCompilationDto newCompilationDto);

    void delete(long compId);

    CompilationDto update(long compId, UpdateCompilationRequest updateCompilationRequest);

    List<CompilationDto> getAll(PublicCompilationParams params);

    CompilationDto getById(long compId);
}
