package com.example.partner.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.partner.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {
    @Select("SELECT * FROM permission")
    List<Permission> findPermissions();

    @Select({
        "<script>",
        "SELECT id FROM permission WHERE name IN ",
        "<foreach collection='names' item='name' open='(' separator=',' close=')'>",
        "#{name}",
        "</foreach>",
        "</script>"
    })
    List<Integer> findPermissionIdsByNames(@Param("names") List<String> names);

    void addPermissionsToUser(@Param("userId") Integer userId, @Param("permissionIds") List<Integer> permissionIds);

    @Select("SELECT name FROM permission WHERE id IN (SELECT permission_id FROM userpermission WHERE user_id = #{userId})")
    List<String> getPermissionsByUserId(@Param("userId") Integer userId);
}