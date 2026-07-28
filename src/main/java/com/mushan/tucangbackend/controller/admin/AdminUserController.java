package com.mushan.tucangbackend.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mushan.tucangbackend.annotation.AdminOperation;
import com.mushan.tucangbackend.annotation.AdminPermission;
import com.mushan.tucangbackend.common.BaseResponse;
import com.mushan.tucangbackend.common.ResultUtils;
import com.mushan.tucangbackend.constant.AdminPermissionConstant;
import com.mushan.tucangbackend.exception.BusinessException;
import com.mushan.tucangbackend.exception.ErrorCode;
import com.mushan.tucangbackend.exception.ThrowUtils;
import com.mushan.tucangbackend.model.dto.admin.AdminUserUpdateRequest;
import com.mushan.tucangbackend.model.dto.user.UserQueryRequest;
import com.mushan.tucangbackend.model.entity.User;
import com.mushan.tucangbackend.model.enums.UserRoleEnum;
import com.mushan.tucangbackend.model.vo.UserVO;
import com.mushan.tucangbackend.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    @Resource
    private UserService userService;

    @PostMapping("/page")
    @AdminPermission(AdminPermissionConstant.USER_MANAGE)
    public BaseResponse<Page<UserVO>> page(@RequestBody UserQueryRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getPageSize() <= 0 || request.getPageSize() > 100, ErrorCode.PARAMS_ERROR);
        Page<User> userPage = userService.page(
                new Page<>(request.getCurrent(), request.getPageSize()),
                userService.getQueryWrapper(request)
        );
        Page<UserVO> result = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        List<UserVO> records = userService.getUserVOList(userPage.getRecords());
        result.setRecords(records);
        return ResultUtils.success(result);
    }

    @GetMapping("/{id}")
    @AdminPermission(AdminPermissionConstant.USER_MANAGE)
    public BaseResponse<UserVO> detail(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(userService.getUserVO(user));
    }

    @PostMapping("/update")
    @AdminPermission(AdminPermissionConstant.USER_MANAGE)
    @AdminOperation(module = "user", action = "update-role-status", targetType = "user", idempotent = true)
    public BaseResponse<Boolean> update(@Valid @RequestBody AdminUserUpdateRequest request,
                                        HttpServletRequest servletRequest) {
        User loginUser = userService.getLoginUser(servletRequest);
        User target = userService.getById(request.getId());
        ThrowUtils.throwIf(target == null, ErrorCode.NOT_FOUND_ERROR);
        if (loginUser.getId().equals(target.getId())
                && ((request.getUserRole() != null && !target.getUserRole().equals(request.getUserRole()))
                || (request.getUserStatus() != null && Integer.valueOf(1).equals(request.getUserStatus())))) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "不能禁用或降级当前账号");
        }
        if (request.getUserRole() != null
                && UserRoleEnum.getEnumByValue(request.getUserRole()) == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户角色无效");
        }
        if (request.getUserStatus() != null
                && request.getUserStatus() != 0 && request.getUserStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户状态无效");
        }
        boolean wasEnabledAdmin = UserRoleEnum.ADMIN.getValue().equals(target.getUserRole())
                && !Integer.valueOf(1).equals(target.getUserStatus());
        boolean willBeEnabledAdmin = (request.getUserRole() == null
                ? UserRoleEnum.ADMIN.getValue().equals(target.getUserRole())
                : UserRoleEnum.ADMIN.getValue().equals(request.getUserRole()))
                && (request.getUserStatus() == null
                ? !Integer.valueOf(1).equals(target.getUserStatus())
                : request.getUserStatus() == 0);
        if (wasEnabledAdmin && !willBeEnabledAdmin) {
            long adminCount = userService.lambdaQuery()
                    .eq(User::getUserRole, UserRoleEnum.ADMIN.getValue())
                    .and(wrapper -> wrapper.eq(User::getUserStatus, 0).or().isNull(User::getUserStatus))
                    .count();
            if (adminCount <= 1) {
                throw new BusinessException(ErrorCode.CONFLICT_ERROR, "必须保留至少一个有效管理员");
            }
        }
        User update = new User();
        update.setId(target.getId());
        update.setUserRole(request.getUserRole());
        update.setUserStatus(request.getUserStatus());
        boolean changed = userService.updateById(update);
        ThrowUtils.throwIf(!changed, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }
}
