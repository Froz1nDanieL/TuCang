package com.mushan.tucangbackend.controller.admin;

import com.mushan.tucangbackend.annotation.AdminOperation;
import com.mushan.tucangbackend.annotation.AdminPermission;
import com.mushan.tucangbackend.common.BaseResponse;
import com.mushan.tucangbackend.common.ResultUtils;
import com.mushan.tucangbackend.constant.AdminPermissionConstant;
import com.mushan.tucangbackend.model.dto.admin.AdminIndexCheckRequest;
import com.mushan.tucangbackend.model.entity.User;
import com.mushan.tucangbackend.model.vo.admin.AdminIndexCheckVO;
import com.mushan.tucangbackend.model.vo.admin.AdminIndexConsistencyVO;
import com.mushan.tucangbackend.model.vo.admin.AdminJobExecutionVO;
import com.mushan.tucangbackend.model.vo.admin.AdminSearchQualityVO;
import com.mushan.tucangbackend.service.AdminSearchQualityService;
import com.mushan.tucangbackend.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/admin/search")
public class AdminSearchQualityController {

    @Resource private AdminSearchQualityService qualityService;
    @Resource private UserService userService;

    @GetMapping("/quality")
    @AdminPermission(AdminPermissionConstant.SEARCH_QUALITY_VIEW)
    public BaseResponse<AdminSearchQualityVO> quality() {
        return ResultUtils.success(qualityService.quality());
    }

    @GetMapping("/index-consistency")
    @AdminPermission(AdminPermissionConstant.SEARCH_QUALITY_VIEW)
    public BaseResponse<AdminIndexConsistencyVO> consistency(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String mismatchType) {
        return ResultUtils.success(qualityService.consistency(current, pageSize, mismatchType));
    }

    @PostMapping("/index-consistency/check")
    @AdminPermission(AdminPermissionConstant.SEARCH_INDEX_REPAIR)
    @AdminOperation(module = "search-index", action = "check", targetType = "index-job", idempotent = true)
    public BaseResponse<AdminJobExecutionVO> check(
            @RequestBody AdminIndexCheckRequest request, HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(qualityService.start(request, user.getId()));
    }

    @PostMapping("/pictures/{id}/repair")
    @AdminPermission(AdminPermissionConstant.SEARCH_INDEX_REPAIR)
    @AdminOperation(module = "search-index", action = "repair", targetType = "picture", idempotent = true)
    public BaseResponse<AdminIndexCheckVO> repair(
            @PathVariable Long id, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(qualityService.repair(id, user.getId()));
    }
}
