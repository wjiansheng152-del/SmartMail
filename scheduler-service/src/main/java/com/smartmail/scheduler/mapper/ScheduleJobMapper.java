package com.smartmail.scheduler.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmail.scheduler.entity.ScheduleJob;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时计划 Mapper。
 */
public interface ScheduleJobMapper extends BaseMapper<ScheduleJob> {

    /**
     * 查询待执行且已到点的计划（run_at 非空且 run_at <= 当前时间，status = pending）。
     */
    List<ScheduleJob> selectPendingRunAt(@Param("now") LocalDateTime now);
}
