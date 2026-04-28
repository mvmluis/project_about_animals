package pt.luis.projectaboutanimals.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import pt.luis.projectaboutanimals.dao.UserRepository;
import pt.luis.projectaboutanimals.model.User;

@Service
public class CurrentUserService {

    private final UserRepository users;

    public CurrentUserService(UserRepository users) {
        this.users = users;
    }

    public User requireUser() {
        String email = requireEmail();
        return users.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Utilizador autenticado não encontrado na BD: " + email));
    }

    public Long requireUserId() {
        return requireUser().getId();
    }

    public String requireEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("Sem autenticação no contexto.");
        }

        // OAuth2 (Google)
        if (auth instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oat = (OAuth2AuthenticationToken) auth;
            OAuth2User p = oat.getPrincipal();

            String email = p.getAttribute("email");
            if (email == null || email.isBlank()) {
                throw new IllegalStateException("OAuth2 sem atributo email (scope email?).");
            }
            return email.trim();
        }

        // Form login / outros
        String name = auth.getName();
        if (name == null || name.isBlank()) {
            throw new IllegalStateException("Email do utilizador autenticado não disponível.");
        }
        return name.trim();
    }
}
