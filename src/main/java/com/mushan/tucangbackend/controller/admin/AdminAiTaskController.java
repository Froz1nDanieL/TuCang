package com.mushan.tucangbackend.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mushan.tucangbackend.annotation.AdminOperation;
import com.mushan.tucangbackend.annotation.AdminPermission;
import com.mushan.tucangbackend.common.BaseResponse;
import com.mushan.tucangbackend.common.ResultUtils;
import com.mushan.tucangbackend.constant.AdminPermissionConstant;
import com.mushan.tucangbackend.model.dto.admin.AdminAiTaskQueryRequest;
import com.mushan.tucangbackend.model.vo.admin.AdminAiTaskStatsVO;
import com.mushan.tucangbackend.model.vo.admin.AdminAiTaskVO;
import com.mushan.tucangbackend.service.AdminAiTaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/admin/ai-tasks")
public class AdminAiTaskController {

    @Resource private AdminAiTaskService adminAiTaskService;

    @PostMapping("/page")
    @AdminPermission(AdminPermissionConstant.AI_TASK_VIEW)
    public BaseResponse<Page<AdminAiTaskVO>> page(@RequestBody AdminAiTaskQueryRequest request) {
        return ResultUtils.success(adminAiTaskService.page(request));
    }

    @GetMapping("/{id}")
    @AdminPermission(AdminPermissionConstant.AI_TASK_VIEW)
    @AdminOperation(module = "ai-task", action = "view-detail", targetType = "ai-task")
    public BaseResponse<AdminAiTaskVO> detail(@PathVariable Long id) {
        return ResultUtils.success(adminAiTaskService.detail(id));
    }

    @GetMapping("/stats")
    @AdminPermission(AdminPermissionConstant.AI_TASK_VIEW)
    public BaseResponse<AdminAiTaskStatsVO> stats(@RequestParam(defaultValue = "7") int days) {
        return ResultUtils.success(adminAiTaskService.stats(days));
    }

    @PostMapping("/{id}/retry")
    @AdminPermission(AdminPermissionConstant.AI_TASK_MANAGE)
    @AdminOperation(module = "ai-task", action = "retry", targetType = "ai-task", idempotent = true)
    public BaseResponse<AdminAiTaskVO> retry(@PathVariable Long id) {
        return ResultUtils.success(adminAiTaskService.retry(id));
    }
}
