package com.mushan.tucangbackend.service;

import com.mushan.tucangbackend.constant.AdminPermissionConstant;
import com.mushan.tucangbackend.model.entity.User;
import com.mushan.tucangbackend.model.enums.UserRoleEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminPermissionServiceTest {

    private final AdminPermissionService service = new AdminPermissionService();

    @Test
    void userCannotAccessAdminPermissions() {
        User user = new User();
        user.setUserRole(UserRoleEnum.USER.getValue());

        assertTrue(service.getPermissions(user).isEmpty());
        assertFalse(service.hasPermission(user, AdminPermissionConstant.DASHBOARD_VIEW));
    }

    @Test
    void reviewerCanReviewButCannotManageUsers() {
        User reviewer = new User();
        reviewer.setUserRole(UserRoleEnum.REVIEWER.getValue());

        assertTrue(service.hasPermission(reviewer, AdminPermissionConstant.PICTURE_REVIEW));
        assertTrue(service.hasPermission(reviewer, AdminPermissionConstant.OPERATION_LOG_SELF));
        assertFalse(service.hasPermission(reviewer, AdminPermissionConstant.USER_MANAGE));
        assertFalse(service.hasPermission(reviewer, AdminPermissionConstant.OPERATION_LOG_ALL));
    }

    @Test
    void adminReceivesAllManagementPermissions() {
        User admin = new User();
        admin.setUserRole(UserRoleEnum.ADMIN.getValue());

        assertTrue(service.hasPermission(admin, AdminPermissionConstant.PICTURE_REVIEW));
        assertTrue(service.hasPermission(admin, AdminPermissionConstant.PICTURE_MANAGE));
        assertTrue(service.hasPermission(admin, AdminPermissionConstant.USER_MANAGE));
        assertTrue(service.hasPermission(admin, AdminPermissionConstant.SPACE_MANAGE));
        assertTrue(service.hasPermission(admin, AdminPermissionConstant.OPERATION_LOG_ALL));
    }
}
