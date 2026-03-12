package pt.luis.projectaboutanimals.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            OAuth2LoginSuccessHandler oauth2SuccessHandler
    ) throws Exception {

        http
                .authorizeHttpRequests(auth -> auth

                        // =========================
                        // PÚBLICOS (static + auth)
                        // =========================
                        .requestMatchers(
                                "/login",
                                "/register",
                                "/error",

                                // favicon / icons
                                "/favicon.ico",
                                "/*.ico",

                                // static resources (Spring Boot: /static, /public, /resources, /META-INF/resources)
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/uploads/**",

                                // se usares webjars
                                "/webjars/**"
                        ).permitAll()

                        // OAuth2 endpoints
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()

                        // =========================
                        // API (CLIENT + ADMIN)
                        // =========================
                        .requestMatchers("/api/chat/**").hasAnyRole("CLIENT", "ADMIN")

                        // =========================
                        // UI ADMIN
                        // =========================
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // =========================
                        // ÁREAS AUTENTICADAS
                        // =========================
                        .requestMatchers("/reports", "/reports/**").hasAnyRole("CLIENT", "ADMIN")
                        .requestMatchers("/my-reports", "/my-reports/**").hasAnyRole("CLIENT", "ADMIN")
                        .requestMatchers("/my-adoptions", "/my-adoptions/**").hasAnyRole("CLIENT", "ADMIN")
                        .requestMatchers("/donations", "/donations/**").hasAnyRole("CLIENT", "ADMIN")

                        // fallback
                        .anyRequest().authenticated()
                )

                // =========================
                // FORM LOGIN
                // =========================
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/reports", true)
                        .permitAll()
                )

                // =========================
                // OAUTH2 LOGIN (Google)
                // =========================
                .oauth2Login(oauth -> oauth
                        .loginPage("/login")
                        .successHandler(oauth2SuccessHandler)
                )

                // =========================
                // LOGOUT
                // =========================
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )

                // opcional (se não usas basic, podes remover)
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}