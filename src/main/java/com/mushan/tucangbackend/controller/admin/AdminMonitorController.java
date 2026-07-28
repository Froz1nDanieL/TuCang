package com.mushan.tucangbackend.controller.admin;

import com.mushan.tucangbackend.annotation.AdminOperation;
import com.mushan.tucangbackend.annotation.AdminPermission;
import com.mushan.tucangbackend.common.BaseResponse;
import com.mushan.tucangbackend.common.ResultUtils;
import com.mushan.tucangbackend.constant.AdminPermissionConstant;
import com.mushan.tucangbackend.model.vo.admin.AdminMonitorVO;
import com.mushan.tucangbackend.service.AdminMonitorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/admin/monitor")
public class AdminMonitorController {

    @Resource
    private AdminMonitorService adminMonitorService;

    @GetMapping("/overview")
    @AdminPermission(AdminPermissionConstant.MONITOR_VIEW)
    @AdminOperation(module = "monitor", action = "query")
    public BaseResponse<AdminMonitorVO> overview() {
        return ResultUtils.success(adminMonitorService.getOverview());
    }
}
