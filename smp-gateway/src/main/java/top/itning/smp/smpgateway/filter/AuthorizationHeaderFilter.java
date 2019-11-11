package top.itning.smp.smpgateway.filter;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import top.itning.smp.smpgateway.entity.LoginUser;
import top.itning.smp.smpgateway.exception.TokenException;
import top.itning.smp.smpgateway.util.JwtUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_DECORATION_FILTER_ORDER;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;

/**
 * {@link HttpHeaders#AUTHORIZATION}请求头检查过滤器
 * 该过滤器检查请求是否包含{@link HttpHeaders#AUTHORIZATION}请求头，并将
 * 其转换为用户信息，如果转换失败将返回错误视图
 *
 * @author itning
 */
@Component
public class AuthorizationHeaderFilter extends ZuulFilter {
    /**
     * 忽略过滤路径
     */
    private static final String[] IGNORE_SERVER_PATH = {};

    @Override
    public String filterType() {
        return PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return PRE_DECORATION_FILTER_ORDER - 1;
    }

    @Override
    public boolean shouldFilter() {
        RequestContext requestContext = RequestContext.getCurrentContext();
        String servletPath = requestContext.getRequest().getServletPath();
        for (String path : IGNORE_SERVER_PATH) {
            if (servletPath.startsWith(path)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Object run() throws ZuulException {
        RequestContext requestContext = RequestContext.getCurrentContext();
        HttpServletRequest request = requestContext.getRequest();
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.isBlank(authorizationHeader)) {
            requestContext.setSendZuulResponse(false);
            requestContext.setResponseStatusCode(HttpStatus.UNAUTHORIZED.value());
            HttpServletResponse response = requestContext.getResponse();
            response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
            try (PrintWriter writer = response.getWriter()) {
                writer.write("{\"code\": 401,\"msg\": \"请先登陆\",\"data\": \"\"}");
                writer.flush();
                requestContext.setResponse(response);
            } catch (IOException e) {
                throw new ZuulException(e.getMessage(), 500, "");
            }
        } else {
            try {
                LoginUser loginUser = JwtUtils.getLoginUser(authorizationHeader);
                Map<String, List<String>> qp = new HashMap<>((int) (5 / 0.75) + 1);
                qp.put("email", Collections.singletonList(loginUser.getEmail()));
                qp.put("name", Collections.singletonList(loginUser.getName()));
                qp.put("tel", Collections.singletonList(loginUser.getTel()));
                qp.put("username", Collections.singletonList(loginUser.getUsername()));
                qp.put("roleId", Collections.singletonList(loginUser.getRole().getId()));
                requestContext.setRequestQueryParams(qp);
            } catch (TokenException e) {
                requestContext.setSendZuulResponse(false);
                requestContext.setResponseStatusCode(e.getCode().value());
                HttpServletResponse response = requestContext.getResponse();
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                try (PrintWriter writer = response.getWriter()) {
                    writer.write("{" +
                            "\"code\":" +
                            e.getCode().value() +
                            "," +
                            "\"msg\":\"" +
                            e.getMsg() +
                            "\",\"data\":\"\"}");
                    writer.flush();
                    requestContext.setResponse(response);
                } catch (IOException ex) {
                    throw new ZuulException(e.getMessage(), 500, "");
                }
            }
        }
        return null;
    }
}
