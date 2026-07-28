package com.mushan.tucangbackend.model.vo.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

@Data
public class AdminIndexConsistencyVO {
    private AdminJobExecutionVO latestJob;
    private Page<AdminIndexRecordVO> records;
}
