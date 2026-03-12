package pt.luis.projectaboutanimals.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pt.luis.projectaboutanimals.dao.UserRepository;
import pt.luis.projectaboutanimals.model.Role;
import pt.luis.projectaboutanimals.model.User;

import java.io.IOException;
import java.util.Set;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public OAuth2LoginSuccessHandler(UserRepository users, @Lazy PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        if (!(authentication instanceof OAuth2AuthenticationToken token)) {
            response.sendRedirect("/reports");
            return;
        }

        OAuth2User oauthUser = token.getPrincipal();

        // Google devolve tipicamente "email" e "name"
        String email = oauthUser.getAttribute("email");
        if (email == null || email.isBlank()) {
            response.sendRedirect("/login?error");
            return;
        }

        String name = oauthUser.getAttribute("name");
        if (name == null || name.isBlank()) name = "Utilizador Google";

        // ✅ para evitar "variable used in lambda should be final"
        final String emailFinal = email.trim();
        final String nameFinal  = name.trim();

        User u = users.findByEmail(emailFinal).orElseGet(() -> {
            User nu = new User();
            nu.setEmail(emailFinal);

            // constraints do teu model
            nu.setName(nameFinal);

            // ✅ obrigatório (nullable=false) e agora COM HASH
            // Não vai ser usado no login por password, mas fica consistente e seguro.
            nu.setPasswordHash(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));

            // role default
            nu.setRole(Role.CLIENT);

            return nu;
        });

        // Se já existe: garante que nome/role não ficam inválidos
        if (u.getName() == null || u.getName().isBlank()) u.setName(nameFinal);
        if (u.getRole() == null) u.setRole(Role.CLIENT);

        users.save(u);

        // ✅ Inject ROLE_CLIENT / ROLE_ADMIN na auth para passar nas tuas regras hasRole(...)
        var mappedAuthorities = Set.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name()));

        OAuth2AuthenticationToken newAuth =
                new OAuth2AuthenticationToken(
                        token.getPrincipal(),
                        mappedAuthorities,
                        token.getAuthorizedClientRegistrationId()
                );

        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(newAuth);

        response.sendRedirect("/reports");
    }
}
