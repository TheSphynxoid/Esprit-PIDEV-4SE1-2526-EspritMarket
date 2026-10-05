package net.thesphynx.espritmarket.Common.Service;

import net.thesphynx.espritmarket.Common.Entity.Role;
import net.thesphynx.espritmarket.Common.Entity.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class CourierProfileWriter {

    private final JdbcTemplate jdbcTemplate;

    public CourierProfileWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void createCourierProfileIfNeeded(User user) {
        if (user == null || user.getRole() != Role.COURIER) {
            return;
        }

        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM courier WHERE user_id = ?", Integer.class, user.getId());
        if (existing != null && existing > 0) {
            return;
        }

        jdbcTemplate.update(
                "INSERT INTO courier (user_id, status, profile_status, created_at) "
                        + "VALUES (?, 'PENDING', 'INCOMPLETE', LOCALTIMESTAMP)",
                user.getId());
    }
}
