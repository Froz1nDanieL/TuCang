package com.mushan.tucangbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mushan.tucangbackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.mushan.tucangbackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.mushan.tucangbackend.model.entity.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mushan.tucangbackend.model.vo.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Danie
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2025-08-14 16:03:16
*/
public interface SpaceUserService extends IService<SpaceUser> {


    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    void validSpaceUser(SpaceUser spaceUser, boolean add);

    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);
}
