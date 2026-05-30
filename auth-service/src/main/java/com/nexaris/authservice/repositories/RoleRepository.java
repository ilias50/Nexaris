package com.nexaris.authservice.repositories;

import com.nexaris.authservice.entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    // Cette méthode est cruciale pour chercher un rôle par son nom (ex: "ADMIN")
    Optional<Role> findByName(String name);
}