package com.mushan.tucangbackend.controller.admin;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mushan.tucangbackend.annotation.AdminOperation;
import com.mushan.tucangbackend.annotation.AdminPermission;
import com.mushan.tucangbackend.common.BaseResponse;
import com.mushan.tucangbackend.common.ResultUtils;
import com.mushan.tucangbackend.constant.AdminPermissionConstant;
import com.mushan.tucangbackend.exception.ErrorCode;
import com.mushan.tucangbackend.exception.ThrowUtils;
import com.mushan.tucangbackend.model.dto.admin.AdminOperationLogQueryRequest;
import com.mushan.tucangbackend.model.entity.AdminOperationLog;
import com.mushan.tucangbackend.model.entity.User;
import com.mushan.tucangbackend.model.enums.UserRoleEnum;
import com.mushan.tucangbackend.model.vo.admin.AdminOperationLogVO;
import com.mushan.tucangbackend.service.AdminOperationLogService;
import com.mushan.tucangbackend.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/operation-logs")
public class AdminOperationLogController {

    @Resource
    private AdminOperationLogService operationLogService;

    @Resource
    private UserService userService;

    @PostMapping("/page")
    @AdminPermission(AdminPermissionConstant.OPERATION_LOG_SELF)
    @AdminOperation(module = "operation-log", action = "query")
    public BaseResponse<Page<AdminOperationLogVO>> page(
            @RequestBody AdminOperationLogQueryRequest request,
            HttpServletRequest servletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getPageSize() <= 0 || request.getPageSize() > 100, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(servletRequest);
        QueryWrapper<AdminOperationLog> wrapper = new QueryWrapper<>();
        if (UserRoleEnum.ADMIN.getValue().equals(loginUser.getUserRole())) {
            wrapper.eq(request.getOperatorId() != null, "operatorId", request.getOperatorId());
        } else {
            wrapper.eq("operatorId", loginUser.getId());
        }
        wrapper.eq(StrUtil.isNotBlank(request.getModule()), "module", request.getModule());
        wrapper.eq(StrUtil.isNotBlank(request.getAction()), "action", request.getAction());
        wrapper.eq(request.getSuccess() != null, "success", request.getSuccess());
        wrapper.ge(request.getStartTime() != null, "createTime", request.getStartTime());
        wrapper.lt(request.getEndTime() != null, "createTime", request.getEndTime());
        String sortField = request.getSortField();
        boolean sortable = Arrays.asList("id", "operatorId", "success", "durationMs", "createTime")
                .contains(sortField);
        wrapper.orderBy(sortable, "ascend".equals(request.getSortOrder()), sortField);
        if (!sortable) {
            wrapper.orderByDesc("createTime");
        }
        Page<AdminOperationLog> logPage = operationLogService.page(
                new Page<>(request.getCurrent(), request.getPageSize()), wrapper
        );
        Page<AdminOperationLogVO> result =
                new Page<>(logPage.getCurrent(), logPage.getSize(), logPage.getTotal());
        result.setRecords(logPage.getRecords().stream()
                .map(AdminOperationLogVO::from)
                .collect(Collectors.toList()));
        return ResultUtils.success(result);
    }
}
