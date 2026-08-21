package com.example.movieapp.dto;

import lombok.Data;

@Data
public class DeleteAccountRequest {
    private String email;
    private String password;
}
