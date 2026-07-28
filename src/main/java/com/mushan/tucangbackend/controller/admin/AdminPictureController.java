package com.mushan.tucangbackend.controller.admin;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mushan.tucangbackend.annotation.AdminOperation;
import com.mushan.tucangbackend.annotation.AdminPermission;
import com.mushan.tucangbackend.common.BaseResponse;
import com.mushan.tucangbackend.common.DeleteRequest;
import com.mushan.tucangbackend.common.ResultUtils;
import com.mushan.tucangbackend.constant.AdminPermissionConstant;
import com.mushan.tucangbackend.exception.BusinessException;
import com.mushan.tucangbackend.exception.ErrorCode;
import com.mushan.tucangbackend.exception.ThrowUtils;
import com.mushan.tucangbackend.model.dto.admin.AdminBatchReviewRequest;
import com.mushan.tucangbackend.model.dto.picture.PictureQueryRequest;
import com.mushan.tucangbackend.model.dto.picture.PictureReviewRequest;
import com.mushan.tucangbackend.model.dto.picture.PictureUpdateRequest;
import com.mushan.tucangbackend.model.entity.Picture;
import com.mushan.tucangbackend.model.entity.User;
import com.mushan.tucangbackend.model.vo.admin.AdminBatchReviewResultVO;
import com.mushan.tucangbackend.model.vo.admin.AdminPictureVO;
import com.mushan.tucangbackend.service.PictureChangeNotifier;
import com.mushan.tucangbackend.service.PictureService;
import com.mushan.tucangbackend.service.UserService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/pictures")
public class AdminPictureController {

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;

    @Resource
    private PictureChangeNotifier pictureChangeNotifier;

    @Resource
    private MeterRegistry meterRegistry;

    @PostMapping("/page")
    @AdminPermission(AdminPermissionConstant.PICTURE_VIEW)
    public BaseResponse<Page<AdminPictureVO>> page(@RequestBody PictureQueryRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getPageSize() <= 0 || request.getPageSize() > 100, ErrorCode.PARAMS_ERROR);
        Page<Picture> picturePage = pictureService.page(
                new Page<>(request.getCurrent(), request.getPageSize()),
                pictureService.getQueryWrapper(request)
        );
        Page<AdminPictureVO> result = new Page<>(
                picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal()
        );
        List<AdminPictureVO> records = picturePage.getRecords().stream()
                .map(AdminPictureVO::from)
                .collect(Collectors.toList());
        Set<Long> userIds = records.stream()
                .map(AdminPictureVO::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, User> users = userIds.isEmpty()
                ? Collections.<Long, User>emptyMap()
                : userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (left, right) -> left));
        for (AdminPictureVO record : records) {
            User owner = users.get(record.getUserId());
            record.setUserName(owner == null ? null : owner.getUserName());
        }
        result.setRecords(records);
        return ResultUtils.success(result);
    }

    @GetMapping("/{id}")
    @AdminPermission(AdminPermissionConstant.PICTURE_VIEW)
    public BaseResponse<AdminPictureVO> detail(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        AdminPictureVO vo = AdminPictureVO.from(picture);
        User owner = userService.getById(picture.getUserId());
        vo.setUserName(owner == null ? null : owner.getUserName());
        return ResultUtils.success(vo);
    }

    @PostMapping("/review")
    @AdminPermission(AdminPermissionConstant.PICTURE_REVIEW)
    @AdminOperation(module = "picture", action = "review", targetType = "picture", idempotent = true)
    public BaseResponse<Boolean> review(@RequestBody PictureReviewRequest reviewRequest,
                                        HttpServletRequest request) {
        ThrowUtils.throwIf(reviewRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        try {
            pictureService.doPictureReview(reviewRequest, loginUser);
            meterRegistry.counter("tucang.picture.review", "result", "success").increment();
            return ResultUtils.success(true);
        } catch (RuntimeException exception) {
            meterRegistry.counter("tucang.picture.review", "result", "failed").increment();
            throw exception;
        }
    }

    @PostMapping("/review/batch")
    @AdminPermission(AdminPermissionConstant.PICTURE_REVIEW)
    @AdminOperation(module = "picture", action = "batch-review", targetType = "picture", idempotent = true)
    public BaseResponse<AdminBatchReviewResultVO> batchReview(
            @Valid @RequestBody AdminBatchReviewRequest batchRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(batchRequest.getIds().size() > 100, ErrorCode.PARAMS_ERROR, "单次最多审核 100 张图片");
        User loginUser = userService.getLoginUser(request);
        AdminBatchReviewResultVO result = new AdminBatchReviewResultVO();
        for (Long id : batchRequest.getIds()) {
            if (id == null || id <= 0) {
                result.getFailures().put(id, "图片 ID 无效");
                continue;
            }
            PictureReviewRequest reviewRequest = new PictureReviewRequest();
            reviewRequest.setId(id);
            reviewRequest.setReviewStatus(batchRequest.getReviewStatus());
            reviewRequest.setReviewMessage(batchRequest.getReviewMessage());
            try {
                pictureService.doPictureReview(reviewRequest, loginUser);
                result.getSuccessIds().add(id);
                meterRegistry.counter("tucang.picture.review", "result", "success").increment();
            } catch (BusinessException exception) {
                if (exception.getCode() == ErrorCode.CONFLICT_ERROR.getCode()) {
                    result.getConflicts().put(id, exception.getMessage());
                } else {
                    result.getFailures().put(id, exception.getMessage());
                }
                meterRegistry.counter("tucang.picture.review", "result", "failed").increment();
            } catch (RuntimeException exception) {
                result.getFailures().put(id, "审核失败");
                meterRegistry.counter("tucang.picture.review", "result", "failed").increment();
            }
        }
        return ResultUtils.success(result);
    }

    @PostMapping("/update")
    @AdminPermission(AdminPermissionConstant.PICTURE_MANAGE)
    @AdminOperation(module = "picture", action = "update", targetType = "picture", idempotent = true)
    public BaseResponse<Boolean> update(@RequestBody PictureUpdateRequest request) {
        ThrowUtils.throwIf(request == null || request.getId() == null, ErrorCode.PARAMS_ERROR);
        Picture oldPicture = pictureService.getById(request.getId());
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        Picture picture = new Picture();
        BeanUtils.copyProperties(request, picture);
        if (request.getTags() != null) {
            picture.setTags(JSONUtil.toJsonStr(request.getTags()));
        }
        picture.setEditTime(new Date());
        pictureService.validPicture(picture);
        boolean updated = pictureService.updateById(picture);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR);
        pictureChangeNotifier.upsert(picture.getId());
        return ResultUtils.success(true);
    }

    @PostMapping("/delete")
    @AdminPermission(AdminPermissionConstant.PICTURE_MANAGE)
    @AdminOperation(module = "picture", action = "delete", targetType = "picture", idempotent = true)
    public BaseResponse<Boolean> delete(@RequestBody DeleteRequest request, HttpServletRequest servletRequest) {
        ThrowUtils.throwIf(request == null || request.getId() <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(servletRequest);
        pictureService.deletePicture(request.getId(), loginUser);
        return ResultUtils.success(true);
    }
}
