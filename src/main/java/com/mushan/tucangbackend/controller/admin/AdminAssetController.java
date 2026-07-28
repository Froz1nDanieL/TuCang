package com.mushan.tucangbackend.controller.admin;

import com.mushan.tucangbackend.annotation.AdminOperation;
import com.mushan.tucangbackend.annotation.AdminPermission;
import com.mushan.tucangbackend.common.BaseResponse;
import com.mushan.tucangbackend.common.ResultUtils;
import com.mushan.tucangbackend.constant.AdminPermissionConstant;
import com.mushan.tucangbackend.model.entity.User;
import com.mushan.tucangbackend.model.vo.admin.AdminAssetTraceVO;
import com.mushan.tucangbackend.model.vo.admin.AdminIndexCheckVO;
import com.mushan.tucangbackend.service.AdminAssetService;
import com.mushan.tucangbackend.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/admin/assets")
public class AdminAssetController {

    @Resource private AdminAssetService adminAssetService;
    @Resource private UserService userService;

    @GetMapping("/{pictureId}/trace")
    @AdminPermission(AdminPermissionConstant.ASSET_TRACE_VIEW)
    public BaseResponse<AdminAssetTraceVO> trace(@PathVariable Long pictureId,
                                                 HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(adminAssetService.trace(pictureId, user.getId()));
    }

    @PostMapping("/{pictureId}/resync-index")
    @AdminPermission(AdminPermissionConstant.ASSET_REPAIR)
    @AdminOperation(module = "asset", action = "resync-index",
            targetType = "picture", idempotent = true)
    public BaseResponse<AdminIndexCheckVO> resync(@PathVariable Long pictureId,
                                                  HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(adminAssetService.resync(pictureId, user.getId()));
    }

    @PostMapping("/{pictureId}/invalidate-cache")
    @AdminPermission(AdminPermissionConstant.ASSET_REPAIR)
    @AdminOperation(module = "asset", action = "invalidate-cache",
            targetType = "picture", idempotent = true)
    public BaseResponse<String> invalidate(@PathVariable Long pictureId) {
        return ResultUtils.success(adminAssetService.invalidateCache());
    }

    @PostMapping("/{pictureId}/regenerate-thumbnail")
    @AdminPermission(AdminPermissionConstant.ASSET_REPAIR)
    @AdminOperation(module = "asset", action = "regenerate-thumbnail",
            targetType = "picture", idempotent = true)
    public BaseResponse<String> regenerateThumbnail(@PathVariable Long pictureId) {
        return ResultUtils.success(adminAssetService.regenerateThumbnail(pictureId));
    }
}
