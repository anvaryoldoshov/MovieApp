package com.example.movieapp.mapper;

import com.example.movieapp.dto.UserNotificationDto;
import com.example.movieapp.entities.UserNotification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserNotificationMapper {
    UserNotificationDto toDto(UserNotification notification);
}
