package com.example.partner.utils;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class DBProp {

    private String url;
    private String username;
    private String password;
}
