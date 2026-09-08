package com.example.movieapp.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "series")
public class Series {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String imagePath;

    @Column(columnDefinition = "TEXT")
    private String title;

    private String status;

    private Long monthlyPrice;   // 1 oylik narx (null = tarif yo'q)
    private Long quarterlyPrice; // 3 oylik narx (null = tarif yo'q)

    private Integer sortOrder; // adminda ro'yxatdagi tartibi (kichik = birinchi)

    private Integer freeEpisodesCount; // birinchi N ta epizod obunasiz ham ochiq (null/0 = yo'q)

    private String bunnyCollectionId; // Bunny Stream'dagi shu serialga tegishli Collection GUID'i (ixtiyoriy)

    @OneToMany(mappedBy = "series", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Banner> banners;

    @OneToMany(mappedBy = "series", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Episode> episodes;

    @ManyToMany
    @JoinTable(
            name = "series_genres",
            joinColumns = @JoinColumn(name = "series_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private List<Genre> genres;

}
