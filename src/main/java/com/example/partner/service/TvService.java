package com.example.partner.service;

import cn.hutool.core.util.StrUtil;
import com.example.partner.common.ApiUrl;
import com.example.partner.common.Constants;
import com.example.partner.common.enums.TvCodeEnum;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TvService {

    String redisKey = "PopularAllTv:";

    private final RestTemplate restTemplate;
    private final ApiUrl apiUrl;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    // 使用代理创建 RestTemplate
    public TvService(ApiUrl apiUrl) {
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

    public List<TvDTO> getPopularTvDetails(String type, int page) {
        String TvPrefix = TvCodeEnum.getValue(type);
        if (StrUtil.isBlank(TvPrefix)) {
            throw new ServiceException("不支持的获取电视信息验证类型");
        }

        List<TvDTO> tvList = getCachedTv(TvPrefix);
        if (tvList == null) {
            // 根据电影类型获取 URL
            String url = getTvUrl(type, page);
            try {
                updateTv(url, type);
            } catch (Exception e) {
                log.error("更新缓存时出错: ", e);
                throw new ServiceException("更新电影信息失败");
            }
            tvList = getCachedTv(TvPrefix); // 再次尝试从缓存中获取
        }

        return tvList;
    }

    private List<TvDTO> getCachedTv(String tvPrefix) {
        return RedisUtils.getCacheObject(Constants.Po_Tv + tvPrefix);
    }

    // 根据类型获取电视urlapi
    private String getTvUrl(String type, int page) {
        if (TvCodeEnum.POPULAR.equals(TvCodeEnum.getEnum(type))) {
            return apiUrl.getTVPopularUrl(1);
        } else if (TvCodeEnum.DAILY.equals(TvCodeEnum.getEnum(type))) {
            return apiUrl.getTVTrendingDayUrl();
        } else if (TvCodeEnum.WEEK.equals(TvCodeEnum.getEnum(type))) {
            return apiUrl.getTVTrendingWeekUrl();
        }
        throw new ServiceException("不支持的获取电视信息验证类型");
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void updatePopularTvCache() {
        String type = TvCodeEnum.POPULAR.getType();
        String url = apiUrl.getTVPopularUrl(1);
        updateTv(url,type);
    }

    @Scheduled(cron = "0 0 0/3 * * ?")
    public void updateDAPopularTvCache() {
        String type = TvCodeEnum.DAILY.getType();
        String url = apiUrl.getTVTrendingDayUrl();
        updateTv(url,type);
    }

    @Scheduled(cron = "0 0 0 */3 * ?")
    public void updateWEPopularTvCache() {
        String type = TvCodeEnum.WEEK.getType();
        String url = apiUrl.getTVTrendingWeekUrl();
        updateTv(url,type);
    }

    private void updateTv( String url,String type) {
        PopularTvResponseDTO popularTvResponseDTO;
        List<TvDTO> tvList;
        popularTvResponseDTO = restTemplate.getForObject(url,PopularTvResponseDTO.class);
        tvList = popularTvResponseDTO.getResults();
        for(int i = 0; i < tvList.size(); i++) {
            String originalVoteAverageStr = tvList.get(i).getVote_average();
            double originalVoteAverage = Double.parseDouble(originalVoteAverageStr);
            int decimalIndex = originalVoteAverageStr.indexOf(".");

            // 检查小数点后是否有超过一位
            if (decimalIndex != -1 && (originalVoteAverageStr.length() - decimalIndex - 1) > 1) {
                double roundedVoteAverage = Math.round(originalVoteAverage * 10) / 10.0; // 四舍五入到一位小数
                tvList.get(i).setVote_average(String.valueOf(roundedVoteAverage));
            }
        }
        String TvPrefix = TvCodeEnum.getValue(type);
        String key = Constants.Po_Tv + TvPrefix;
        if(TvCodeEnum.POPULAR.equals(TvCodeEnum.getEnum(type))) {
            RedisUtils.setCacheObject(key, tvList, 24, TimeUnit.HOURS);
        }
        if(TvCodeEnum.DAILY.equals(TvCodeEnum.getEnum(type))) {
            RedisUtils.setCacheObject(key, tvList, 24, TimeUnit.DAYS);
        }
        if(TvCodeEnum.WEEK.equals(TvCodeEnum.getEnum(type))) {
            RedisUtils.setCacheObject(key,tvList,4,TimeUnit.DAYS);
        }
    }

    public void cacheAllPopularTv() {
        List<TvDTO> allTv = new ArrayList<>();
        try {
            // 获取第一页的数据，同时获取总页数
            PopularTvResponseDTO firstPageResponse = restTemplate
                    .getForObject(apiUrl.getTVPopularUrl(1), PopularTvResponseDTO.class);
            if (firstPageResponse == null || firstPageResponse.getResults() == null) {
                throw new RuntimeException("无法获取第一页数据");
            }

            // 添加第一页数据到列表中
            allTv.addAll(firstPageResponse.getResults());

            // 获取总页数，并限制最多请求 500 页（API 限制）
            int totalPages = Math.min(firstPageResponse.getTotalPages(), 500);

            Semaphore semaphore = new Semaphore(10);
            CountDownLatch latch = new CountDownLatch(totalPages - 1); // 计数器，减去第一页

            for (int page = 2; page <= totalPages; page++) {
                final int currentPage = page;
                executorService.submit(() -> {
                    try {
                        semaphore.acquire();
                        List<TvDTO> pageData = fetchTvData(currentPage);
                        synchronized (allTv) { // 确保线程安全
                            if (pageData != null) {
                                allTv.addAll(pageData);  // 添加到总列表
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        semaphore.release();
                        latch.countDown(); // 计数器减一
                    }
                });
            }

            // 等待所有线程完成
            latch.await();

            // 根据流行度（popularity）进行降序排序
            List<TvDTO> sortedTvShows = allTv.stream()
                    .sorted(Comparator.comparing(TvDTO::getPopularity).reversed())
                    .collect(Collectors.toList());

            // 分页缓存到 Redis
            cacheDataInRedis(sortedTvShows);

        } catch (Exception e) {
            throw new RuntimeException("缓存分页数据时出错", e);
        }
    }

    private List<TvDTO> fetchTvData(int page) {
        PopularTvResponseDTO response = restTemplate.getForObject(apiUrl.getTVPopularUrl(page), PopularTvResponseDTO.class);
        return response != null ? response.getResults() : new ArrayList<>();
    }

    private void cacheDataInRedis(List<TvDTO> sortedTvShows) {
        int pageSize = 20;  // 每页20个数据
        int totalItems = sortedTvShows.size();
        int totalPagesAfterSorting = (int) Math.ceil((double) totalItems / pageSize);

        for (int page = 1; page <= totalPagesAfterSorting; page++) {
            int start = (page - 1) * pageSize;
            int end = Math.min(start + pageSize, totalItems);

            // 获取当前页数据
            List<TvDTO> tvDTOList = new ArrayList<>(sortedTvShows.subList(start, end));

            // 将当前页数据缓存到 Redis
            RedisUtils.setCacheObject(redisKey + "_page_" + page, tvDTOList, 4, TimeUnit.HOURS);
        }
    }

    public List<MediaDTO> getTvMedia() {

        List<MediaDTO> latestOfficialTrailers = new ArrayList<>();
        int tvCount = 20;
        try {
            int currentPage = 1;
            while (latestOfficialTrailers.size() < tvCount) {
                List<TvDTO> tvDTOList = RedisUtils.getCacheObject("onAirTv:page:" + currentPage);
                if (tvDTOList == null || tvDTOList.isEmpty()) {
                    break; // 如果没有数据，则跳出循环
                }
                List<Long> tvIds = tvDTOList.stream()
                        .map(TvDTO::getId)
                        .collect(Collectors.toList());

                List<CompletableFuture<Void>> futures = new ArrayList<>();

                for (Long tvId : tvIds) {
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        try {
                            PopularMovieMediaResponse popularTvResponseDTO = restTemplate.getForObject(apiUrl.getTvVideoUrl(tvId), PopularMovieMediaResponse.class);

                            if (popularTvResponseDTO != null && popularTvResponseDTO.getResults() != null) {
                                List<MediaDTO> trailers = popularTvResponseDTO.getResults().stream()
                                        .filter(video -> "Trailer".equalsIgnoreCase(video.getType()) && "YouTube".equalsIgnoreCase(video.getSite()))
                                        .collect(Collectors.toList());

                                if (!trailers.isEmpty()) {
                                    MediaDTO mostRecentTrailer = trailers.stream()
                                            .max(Comparator.comparing(MediaDTO::getPublishedAtAsLocalDateTime))
                                            .orElse(null);

                                    String tvBackImage = tvDTOList.stream()
                                            .filter(tv -> tv.getId().equals(tvId))
                                            .map(TvDTO::getBackdrop_path)
                                            .findFirst()
                                            .orElse("无背景图海报");

                                    if (mostRecentTrailer != null && tvBackImage != null) {
                                        String tvName = tvDTOList.stream()
                                                .filter(tv -> tv.getId().equals(tvId))
                                                .map(TvDTO::getName)
                                                .findFirst()
                                                .orElse("Unknown Title");

                                        // 获取预告片海报
                                        MovieImagesResponse movieTvImagesResponse = restTemplate.getForObject(
                                                apiUrl.getTvImagesUrl(tvId),
                                                MovieImagesResponse.class
                                        );

                                        if (movieTvImagesResponse != null && movieTvImagesResponse.getBackdrops() != null && !movieTvImagesResponse.getBackdrops().isEmpty()) {
                                            ImageMovieDTO firstImageMovieDTO = movieTvImagesResponse.getBackdrops().get(0);
                                            mostRecentTrailer.setFilePath(firstImageMovieDTO.getFile_path());
                                        } else {
                                            log.error("获取剧集 ID {} 的海报出错（该影片可能没有海报）", tvId);
                                        }

                                        mostRecentTrailer.setBackdrop_path(tvBackImage);
                                        mostRecentTrailer.setMovieTitle(tvName);

                                        // 使用 tvId 和其他字段进行去重
                                        boolean exists = latestOfficialTrailers.stream()
                                                .anyMatch(trailer -> trailer.getMovieTitle().equals(mostRecentTrailer.getMovieTitle())
                                                        && trailer.getPublishedAtAsLocalDateTime().equals(mostRecentTrailer.getPublishedAtAsLocalDateTime()));

                                        if (!exists) {
                                            synchronized (latestOfficialTrailers) {
                                                latestOfficialTrailers.add(mostRecentTrailer); // 添加到最新官方预告片列表中
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("获取剧集 ID " + tvId + " 的预告片出错: " + e.getMessage());
                        }
                    });

                    futures.add(future);
                }

                // 等待所有请求完成
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

                // 如果当前页的预告片数量不足 20，继续请求下一页
                if (latestOfficialTrailers.size() < tvCount) {
                    currentPage++;
                }
            }
            // 存储到 Redis，确保 latestOfficialTrailers 不包含 publishedAtAsLocalDateTime
            if (!latestOfficialTrailers.isEmpty()) {
                RedisUtils.setCacheObject("popularTvMedia:", latestOfficialTrailers, 4, TimeUnit.HOURS);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return latestOfficialTrailers;
    }

    public List<TvDTO> getOnAirTvDetails() {
        List<TvDTO> allOnAirTvShows = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(10); // 创建一个线程池
        List<CompletableFuture<List<TvDTO>>> futures = new ArrayList<>();

        try {
            // 获取第一页数据
            int page = 1;
            String url = apiUrl.getTVOnAirUrl(page);
            PopularTvResponseDTO firstPageResponse = restTemplate.getForObject(url, PopularTvResponseDTO.class);

            if (firstPageResponse == null || firstPageResponse.getResults() == null) {
                throw new ServiceException("无法获取正在播出的电视节目");
            }

            // 添加第一页的结果
            allOnAirTvShows.addAll(firstPageResponse.getResults());
            int totalPages = firstPageResponse.getTotalPages();

            // 并发请求后续页面
            for (page = 2; page <= totalPages; page++) {
                final int currentPage = page; 
                CompletableFuture<List<TvDTO>> future = CompletableFuture.supplyAsync(() -> {
                    String pageUrl = apiUrl.getTVOnAirUrl(currentPage);
                    PopularTvResponseDTO pageResponse = restTemplate.getForObject(pageUrl, PopularTvResponseDTO.class);
                    return (pageResponse != null && pageResponse.getResults() != null) ? pageResponse.getResults() : new ArrayList<>();
                }, executorService);
                futures.add(future);
            }

            // 等待所有请求完成并收集结果
            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            allOf.join(); // 等待所有请求完成

            // 合并所有结果
            for (CompletableFuture<List<TvDTO>> future : futures) {
                allOnAirTvShows.addAll(future.get());
            }

            // 按照首播日期排序
            allOnAirTvShows.sort(Comparator.comparing(TvDTO::getFirst_air_date).reversed()
                    .thenComparing(Comparator.comparing(TvDTO::getPopularity).reversed()));

            // 分页存储到 Redis，每页 20 条数据
            int pageSize = 20;
            int totalItems = allOnAirTvShows.size();
            int totalPagesToStore = (int) Math.ceil((double) totalItems / pageSize);

            for (int i = 0; i < totalPagesToStore; i++) {
                int start = i * pageSize;
                int end = Math.min(start + pageSize, totalItems);
                List<TvDTO> pageItems = new ArrayList<>(allOnAirTvShows.subList(start, end));
                RedisUtils.setCacheObject("onAirTv:page:" + (i + 1), pageItems, 4, TimeUnit.HOURS);
            }

        } catch (Exception e) {
            throw new ServiceException("获取正在播出的电视节目时出错", e);
        } finally {
            executorService.shutdown(); // 关闭线程池
        }

        return allOnAirTvShows;
    }

}
