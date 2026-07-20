package com.mushan.tucangbackend.manager.auth;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.json.JSONUtil;
import com.mushan.tucangbackend.exception.BusinessException;
import com.mushan.tucangbackend.exception.ErrorCode;
import com.mushan.tucangbackend.manager.auth.model.SpaceUserPermissionConstant;
import com.mushan.tucangbackend.model.entity.Picture;
import com.mushan.tucangbackend.model.entity.Space;
import com.mushan.tucangbackend.model.entity.SpaceUser;
import com.mushan.tucangbackend.model.entity.User;
import com.mushan.tucangbackend.model.enums.SpaceRoleEnum;
import com.mushan.tucangbackend.model.enums.SpaceTypeEnum;
import com.mushan.tucangbackend.service.PictureService;
import com.mushan.tucangbackend.service.SpaceService;
import com.mushan.tucangbackend.service.SpaceUserService;
import com.mushan.tucangbackend.service.UserService;
import com.mushan.tucangbackend.utils.HttpRequestContentTypeUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static com.mushan.tucangbackend.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 自定义权限加载接口实现类
 */
@Component    // 保证此类被 SpringBoot 扫描，完成 Sa-Token 的自定义权限验证扩展
public class StpInterfaceImpl implements StpInterface {

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;


    /**
     * 返回一个账号所拥有的权限码集合 
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 判断 loginType，仅对类型为 "space" 进行权限校验
        if (!StpKit.SPACE_TYPE.equals(loginType)) {
            return new ArrayList<>();
        }
        // 管理员权限，表示权限校验通过
        List<String> ADMIN_PERMISSIONS = spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        // 获取上下文对象
        SpaceUserAuthContext authContext = getAuthContextByRequest();
        // 获取 userId
        User loginUser = (User) StpKit.SPACE.getSessionByLoginId(loginId).get(USER_LOGIN_STATE);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户未登录");
        }
        Long userId = loginUser.getId();

        // 没有具体资源时，仅允许普通用户新增公共图片，不能授予编辑、删除或成员管理权限
        if (isAllFieldsNull(authContext)) {
            return getPermissionsForEmptyContext(loginUser, ADMIN_PERMISSIONS);
        }

        // 图片接口优先按实际 pictureId 鉴权，不能被额外传入的 spaceId 绕过
        Long pictureId = authContext.getPictureId();
        if (pictureId != null) {
            Picture picture = pictureService.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .select(Picture::getId, Picture::getSpaceId, Picture::getUserId)
                    .one();
            if (picture == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到图片信息");
            }
            Long pictureSpaceId = picture.getSpaceId();
            // 公共图库：本人或系统管理员可操作，其他用户仅可查看
            if (pictureSpaceId == null) {
                if (picture.getUserId().equals(userId) || userService.isAdmin(loginUser)) {
                    return ADMIN_PERMISSIONS;
                }
                return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
            }
            return getPermissionsBySpace(pictureSpaceId, userId, loginUser, ADMIN_PERMISSIONS);
        }

        // 如果有 spaceUserId，必然是团队空间，通过数据库查询 SpaceUser 对象
        Long spaceUserId = authContext.getSpaceUserId();
        if (spaceUserId != null) {
            SpaceUser spaceUser = spaceUserService.getById(spaceUserId);
            if (spaceUser == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到空间用户信息");
            }
            // 取出当前登录用户对应的 spaceUser
            SpaceUser loginSpaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceUser.getSpaceId())
                    .eq(SpaceUser::getUserId, userId)
                    .one();
            if (loginSpaceUser == null) {
                return new ArrayList<>();
            }
            // 这里会导致管理员在私有空间没有权限，可以再查一次库处理
            return spaceUserAuthManager.getPermissionsByRole(loginSpaceUser.getSpaceRole());
        }

        // 没有图片或空间成员上下文时，按 spaceId 鉴权
        Long spaceId = authContext.getSpaceId();
        if (spaceId == null) {
            return Collections.emptyList();
        }
        return getPermissionsBySpace(spaceId, userId, loginUser, ADMIN_PERMISSIONS);
    }

    private List<String> getPermissionsBySpace(Long spaceId, Long userId, User loginUser,
                                                List<String> adminPermissions) {
        // 获取 Space 对象
        Space space = spaceService.getById(spaceId);
        if (space == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到空间信息");
        }
        // 根据 Space 类型判断权限
        if (space.getSpaceType() == SpaceTypeEnum.PRIVATE.getValue()) {
            // 私有空间，仅本人或管理员有权限
            if (space.getUserId().equals(userId) || userService.isAdmin(loginUser)) {
                return adminPermissions;
            }
            return Collections.emptyList();
        }
        // 团队空间，查询 SpaceUser 并获取角色和权限
        SpaceUser spaceUser = spaceUserService.lambdaQuery()
                .eq(SpaceUser::getSpaceId, spaceId)
                .eq(SpaceUser::getUserId, userId)
                .one();
        if (spaceUser == null) {
            return Collections.emptyList();
        }
        return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
    }

    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return new ArrayList<>();
    }

    private boolean isAllFieldsNull(Object object) {
        if (object == null) {
            return true; // 对象本身为空
        }
        SpaceUserAuthContext context = (SpaceUserAuthContext) object;
        return context.getId() == null
                && context.getPictureId() == null
                && context.getSpaceId() == null
                && context.getSpaceUserId() == null;
    }

    List<String> getPermissionsForEmptyContext(User loginUser, List<String> adminPermissions) {
        if (userService.isAdmin(loginUser)) {
            return adminPermissions;
        }
        return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_UPLOAD);
    }

    /**
     * 从请求中获取上下文对象
     */
    private SpaceUserAuthContext getAuthContextByRequest() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        SpaceUserAuthContext authRequest;
        // 兼容 get 和 post 操作
        if (HttpRequestContentTypeUtils.isJsonRequest(request)) {
            String body = ServletUtil.getBody(request);
            authRequest = StrUtil.isBlank(body)
                    ? new SpaceUserAuthContext()
                    : JSONUtil.toBean(body, SpaceUserAuthContext.class);
        } else {
            Map<String, String> paramMap = ServletUtil.getParamMap(request);
            authRequest = BeanUtil.toBean(paramMap, SpaceUserAuthContext.class);
        }
        if (authRequest == null) {
            authRequest = new SpaceUserAuthContext();
        }
        fillPathVariables(request, authRequest);
        normalizeContextByModule(request, authRequest);
        return authRequest;
    }

    @SuppressWarnings("unchecked")
    private void fillPathVariables(HttpServletRequest request, SpaceUserAuthContext authRequest) {
        Object attribute = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!(attribute instanceof Map)) {
            return;
        }
        Map<String, String> pathVariables = (Map<String, String>) attribute;
        if (authRequest.getId() == null) {
            authRequest.setId(parseLongPathVariable(pathVariables.get("id")));
        }
        if (authRequest.getPictureId() == null) {
            authRequest.setPictureId(parseLongPathVariable(pathVariables.get("pictureId")));
        }
        if (authRequest.getSpaceId() == null) {
            authRequest.setSpaceId(parseLongPathVariable(pathVariables.get("spaceId")));
        }
    }

    private Long parseLongPathVariable(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "资源 id 格式错误");
        }
    }

    private void normalizeContextByModule(HttpServletRequest request, SpaceUserAuthContext authRequest) {
        String servletPath = StrUtil.removePrefix(request.getServletPath(), "/");
        String moduleName = StrUtil.subBefore(servletPath, "/", false);
        Long id = authRequest.getId();
        switch (moduleName) {
            case "picture":
                if (ObjUtil.isNotNull(id)) {
                    authRequest.setPictureId(id);
                }
                authRequest.setSpaceUserId(null);
                if (authRequest.getPictureId() != null) {
                    authRequest.setSpaceId(null);
                }
                break;
            case "spaceUser":
                authRequest.setPictureId(null);
                // spaceUserId 只能来自该模块实际使用的通用 id，不能信任额外 JSON 字段
                authRequest.setSpaceUserId(id);
                if (id != null) {
                    authRequest.setSpaceId(null);
                }
                break;
            case "space":
                authRequest.setPictureId(null);
                authRequest.setSpaceUserId(null);
                if (ObjUtil.isNotNull(id)) {
                    authRequest.setSpaceId(id);
                }
                break;
            default:
                authRequest.setPictureId(null);
                authRequest.setSpaceId(null);
                authRequest.setSpaceUserId(null);
        }
        // id 只是路由适配字段，完成映射后不再参与权限计算
        authRequest.setId(null);
    }

}
