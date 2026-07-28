package com.mushan.tucangbackend.model.dto.admin;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class AdminUserUpdateRequest implements Serializable {

    @NotNull
    private Long id;
    private String userRole;
    private Integer userStatus;

    private static final long serialVersionUID = 1L;
}
