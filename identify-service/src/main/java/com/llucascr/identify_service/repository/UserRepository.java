package com.llucascr.identify_service.repository;

import com.llucascr.identify_service.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByCpf(String cpf);
    boolean existsByCpf(String cpf);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    @Query("""
            select u from User u
            where lower(u.fullName) like lower(:pattern) escape '\\'
               or lower(u.email) like lower(:pattern) escape '\\'
               or u.cpf = :query
            order by u.fullName, u.id
            """)
    List<User> search(@Param("query") String query, @Param("pattern") String pattern);
}
