package com.mushan.tucangbackend.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mushan.tucangbackend.annotation.AdminOperation;
import com.mushan.tucangbackend.annotation.AdminPermission;
import com.mushan.tucangbackend.common.BaseResponse;
import com.mushan.tucangbackend.common.DeleteRequest;
import com.mushan.tucangbackend.common.ResultUtils;
import com.mushan.tucangbackend.constant.AdminPermissionConstant;
import com.mushan.tucangbackend.exception.ErrorCode;
import com.mushan.tucangbackend.exception.ThrowUtils;
import com.mushan.tucangbackend.model.dto.space.SpaceQueryRequest;
import com.mushan.tucangbackend.model.dto.space.SpaceUpdateRequest;
import com.mushan.tucangbackend.model.entity.Space;
import com.mushan.tucangbackend.model.vo.SpaceVO;
import com.mushan.tucangbackend.service.SpaceService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/admin/spaces")
public class AdminSpaceController {

    @Resource
    private SpaceService spaceService;

    @PostMapping("/page")
    @AdminPermission(AdminPermissionConstant.SPACE_MANAGE)
    public BaseResponse<Page<SpaceVO>> page(@RequestBody SpaceQueryRequest request,
                                            HttpServletRequest servletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getPageSize() <= 0 || request.getPageSize() > 100, ErrorCode.PARAMS_ERROR);
        Page<Space> page = spaceService.page(
                new Page<>(request.getCurrent(), request.getPageSize()),
                spaceService.getQueryWrapper(request)
        );
        return ResultUtils.success(spaceService.getSpaceVOPage(page, servletRequest));
    }

    @GetMapping("/{id}")
    @AdminPermission(AdminPermissionConstant.SPACE_MANAGE)
    public BaseResponse<SpaceVO> detail(@PathVariable Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(spaceService.getSpaceVO(space, request));
    }

    @PostMapping("/update")
    @AdminPermission(AdminPermissionConstant.SPACE_MANAGE)
    @AdminOperation(module = "space", action = "update", targetType = "space", idempotent = true)
    public BaseResponse<Boolean> update(@RequestBody SpaceUpdateRequest request) {
        ThrowUtils.throwIf(request == null || request.getId() == null || request.getId() <= 0,
                ErrorCode.PARAMS_ERROR);
        Space oldSpace = spaceService.getById(request.getId());
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        Space space = new Space();
        BeanUtils.copyProperties(request, space);
        spaceService.fillSpaceBySpaceLevel(space);
        spaceService.validSpace(space, false);
        boolean changed = spaceService.updateById(space);
        ThrowUtils.throwIf(!changed, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @PostMapping("/delete")
    @AdminPermission(AdminPermissionConstant.SPACE_MANAGE)
    @AdminOperation(module = "space", action = "delete", targetType = "space", idempotent = true)
    public BaseResponse<Boolean> delete(@RequestBody DeleteRequest request) {
        ThrowUtils.throwIf(request == null || request.getId() <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(spaceService.getById(request.getId()) == null, ErrorCode.NOT_FOUND_ERROR);
        boolean deleted = spaceService.removeById(request.getId());
        ThrowUtils.throwIf(!deleted, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }
}
