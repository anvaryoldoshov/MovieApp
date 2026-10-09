package com.example.movieapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentPageDto {
    private List<CommentDto> items;
    private int page;
    private boolean hasMore;
    private long total;
}
