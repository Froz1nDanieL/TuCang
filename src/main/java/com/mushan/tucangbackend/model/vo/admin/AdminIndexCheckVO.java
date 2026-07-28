package com.mushan.tucangbackend.model.vo.admin;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AdminIndexCheckVO {
    private Long pictureId;
    private boolean mysqlExists;
    private boolean esExists;
    private List<String> mismatchTypes = new ArrayList<>();
    private String status;
    private String message;

    public boolean isConsistent() {
        return mismatchTypes.isEmpty() && "UP".equals(status);
    }
}
