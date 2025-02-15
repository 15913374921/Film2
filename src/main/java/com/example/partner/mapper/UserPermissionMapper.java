package com.example.partner.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.partner.entity.UserPermission;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserPermissionMapper extends BaseMapper<UserPermission> {
    @Select("SELECT p.name FROM UserPermission up JOIN Permission p ON up.permission_id = p.id WHERE up.user_id = #{userId}")
    List<String> findPermissionsByUserId(Integer userId);

    @Insert({
            "<script>",
            "INSERT INTO userpermission (user_id, permission_id) VALUES ",
            "<foreach collection='permissionIds' item='permissionId' separator=','>",
            "(#{userId}, #{permissionId})",
            "</foreach>",
            "</script>"
    })
    void addPermissionsToUser(@Param("userId") Integer userId, @Param("permissionIds") List<Integer> permissionIds);
}
