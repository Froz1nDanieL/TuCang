package com.mushan.tucangbackend.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mushan.tucangbackend.annotation.AdminPermission;
import com.mushan.tucangbackend.common.BaseResponse;
import com.mushan.tucangbackend.common.ResultUtils;
import com.mushan.tucangbackend.constant.AdminPermissionConstant;
import com.mushan.tucangbackend.exception.ErrorCode;
import com.mushan.tucangbackend.exception.ThrowUtils;
import com.mushan.tucangbackend.model.dto.admin.AdminReviewQueueRequest;
import com.mushan.tucangbackend.model.dto.picture.PictureQueryRequest;
import com.mushan.tucangbackend.model.entity.Picture;
import com.mushan.tucangbackend.model.entity.PictureReviewRecord;
import com.mushan.tucangbackend.model.entity.Space;
import com.mushan.tucangbackend.model.entity.User;
import com.mushan.tucangbackend.model.vo.admin.AdminPictureVO;
import com.mushan.tucangbackend.model.vo.admin.AdminReviewRecordVO;
import com.mushan.tucangbackend.model.vo.admin.AdminReviewStatsVO;
import com.mushan.tucangbackend.service.AdminPermissionService;
import com.mushan.tucangbackend.service.PictureReviewRecordService;
import com.mushan.tucangbackend.service.PictureService;
import com.mushan.tucangbackend.service.SpaceService;
import com.mushan.tucangbackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/reviews")
public class AdminReviewController {

    @Resource private PictureService pictureService;
    @Resource private UserService userService;
    @Resource private SpaceService spaceService;
    @Resource private PictureReviewRecordService recordService;
    @Resource private AdminPermissionService permissionService;

    @Value("${tucang.admin.review-sla-minutes:30}")
    private long reviewSlaMinutes;

    @PostMapping("/queue")
    @AdminPermission(AdminPermissionConstant.REVIEW_WORKBENCH)
    public BaseResponse<Page<AdminPictureVO>> queue(@RequestBody AdminReviewQueueRequest request) {
        ThrowUtils.throwIf(request == null || request.getPageSize() <= 0 || request.getPageSize() > 50,
                ErrorCode.PARAMS_ERROR);
        PictureQueryRequest query = new PictureQueryRequest();
        query.setCurrent(request.getCurrent());
        query.setPageSize(request.getPageSize());
        query.setId(request.getPictureId());
        query.setSearchText(request.getSearchText());
        query.setSourceType(request.getSourceType());
        query.setSpaceId(request.getSpaceId());
        query.setUserId(request.getUserId());
        query.setReviewStatus(request.getReviewStatus() == null ? 0 : request.getReviewStatus());
        if ("interaction".equals(request.getSortField())) {
            query.setSortField("likeCount");
            query.setSortOrder("descend");
        } else {
            query.setSortField("createTime");
            query.setSortOrder("ascend");
        }
        Page<Picture> source = pictureService.page(
                new Page<Picture>(request.getCurrent(), request.getPageSize()),
                pictureService.getQueryWrapper(query));
        Page<AdminPictureVO> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        List<AdminPictureVO> records = source.getRecords().stream()
                .map(AdminPictureVO::from).collect(Collectors.toList());
        enrich(records);
        result.setRecords(records);
        return ResultUtils.success(result);
    }

    @GetMapping("/stats")
    @AdminPermission(AdminPermissionConstant.REVIEW_STATS_SELF)
    public BaseResponse<AdminReviewStatsVO> stats(
            @RequestParam(defaultValue = "7") int days, HttpServletRequest request) {
        ThrowUtils.throwIf(days != 7 && days != 30, ErrorCode.PARAMS_ERROR);
        User login = userService.getLoginUser(request);
        Long reviewerId = permissionService.hasPermission(login, AdminPermissionConstant.REVIEW_STATS_ALL)
                ? null : login.getId();
        Date start = new Date(System.currentTimeMillis() - days * 24L * 60 * 60 * 1000);
        List<PictureReviewRecord> decisions = recordService.listForStats(start, reviewerId);
        AdminReviewStatsVO vo = new AdminReviewStatsVO();
        vo.setPendingCount(pictureService.lambdaQuery().eq(Picture::getReviewStatus, 0).count());
        Picture oldest = pictureService.lambdaQuery().eq(Picture::getReviewStatus, 0)
                .orderByAsc(Picture::getCreateTime).last("LIMIT 1").one();
        vo.setOldestWaitSeconds(oldest == null || oldest.getCreateTime() == null ? 0
                : Math.max(0L, (System.currentTimeMillis() - oldest.getCreateTime().getTime()) / 1000));
        vo.setProcessedCount(decisions.size());
        long durationTotal = 0;
        long durationCount = 0;
        for (PictureReviewRecord decision : decisions) {
            if (decision.getToStatus() == 2) {
                vo.setRejectedCount(vo.getRejectedCount() + 1);
                String reason = decision.getReasonCode() == null ? "UNSPECIFIED" : decision.getReasonCode();
                vo.getRejectionReasons().put(reason,
                        vo.getRejectionReasons().getOrDefault(reason, 0L) + 1L);
            }
            if (decision.getDurationMs() != null) {
                durationTotal += decision.getDurationMs();
                durationCount++;
            }
        }
        vo.setAverageDurationMs(durationCount == 0 ? 0 : durationTotal / durationCount);
        return ResultUtils.success(vo);
    }

    @GetMapping("/pictures/{id}/history")
    @AdminPermission(AdminPermissionConstant.ASSET_TRACE_VIEW)
    public BaseResponse<List<AdminReviewRecordVO>> history(@PathVariable Long id) {
        List<PictureReviewRecord> records = recordService.listByPicture(id);
        Set<Long> userIds = records.stream().map(PictureReviewRecord::getReviewerId)
                .collect(Collectors.toSet());
        Map<Long, User> users = userIds.isEmpty() ? Collections.<Long, User>emptyMap()
                : userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
        return ResultUtils.success(records.stream().map(record -> {
            AdminReviewRecordVO vo = new AdminReviewRecordVO();
            BeanUtils.copyProperties(record, vo);
            User reviewer = users.get(record.getReviewerId());
            vo.setReviewerName(reviewer == null ? null : reviewer.getUserName());
            return vo;
        }).collect(Collectors.toList()));
    }

    private void enrich(List<AdminPictureVO> records) {
        Set<Long> userIds = records.stream().map(AdminPictureVO::getUserId).collect(Collectors.toSet());
        Set<Long> spaceIds = records.stream().map(AdminPictureVO::getSpaceId)
                .filter(id -> id != null).collect(Collectors.toSet());
        Map<Long, User> users = userIds.isEmpty() ? Collections.<Long, User>emptyMap()
                : userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
        Map<Long, Space> spaces = spaceIds.isEmpty() ? Collections.<Long, Space>emptyMap()
                : spaceService.listByIds(spaceIds).stream()
                .collect(Collectors.toMap(Space::getId, Function.identity(), (a, b) -> a));
        Map<Long, Long> rejectCounts = new HashMap<>();
        Date since = new Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000);
        for (Long userId : userIds) {
            rejectCounts.put(userId, pictureService.lambdaQuery()
                    .eq(Picture::getUserId, userId)
                    .eq(Picture::getReviewStatus, 2)
                    .ge(Picture::getReviewTime, since).count());
        }
        long now = System.currentTimeMillis();
        for (AdminPictureVO record : records) {
            User user = users.get(record.getUserId());
            Space space = spaces.get(record.getSpaceId());
            record.setUserName(user == null ? null : user.getUserName());
            record.setSpaceName(space == null ? null : space.getSpaceName());
            record.setRecentRejectCount(rejectCounts.getOrDefault(record.getUserId(), 0L));
            record.setWaitSeconds(record.getCreateTime() == null ? 0
                    : Math.max(0L, (now - record.getCreateTime().getTime()) / 1000));
        }
    }
}
