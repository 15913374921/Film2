package com.example.partner.controller;

import com.example.partner.domain.MediaDTO;
import com.example.partner.domain.MovieDTO;
import com.example.partner.domain.TvDTO;
import com.example.partner.service.MovieService;
import com.example.partner.service.TvService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.example.partner.common.Result;

import org.springframework.web.bind.annotation.RestController;

/**
* <p>
*  前端控制器
* </p>
*
* @author LZY
* @since 2024-10-26
*/
@RestController
@RequestMapping("/movie")
public class MovieController {


    private MovieService movieService;

    @Autowired
    public MovieController(MovieService tmdbService) {
        this.movieService = tmdbService;
    }

    @ApiOperation(value = "获取电影详情", notes = "根据电影 ID 获取电影详细信息")
    @GetMapping("/{id}")
    public Result getMovie(@PathVariable Long id) {
        MovieDTO movieDetails = movieService.getMovieDetails(id);
        return Result.success(movieDetails);
    }

    @ApiOperation(value = "获取热门电影详情", notes = "根据电影类型和页码获取热门电影")
    @GetMapping("")
    public Result getPopularMovie(@RequestParam String type,@RequestParam(defaultValue = "1") int page) {
        List<MovieDTO> popularMovies = movieService.getPopularMovieDetails(type,page);
        return Result.success(popularMovies);
    }

    @ApiOperation(value = "获取热门预告片", notes = "获取热门预告片")
    @GetMapping("/trailerList")
    public Result getLatestTrailers() {
        List<MediaDTO> latestTrailers = movieService.getUpComingMedia();
        return Result.success(latestTrailers);
    }
    

}
