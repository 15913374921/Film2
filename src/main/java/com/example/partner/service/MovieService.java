package com.example.partner.service;

import cn.hutool.core.util.StrUtil;
import com.example.partner.common.ApiUrl;
import com.example.partner.common.Constants;
import com.example.partner.common.enums.MovieCodeEnum;
import com.example.partner.domain.*;
import com.example.partner.exception.ServiceException;
import com.example.partner.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MovieService {


    private final RestTemplate restTemplate;

    private final ApiUrl apiUrl;

    private final String redisKey = "upcoming:Pages_page_";
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);


    // 使用代理创建 RestTemplate
    public MovieService(ApiUrl apiUrl) {
        this.restTemplate = createRestTemplateWithProxy();
        this.apiUrl = apiUrl;
    }

    // 创建代理
    private RestTemplate createRestTemplateWithProxy() {
        // 创建一个代理对象，指向 Clash 的本地地址和端口
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 7890)))
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS);

        OkHttpClient okHttpClient = builder.build();
        ClientHttpRequestFactory requestFactory = new OkHttp3ClientHttpRequestFactory(okHttpClient);
        return new RestTemplate(requestFactory);
    }

    // 根据id获取电影
    public MovieDTO getMovieDetails(Long movieId) {
        MovieDTO movieDTO = null;
        String url = apiUrl.getMovieDetailsUrl(movieId);
        movieDTO = restTemplate.getForObject(url, MovieDTO.class);
        return movieDTO;
    }

    // 获取热门电影
    public List<MovieDTO> getPopularMovieDetails(String type, int page) {
        String moviePrefix = MovieCodeEnum.getValue(type);
        if (StrUtil.isBlank(moviePrefix)) {
            throw new ServiceException("不支持的获取电影信息验证类型");
        }

        List<MovieDTO> movieList = getCachedMovies(moviePrefix);
        if (movieList == null) {
            // 根据电影类型获取 URL
            String url = getMovieUrl(type, page);
            try {
                updateMovie(url, type);
            } catch (Exception e) {
                log.error("更新缓存时出错: ", e);
                throw new ServiceException("更新电影信息失败");
            }
            movieList = getCachedMovies(moviePrefix); // 再次尝试从缓存中获取
        }

        return movieList;
    }

    // 在缓存中获取值方法
    private List<MovieDTO> getCachedMovies(String moviePrefix) {
        return RedisUtils.getCacheObject(Constants.Po_Movies + moviePrefix);
    }

    // 根据类型获取电影urlapi
    private String getMovieUrl(String type, int page) {
        if (MovieCodeEnum.POPULAR.equals(MovieCodeEnum.getEnum(type))) {
            return apiUrl.getMoviePopularUrl();
        } else if (MovieCodeEnum.DAILY.equals(MovieCodeEnum.getEnum(type))) {
            return apiUrl.getMovieTrendingDayUrl();
        } else if (MovieCodeEnum.WEEK.equals(MovieCodeEnum.getEnum(type))) {
            return apiUrl.getMovieTrendingWeekUrl();
        }
        throw new ServiceException("不支持的获取电影信息验证类型");
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void updatePopularMoviesCache() {
        String type = MovieCodeEnum.POPULAR.getType();
        String url = apiUrl.getMoviePopularUrl();
        updateMovie(url,type);
    }

    @Scheduled(cron = "0 0 0/3 * * ?")
    public void updateDAPopularMoviesCache() {
        String type = MovieCodeEnum.DAILY.getType();
        String url = apiUrl.getMovieTrendingDayUrl();
        System.out.println(url);
        updateMovie(url,type);
    }

    @Scheduled(cron = "0 0 0 */3 * ?")
    public void updateWEPopularMoviesCache() {
        String type = MovieCodeEnum.WEEK.getType();
        String url = apiUrl.getMovieTrendingWeekUrl();
        updateMovie(url,type);
    }

    private void updateMovie( String url,String type) {
        PopularMoviesResponseDTO popularMoviesResponseDTO;
        List<MovieDTO> movieList;
        popularMoviesResponseDTO = restTemplate.getForObject(url,PopularMoviesResponseDTO.class);
        movieList = popularMoviesResponseDTO.getResults();
        for(int i = 0; i < movieList.size(); i++) {
            String originalVoteAverageStr = movieList.get(i).getVote_average();
            double originalVoteAverage = Double.parseDouble(originalVoteAverageStr);
            int decimalIndex = originalVoteAverageStr.indexOf(".");

            // 检查小数点后是否有超过一位
            if (decimalIndex != -1 && (originalVoteAverageStr.length() - decimalIndex - 1) > 1) {
                double roundedVoteAverage = Math.round(originalVoteAverage * 10) / 10.0; // 四舍五入到一位小数
                movieList.get(i).setVote_average(String.valueOf(roundedVoteAverage));
            }
        }
        String MoviePrefix = MovieCodeEnum.getValue(type);
        String key = Constants.Po_Movies + MoviePrefix;
        if(MovieCodeEnum.POPULAR.equals(MovieCodeEnum.getEnum(type))) {
            RedisUtils.setCacheObject(key, movieList, 24, TimeUnit.HOURS);
        }
        if(MovieCodeEnum.DAILY.equals(MovieCodeEnum.getEnum(type))) {
            RedisUtils.setCacheObject(key, movieList, 24, TimeUnit.DAYS);
        }
        if(MovieCodeEnum.WEEK.equals(MovieCodeEnum.getEnum(type))) {
            RedisUtils.setCacheObject(key,movieList,4,TimeUnit.DAYS);
        }
    }

    public void cacheAllPopularMovies() {
        List<MovieDTO> allMovies = new ArrayList<>();
        List<Future<List<MovieDTO>>> futures = new ArrayList<>();
        try {
            // 第一次请求获取第一页数据和总页数
            PopularMoviesResponseDTO firstPageResponse = restTemplate
                    .getForObject(apiUrl.getMovieUpcomingMediaUrl(1), PopularMoviesResponseDTO.class);
            if (firstPageResponse == null || firstPageResponse.getResults() == null) {
                throw new RuntimeException("无法获取第一页数据");
            }

            allMovies.addAll(firstPageResponse.getResults());
            int totalPages = firstPageResponse.getTotalPages();

            // 限制并发请求数量,每批次最多10个并发请求
            int batchSize = 10;
            for (int page = 2; page <= totalPages; page += batchSize) {
                List<Future<List<MovieDTO>>> batchFutures = new ArrayList<>();
                for (int i = 0; i < batchSize && (page + i) <= totalPages; i++) {
                    final int currentPage = page + i;
                    batchFutures.add(executorService.submit(() -> fetchMovieData(currentPage)));
                }

                // 等待当前批次完成
                for (Future<List<MovieDTO>> future : batchFutures) {
                    try {
                        List<MovieDTO> pageData = future.get(5, TimeUnit.SECONDS);  // 设置超时时间
                        if (pageData != null) {
                            allMovies.addAll(pageData);
                        }
                    } catch (TimeoutException e) {
                        log.error("获取数据超时", e);
                    } catch (Exception e) {
                        log.error("获取页面数据失败", e);
                    }
                }
            }

            // 获取当前日期和两个星期后的日期
            LocalDate today = LocalDate.now();
            LocalDate twoWeeksLater = today.plusWeeks(2);

            // 过滤出两个星期后上映的电影并按照欢迎度降序排序
            List<MovieDTO> sortedMovies = allMovies.stream()
                    .filter(movie -> {
                        String releaseDateStr = movie.getRelease_date();
                        if (releaseDateStr == null || releaseDateStr.isEmpty()) {
                            return false; // 如果 release_date 为空，跳过该电影
                        }
                        try {
                            LocalDate releaseDate = LocalDate.parse(releaseDateStr);
                            return releaseDate.isAfter(today) && releaseDate.isBefore(twoWeeksLater.plusDays(1));
                        } catch (DateTimeParseException e) {
                            log.error("解析日期失败: {}", releaseDateStr, e);
                            return false; // 如果解析失败，跳过该电影
                        }
                    })
                    .sorted(Comparator.comparing(MovieDTO::getPopularity).reversed()) // 按照欢迎度降序排序
                    .collect(Collectors.toList());

            // 缓存结果
            cacheDataInRedis(sortedMovies);
        } catch (Exception e) {
            log.error("缓存分页数据时出错", e);
            throw new RuntimeException("缓存分页数据时出错", e);
        }
    }

    private List<MovieDTO> fetchMovieData(int page) {
        try {
            PopularMoviesResponseDTO response = restTemplate.getForObject(
                    apiUrl.getMovieUpcomingMediaUrl(page), 
                    PopularMoviesResponseDTO.class
            );
            return response != null ? response.getResults() : new ArrayList<>();
        } catch (Exception e) {
            log.error("获取第 {} 页数据失败", page, e);
            return new ArrayList<>();
        }
    }

    private void cacheDataInRedis(List<MovieDTO> sortedMovieShows) {

        int pageSize = 20;
        int totalItems = sortedMovieShows.size();
        int totalPagesAfterSorting = (int) Math.ceil((double) totalItems / pageSize);

        // 并发缓存数据到Redis
        List<Future<?>> cacheFutures = new ArrayList<>();
        for (int page = 1; page <= totalPagesAfterSorting; page++) {
            final int currentPage = page;
            cacheFutures.add(executorService.submit(() -> {
                int start = (currentPage - 1) * pageSize;
                int end = Math.min(start + pageSize, totalItems);
                List<MovieDTO> pageData = new ArrayList<>(sortedMovieShows.subList(start, end));
                RedisUtils.setCacheObject(redisKey + "_page_" + currentPage, pageData, 4, TimeUnit.HOURS);
            }));
        }

        // 等待所有缓存操作完成
        for (Future<?> future : cacheFutures) {
            try {
                future.get(2, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("缓存数据到Redis失败", e);
            }
        }
    }

    public List<MediaDTO> getUpComingMedia() {
        List<MediaDTO> latestOfficialTrailers = new ArrayList<>();
        try {
            List<MovieDTO> movieDTOList = RedisUtils.getCacheObject(redisKey + "_page_1");
            if (movieDTOList == null || movieDTOList.isEmpty()) {
                log.warn("未找到缓存的电影数据");
                return latestOfficialTrailers;
            }

            List<Long> movieIds = movieDTOList.stream()
                    .map(MovieDTO::getId)
                    .collect(Collectors.toList());

            // 并发获取预告片数据
            List<Future<MediaDTO>> trailerFutures = movieIds.stream()
                    .map(movieId -> executorService.submit(() -> fetchMovieTrailer(movieId)))
                    .collect(Collectors.toList());

            

            // 收集结果
            for (Future<MediaDTO> future : trailerFutures) {
                try {
                    MediaDTO trailer = future.get(3, TimeUnit.SECONDS);
                    if (trailer != null) {
                        latestOfficialTrailers.add(trailer);
                    }
                } catch (TimeoutException e) {
                    log.error("获取预告片超时", e);
                } catch (Exception e) {
                    log.error("获取预告片失败", e);
                }
            }

            if (!latestOfficialTrailers.isEmpty()) {
                RedisUtils.setCacheObject(Constants.Me_Movies, latestOfficialTrailers, 4, TimeUnit.HOURS);
            }
        } catch (Exception e) {
            log.error("处理预告片数据失败", e);
            throw new RuntimeException(e);
        }
        return latestOfficialTrailers;
    }

    private MediaDTO fetchMovieTrailer(Long movieId) {
        List<ImageMovieDTO> imageMovieDTOList;
        String firstImageFilePath = null;
        try {
            // 获取电影信
            MovieDTO movieDTO = restTemplate.getForObject(
                    apiUrl.getMovieDetailsUrl(movieId), 
                    MovieDTO.class
            );

            // 获取预告片数据
            PopularMovieMediaResponse response = restTemplate.getForObject(
                    apiUrl.getMovieVideoUrl(movieId), 
                    PopularMovieMediaResponse.class
            );
            // 获取预告片海报
            MovieImagesResponse movieImagesResponse = restTemplate.getForObject(
                    apiUrl.getMovieImagesUrl(movieId), 
                    MovieImagesResponse.class
            );

            imageMovieDTOList = movieImagesResponse.getBackdrops();

            if (movieImagesResponse != null && movieImagesResponse.getBackdrops() != null && !movieImagesResponse.getBackdrops().isEmpty()) {
                imageMovieDTOList = movieImagesResponse.getBackdrops();
                
                // 获取第一张图片
                ImageMovieDTO firstImageMovieDTO = imageMovieDTOList.get(0);
                firstImageFilePath = firstImageMovieDTO.getFile_path();

            } else {
                log.error("获取电影 ID {} 的海报出错（该影片可能没有海报）",movieId);
            }

            if (response != null && response.getResults() != null) {
                // 获取最新的预告片
                MediaDTO trailer = response.getResults().stream()
                        .filter(video -> "Trailer".equalsIgnoreCase(video.getType()) 
                                && "YouTube".equalsIgnoreCase(video.getSite()))
                        .max(Comparator.comparing(MediaDTO::getPublishedAtAsLocalDateTime))
                        .orElse(null);

                if (trailer != null) {
                    // 将电影名称添加到预告片中
                    trailer.setMovieTitle(movieDTO.getTitle());
                    trailer.setBackdrop_path(movieDTO.getBackdrop_path());
                    trailer.setFilePath(firstImageFilePath);
                    return trailer;
                }
            }
        } catch (Exception e) {
            log.error("获取电影 ID {} 的预告片出错", movieId, e);
        }
        return null;
    }


}
