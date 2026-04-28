package pt.luis.projectaboutanimals.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import pt.luis.projectaboutanimals.service.UserService;

@Component
public class AdminSeeder implements CommandLineRunner {

    private final UserService users;

    public AdminSeeder(UserService users) {
        this.users = users;
    }

    @Override
    public void run(String... args) {
        // Troca a password depois
        users.ensureAdminSeed("Admin", "admin@local.pt", "admin123");
    }
}
