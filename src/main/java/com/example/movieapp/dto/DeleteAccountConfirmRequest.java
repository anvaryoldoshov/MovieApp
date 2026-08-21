package com.example.movieapp.dto;

import lombok.Data;

@Data
public class DeleteAccountConfirmRequest {
    private String email;
    private String code;
}
