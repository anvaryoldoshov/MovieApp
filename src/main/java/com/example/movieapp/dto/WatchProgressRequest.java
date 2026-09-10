package com.example.movieapp.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WatchProgressRequest {

    @NotNull
    @Min(0)
    private Integer positionSeconds;
}
