package com.mushan.tucangbackend.service;

import com.mushan.tucangbackend.model.es.PictureEsDTO;
import com.mushan.tucangbackend.model.vo.admin.AdminMonitorVO;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AdminMonitorService {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private DataSource dataSource;

    @Resource
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired(required = false)
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    public AdminMonitorVO getOverview() {
        AdminMonitorVO vo = new AdminMonitorVO();
        vo.setUptimeSeconds(ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
        vo.setAvailableProcessors(Runtime.getRuntime().availableProcessors());
        vo.setSystemLoadAverage(ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage());
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        vo.setHeapUsedBytes(memory.getHeapMemoryUsage().getUsed());
        vo.setHeapMaxBytes(memory.getHeapMemoryUsage().getMax());
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        vo.setThreadCount(threads.getThreadCount());
        vo.setPeakThreadCount(threads.getPeakThreadCount());
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikari = (HikariDataSource) dataSource;
            if (hikari.getHikariPoolMXBean() != null) {
                vo.setDbActiveConnections(hikari.getHikariPoolMXBean().getActiveConnections());
                vo.setDbIdleConnections(hikari.getHikariPoolMXBean().getIdleConnections());
            }
        }
        vo.setDependencies(getDependencyStatus());
        return vo;
    }

    public Map<String, String> getDependencyStatus() {
        Map<String, String> status = new LinkedHashMap<>();
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            status.put("database", "UP");
        } catch (RuntimeException exception) {
            status.put("database", "DOWN");
        }
        RedisConnection connection = null;
        try {
            connection = redisConnectionFactory.getConnection();
            status.put("redis", "PONG".equalsIgnoreCase(connection.ping()) ? "UP" : "DOWN");
        } catch (RuntimeException exception) {
            status.put("redis", "DOWN");
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
        try {
            boolean exists = elasticsearchRestTemplate != null
                    && elasticsearchRestTemplate.indexOps(PictureEsDTO.class).exists();
            status.put("elasticsearch", exists ? "UP" : "DOWN");
        } catch (RuntimeException exception) {
            status.put("elasticsearch", "DOWN");
        }
        return status;
    }
}
