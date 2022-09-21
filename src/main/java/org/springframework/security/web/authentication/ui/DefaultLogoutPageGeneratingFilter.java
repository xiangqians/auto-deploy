package org.springframework.security.web.authentication.ui;

import org.springframework.core.log.LogMessage;
import org.springframework.security.config.annotation.web.configurers.DefaultLoginPageConfigurer;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

/**
 * Generates a default log out page.
 * <p>
 * {@link DefaultLoginPageConfigurer}
 * https://github.com/spring-projects/spring-security/blob/5.8.x/web/src/main/java/org/springframework/security/web/authentication/ui/DefaultLogoutPageGeneratingFilter.java
 *
 * @author Rob Winch
 * @since 5.1
 */
public class DefaultLogoutPageGeneratingFilter extends OncePerRequestFilter {

    private RequestMatcher matcher = new AntPathRequestMatcher("/logout", "GET");

    private Function<HttpServletRequest, Map<String, String>> resolveHiddenInputs = (request) -> Collections.emptyMap();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (this.matcher.matches(request)) {
            renderLogout(request, response);
        } else {
            if (logger.isTraceEnabled()) {
                logger.trace(LogMessage.format("Did not render default logout page since request did not match [%s]",
                        this.matcher));
            }
            filterChain.doFilter(request, response);
        }
    }

    private void renderLogout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n");
        sb.append("<html lang=\"en\">\n");
        sb.append("  <head>\n");
        sb.append("    <meta charset=\"utf-8\">\n");
        sb.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1, shrink-to-fit=no\">\n");
        sb.append("    <meta name=\"description\" content=\"\">\n");
        sb.append("    <meta name=\"author\" content=\"\">\n");
        sb.append("    <title>Confirm Log Out?</title>\n");
        sb.append("    <link href=\"/static/security/bootstrap.min.css\" "
                + "rel=\"stylesheet\""
                + "crossorigin=\"anonymous\">\n");
        sb.append("    <link href=\"/static/security/signin.css\" "
                + "rel=\"stylesheet\" crossorigin=\"anonymous\"/>\n");
        sb.append("  </head>\n");
        sb.append("  <body>\n");
        sb.append("     <div class=\"container\">\n");
        sb.append("      <form class=\"form-signin\" method=\"post\" action=\"" + request.getContextPath()
                + "/logout\">\n");
        sb.append("        <h2 class=\"form-signin-heading\">Are you sure you want to log out?</h2>\n");
        sb.append(renderHiddenInputs(request)
                + "        <button class=\"btn btn-lg btn-primary btn-block\" type=\"submit\">Log Out</button>\n");
        sb.append("      </form>\n");
        sb.append("    </div>\n");
        sb.append("  </body>\n");
        sb.append("</html>");
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write(sb.toString());
    }

    /**
     * Sets a Function used to resolve a Map of the hidden inputs where the key is the
     * name of the input and the value is the value of the input. Typically this is used
     * to resolve the CSRF token.
     *
     * @param resolveHiddenInputs the function to resolve the inputs
     */
    public void setResolveHiddenInputs(Function<HttpServletRequest, Map<String, String>> resolveHiddenInputs) {
        Assert.notNull(resolveHiddenInputs, "resolveHiddenInputs cannot be null");
        this.resolveHiddenInputs = resolveHiddenInputs;
    }

    private String renderHiddenInputs(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> input : this.resolveHiddenInputs.apply(request).entrySet()) {
            sb.append("<input name=\"");
            sb.append(input.getKey());
            sb.append("\" type=\"hidden\" value=\"");
            sb.append(input.getValue());
            sb.append("\" />\n");
        }
        return sb.toString();
    }

}

