package net.thesphynx.espritmarket.Common.Service;

import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAll() {
        return userRepository.findAll();
    }

    public Optional<User> getById(Long id) {
        return userRepository.findById(id);
    }

    public User create(User user) {
        encodePasswordIfNeeded(user);
        return userRepository.save(user);
    }

    public User update(Long id, User user) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            user.setPassword(existing.getPassword());
        } else {
            encodePasswordIfNeeded(user);
        }
        user.setId(id);
        return userRepository.save(user);
    }

    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    private void encodePasswordIfNeeded(User user) {
        String password = user.getPassword();
        if (password != null && !password.isBlank() && !password.startsWith("$2")) {
            user.setPassword(passwordEncoder.encode(password));
        }
    }
}
