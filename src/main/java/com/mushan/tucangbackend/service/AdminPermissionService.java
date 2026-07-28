package com.mushan.tucangbackend.service;

import com.mushan.tucangbackend.constant.AdminPermissionConstant;
import com.mushan.tucangbackend.model.entity.User;
import com.mushan.tucangbackend.model.enums.UserRoleEnum;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class AdminPermissionService {

    private static final Set<String> REVIEWER_PERMISSIONS = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList(
                    AdminPermissionConstant.DASHBOARD_VIEW,
                    AdminPermissionConstant.PICTURE_VIEW,
                    AdminPermissionConstant.PICTURE_REVIEW,
                    AdminPermissionConstant.OPERATION_LOG_SELF,
                    AdminPermissionConstant.MONITOR_VIEW
            ))
    );

    private static final Set<String> ADMIN_PERMISSIONS = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList(
                    AdminPermissionConstant.DASHBOARD_VIEW,
                    AdminPermissionConstant.PICTURE_VIEW,
                    AdminPermissionConstant.PICTURE_REVIEW,
                    AdminPermissionConstant.PICTURE_MANAGE,
                    AdminPermissionConstant.USER_MANAGE,
                    AdminPermissionConstant.SPACE_MANAGE,
                    AdminPermissionConstant.OPERATION_LOG_SELF,
                    AdminPermissionConstant.OPERATION_LOG_ALL,
                    AdminPermissionConstant.MONITOR_VIEW
            ))
    );

    public Set<String> getPermissions(User user) {
        if (user == null) {
            return Collections.emptySet();
        }
        if (UserRoleEnum.ADMIN.getValue().equals(user.getUserRole())) {
            return ADMIN_PERMISSIONS;
        }
        if (UserRoleEnum.REVIEWER.getValue().equals(user.getUserRole())) {
            return REVIEWER_PERMISSIONS;
        }
        return Collections.emptySet();
    }

    public boolean hasPermission(User user, String permission) {
        return getPermissions(user).contains(permission);
    }
}
