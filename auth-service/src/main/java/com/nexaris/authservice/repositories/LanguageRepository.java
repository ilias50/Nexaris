package com.nexaris.authservice.repositories;

import com.nexaris.authservice.entities.Language;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LanguageRepository extends JpaRepository<Language, Integer> {
    Optional<Language> findByCode(String code);
}