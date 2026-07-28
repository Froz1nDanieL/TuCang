package com.mushan.tucangbackend.model.dto.admin;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

@Data
public class AdminBatchReviewRequest implements Serializable {

    @NotEmpty
    private List<Long> ids;
    private Integer reviewStatus;
    private String reviewMessage;
    private String reasonCode;

    private static final long serialVersionUID = 1L;
}
