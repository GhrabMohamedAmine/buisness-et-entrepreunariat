package com.example.testp1.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Source {
    public String name; // e.g., "CNBC"

    public Source() {}
}