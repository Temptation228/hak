package com.borisey.personal_finance.repo;

import com.borisey.personal_finance.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username) throws UsernameNotFoundException;
    boolean existsByUsername(String username);
}
