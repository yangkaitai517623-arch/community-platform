package com.community.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT * FROM sys_user WHERE username = #{username} AND deleted = 0")
    SysUser findByUsername(String username);

    @Select("SELECT * FROM sys_user WHERE role = 0 AND deleted = 0 ORDER BY created_at DESC")
    List<SysUser> findAllUsers();

    @Select("SELECT COUNT(*) FROM sys_user WHERE role = 0 AND deleted = 0")
    int countUsers();

    @Select("SELECT COUNT(*) FROM sys_user WHERE role = 0 AND status = 1 AND deleted = 0")
    int countActiveUsers();
}
