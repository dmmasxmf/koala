package org.igetwell.system.member.mapper;

import org.igetwell.system.member.entity.Member;

public interface MemberMapper {

    int deleteById(Long id);

    int insert(Member member);

    Member get(Long id);

    Member getMemberNo(String memberNo);

    int update(Member member);
}