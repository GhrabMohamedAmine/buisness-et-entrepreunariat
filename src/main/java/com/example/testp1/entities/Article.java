package com.example.testp1.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


@JsonIgnoreProperties(ignoreUnknown = true)
public class Article {
    public String title;
    public String description;
    public String url;

    @JsonProperty("urlToImage")
    public String urlToImage;

    public String publishedAt;

    // This maps the nested source object
    public Source source;
    public String content;

    public Article() {}
}