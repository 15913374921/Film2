package com.example.partner.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaWrapper {
    private Object mediaDetail;
    private String type;
} 