package ru.practicum.service;

import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.user.AdminUserParams;
import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.UserMapper;
import ru.practicum.model.QUser;
import ru.practicum.model.User;
import ru.practicum.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;

    private final UserRepository userRepository;

    @Override
    public UserShortDto getById(long userId) {
        User userFromRepository = getUserFromRepository(userId);
        return userMapper.toUserShortDto(userFromRepository);
    }

    @Override
    public List<UserShortDto> getAllUsersByIds(List<Long> userIds) {
        List<User> allById = userRepository.findAllById(userIds);
        return allById.stream().map(userMapper::toUserShortDto).toList();
    }

    @Override
    public List<UserDto> getAll(AdminUserParams params) {
        QUser user = QUser.user;

        if (params.getIds() != null && !params.getIds().isEmpty()) {
            BooleanExpression condition = user.id.in(params.getIds());
            List<User> users = (List<User>) userRepository.findAll(condition);
            return users.stream()
                    .map(userMapper::toUserDto)
                    .collect(Collectors.toList());
        }

        PageRequest pageRequest = PageRequest.of(params.getFrom() / params.getSize(), params.getSize());
        Page<User> usersPage = userRepository.findAll(pageRequest);

        return usersPage.stream()
                .map(userMapper::toUserDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserDto create(NewUserRequest dto) {
        User user = userMapper.toUser(dto);

        return userMapper.toUserDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public void delete(long id) {
        getUserFromRepository(id);
        userRepository.deleteById(id);
    }

    private User getUserFromRepository(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id= " + userId + " was not found"));
    }
}
