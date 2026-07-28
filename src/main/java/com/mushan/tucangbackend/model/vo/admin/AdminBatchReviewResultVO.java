package com.mushan.tucangbackend.model.vo.admin;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class AdminBatchReviewResultVO implements Serializable {

    private List<Long> successIds = new ArrayList<>();
    private Map<Long, String> conflicts = new LinkedHashMap<>();
    private Map<Long, String> failures = new LinkedHashMap<>();

    private static final long serialVersionUID = 1L;
}
