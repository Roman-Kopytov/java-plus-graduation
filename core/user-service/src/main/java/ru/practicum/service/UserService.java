package ru.practicum.service;


import ru.practicum.dto.user.AdminUserParams;
import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;

import java.util.List;

public interface UserService {
    UserShortDto getById(long userId);

    List<UserShortDto> getAllUsersByIds(List<Long> userIds);

    List<UserDto> getAll(AdminUserParams params);

    UserDto create(NewUserRequest dto);

    void delete(long id);
}
