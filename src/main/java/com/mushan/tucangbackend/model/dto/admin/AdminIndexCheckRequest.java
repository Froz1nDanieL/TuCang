package com.mushan.tucangbackend.model.dto.admin;

import lombok.Data;

@Data
public class AdminIndexCheckRequest {
    private String scope = "SAMPLE";
    private Integer sampleSize = 100;
}
