package jb.owaspextension;

import java.io.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import net.jforum.context.*;
import net.jforum.context.web.*;
import net.jforum.util.legacy.commons.fileupload.servlet.*;

import org.owasp.csrfguard.*;
import org.owasp.csrfguard.action.*;
import org.owasp.csrfguard.http.*;

/**
 * Didn't use OWASP filter because couldn't map jforum actions to urls
 * consistently. Copied from OWASP and added getJForumMethodName() and changed
 * logic near isValidRequest line.
 * 
 * @author Jeanne Boyarsky
 * @version $Id: $
 */
public class CsrfFilter implements Filter {
    public static final String OWASP_CSRF_TOKEN_NAME = "OWASP_CSRFTOKEN";
    private FilterConfig filterConfig = null;

    @Override
    public void destroy() {
        filterConfig = null;
    }

    private String getJForumMethodName(HttpServletRequest req) throws IOException {
        String module = null;
        boolean multiPart = ServletFileUpload.isMultipartContent(new ServletRequestContext(req));
        /*
         * If a multipart request, we know that CSRF protection is needed (it is
         * a post/upload). Don't actually look up the module since that will
         * cause the input stream to get read and then be unavailable for the
         * real request.
         */
        if (multiPart) {
            module = "multipart request: " + req.getRequestURI();
        } else {
            RequestContext request = new WebRequestContext(req);
            module = request.getAction();
            if (module == null) {
                module = "unknown module for " + req.getRequestURI();
            }
        }
        
        return module;
    }
   
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException,
            ServletException {
        /** only work with HttpServletRequest objects **/
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            CsrfGuard csrfGuard = CsrfGuard.getInstance();
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpSession session = httpRequest.getSession(false);
            if (session == null) {
                // If there is no session, no harm can be done
                filterChain.doFilter(httpRequest, (HttpServletResponse) response);
                // added this because wasn't creating tokens on initial request
                session = httpRequest.getSession(true);
                csrfGuard.updateTokens(httpRequest);
                return;
            }
            csrfGuard.getLogger().log(String.format("CsrfGuard analyzing request %s", httpRequest.getRequestURI()));
            InterceptRedirectResponse httpResponse = new InterceptRedirectResponse((HttpServletResponse) response,
                    httpRequest, csrfGuard);
            // if(MultipartHttpServletRequest.isMultipartRequest(httpRequest)) {
            // httpRequest = new MultipartHttpServletRequest(httpRequest);
            // }
            /**
             * Custom code
             */
            String name = getJForumMethodName(httpRequest);
            CsrfHttpServletRequestWrapper csrfRequestWrapper = new CsrfHttpServletRequestWrapper(httpRequest, name);
            if (session.isNew() && csrfGuard.isUseNewTokenLandingPage()) {
                csrfGuard.writeLandingPage(httpRequest, httpResponse);
            } else if (csrfGuard.isValidRequest(csrfRequestWrapper, httpResponse)) {
                filterChain.doFilter(httpRequest, httpResponse);
            } else {
                /** invalid request - nothing to do - actions already executed **/
            }
            /** update tokens **/
            csrfGuard.updateTokens(httpRequest);
        } else {
            filterConfig.getServletContext().log(
                    String.format("[WARNING] CsrfGuard does not know how to work with requests of class %s ", request
                            .getClass().getName()));
            filterChain.doFilter(request, response);
        }
    }
    
    @Override
    public void init(@SuppressWarnings("hiding") FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
    }
}

