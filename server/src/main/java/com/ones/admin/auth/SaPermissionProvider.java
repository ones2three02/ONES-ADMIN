package com.ones.admin.auth;

import cn.dev33.satoken.stp.StpInterface;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SaPermissionProvider implements StpInterface {

    private final AuthService authService;

    public SaPermissionProvider(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return authService.getPermissionList(Long.valueOf(String.valueOf(loginId)));
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return authService.getRoleList(Long.valueOf(String.valueOf(loginId)));
    }
}
