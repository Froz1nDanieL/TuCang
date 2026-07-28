package com.mushan.tucangbackend.controller.admin;

import com.mushan.tucangbackend.annotation.AdminPermission;
import com.mushan.tucangbackend.common.BaseResponse;
import com.mushan.tucangbackend.common.ResultUtils;
import com.mushan.tucangbackend.constant.AdminPermissionConstant;
import com.mushan.tucangbackend.model.entity.Picture;
import com.mushan.tucangbackend.model.entity.Space;
import com.mushan.tucangbackend.model.entity.User;
import com.mushan.tucangbackend.model.enums.PictureReviewStatusEnum;
import com.mushan.tucangbackend.model.vo.admin.AdminDashboardVO;
import com.mushan.tucangbackend.service.AdminMonitorService;
import com.mushan.tucangbackend.service.PictureService;
import com.mushan.tucangbackend.service.SpaceService;
import com.mushan.tucangbackend.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/admin/dashboard")
public class AdminDashboardController {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private AdminMonitorService adminMonitorService;

    @GetMapping("/overview")
    @AdminPermission(AdminPermissionConstant.DASHBOARD_VIEW)
    public BaseResponse<AdminDashboardVO> overview() {
        AdminDashboardVO vo = new AdminDashboardVO();
        vo.setUserCount(userService.count());
        vo.setPictureCount(pictureService.count());
        vo.setPendingPictureCount(pictureService.lambdaQuery()
                .eq(Picture::getReviewStatus, PictureReviewStatusEnum.REVIEWING.getValue()).count());
        vo.setSpaceCount(spaceService.count());

        LocalDate today = LocalDate.now();
        for (int offset = 6; offset >= 0; offset--) {
            LocalDate day = today.minusDays(offset);
            Timestamp start = Timestamp.valueOf(day.atStartOfDay());
            Timestamp end = Timestamp.valueOf(day.plusDays(1).atStartOfDay());
            vo.getTrendDates().add(day.format(DateTimeFormatter.ofPattern("MM-dd")));
            vo.getUserTrend().add(userService.lambdaQuery()
                    .ge(User::getCreateTime, start).lt(User::getCreateTime, end).count());
            vo.getPictureTrend().add(pictureService.lambdaQuery()
                    .ge(Picture::getCreateTime, start).lt(Picture::getCreateTime, end).count());
        }
        vo.setServiceStatus(adminMonitorService.getDependencyStatus());
        return ResultUtils.success(vo);
    }
}
