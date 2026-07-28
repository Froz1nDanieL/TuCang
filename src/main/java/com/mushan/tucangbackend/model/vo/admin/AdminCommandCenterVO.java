package com.mushan.tucangbackend.model.vo.admin;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class AdminCommandCenterVO {
    private int days;
    private List<StatusItem> statuses = new ArrayList<>();
    private Trend trend = new Trend();

    @Data
    public static class StatusItem {
        private String key;
        private String label;
        private String status;
        private String value;
        private String description;
        private String targetPath;
    }

    @Data
    public static class Trend {
        private List<String> dates = new ArrayList<>();
        private List<Long> uploads = new ArrayList<>();
        private List<Long> reviewPassed = new ArrayList<>();
        private List<Long> reviewRejected = new ArrayList<>();
        private List<Long> aiTasks = new ArrayList<>();
    }

    @Data
    public static class ActionItem {
        private String type;
        private String severity;
        private String title;
        private String description;
        private String targetPath;
        private Date time;
    }
}
