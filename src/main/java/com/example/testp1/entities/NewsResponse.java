package com.example.testp1.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NewsResponse {

    // Matches "status": "ok"
    public String status;

    // Matches "totalResults": 48
    public int totalResults;

    // Matches "articles": [...]
    // This will hold the list of Article objects we defined earlier
    public List<Article> articles;

    // Default constructor for Jackson
    public NewsResponse() {}
}