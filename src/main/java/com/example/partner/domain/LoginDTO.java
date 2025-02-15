package com.example.partner.domain;

import com.example.partner.entity.Admin;
import com.example.partner.entity.User;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)  // 只序列化非空字段
public class LoginDTO implements Serializable {
    private static final long serialVersionUID  = 1L;

    private User user;
    private Admin admin;
    private String token;
    private List<MovieDTO> movieDTO;
    private List<TvDTO> tvDTO;

    // 创建一个只包含admin和token的静态方法
    public static LoginDTO adminLogin(Admin admin, String token) {
        return LoginDTO.builder()
                .admin(admin)
                .token(token)
                .build();
    }
}
