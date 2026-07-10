package com.example.movieapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetDetailsResponse {
    private Long id;
    private String title;
    private List<EpisodePartDto> parts;
    private boolean hasAccess;
}
