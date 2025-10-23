package com.mushan.tucangbackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mushan.tucangbackend.model.dto.aigenhistory.AiGenHistoryAddRequest;
import com.mushan.tucangbackend.model.entity.AiGenHistory;
import com.mushan.tucangbackend.model.entity.User;
import com.mushan.tucangbackend.model.vo.AiGenHistoryVO;
import com.mushan.tucangbackend.service.AiGenHistoryService;
import com.mushan.tucangbackend.mapper.AiGenHistoryMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.stereotype.Service;
import cn.hutool.json.JSONUtil;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author Danie
* @description 针对表【ai_gen_history】的数据库操作Service实现
* @createDate 2025-10-15 20:16:33
*/
@Service
public class AiGenHistoryServiceImpl extends ServiceImpl<AiGenHistoryMapper, AiGenHistory>
    implements AiGenHistoryService{

    @Override
    public long addAiGenHistory(AiGenHistoryAddRequest aiGenHistoryAddRequest, User loginUser) {
        AiGenHistory aiGenHistory = new AiGenHistory();
        aiGenHistory.setUserId(loginUser.getId());
        aiGenHistory.setPrompt(aiGenHistoryAddRequest.getPrompt());
        aiGenHistory.setTaskId(aiGenHistoryAddRequest.getTaskId());
        aiGenHistory.setStatus(aiGenHistoryAddRequest.getStatus());
        aiGenHistory.setCreateTime(new Date());
        this.save(aiGenHistory);
        return aiGenHistory.getId();
    }

    @Override
    public List<AiGenHistoryVO> listUserAiGenHistories(Long userId) {
        // 计算三天前的时间
        Date threeDaysAgo = new Date(System.currentTimeMillis() - 3L * 24 * 60 * 60 * 1000);
        
        QueryWrapper<AiGenHistory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.gt("createTime", threeDaysAgo); // 只查询三天内的记录
        queryWrapper.orderByDesc("createTime");
        return this.list(queryWrapper).stream().map(this::convertToVO).collect(Collectors.toList());
    }
    
    @Override
    public int deleteBefore(Date beforeDate) {
        UpdateWrapper<AiGenHistory> updateWrapper = new UpdateWrapper<>();
        updateWrapper.lt("createTime", beforeDate);
        return this.getBaseMapper().delete(updateWrapper);
    }
    
    /**
     * 将AiGenHistory实体转换为AiGenHistoryVO视图对象
     * 
     * @param aiGenHistory AI生成历史实体
     * @return AI生成历史视图对象
     */
    private AiGenHistoryVO convertToVO(AiGenHistory aiGenHistory) {
        AiGenHistoryVO vo = new AiGenHistoryVO();
        vo.setId(aiGenHistory.getId());
        vo.setUserId(aiGenHistory.getUserId());
        vo.setPrompt(aiGenHistory.getPrompt());
        vo.setTaskId(aiGenHistory.getTaskId());
        
        // 将JSON格式的imageUrl转换为List<String>
        String imageUrl = aiGenHistory.getImageUrl();
        if (imageUrl != null && JSONUtil.isTypeJSON(imageUrl)) {
            vo.setImageUrlList(JSONUtil.toList(imageUrl, String.class));
        } else if (imageUrl != null && !imageUrl.isEmpty()) {
            // 如果不是JSON格式但有值，则作为单个元素添加到列表中
            vo.setImageUrlList(Collections.singletonList(imageUrl));
        } else {
            vo.setImageUrlList(Collections.emptyList());
        }
        
        vo.setStatus(aiGenHistory.getStatus());
        vo.setCreateTime(aiGenHistory.getCreateTime());
        return vo;
    }
}