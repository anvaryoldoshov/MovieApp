package com.example.movieapp.dto;

import com.example.movieapp.enums.Role;
import lombok.Data;

@Data
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private Long userId;
    private Role role;
}
