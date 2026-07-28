package com.mushan.tucangbackend.model.vo.admin;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class AdminAssetTraceVO {
    private AdminPictureVO picture;
    private String ownerName;
    private String spaceName;
    private String aiTaskId;
    private String aiModelName;
    private List<Check> checks = new ArrayList<>();
    private List<TimelineItem> timeline = new ArrayList<>();

    @Data
    public static class Check {
        private String system;
        private String status;
        private String summary;
        private String detail;
    }

    @Data
    public static class TimelineItem {
        private String type;
        private String title;
        private String description;
        private Date time;
        private String status;
    }
}
