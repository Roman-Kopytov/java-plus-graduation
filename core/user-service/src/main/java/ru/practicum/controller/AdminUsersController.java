package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.user.AdminUserParams;
import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.service.UserService;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminUsersController {
    private final UserService userService;

    @GetMapping("/users")
    public List<UserDto> getAll(@RequestParam(value = "ids", required = false) List<Long> ids,
                                @RequestParam(value = "from", defaultValue = "0") int from,
                                @RequestParam(value = "size", defaultValue = "10") int size) {
        AdminUserParams params = AdminUserParams.builder()
                .ids(ids)
                .from(from)
                .size(size)
                .build();
        return userService.getAll(params);
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@Validated @RequestBody NewUserRequest dto) {
        return userService.create(dto);
    }

    @DeleteMapping("/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long userId) {
        userService.delete(userId);
    }
}
