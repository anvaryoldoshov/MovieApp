package com.example.movieapp.repository;

import com.example.movieapp.entities.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenreRepo extends JpaRepository<Genre, Long> {

    boolean existsByNameIgnoreCase(String name);

}
