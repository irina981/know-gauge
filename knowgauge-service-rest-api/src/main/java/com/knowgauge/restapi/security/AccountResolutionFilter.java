package com.knowgauge.restapi.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import com.knowgauge.core.model.Tenant;
import com.knowgauge.core.model.User;
import com.knowgauge.core.model.enums.Role;
import com.knowgauge.core.model.enums.TenantType;
import com.knowgauge.core.service.account.AccountService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Runs after the bearer token is validated: resolves the local user (creating it and its tenant on the first
 * sign-in), stores it for {@link SignedInAccount#current()}, and sets the user's roles from {@code user_roles} as
 * the authorities ({@code ROLE_ADMIN}, {@code ROLE_TENANT_ADMIN}); the token's own claims grant nothing (ADR012).
 * Anonymous requests pass through unchanged. Not a bean, so the servlet container doesn't register it a second time
 * outside the security chain.
 */
@Slf4j
public class AccountResolutionFilter extends OncePerRequestFilter {

	/** Sent by an {@code ADMIN} to read another user's workspace (their tenant): the tenant id (ADR013). */
	public static final String TENANT_HEADER = "X-KG-Tenant";

	private final AccountResolver accountResolver;
	private final AccountService accountService;

	public AccountResolutionFilter(AccountResolver accountResolver, AccountService accountService) {
		this.accountResolver = accountResolver;
		this.accountService = accountService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication instanceof JwtAuthenticationToken token) {
			User user;
			try {
				user = accountResolver.resolve(token.getToken());
			} catch (IllegalArgumentException e) {
				log.warn("Rejected token of subject {}: {}", token.getToken().getSubject(), e.getMessage());
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
				return;
			}
			// Authorization is KnowGauge's (ADR012): the authorities are the user's roles from user_roles, never claims
			// of the identity provider.
			List<GrantedAuthority> authorities = new ArrayList<>();
			for (Role role : user.getRoles()) {
				authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
			}
			boolean applicationAdmin = user.hasRole(Role.ADMIN);
			Long viewedTenantId;
			try {
				viewedTenantId = viewedTenant(request, user, applicationAdmin);
			} catch (TenantViewRefused e) {
				log.warn("User {} refused tenant view {}: {}", user.getId(), request.getHeader(TENANT_HEADER),
						e.getMessage());
				response.sendError(e.status, e.getMessage());
				return;
			}
			SecurityContextHolder.getContext()
					.setAuthentication(new JwtAuthenticationToken(token.getToken(), authorities, token.getName()));
			request.setAttribute(SignedInAccount.REQUEST_ATTRIBUTE,
					new SignedInAccount(user, applicationAdmin, viewedTenantId));
		}
		chain.doFilter(request, response);
	}

	/**
	 * The tenant an {@code ADMIN} asked to read with {@link #TENANT_HEADER} (ADR013), or {@code null} for the
	 * user's own. Reads of the workspace APIs (content, tests, attempts, invitations) honour it; the view never changes
	 * anything, so any other method with the header is refused.
	 */
	private Long viewedTenant(HttpServletRequest request, User user, boolean admin) {
		String header = request.getHeader(TENANT_HEADER);
		if (header == null || header.isBlank() || !isWorkspaceApi(request.getRequestURI())) {
			return null;
		}
		long tenantId;
		try {
			tenantId = Long.parseLong(header.trim());
		} catch (NumberFormatException e) {
			throw new TenantViewRefused(HttpServletResponse.SC_BAD_REQUEST, "Invalid library id.");
		}
		if (!admin) {
			throw new TenantViewRefused(HttpServletResponse.SC_FORBIDDEN, "Only an admin can view other libraries.");
		}
		if (user.getTenantId() != null && tenantId == user.getTenantId()) {
			return null;
		}
		Tenant tenant = accountService.findTenant(tenantId)
				.orElseThrow(() -> new TenantViewRefused(HttpServletResponse.SC_NOT_FOUND, "No such library."));
		if (tenant.getType() == TenantType.LIBRARY) {
			// Everyone reads the library anyway, and an admin may change it: no special view needed.
			return null;
		}
		if (!"GET".equals(request.getMethod()) && !"HEAD".equals(request.getMethod())) {
			throw new TenantViewRefused(HttpServletResponse.SC_FORBIDDEN,
					"Another user's library is read-only.");
		}
		return tenant.getId();
	}

	/**
	 * The APIs that work in the current tenant: everything under {@code /api/} except the caller's own account
	 * ({@code /api/me}), administration and the participants' public API.
	 */
	static boolean isWorkspaceApi(String uri) {
		return uri.startsWith("/api/") && !uri.equals("/api/me") && !uri.startsWith("/api/me/")
				&& !uri.startsWith("/api/admin/") && !uri.startsWith("/api/public/");
	}

	private static final class TenantViewRefused extends RuntimeException {
		private static final long serialVersionUID = 1L;
		private final int status;

		TenantViewRefused(int status, String message) {
			super(message);
			this.status = status;
		}
	}
}
