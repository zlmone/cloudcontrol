package edu.ucar.unidata.cloudcontrol.config;

import java.util.Map;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.web.authentication.ExceptionMappingAuthenticationFailureHandler;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableWebSecurity
@EnableWebMvc
public class SecurityConfig extends WebSecurityConfigurerAdapter {

   @Override
    protected void configure(HttpSecurity http) throws Exception {

        Map<String, String> failureUrlMap = new HashMap<String, String>();
        failureUrlMap.put("org.springframework.security.authentication.BadCredentialsException", "/login/badCredentials");
        failureUrlMap.put("org.springframework.security.authentication.LockedException", "/login/accountLocked");
        failureUrlMap.put("org.springframework.security.authentication.DisabledException", "/login/accountDisabled");

        ExceptionMappingAuthenticationFailureHandler authFailureHandler = new ExceptionMappingAuthenticationFailureHandler();
        authFailureHandler.setExceptionMappings(failureUrlMap);

        http
            .authorizeRequests()
                .mvcMatchers("/denied", "/gettingStarted", "/about", "/welcome", "/welcome/register", "/index.jsp")
                    .permitAll()
                .antMatchers("/**")
                    .authenticated()
            .and()
            .formLogin()
                .loginPage("/j_spring_security_check")
                .usernameParameter("userName")
                .failureHandler(authFailureHandler)
                .permitAll()
            .and()
            .logout()
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))  // HTTP GET
                .logoutSuccessUrl("/login");
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth
            .inMemoryAuthentication()
            .withUser("user")
            .password("password")
            .roles("USER");

        auth
            .inMemoryAuthentication()
            .withUser("admin")
            .password("password")
            .roles("ADMIN");

        auth
            .inMemoryAuthentication()
            .withUser("userAccountLocked")
            .password("password")
            .roles("USER")
            .accountLocked(true);

        auth
            .inMemoryAuthentication()
            .withUser("userAccountDisabled")
            .password("password")
            .roles("USER")
            .disabled(true);
    }
}


