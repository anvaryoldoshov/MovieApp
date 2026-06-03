package com.example.movieapp.mapper;

import com.example.movieapp.dto.AuthResponse;
import com.example.movieapp.dto.UserDto;
import com.example.movieapp.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "token", ignore = true)
    @Mapping(target = "refreshToken", ignore = true)
    @Mapping(target = "deviceId", ignore = true)
    AuthResponse toAuthResponse(User user);

    UserDto toUserDto(User user);

    UserDto toDto(User updatedUser);
}
