package com.mushan.tucangbackend.model.dto.admin;

import com.mushan.tucangbackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AdminReviewQueueRequest extends PageRequest {
    private Long pictureId;
    private String searchText;
    private String sourceType;
    private Long spaceId;
    private Long userId;
    private Integer reviewStatus;
}
