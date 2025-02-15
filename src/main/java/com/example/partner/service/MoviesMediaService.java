package com.example.partner.service;

import com.example.partner.common.ApiUrl;
import com.example.partner.domain.MediaDTO;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class MoviesMediaService {

    private final RestTemplate restTemplate;
    private final ApiUrl apiUrl;

    // 使用代理创建 RestTemplate
    public MoviesMediaService(ApiUrl apiUrl) {
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

}
