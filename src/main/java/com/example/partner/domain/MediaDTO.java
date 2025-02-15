package com.example.partner.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MediaDTO {
    private String name;
    private String key;
    private String site;
    private String movieTitle;

    private String filePath;

    private String backdrop_path;

    @JsonProperty("published_at")
    private String publishedAt;

    private String type;

    @JsonIgnore
    public LocalDateTime getPublishedAtAsLocalDateTime() {
        if (publishedAt != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            return LocalDateTime.parse(publishedAt, formatter);
        }
        return null;
    }

    @JsonIgnore
    public String[] getPublishedAtAsArray() {
        // 这里可以返回数组格式，但它在序列化时将被忽略
        if (publishedAt != null) {
            LocalDateTime dateTime = getPublishedAtAsLocalDateTime();
            return new String[]{
                    String.valueOf(dateTime.getYear()),
                    String.valueOf(dateTime.getMonthValue()),
                    String.valueOf(dateTime.getDayOfMonth()),
                    String.valueOf(dateTime.getHour()),
                    String.valueOf(dateTime.getMinute()),
                    String.valueOf(dateTime.getSecond())
            };
        }
        return null;
    }

}
