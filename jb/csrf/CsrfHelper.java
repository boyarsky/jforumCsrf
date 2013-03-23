package jb.csrf;

import javax.servlet.http.*;

import net.jforum.*;
import net.jforum.entities.*;
import net.jforum.util.preferences.*;

public class CsrfHelper {
    public static boolean isLoggedInUser(HttpServletRequest request) {
        return request.getSession() != null && "1".equals(request.getSession().getAttribute(ConfigKeys.LOGGED));
    }

    public static int getJforumUserId(HttpServletRequest request) {
        if (!isLoggedInUser(request)) {
            return -1;
        }
        String id = request.getSession().getId();
        if (id == null) {
            return -1;
        }
        UserSession userSession = SessionFacade.getUserSession(id);
        if (userSession == null) {
            return -1;
        }
        return userSession.getUserId();
    }
}
