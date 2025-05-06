package com.borisey.personal_finance.repo;

import com.borisey.personal_finance.models.PersonType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PersonTypeRepository extends JpaRepository<PersonType, Long> {
    Optional<PersonType> findByCode(String code);
}
