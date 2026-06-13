package com.mse.edu.forum.security;

import com.mse.edu.forum.service.MaintenanceModeState;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class MaintenanceModeFilter extends OncePerRequestFilter {

	private final MaintenanceModeState maintenanceModeState;

	public MaintenanceModeFilter(MaintenanceModeState maintenanceModeState) {
		this.maintenanceModeState = maintenanceModeState;
	}

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain)
			throws ServletException, IOException {
		var status = maintenanceModeState.snapshot();
		if (!status.restoreInProgress() || isAllowedRequest(request)) {
			filterChain.doFilter(request, response);
			return;
		}

		response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
		response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(status.retryAfterSeconds()));
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter()
				.write("""
						{"error":"service_unavailable","message":"Restore in progress","retryAfterSeconds":%d}
						""".formatted(status.retryAfterSeconds()));
	}

	private boolean isAllowedRequest(HttpServletRequest request) {
		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			return true;
		}

		String path = request.getRequestURI();
		return path.equals("/auth/login")
				|| path.startsWith("/actuator/health")
				|| path.equals("/actuator/info")
				|| path.equals("/livez")
				|| path.equals("/readyz")
				|| path.startsWith("/admin/maintenance")
				|| path.equals("/error");
	}
}
