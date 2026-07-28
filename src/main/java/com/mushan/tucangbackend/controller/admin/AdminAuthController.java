package com.mushan.tucangbackend.controller.admin;

import com.mushan.tucangbackend.annotation.AdminOperation;
import com.mushan.tucangbackend.annotation.AdminPermission;
import com.mushan.tucangbackend.common.BaseResponse;
import com.mushan.tucangbackend.common.ResultUtils;
import com.mushan.tucangbackend.constant.AdminPermissionConstant;
import com.mushan.tucangbackend.exception.BusinessException;
import com.mushan.tucangbackend.exception.ErrorCode;
import com.mushan.tucangbackend.exception.ThrowUtils;
import com.mushan.tucangbackend.model.dto.user.UserLoginRequest;
import com.mushan.tucangbackend.model.entity.User;
import com.mushan.tucangbackend.model.enums.UserRoleEnum;
import com.mushan.tucangbackend.model.vo.LoginUserVO;
import com.mushan.tucangbackend.model.vo.admin.AdminMeVO;
import com.mushan.tucangbackend.service.AdminPermissionService;
import com.mushan.tucangbackend.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/admin")
public class AdminAuthController {

    @Resource
    private UserService userService;

    @Resource
    private AdminPermissionService adminPermissionService;

    @PostMapping("/login")
    @AdminOperation(module = "auth", action = "login")
    public BaseResponse<LoginUserVO> login(
            @RequestBody UserLoginRequest loginRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(loginRequest == null, ErrorCode.PARAMS_ERROR);
        LoginUserVO loginUser = userService.userLogin(
                loginRequest.getUserAccount(),
                loginRequest.getUserPassword(),
                request
        );
        String role = loginUser.getUserRole();
        if (!UserRoleEnum.ADMIN.getValue().equals(role)
                && !UserRoleEnum.REVIEWER.getValue().equals(role)) {
            userService.userLogout(request);
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "该账号无权访问后台管理系统");
        }
        return ResultUtils.success(loginUser);
    }

    @GetMapping("/me")
    @AdminPermission(AdminPermissionConstant.DASHBOARD_VIEW)
    public BaseResponse<AdminMeVO> me(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        AdminMeVO vo = new AdminMeVO();
        vo.setUser(userService.getUserVO(loginUser));
        vo.setPermissions(adminPermissionService.getPermissions(loginUser));
        return ResultUtils.success(vo);
    }

    @PostMapping("/logout")
    @AdminPermission(AdminPermissionConstant.DASHBOARD_VIEW)
    @AdminOperation(module = "auth", action = "logout")
    public BaseResponse<Boolean> logout(HttpServletRequest request) {
        return ResultUtils.success(userService.userLogout(request));
    }
}
