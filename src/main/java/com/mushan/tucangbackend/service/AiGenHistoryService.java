package com.mushan.tucangbackend.service;

import com.mushan.tucangbackend.model.dto.aigenhistory.AiGenHistoryAddRequest;
import com.mushan.tucangbackend.model.entity.AiGenHistory;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mushan.tucangbackend.model.entity.User;
import com.mushan.tucangbackend.model.vo.AiGenHistoryVO;

import java.util.List;

/**
* @author Danie
* @description 针对表【ai_gen_history】的数据库操作Service
* @createDate 2025-10-15 20:16:33
*/
public interface AiGenHistoryService extends IService<AiGenHistory> {

    /**
     * 添加AI生成历史记录
     *
     * @param aiGenHistoryAddRequest AI生成历史添加请求
     * @param loginUser 当前登录用户
     */
    long addAiGenHistory(AiGenHistoryAddRequest aiGenHistoryAddRequest, User loginUser);

    /**
     * 获取用户AI生成历史列表
     *
     * @param userId 用户ID
     * @return AI生成历史列表
     */
    List<AiGenHistoryVO> listUserAiGenHistories(Long userId);

}