package com.example.partner.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MovieDTO {

    private Long id;
    private String poster_path;
    private String backdrop_path;
    private String title;
    private String overview;
    private String release_date;
    private String vote_average;
    private String original_title;
    private double popularity;
}
