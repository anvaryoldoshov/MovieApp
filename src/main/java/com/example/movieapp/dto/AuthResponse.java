
package com.example.movieapp.dto;

import com.example.movieapp.enums.Role;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private Long id;
    private String username;
    private String email;
    private Long userId;
    private Role role;
    private String token;
    private String refreshToken;
    private String deviceId;
}