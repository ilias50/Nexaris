package com.nexaris.authservice.repositories;

import com.nexaris.authservice.entities.Privilege;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrivilegeRepository extends JpaRepository<Privilege, Integer> {

    // Pour vérifier si un droit existe déjà ou pour le récupérer par son libellé
    Optional<Privilege> findByName(String name);
}