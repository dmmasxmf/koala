package org.igetwell.oauth.security;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.igetwell.common.constans.SecurityConstants;
import org.igetwell.common.constans.cache.CacheKey;
import org.igetwell.common.data.tenant.TenantContextHolder;
import org.igetwell.oauth.security.provider.IUserDetailsService;
import org.igetwell.system.entity.SystemUser;
import org.igetwell.system.feign.SystemRoleClient;
import org.igetwell.system.feign.SystemUserClient;
import org.igetwell.system.vo.SystemRoleVo;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@AllArgsConstructor
public class SpringSecurityService implements IUserDetailsService {

    private final SystemUserClient systemUserClient;

    private final SystemRoleClient systemRoleClient;

    private final CacheManager cacheManager;

    @Override
    @SneakyThrows
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Cache cache = cacheManager.getCache(CacheKey.USER_DETAILS);
        if (cache != null && cache.get(username) != null) {
            return (KoalaUser) cache.get(username).get();
        }
        UserDetails systemUserDetails = getUserDetails(username);
        cache.put(username, systemUserDetails);
        return systemUserDetails;
    }

    /**
     * 构建UserDetails
     * @param username
     * @return
     */
    private UserDetails getUserDetails(String username){
        String tenant = TenantContextHolder.getTenantId();
        if (StringUtils.isEmpty(tenant)){
            throw new UsernameNotFoundException("租户ID不能为空");
        }
        SystemUser systemUser = systemUserClient.loadByUsername(tenant, username);
        if (null == systemUser){
            throw new UsernameNotFoundException("用户账号: " + username + " 不存在");
        }
        //这个方法里实现了根据用户查询用户所有的角色
        List<SystemRoleVo> roles = systemRoleClient.loadByTenant(tenant, systemUser.getId());

        //定义权限集合
        Collection<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
        roles.forEach(role -> {
            authorities.add(new SimpleGrantedAuthority(role.getRoleAlias()));
        });

        return new KoalaUser(systemUser.getId(), systemUser.getTenantId(), systemUser.getRoleId(), systemUser.getDeptId(),
                systemUser.getUsername(), SecurityConstants.BCRYPT + systemUser.getPassword(),
                true, systemUser.isAccountNonExpired(), systemUser.isAccountNonLocked(),
                systemUser.isCredentialsNonExpired(), authorities);
    }

    @Override
    public UserDetails loadUserByMobile(String mobile) throws UsernameNotFoundException {
        Cache cache = cacheManager.getCache(CacheKey.USER_DETAILS);
        if (cache != null && cache.get(mobile) != null) {
            return (KoalaUser) cache.get(mobile).get();
        }
        UserDetails systemUserDetails = getMobileUserDetails(mobile);
        cache.put(mobile, systemUserDetails);
        return systemUserDetails;
    }


    /**
     * 构建UserDetails
     * @param mobile
     * @return
     */
    private UserDetails getMobileUserDetails(String mobile){
        String tenant = TenantContextHolder.getTenantId();
        if (StringUtils.isEmpty(tenant)){
            throw new UsernameNotFoundException("租户ID不能为空");
        }
        SystemUser systemUser = systemUserClient.loadByMobile(mobile);
        if (null == systemUser){
            throw new UsernameNotFoundException("手机号: " + mobile + " 不存在");
        }
        //这个方法里实现了根据用户查询用户所有的角色
        List<SystemRoleVo> roles = systemRoleClient.loadByTenant(tenant, systemUser.getId());
        //定义权限集合
        Collection<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
        roles.forEach(role -> {
            authorities.add(new SimpleGrantedAuthority(role.getRoleAlias()));
        });

        return new KoalaUser(systemUser.getId(), systemUser.getTenantId(), systemUser.getRoleId(), systemUser.getDeptId(),
                systemUser.getUsername(), SecurityConstants.BCRYPT + systemUser.getPassword(),
                systemUser.isEnabled(), systemUser.isAccountNonExpired(), systemUser.isAccountNonLocked(),
                systemUser.isCredentialsNonExpired(), authorities);
    }
}
