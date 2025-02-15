package com.example.partner.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PopularMovieMediaResponse {

    @JsonProperty("results")  // 指定 JSON 字段映射到这个属性
    private List<MediaDTO> results;

    @JsonProperty("id")
    private Long Id;
}
