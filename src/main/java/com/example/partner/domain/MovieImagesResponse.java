package com.example.partner.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MovieImagesResponse {
    private List<ImageMovieDTO> backdrops;
}
