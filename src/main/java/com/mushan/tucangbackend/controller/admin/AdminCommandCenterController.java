package com.mushan.tucangbackend.controller.admin;

import com.mushan.tucangbackend.annotation.AdminPermission;
import com.mushan.tucangbackend.common.BaseResponse;
import com.mushan.tucangbackend.common.ResultUtils;
import com.mushan.tucangbackend.constant.AdminPermissionConstant;
import com.mushan.tucangbackend.model.entity.User;
import com.mushan.tucangbackend.model.vo.admin.AdminCommandCenterVO;
import com.mushan.tucangbackend.service.AdminCommandCenterService;
import com.mushan.tucangbackend.service.UserService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/admin/command-center")
public class AdminCommandCenterController {

    @Resource private AdminCommandCenterService commandCenterService;
    @Resource private UserService userService;

    @GetMapping("/overview")
    @AdminPermission(AdminPermissionConstant.DASHBOARD_VIEW)
    public BaseResponse<AdminCommandCenterVO> overview(
            @RequestParam(defaultValue = "7") int days, HttpServletRequest request) {
        return ResultUtils.success(commandCenterService.overview(
                days, userService.getLoginUser(request)));
    }

    @GetMapping("/actions")
    @AdminPermission(AdminPermissionConstant.DASHBOARD_VIEW)
    public BaseResponse<List<AdminCommandCenterVO.ActionItem>> actions(
            @RequestParam(defaultValue = "10") int limit, HttpServletRequest request) {
        return ResultUtils.success(commandCenterService.actions(
                limit, userService.getLoginUser(request)));
    }

    @GetMapping("/events")
    @AdminPermission(AdminPermissionConstant.DASHBOARD_VIEW)
    public BaseResponse<List<AdminCommandCenterVO.ActionItem>> events(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date before,
            HttpServletRequest request) {
        return ResultUtils.success(commandCenterService.events(
                limit, userService.getLoginUser(request), before));
    }
}
