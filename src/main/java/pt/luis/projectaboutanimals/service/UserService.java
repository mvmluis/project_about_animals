package pt.luis.projectaboutanimals.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pt.luis.projectaboutanimals.model.Role;
import pt.luis.projectaboutanimals.model.User;
import pt.luis.projectaboutanimals.dao.UserRepository;

@Service
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public UserService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    public User registerClient(String name, String email, String rawPassword) {
        if (users.existsByEmail(email)) {
            throw new IllegalArgumentException("Email já existe.");
        }
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(rawPassword));
        u.setRole(Role.CLIENT);
        return users.save(u);
    }

    public User ensureAdminSeed(String name, String email, String rawPassword) {
        return users.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setName(name);
            u.setEmail(email);
            u.setPasswordHash(encoder.encode(rawPassword));
            u.setRole(Role.ADMIN);
            return users.save(u);
        });
    }
}
