package com.example.partner.common;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Component
public class ApiUrl {
    private final String BASE_URL = "https://api.themoviedb.org/3";
    @Value("${tmdb.api.key}")
    private String apiKey;
    private final String MOVIE_POPULAR_URL = BASE_URL + "/movie/popular";
    private final String MOVIE_TRENDING_DAY_URL = BASE_URL + "/trending/movie/day";
    private final String MOVIE_TRENDING_WEEK_URL = BASE_URL + "/trending/movie/week";

    private final String TV_POPULAR_URL = BASE_URL + "/tv/popular";
    private final String TV_TRENDING_DAY_URL = BASE_URL + "/trending/tv/day";
    private final String TV_TRENDING_WEEK_URL = BASE_URL + "/trending/tv/week";
    private final String MOVIE_POPULAR_MEDIA_URL = BASE_URL + "/movie/upcoming";


    public String getMovieDetailsUrl(Long movieId) {
        return BASE_URL + "/movie/" + movieId + "?api_key=" + apiKey + "&language=zh-CN";
    }

    public String getMoviePopularUrl() {
        return MOVIE_POPULAR_URL + "?api_key=" + apiKey + "&language=zh-CN";
    }

    public String getMovieTrendingDayUrl() {
        return MOVIE_TRENDING_DAY_URL + "?api_key=" + apiKey + "&language=zh-CN";
    }

    public String getMovieTrendingWeekUrl() {
        return MOVIE_TRENDING_WEEK_URL + "?api_key=" + apiKey + "&language=zh-CN";
    }

    public String getTVPopularUrl(int page) {
        return TV_POPULAR_URL + "?api_key=" + apiKey + "&language=zh-CN&page=" + page;
    }

    public String getTVTrendingDayUrl() {
        return TV_TRENDING_DAY_URL + "?api_key=" + apiKey + "&language=zh-CN";
    }

    public String getTVTrendingWeekUrl() {
        return TV_TRENDING_WEEK_URL + "?api_key=" + apiKey + "&language=zh-CN";
    }

    public String getMovieUpcomingMediaUrl(int page){ return MOVIE_POPULAR_MEDIA_URL + "?api_key=" + apiKey +"&language=zh-CN&page=" + page;}

    public String getMovieVideoUrl(Long movieId) {
        return BASE_URL + "/movie/" + movieId + "/videos?api_key=" + apiKey + "&language=en-US";
    }

    public String getTvVideoUrl(Long tvId) {
        return BASE_URL + "/tv/" + tvId + "/videos?api_key=" + apiKey + "&language=en-US";
    }

    public String getMovieImagesUrl(Long movieId) {
        return BASE_URL + "/movie/" + movieId + "/images?api_key=" + apiKey;
    }

    public String getTVOnAirUrl(int page) {
        return BASE_URL + "/tv/on_the_air?api_key=" + apiKey + "&language=zh-CN&page=" + page;
    }

    public String getTvImagesUrl(Long tvId) {
        return BASE_URL + "/tv/" + tvId + "/images?api_key=" + apiKey;
    }

    public String getTvDetailsUrl(Long Tvid) {
        return BASE_URL + "/tv/" + Tvid + "?api_key=" + apiKey + "&language=zh-CN";
    }
}
