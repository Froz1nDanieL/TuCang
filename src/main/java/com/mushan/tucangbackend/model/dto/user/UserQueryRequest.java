package com.mushan.tucangbackend.model.dto.user;

import com.mushan.tucangbackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 账号或昵称关键字
     */
    private String searchText;

    /**
     * 简介
     */
    private String userProfile;

    /**
     * 用户角色：user/reviewer/admin
     */
    private String userRole;

    /**
     * 账号状态：0-正常，1-禁用
     */
    private Integer userStatus;

    private static final long serialVersionUID = 1L;
}
