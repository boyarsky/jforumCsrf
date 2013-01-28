package jb.owaspextension;

import javax.servlet.http.*;

import net.jforum.context.*;

public class AjaxHelper {
    /**
     * OWASP adds "OWASP CSRFGuard Project"
     * 
     * @param request
     * @return
     */
    public static boolean isAjax(HttpServletRequest request) {
        return isAjax(request.getHeader("X-Requested-With"));
    }
    
    public static boolean isAjax(RequestContext request) {
        return isAjax(request.getHeader("X-Requested-With"));
    }
    
    private static boolean isAjax(String header) {
        if (header == null) {
            return false;
        }
        return header.startsWith("XMLHttpRequest");
    }
   
}

