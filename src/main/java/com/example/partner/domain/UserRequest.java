package com.example.partner.domain;


import lombok.Data;

@Data
public class UserRequest {
    private String username;
    private String password;
    private String name;
    private String email;
    private String emailCode;
    private String type;
}
