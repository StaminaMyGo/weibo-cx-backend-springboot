package com.hn.it.weibo.mapper;

import com.hn.it.weibo.entity.ReportItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ReportMapper {
    public List<ReportItem> reportPublishByUser();
    
    /**
     * 执行动态 SQL 查询
     * @param sql 动态生成的 SQL 语句
     * @return 查询结果列表
     */
    public List<Map<String, Object>> reportBySql(@Param("sql") String sql);
}
