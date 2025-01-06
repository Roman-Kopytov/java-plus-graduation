package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.feignclient.UserClient;
import ru.practicum.service.UserService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/feign/users")
public class UserFeignController implements UserClient {
    private final UserService userService;

    @Override
    @GetMapping("/{userId}")
    public UserShortDto getUserById(@PathVariable long userId) {
        return userService.getById(userId);
    }

    @Override
    @GetMapping
    public List<UserShortDto> getUsersByIds(@RequestBody List<Long> userIds) {

        return userService.getAllUsersByIds(userIds);
    }
}
