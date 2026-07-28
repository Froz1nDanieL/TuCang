package com.mushan.tucangbackend.model.vo.admin;

import lombok.Data;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class AdminMonitorVO implements Serializable {

    private long uptimeSeconds;
    private int availableProcessors;
    private double systemLoadAverage;
    private long heapUsedBytes;
    private long heapMaxBytes;
    private int threadCount;
    private int peakThreadCount;
    private Integer dbActiveConnections;
    private Integer dbIdleConnections;
    private Map<String, String> dependencies = new LinkedHashMap<>();

    private static final long serialVersionUID = 1L;
}
