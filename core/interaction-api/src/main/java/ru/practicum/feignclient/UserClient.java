package ru.practicum.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.dto.user.UserShortDto;

import java.util.List;

@FeignClient(name = "ewm-user-service")
public interface UserClient {
    @GetMapping("feign/users/{userId}")
    UserShortDto getUserById(@PathVariable long userId);

    @GetMapping("feign/users")
    List<UserShortDto> getUsersByIds(@RequestBody List<Long> userIds);
}
