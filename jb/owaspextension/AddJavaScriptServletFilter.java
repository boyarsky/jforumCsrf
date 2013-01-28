package jb.owaspextension;

import java.io.*;
import java.util.regex.*;

import javax.servlet.*;
import javax.servlet.http.*;

import net.jforum.*;
import net.jforum.util.preferences.*;

import com.javaranch.jforum.url.*;

/**
 * Adds script tag to all pages: <script type="text/javascript" src=
 * "${contextPath}/JavaScriptServlet</script>" ></script>
 * 
 * @author Jeanne Boyarsky
 * @version $Id: $
 */
public class AddJavaScriptServletFilter implements Filter {
    public void init(FilterConfig arg0) {
        // intentionally left empty
    }

    public void destroy() {
        // intentionally left empty
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest servletRequest = (HttpServletRequest) request;
            MutableRedirectHttpResponse mResponse = new MutableRedirectHttpResponse(servletRequest.getContextPath(),
                    (HttpServletResponse) response);
            doChain(servletRequest, mResponse, response, chain);
        }
    }

    // based on UrlFilter implementation
    private void doChain(HttpServletRequest request, MutableRedirectHttpResponse mResponse, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        String encoding = SystemGlobals.getValue(ConfigKeys.ENCODING);
        OutputStream out = response.getOutputStream();
        // call JForum
        chain.doFilter(request, mResponse);
        String contentType = mResponse.getContentType();
        // only need to replace URLs on html pages
        // if not logged in, don't add javascript/csrf token
        if (contentType != null && contentType.startsWith("text/html") && !isDownload(request)
                && request.getSession() != null && "1".equals(request.getSession().getAttribute(ConfigKeys.LOGGED))) {
            String content = new String(mResponse.getContent(), encoding);
            byte[] result = content.getBytes(encoding);
            result = addScriptTag(request.getContextPath(), result, encoding);
            response.setContentLength(result.length);
            out.write(result);
        } else {
            out.write(mResponse.getContent());
        }
        out.flush();
        out.close();
    }

    //
    private byte[] addScriptTag(String contextPath, byte[] before, String encoding) throws UnsupportedEncodingException {
        String beforeString = new String(before, encoding);
        String afterString = addScriptTag(contextPath, beforeString);
        return afterString.getBytes(encoding);
    }

    public String addScriptTag(String contextPath, String before) {
        Pattern p = Pattern.compile("(.*)</head>", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(before);
        StringBuffer result = new StringBuffer();
        while (m.find()) {
            String beforeHead = m.group(1);
            String add = "<script type=\"text/javascript\" src=\"" + contextPath + "/JavaScriptServlet\"></script>";
            String replacement = Matcher.quoteReplacement(beforeHead + add + "</head>");
            m.appendReplacement(result, replacement);
        }
        m.appendTail(result);
        return result.toString();
    }

    /*
     * JForum is sending downloads as text/html. This is incorrect, but it was
     * easier to fix here so the filter doesn't turn it into a string and
     * corrupt the output.
     */
    private boolean isDownload(HttpServletRequest request) {
        return request.getRequestURI().matches(".*forums/posts/downloadAttach/\\d+");
    }
}

