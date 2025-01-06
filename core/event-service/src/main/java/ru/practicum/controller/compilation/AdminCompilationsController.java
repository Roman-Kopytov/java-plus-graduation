package ru.practicum.controller.compilation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.dto.compilation.UpdateCompilationRequest;
import ru.practicum.service.compilation.CompilationService;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminCompilationsController {
    private final CompilationService compilationService;

    @PostMapping("/compilations")
    @ResponseStatus(HttpStatus.CREATED)
    public CompilationDto create(@Valid @RequestBody final NewCompilationDto newCompilationDto) {
        return compilationService.create(newCompilationDto);
    }

    @DeleteMapping("/compilations/{compId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long compId) {
        compilationService.delete(compId);
    }

    @PatchMapping("/compilations/{compId}")
    public CompilationDto update(@PathVariable long compId,
                                 @Valid @RequestBody UpdateCompilationRequest updateCompilationRequest) {
        return compilationService.update(compId, updateCompilationRequest);
    }
}
