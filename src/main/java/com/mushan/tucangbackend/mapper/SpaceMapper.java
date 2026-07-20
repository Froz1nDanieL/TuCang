package com.mushan.tucangbackend.mapper;

import com.mushan.tucangbackend.model.entity.Space;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
* @author Danie
* @description 针对表【space(空间)】的数据库操作Mapper
* @createDate 2025-08-04 15:01:50
* @Entity com.mushan.tucangbackend.model.entity.Space
*/
public interface SpaceMapper extends BaseMapper<Space> {

    /**
     * 原子更新空间用量，并在同一条 SQL 中校验上下限。
     *
     * @return 更新行数；0 表示空间不存在、已删除或额度不足
     */
    @Update("UPDATE space "
            + "SET totalSize = totalSize + #{sizeDelta}, "
            + "totalCount = totalCount + #{countDelta} "
            + "WHERE id = #{spaceId} AND isDelete = 0 "
            + "AND totalSize + #{sizeDelta} BETWEEN 0 AND maxSize "
            + "AND totalCount + #{countDelta} BETWEEN 0 AND maxCount")
    int updateSpaceUsage(@Param("spaceId") Long spaceId,
                         @Param("sizeDelta") long sizeDelta,
                         @Param("countDelta") long countDelta);
}




