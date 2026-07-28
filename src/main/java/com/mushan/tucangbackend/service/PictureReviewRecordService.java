package com.mushan.tucangbackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mushan.tucangbackend.mapper.PictureReviewRecordMapper;
import com.mushan.tucangbackend.model.entity.Picture;
import com.mushan.tucangbackend.model.entity.PictureReviewRecord;
import com.mushan.tucangbackend.model.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Service
public class PictureReviewRecordService {

    @Resource
    private PictureReviewRecordMapper mapper;

    public void recordDecision(Picture picture, Integer toStatus, String reasonCode,
                               String message, User reviewer) {
        mapper.insert(build(picture, toStatus, reasonCode, message, reviewer, 0));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordConflict(Picture picture, Integer toStatus, String reasonCode,
                               String message, User reviewer) {
        mapper.insert(build(picture, toStatus, reasonCode, message, reviewer, 1));
    }

    public List<PictureReviewRecord> listByPicture(Long pictureId) {
        return mapper.selectPage(
                new Page<PictureReviewRecord>(1, 100),
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<PictureReviewRecord>()
                        .eq("pictureId", pictureId)
                        .orderByDesc("createTime")
        ).getRecords();
    }

    public List<PictureReviewRecord> listForStats(Date startTime, Long reviewerId) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<PictureReviewRecord> query =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        query.ge("createTime", startTime)
                .eq(reviewerId != null, "reviewerId", reviewerId)
                .eq("conflict", 0)
                .orderByDesc("createTime");
        return mapper.selectList(query);
    }

    private PictureReviewRecord build(Picture picture, Integer toStatus, String reasonCode,
                                      String message, User reviewer, int conflict) {
        PictureReviewRecord record = new PictureReviewRecord();
        record.setPictureId(picture.getId());
        record.setFromStatus(picture.getReviewStatus());
        record.setToStatus(toStatus);
        record.setReviewerId(reviewer.getId());
        record.setReviewerRole(reviewer.getUserRole());
        record.setReasonCode(reasonCode);
        record.setReviewMessage(message);
        record.setDurationMs(picture.getCreateTime() == null
                ? null : Math.max(0L, System.currentTimeMillis() - picture.getCreateTime().getTime()));
        record.setConflict(conflict);
        record.setCreateTime(new Date());
        return record;
    }
}
