package net.thesphynx.espritmarket.Common.Repository;

import net.thesphynx.espritmarket.Common.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByEmail(String email);
	Optional<User> findByName(String name);

	List<User> findByEmailEndingWithIgnoreCase(String domain);
}
