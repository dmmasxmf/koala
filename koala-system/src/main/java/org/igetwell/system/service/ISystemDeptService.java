package org.igetwell.system.service;

import org.igetwell.common.uitls.ResponseEntity;
import org.igetwell.system.entity.SystemDept;
import org.igetwell.system.vo.DeptTree;

import java.util.List;

public interface ISystemDeptService {

    /**
     * 查询所有部门(树形结构)
     * @param tenantId
     * @return
     */
    List<DeptTree> getList(String tenantId);

    /**
     * 获取部门子级
     * 单表(只查询部门表)
     * @param deptId 部门id
     * @return deptIds
     */
    List<Long> getDeptAncestors(Long deptId);

    /**
     * 获取部门子级
     * 中间表(查询部门关系表)
     * @param deptId 部门id
     * @return deptIds
     */
    List<Long> getAncestors(Long deptId);

    void deleteById(Long id);

    ResponseEntity insert(SystemDept systemDept);

    ResponseEntity update(SystemDept systemDept);
}
