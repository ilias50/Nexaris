package com.nexaris.authservice.repositories;

import com.nexaris.authservice.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    // Pour la connexion (Login)
    Optional<User> findByEmail(String email);

    // Bonus : Vérifier si l'email existe déjà (renvoie un boolean, très rapide)
    boolean existsByEmail(String email);

}