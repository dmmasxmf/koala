package org.igetwell.system.mapper;

import java.util.List;

public interface SystemDeptMapper {


    /**
     * 获取部门子级
     * 单表(只查询部门表)
     * @param deptId 部门id
     * @return deptIds
     */
    public List<Long> getDeptAncestors(Long deptId);

    /**
     * 获取部门子级
     * 中间表(查询部门关系表)
     * @param deptId 部门id
     * @return deptIds
     */
    public List<Long> getAncestors(Long deptId);
}