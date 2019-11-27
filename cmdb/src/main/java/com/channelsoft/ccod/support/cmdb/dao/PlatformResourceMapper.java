package com.channelsoft.ccod.support.cmdb.dao;

import com.channelsoft.ccod.support.cmdb.vo.PlatformResourceVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @ClassName: PlatformResourceMapper
 * @Author: lanhb
 * @Description: 用来定义PlatformResource的dao接口
 * @Date: 2019/11/26 23:08
 * @Version: 1.0
 */
@Component
@Mapper
public interface PlatformResourceMapper {
    /**
     * 根据平台id查询
     * @param platformId 平台id
     * @param domainId  域id
     * @param hostIp 服务器ip
     * @return 查询结果
     * @throws DataAccessException
     */
    PlatformResourceVo selectByPlatform(@Param("platformId")String platformId, @Param("domainId")String domainId, @Param("hostIp")String hostIp) throws DataAccessException;

    /**
     * 查询所有平台资源
     * @return 查询结果
     * @throws DataAccessException
     */
    List<PlatformResourceVo> select() throws DataAccessException;
}
