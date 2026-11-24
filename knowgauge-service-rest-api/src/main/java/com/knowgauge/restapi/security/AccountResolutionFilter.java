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

import com.knowgauge.core.model.User;
import com.knowgauge.core.model.enums.Role;

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

	private final AccountResolver accountResolver;

	public AccountResolutionFilter(AccountResolver accountResolver) {
		this.accountResolver = accountResolver;
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
			SecurityContextHolder.getContext()
					.setAuthentication(new JwtAuthenticationToken(token.getToken(), authorities, token.getName()));
			request.setAttribute(SignedInAccount.REQUEST_ATTRIBUTE, new SignedInAccount(user, applicationAdmin));
		}
		chain.doFilter(request, response);
	}
}
