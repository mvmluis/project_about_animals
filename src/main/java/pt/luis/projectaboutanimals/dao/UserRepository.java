package pt.luis.projectaboutanimals.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.luis.projectaboutanimals.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
