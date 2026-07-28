package com.mushan.tucangbackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mushan.tucangbackend.mapper.AdminOperationLogMapper;
import com.mushan.tucangbackend.model.entity.AdminOperationLog;
import com.mushan.tucangbackend.service.AdminOperationLogService;
import org.springframework.stereotype.Service;

@Service
public class AdminOperationLogServiceImpl
        extends ServiceImpl<AdminOperationLogMapper, AdminOperationLog>
        implements AdminOperationLogService {
}
