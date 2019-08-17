package org.igetwell.system.configure;

import lombok.AllArgsConstructor;
import org.igetwell.system.security.MyFilterSecurityInterceptor;
import org.igetwell.system.security.SpringSecurityService;
import org.igetwell.system.security.extractor.JwtTokenExtractor;
import org.igetwell.system.security.filter.CORSFilter;
import org.igetwell.system.security.filter.JwtAuthenticationEntryPoint;
import org.igetwell.system.security.handler.AuthenticationAccessDeniedHandler;
import org.igetwell.system.security.handler.AuthenticationFailureHandler;
import org.igetwell.system.security.handler.AuthenticationSuccessHandler;
import org.igetwell.system.support.KoalaPasswordEncoderFactories;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableWebSecurity
@AllArgsConstructor
public class SpringSecurityWebAppConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private SpringSecurityService springSecurityService;

    @Autowired
    private JwtAuthenticationEntryPoint unauthorizedHandler;
    @Autowired
    private AuthenticationSuccessHandler successHandler;
    @Autowired
    private AuthenticationFailureHandler failureHandler;
    @Autowired
    private AuthenticationAccessDeniedHandler accessDeniedHandler;
    @Autowired
    private CORSFilter corsFilter;
    @Autowired
    private JwtTokenExtractor jwtTokenExtractor;

    @Autowired
    private MyFilterSecurityInterceptor myFilterSecurityInterceptor;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable() // oauth server 不需要 csrf 防护
                .httpBasic().disable() // 禁止 basic 认证
                .authorizeRequests()
                .antMatchers("/oauth/token", "/oauth/token/**").permitAll()
                .and()
                .authorizeRequests()
                .antMatchers("/**").authenticated() //其他请求都需要登录后访问
                .and()
                .formLogin()
                .loginProcessingUrl("/login")
                .successHandler(successHandler)
                .failureHandler(failureHandler)
                .and().exceptionHandling()
                .accessDeniedHandler(accessDeniedHandler)
                .authenticationEntryPoint(unauthorizedHandler)
                // 基于token，所以不需要session
                .and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .addFilterBefore(myFilterSecurityInterceptor, FilterSecurityInterceptor.class)
                .addFilterBefore(jwtTokenExtractor, UsernamePasswordAuthenticationFilter.class);
    }
    /*@Override
    public void configure(HttpSecurity http) throws Exception {
        http.csrf().disable();
        http.requestMatchers().antMatchers("/oauth/**","/login/**","/logout/**")
                .and()
                .authorizeRequests()
                .antMatchers("/oauth/**").authenticated()
                .and()
                .formLogin().permitAll();
    }*/


    @Autowired
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth
                .userDetailsService(springSecurityService)
                .passwordEncoder(new BCryptPasswordEncoder());
    }

    @Bean
    protected AuthenticationManager authenticationManager() throws Exception {
        return super.authenticationManager();
    }
    /*@Bean
    protected JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter(){
        return new JwtAuthenticationTokenFilter();
    }*/

    /*@Bean
    public PasswordEncoder passwordEncoder(){
        //密码加密
        return new BCryptPasswordEncoder();
    }*/

    @Bean
    public PasswordEncoder passwordEncoder() {
        return KoalaPasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
