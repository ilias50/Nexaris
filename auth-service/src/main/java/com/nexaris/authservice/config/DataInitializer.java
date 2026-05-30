package com.nexaris.authservice.config;

import com.nexaris.authservice.entities.Country;
import com.nexaris.authservice.entities.Language;
import com.nexaris.authservice.entities.Role;
import com.nexaris.authservice.entities.User;
import com.nexaris.authservice.repositories.CountryRepository;
import com.nexaris.authservice.repositories.LanguageRepository;
import com.nexaris.authservice.repositories.RoleRepository;
import com.nexaris.authservice.repositories.UserRepository;
import com.nexaris.authservice.entities.GlobalSetting;
import com.nexaris.authservice.repositories.GlobalSettingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository,
                                   RoleRepository roleRepository, // Ajoute ceci
                                   CountryRepository countryRepository,
                                   LanguageRepository languageRepository,
                                   GlobalSettingRepository settingsRepository,
                                   PasswordEncoder passwordEncoder,
                                   @Value("${APP_BOOTSTRAP_ADMIN_EMAIL:}") String bootstrapAdminEmail,
                                   @Value("${APP_BOOTSTRAP_ADMIN_PASSWORD:}") String bootstrapAdminPassword,
                                   @Value("${APP_BOOTSTRAP_ADMIN_FIRST_NAME:System}") String bootstrapAdminFirstName,
                                   @Value("${APP_BOOTSTRAP_ADMIN_LAST_NAME:Admin}") String bootstrapAdminLastName) {
        return args -> {
            // 1. Initialisation du réglage (comme avant)
            if (settingsRepository.findByName("registration_enabled").isEmpty()) {
                settingsRepository.save(new GlobalSetting(null, "registration_enabled", "true"));
            }

            // 2. Gestion des Rôles (On s'assure qu'ils existent en base)
            Role roleUser = roleRepository.findByName("ROLE_USER")
                    .orElseGet(() -> roleRepository.save(new Role(null, "ROLE_USER", null)));

            Role roleAdmin = roleRepository.findByName("ROLE_ADMIN")
                    .orElseGet(() -> roleRepository.save(new Role(null, "ROLE_ADMIN", null)));

            // 3. Pays et langue par défaut (nécessaires pour fk_country / fk_language non-null)
            Country defaultCountry = countryRepository.findByCode("BE")
                    .orElseGet(() -> countryRepository.save(new Country(null, "BE", "Belgique", "+32")));

            Language defaultLanguage = languageRepository.findByCode("FR")
                    .orElseGet(() -> languageRepository.save(new Language(null, "FR", "Français")));

            // 4. Création du premier admin (pilotée par variables d'environnement)
            if (bootstrapAdminEmail == null || bootstrapAdminEmail.isBlank()) {
                log.warn("APP_BOOTSTRAP_ADMIN_EMAIL absent: création automatique de l'admin ignorée.");
                return;
            }
            if (bootstrapAdminPassword == null || bootstrapAdminPassword.isBlank()) {
                log.warn("APP_BOOTSTRAP_ADMIN_PASSWORD absent: création automatique de l'admin ignorée.");
                return;
            }

            String adminEmail = bootstrapAdminEmail.trim().toLowerCase();
            if (userRepository.findByEmail(adminEmail).isEmpty()) {
                User admin = new User();
                admin.setFirstName(bootstrapAdminFirstName);
                admin.setLastName(bootstrapAdminLastName);
                admin.setEmail(adminEmail);
                admin.setPassword(passwordEncoder.encode(bootstrapAdminPassword));
                admin.setCountry(defaultCountry);
                admin.setLanguage(defaultLanguage);
                admin.setRoles(Set.of(roleUser, roleAdmin));

                userRepository.save(admin);
                log.info("Admin bootstrap créé avec les objets Rôles et localisation : {}", adminEmail);
            }
        };
    }
}