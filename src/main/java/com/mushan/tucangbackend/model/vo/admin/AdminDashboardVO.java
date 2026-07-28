package com.mushan.tucangbackend.model.vo.admin;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class AdminDashboardVO implements Serializable {

    private Long userCount;
    private Long pictureCount;
    private Long pendingPictureCount;
    private Long spaceCount;
    private List<String> trendDates = new ArrayList<>();
    private List<Long> userTrend = new ArrayList<>();
    private List<Long> pictureTrend = new ArrayList<>();
    private Map<String, String> serviceStatus = new LinkedHashMap<>();

    private static final long serialVersionUID = 1L;
}
