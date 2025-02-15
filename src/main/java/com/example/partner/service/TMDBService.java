package com.example.partner.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.partner.common.ApiUrl;

import com.example.partner.exception.ServiceException;
import com.example.partner.utils.RedisUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.OkHttpClient;
import com.fasterxml.jackson.core.type.TypeReference;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class TMDBService {
    private final RestTemplate restTemplate;

    private final ApiUrl apiUrl;

    private final ObjectMapper objectMapper;

    public TMDBService(ApiUrl apiUrl,ObjectMapper objectMapper) {
        this.restTemplate = createRestTemplateWithProxy();
        this.apiUrl = apiUrl;
        this.objectMapper = objectMapper;
    }

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

    public ResponseEntity<String> getDetail(Long id, String type) {
        String urlWithParams;
        if ("movie".equals(type)) {
            urlWithParams = apiUrl.getMovieDetailsUrl(id);
        } else if ("tv".equals(type)) {
            urlWithParams = apiUrl.getTvDetailsUrl(id);
        } else {
            return ResponseEntity.badRequest().body("找不到此类型");
        }

        ResponseEntity<String> response = restTemplate.getForEntity(urlWithParams, String.class);
        String responseBody = response.getBody();

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(responseBody);

            // 检查 overview 是否为空
            String overview = rootNode.path("overview").asText();
            if (overview.isEmpty()) {
                // 从备用 URL 获取 overview
                String alternativeUrl;
                if ("movie".equals(type)) {
                    alternativeUrl = "https://api.themoviedb.org/3/movie/" + id + "?api_key=1ad3fcc3a67f178a8c8958896787f031&language=en-US";
                } else {
                    alternativeUrl = "https://api.themoviedb.org/3/tv/" + id + "?api_key=1ad3fcc3a67f178a8c8958896787f031&language=en-US";
                }

                ResponseEntity<String> alternativeResponse = restTemplate.getForEntity(alternativeUrl, String.class);
                String alternativeResponseBody = alternativeResponse.getBody();

                // 从备用响应中提取 overview
                JsonNode alternativeRootNode = objectMapper.readTree(alternativeResponseBody);
                String alternativeOverview = alternativeRootNode.path("overview").asText();

                // 将新的 overview 拼接回去
                ((ObjectNode) rootNode).put("overview", alternativeOverview);
            }

            // 返回更新后的 JSON
            String updatedResponse = objectMapper.writeValueAsString(rootNode);
            return ResponseEntity.ok(updatedResponse);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("解析 JSON 失败");
        }
    }

    public ResponseEntity<String> getWatch(Long id,String type){
        String urlWithParams;
        if ("movie".equals(type)) {
            urlWithParams = "https://api.themoviedb.org/3/movie/{id}/watch/providers?api_key=1ad3fcc3a67f178a8c8958896787f031".replace("{id}", String.valueOf(id));
        } else if ("tv".equals(type)) {
            urlWithParams = "https://api.themoviedb.org/3/tv/{id}/watch/providers?api_key=1ad3fcc3a67f178a8c8958896787f031".replace("{id}", String.valueOf(id));
        } else {
            return ResponseEntity.badRequest().body("找不到此类型");
        }
        ResponseEntity<String> response = restTemplate.getForEntity(urlWithParams, String.class);
        return response;
    }

    public ResponseEntity<String> getCredits(Long id, String type) {
        String urlWithParams;
        if ("movie".equals(type)) {
            urlWithParams = "https://api.themoviedb.org/3/movie/{id}/credits?api_key=1ad3fcc3a67f178a8c8958896787f031&language=zh-CN".replace("{id}", String.valueOf(id));
        } else if ("tv".equals(type)) {
            urlWithParams = "https://api.themoviedb.org/3/tv/{id}/credits?api_key=1ad3fcc3a67f178a8c8958896787f031".replace("{id}", String.valueOf(id));
        } else {
            return ResponseEntity.badRequest().body("不到此类型");
        }
        ResponseEntity<String> response = restTemplate.getForEntity(urlWithParams, String.class);
        return response;
    }

    public ResponseEntity<String> getSimilar(Long id, String type) {
        String urlWithParams;
        if ("movie".equals(type)) {
            urlWithParams = "https://api.themoviedb.org/3/movie/{id}/similar?api_key=1ad3fcc3a67f178a8c8958896787f031&language=zh-CN".replace("{id}", String.valueOf(id));
            System.out.println(urlWithParams);
        } else if ("tv".equals(type)) {
            urlWithParams = "https://api.themoviedb.org/3/tv/{id}/similar?api_key=1ad3fcc3a67f178a8c8958896787f031&language=zh-CN".replace("{id}", String.valueOf(id));
        } else {
            return ResponseEntity.badRequest().body("找不到此类型");
        }
        ResponseEntity<String> response = restTemplate.getForEntity(urlWithParams, String.class);
        return response;
    }

    public Page<JsonNode> getSearchMovieTv(Integer pageNum, String type, Integer pageSize, String q) throws Exception {
        String cacheKey = String.format("search:%s:%s:%d:%d", type, q, pageNum, pageSize);

        // 检查缓存
        Page<JsonNode> cachedPage = RedisUtils.getCacheObject(cacheKey);
        if (cachedPage != null) {
            return cachedPage;
        }

        // 获取指定页码的数据
        String urlWithParams = buildUrl(type, q, pageNum);
        ResponseEntity<String> response = restTemplate.getForEntity(urlWithParams, String.class);

        // 解析 JSON 响应
        JsonNode rootNode = objectMapper.readTree(response.getBody());
        JsonNode resultsNode = rootNode.path("results");
        long totalResults = rootNode.path("total_results").asLong();

        // 创建 MyBatis-Plus 的分页对象
        Page<JsonNode> page = new Page<>(pageNum, pageSize);
        List<JsonNode> content = objectMapper.convertValue(resultsNode, new TypeReference<List<JsonNode>>() {});
        page.setRecords(content);
        page.setTotal(totalResults);

        // 存储到 Redis
        RedisUtils.setCacheObject(cacheKey, page, 4, TimeUnit.HOURS);

        // 返回当前页数据
        return page;
    }

    private String buildUrl(String type, String q, int pageNum) {
        String apiKey = "1ad3fcc3a67f178a8c8958896787f031";
        if ("movies".equals(type)) {
            return String.format("https://api.themoviedb.org/3/search/movie?api_key=%s&query=%s&page=%d&language=zh-CN", apiKey, q, pageNum);
        } else if ("tvshows".equals(type)) {
            return String.format("https://api.themoviedb.org/3/search/tv?api_key=%s&query=%s&page=%d&language=zh-CN", apiKey, q, pageNum);
        } else {
            throw new IllegalArgumentException("无此类型");
        }
    }
}
