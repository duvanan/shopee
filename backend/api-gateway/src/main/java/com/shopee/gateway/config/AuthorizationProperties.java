package com.shopee.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Anh xa "route can quyen gi" duoc khai bao tap trung o day (nap tu
 * config-repo/api-gateway.yml qua Config Server), thay vi rai
 * @PreAuthorize o tung microservice. Xem docs/architecture/rbac.md muc 6.
 */
@ConfigurationProperties(prefix = "authorization")
public class AuthorizationProperties {

    private List<RouteRule> publicRoutes = new ArrayList<>();
    private List<RouteRule> authenticatedOnly = new ArrayList<>();
    private List<RouteRule> permissionRequired = new ArrayList<>();

    public List<RouteRule> getPublicRoutes() {
        return publicRoutes;
    }

    public void setPublicRoutes(List<RouteRule> publicRoutes) {
        this.publicRoutes = publicRoutes;
    }

    public List<RouteRule> getAuthenticatedOnly() {
        return authenticatedOnly;
    }

    public void setAuthenticatedOnly(List<RouteRule> authenticatedOnly) {
        this.authenticatedOnly = authenticatedOnly;
    }

    public List<RouteRule> getPermissionRequired() {
        return permissionRequired;
    }

    public void setPermissionRequired(List<RouteRule> permissionRequired) {
        this.permissionRequired = permissionRequired;
    }

    public static class RouteRule {
        /** Ant-style path pattern, vd /api/products/** */
        private String path;
        /** null = ap dung cho moi HTTP method */
        private String method;
        /** chi dung cho nhom permissionRequired */
        private String permission;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getPermission() {
            return permission;
        }

        public void setPermission(String permission) {
            this.permission = permission;
        }
    }
}
