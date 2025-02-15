package com.example.partner.controller;


import com.example.partner.common.Result;
import com.example.partner.domain.MediaDTO;
import com.example.partner.domain.TvDTO;
import com.example.partner.service.TvService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tv")
public class TvController {

    @Autowired
    private TvService tvService;

    @ApiOperation(value = "获取热门剧集详情", notes = "根据电影类型和页码获取热门剧集")
    @GetMapping("")
    public Result getPopularTV(@RequestParam String type, @RequestParam(defaultValue = "1") int page) {
        List<TvDTO> popularTv = tvService.getPopularTvDetails(type,page);
        return Result.success(popularTv);
    }

    @ApiOperation(value = "获取热门剧集预告片", notes = "获取热门剧集预告片")
    @GetMapping("/trailerList")
    public Result getLatestTrailers() {
        List<MediaDTO> latestTrailers = tvService.getTvMedia();
        return Result.success(latestTrailers);
    }
}
