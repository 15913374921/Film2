package com.example.partner.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TvDTO {
    private Long id;
    private String name;
    private String backdrop_path;
    private String original_name;
    private String poster_path;
    private String first_air_date;
    private String vote_average;
    private double popularity;

}
