package ch.hsr.servicestoolkit.editor.web.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Fallback filter for Single Page App routing.
 * Forwards non-API, non-static requests to /index.html.
 */
public class SinglePageAppFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(SinglePageAppFilter.class);

    @Override
    public void init(FilterConfig config) throws ServletException {
        log.debug("Initializing SinglePageAppFilter");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();

        // Let static files, API endpoints, and other system paths through
        if (isStaticResource(path) || isApiRequest(path) || isSystemPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        // For unmatched paths (SPA routes), forward to /index.html
        if (!path.equals("/") && !path.isEmpty()) {
            log.debug("Forwarding SPA route {} to /index.html", path);
            RequestDispatcher dispatcher = request.getServletContext().getRequestDispatcher("/index.html");
            dispatcher.forward(request, response);
            return;
        }

        // Default: continue filter chain
        chain.doFilter(request, response);
    }

    private boolean isStaticResource(String path) {
        return path.matches("^/(assets|scripts|bower_components|swagger-ui)/.*") ||
               path.matches(".*\\.(js|css|png|jpg|gif|svg|ttf|woff|woff2|eot)$");
    }

    private boolean isApiRequest(String path) {
        return path.startsWith("/api/") || path.startsWith("/engine/");
    }

    private boolean isSystemPath(String path) {
        return path.matches("^/(h2-console|metrics|health|actuator|admin)/.*") ||
               path.equals("/") ||
               path.equals("/index.html");
    }

    @Override
    public void destroy() {
        log.debug("Destroying SinglePageAppFilter");
    }
}
