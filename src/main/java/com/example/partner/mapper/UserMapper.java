package com.example.partner.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.partner.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author LZY
 * @since 2024-09-19
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("select * from sys_user where uid = #{uid}")
    User selectByUid(String uid);

    @Select("select 1")
    Integer select1();
}
