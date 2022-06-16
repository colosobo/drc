package com.timevale.drc.pd.service.user;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 *
 */
@Slf4j
public class HTTPBasicAuthorizeFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setCharacterEncoding("UTF-8");
        httpResponse.setContentType("application/json; charset=utf-8");
        if (HostNameUtil.getIp().startsWith("192")) {
            // 本地调试时使用.
            httpResponse.setHeader("Access-Control-Allow-Origin", "http://localhost:8080");
        }
        String token = httpRequest.getHeader(UserUtil.token);

        if (!StringUtils.isEmpty(token)) {
            UserInfo userInfo = UserUtil.parse(token);
            UserContext.set(userInfo);
        } else {
            UserInfo userInfo = UserUtil.parse(token);
            userInfo.setId("system");
            UserContext.set(userInfo);
        }

        try {
            chain.doFilter(httpRequest, response);
        } finally {
            UserContext.remove();
        }
    }

    @Override
    public void destroy() {

    }

}
