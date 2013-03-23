package jb.csrf;

import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import net.jforum.*;
import net.jforum.context.*;
import net.jforum.context.web.*;
import net.jforum.dao.jdbc.*;
import net.jforum.entities.*;
import net.jforum.util.legacy.commons.fileupload.servlet.*;
import net.jforum.util.preferences.*;

import org.apache.log4j.*;
import org.owasp.csrfguard.*;

/**
 * Didn't use OWASP filter because couldn't map jforum actions to urls
 * consistently. Copied from OWASP and added getJForumMethodName() and changed
 * logic near isValidRequest line. Then changed it a lot of add database support
 * as described in JdbcCsrfDao
 * 
 * @author Jeanne Boyarsky
 * @version $Id: $
 */
public class CsrfFilter implements Filter {
    private static final Logger logger = Logger.getLogger(CsrfFilter.class);
    public static final String OWASP_CSRF_TOKEN_NAME = "OWASP_CSRFTOKEN";
    private FilterConfig filterConfig = null;
    private JdbcCsrfDao dao = new JdbcCsrfDao();

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
                int userId = CsrfHelper.getJforumUserId(httpRequest);
                updateTokenInDatabase(userId, httpRequest);
                return;
            }
            // if not logged in, don't need csrf check
            if (!CsrfHelper.isLoggedInUser(httpRequest)) {
                filterChain.doFilter(httpRequest, response);
                return;
            }
            logger.debug("CsrfGuard analyzing request " + httpRequest.getRequestURI());
            // if(MultipartHttpServletRequest.isMultipartRequest(httpRequest)) {
            // httpRequest = new MultipartHttpServletRequest(httpRequest);
            // }
            /**
             * Custom code
             */
            final CsrfHttpServletResponse httpResponse = new CsrfHttpServletResponse((HttpServletResponse) response,
                    httpRequest, csrfGuard);
            final String name = getJForumMethodName(httpRequest);
            final CsrfHttpServletRequestWrapper csrfRequestWrapper = new CsrfHttpServletRequestWrapper(httpRequest,
                    name);
            if (session.isNew() && csrfGuard.isUseNewTokenLandingPage()) {
                csrfGuard.writeLandingPage(httpRequest, httpResponse);
            } else if (csrfGuard.isValidRequest(csrfRequestWrapper, httpResponse)) {
                filterChain.doFilter(httpRequest, httpResponse);
            } else {
                /** invalid request - nothing to do - actions already executed **/
            }
            checkIfNeedToUpdateTokenInSessionAndDatabase(csrfGuard, httpRequest);
        } else {
            logger.warn("CsrfGuard does not know how to work with requests of class " + request.getClass().getName());
            filterChain.doFilter(request, response);
        }
    }

    private void checkIfNeedToUpdateTokenInSessionAndDatabase(CsrfGuard csrfGuard, HttpServletRequest httpRequest) {
        String originalToken = (String) httpRequest.getSession().getAttribute(CsrfFilter.OWASP_CSRF_TOKEN_NAME);
        csrfGuard.updateTokens(httpRequest);
        int userId = CsrfHelper.getJforumUserId(httpRequest);
        String newToken = (String) httpRequest.getSession().getAttribute(CsrfFilter.OWASP_CSRF_TOKEN_NAME);
        if (originalToken == null || !originalToken.equals(newToken)) {
            updateTokenInDatabase(userId, httpRequest);
        }
    }

    private void updateTokenInDatabase(int userId, HttpServletRequest httpRequest) {
        String newToken = (String) httpRequest.getSession().getAttribute(CsrfFilter.OWASP_CSRF_TOKEN_NAME);
        // new token should never be null, but you never know
        if (newToken != null) {
            dao.insert(userId, newToken);
        }
    }

    @Override
    public void init(@SuppressWarnings("hiding") FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
    }
}
