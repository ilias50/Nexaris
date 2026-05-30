package com.nexaris.authservice.services;

import com.nexaris.authservice.config.JwtTokenProvider;
import com.nexaris.authservice.dtos.LoginRequest;
import com.nexaris.authservice.dtos.LoginResponse;
import com.nexaris.authservice.dtos.CountryOptionResponse;
import com.nexaris.authservice.dtos.UserRegistrationRequest;
import com.nexaris.authservice.dtos.UserResponse;
import com.nexaris.authservice.entities.*;
import com.nexaris.authservice.exceptions.InvalidCredentialsException;
import com.nexaris.authservice.exceptions.ResourceAlreadyExistsException;
import com.nexaris.authservice.exceptions.ResourceNotFoundException;
import com.nexaris.authservice.exceptions.UnauthorizedException;
import com.nexaris.authservice.repositories.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AuthService {
    private static final long MAX_PROFILE_IMAGE_SIZE = 2_000_000L;
    private final Path profileImageDirectory;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final PrivilegeRepository privilegeRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final CountryRepository countryRepository;
    private final LanguageRepository languageRepository;
    private final GlobalSettingRepository settingsRepository;
    private final NotificationClient notificationClient;
    private final OrgRightsClient orgRightsClient;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       RoleRepository roleRepository,
                       PrivilegeRepository privilegeRepository,
                       JwtTokenProvider jwtTokenProvider,
                       CountryRepository countryRepository,
                       LanguageRepository languageRepository,
                       GlobalSettingRepository settingsRepository,
                       NotificationClient notificationClient,
                       OrgRightsClient orgRightsClient,
                       @Value("${app.profile-images-dir:/tmp/nexaris/uploads/profile-images}") String profileImagesDir) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.privilegeRepository = privilegeRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.countryRepository = countryRepository;
        this.languageRepository = languageRepository;
        this.settingsRepository = settingsRepository;
        this.notificationClient = notificationClient;
        this.orgRightsClient = orgRightsClient;
        String safeProfileImagesDir =
            (profileImagesDir == null || profileImagesDir.isBlank())
                ? "/tmp/nexaris/uploads/profile-images"
                : profileImagesDir;
        this.profileImageDirectory = Paths.get(safeProfileImagesDir);
    }

    @Transactional
    public User register(UserRegistrationRequest req) {
        return createUser(req, true);
    }

    @Transactional
    public User registerByAdmin(UserRegistrationRequest req) {
        return createUser(req, false);
    }

    public boolean isRegistrationEnabled() {
        return settingsRepository.isRegistrationEnabled();
    }

    private User createUser(UserRegistrationRequest req, boolean enforceRegistrationOpen) {
        if (enforceRegistrationOpen && !settingsRepository.isRegistrationEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "L'inscription est fermée.");
        }

        if (userRepository.existsByEmail(req.getEmail())) {
            throw new ResourceAlreadyExistsException("L'email " + req.getEmail() + " est déjà utilisé");
        }

        User newUser = new User();
        newUser.setFirstName(req.getFirstName());
        newUser.setLastName(req.getLastName());
        newUser.setEmail(req.getEmail());
        newUser.setPassword(passwordEncoder.encode(req.getPassword()));
        newUser.setEnabled(true);
        newUser.setProfileImageUrl(req.getProfileImageUrl());

        Country country = countryRepository.findByCode("BE")
                .orElseThrow(() -> new ResourceNotFoundException("Erreur configuration : Pays 'BE' introuvable"));
        newUser.setCountry(country);

        Language language = languageRepository.findByCode("FR")
                .orElseThrow(() -> new ResourceNotFoundException("Erreur configuration : Langue 'FR' introuvable"));
        newUser.setLanguage(language);

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new ResourceNotFoundException("Erreur configuration : Rôle 'ROLE_USER' introuvable"));

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        newUser.setRoles(roles);

        User savedUser = userRepository.save(newUser);
        notificationClient.sendWelcomeEmail(savedUser.getEmail(), savedUser.getFirstName());
        return savedUser;
    }

    public LoginResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> {
                    log.warn("Tentative de connexion échouée : email {} non trouvé", req.getEmail());
                    return new InvalidCredentialsException("Identifiants incorrects");
                });

        if (!user.isEnabled()) {
            log.warn("Tentative de connexion sur compte désactivé : {}", req.getEmail());
            throw new UnauthorizedException("Compte désactivé. Veuillez contacter l'administration.");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            log.warn("Mot de passe incorrect pour l'utilisateur : {}", req.getEmail());
            throw new InvalidCredentialsException("Identifiants incorrects");
        }

        log.info("Connexion réussie pour l'utilisateur : {}", req.getEmail());

        java.util.List<String> roleNames = user.getRoles().stream()
                .map(r -> r.getName())
                .toList();

        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), roleNames);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        Long expiresIn = jwtTokenProvider.getExpirationTimeInSeconds();

        return new LoginResponse(token, refreshToken, expiresIn, convertUserToResponse(user));
    }

    public List<CountryOptionResponse> getCountries() {
    return countryRepository.findAll().stream()
        .filter(country -> country.getCode() != null && country.getName() != null)
        .map(country -> new CountryOptionResponse(
            country.getCode().trim().toUpperCase(Locale.ROOT),
            country.getName().trim()
        ))
        .sorted(Comparator.comparing(CountryOptionResponse::name, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(CountryOptionResponse::code))
        .toList();
    }

    @Transactional
    public User updateUser(Integer id, UserRegistrationRequest updates) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        if (updates.getFirstName() != null) user.setFirstName(updates.getFirstName());
        if (updates.getLastName() != null) user.setLastName(updates.getLastName());

        if (updates.getEmail() != null && !updates.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(updates.getEmail())) {
                throw new ResourceAlreadyExistsException("Le nouvel email est déjà utilisé");
            }
            user.setEmail(updates.getEmail());
        }

        if (updates.getPassword() != null && !updates.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(updates.getPassword()));
        }

        if (updates.getProfileImageUrl() != null) {
            user.setProfileImageUrl(updates.getProfileImageUrl());
        }

        if (updates.getCountryCode() != null && !updates.getCountryCode().isBlank()) {
            String countryCode = updates.getCountryCode().trim().toUpperCase(Locale.ROOT);
            user.setCountry(countryRepository.findByCode(countryCode)
                .orElseThrow(() -> new ResourceNotFoundException("Pays introuvable")));
        } else if (updates.getFkCountry() != null) {
            user.setCountry(countryRepository.findById(updates.getFkCountry())
                    .orElseThrow(() -> new ResourceNotFoundException("Pays introuvable")));
        }

        if (updates.getLanguageCode() != null && !updates.getLanguageCode().isBlank()) {
            String languageCode = updates.getLanguageCode().trim().toUpperCase(Locale.ROOT);
            user.setLanguage(languageRepository.findByCode(languageCode)
                .orElseThrow(() -> new ResourceNotFoundException("Langue introuvable")));
        } else if (updates.getFkLanguage() != null) {
            user.setLanguage(languageRepository.findById(updates.getFkLanguage())
                    .orElseThrow(() -> new ResourceNotFoundException("Langue introuvable")));
        }

        return user;
    }

    @Transactional
    public void changePassword(Integer userId, String currentPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nouveau mot de passe doit contenir au moins 8 caractères");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new InvalidCredentialsException("Le mot de passe actuel est incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
    }

    public void logout(String token) {
        jwtTokenProvider.revokeToken(token);
    }

    @Transactional
    public void deactivateUser(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'ID : " + id));
        user.setEnabled(false);
        // Pas besoin de .save() car @Transactional gère la synchronisation (Dirty Checking)
    }

    @Transactional
    public void anonymizeUser(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        user.setFirstName("Utilisateur_Supprimé");
        user.setLastName(id.toString());
        user.setEmail("deleted_" + id + "_" + UUID.randomUUID().toString().substring(0, 8) + "@nexaris.internal");
        user.setPassword("ANONYMIZED_" + UUID.randomUUID());
        deleteExistingProfileImageFile(id);
        user.setProfileImageUrl(null);
        user.setEnabled(false);
        if (user.getRoles() != null) {
            try {
                user.getRoles().clear();
            } catch (UnsupportedOperationException ex) {
                user.setRoles(new HashSet<>());
            }
        }

        orgRightsClient.purgeUserRights(id);
    }

    @Transactional
    public User addRoleToUser(Integer userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        String formattedRole = roleName.toUpperCase().startsWith("ROLE_") ? roleName.toUpperCase() : "ROLE_" + roleName.toUpperCase();

        Role role = roleRepository.findByName(formattedRole)
                .orElseThrow(() -> new ResourceNotFoundException("Rôle " + formattedRole + " n'existe pas"));

        user.getRoles().add(role);
        return user;
    }

    @Transactional
    public User removeRoleFromUser(Integer userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        String formattedRole = roleName == null
                ? ""
                : (roleName.toUpperCase().startsWith("ROLE_") ? roleName.toUpperCase() : "ROLE_" + roleName.toUpperCase());

        if ("ROLE_USER".equals(formattedRole)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le role ROLE_USER est obligatoire et ne peut pas etre supprime");
        }

        user.getRoles().removeIf(r -> r.getName().equalsIgnoreCase(formattedRole));
        return user;
    }

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    public List<UserResponse> findAllUserResponses() {
        return userRepository.findAll().stream()
                .map(this::convertUserToResponse)
                .toList();
    }

    public void ensureSelfOrAdmin(Authentication authentication, Integer targetUserId) {
        Integer currentUserId = extractCurrentUserId(authentication);
        if (currentUserId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Utilisateur non authentifie");
        }
        if (!isAdmin(authentication) && !currentUserId.equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces refuse");
        }
    }

    public void ensureAdmin(Authentication authentication) {
        if (!isAdmin(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces administrateur requis");
        }
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private Integer extractCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Integer id) {
            return id;
        }
        if (principal instanceof String principalAsString) {
            try {
                return Integer.parseInt(principalAsString);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    public User findById(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur avec l'ID " + id + " introuvable."));
    }

    public UserResponse getUserResponse(Integer id) {
        return convertUserToResponse(findById(id));
    }

    @Transactional
    public String uploadProfileImage(Integer userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le fichier image est obligatoire");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le fichier doit être une image");
        }

        if (file.getSize() > MAX_PROFILE_IMAGE_SIZE) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(413), "Image trop volumineuse (max 2 MB)");
        }

        User user = findById(userId);

        try {
            Files.createDirectories(profileImageDirectory);
            deleteExistingProfileImageFile(userId);

            String extension = resolveExtension(contentType);
            String fileName = "user-" + userId + extension;
            Path filePath = profileImageDirectory.resolve(fileName).normalize();
            file.transferTo(filePath);

            String publicUrl = "/api/v1/auth/user/" + userId + "/profile-image";
            user.setProfileImageUrl(publicUrl);
            return publicUrl;
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Impossible d'enregistrer l'image");
        }
    }

    public Resource getProfileImage(Integer userId) {
        findById(userId);
        Path imagePath = findProfileImagePath(userId);
        if (imagePath == null) {
            throw new ResourceNotFoundException("Photo de profil introuvable");
        }

        try {
            return new UrlResource(imagePath.toUri());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Impossible de lire l'image");
        }
    }

    public String getProfileImageContentType(Integer userId) {
        Path imagePath = findProfileImagePath(userId);
        if (imagePath == null) {
            return "application/octet-stream";
        }

        try {
            String detected = Files.probeContentType(imagePath);
            return detected != null ? detected : "application/octet-stream";
        } catch (IOException ex) {
            return "application/octet-stream";
        }
    }

    @Transactional
    public void deleteProfileImage(Integer userId) {
        User user = findById(userId);
        deleteExistingProfileImageFile(userId);
        user.setProfileImageUrl(null);
    }

    private void deleteExistingProfileImageFile(Integer userId) {
        Path existing = findProfileImagePath(userId);
        if (existing != null) {
            try {
                Files.deleteIfExists(existing);
            } catch (IOException ex) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Impossible de supprimer l'image existante");
            }
        }
    }

    private Path findProfileImagePath(Integer userId) {
        if (!Files.exists(profileImageDirectory)) {
            return null;
        }

        try (var stream = Files.list(profileImageDirectory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith("user-" + userId + "."))
                    .sorted(Comparator.reverseOrder())
                    .findFirst()
                    .orElse(null);
        } catch (IOException ex) {
            return null;
        }
    }

    private String resolveExtension(String contentType) {
        return switch (contentType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }

    private UserResponse convertUserToResponse(User user) {
        List<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
            user.getProfileImageUrl(),
                user.getCountry() != null ? user.getCountry().getCode() : null,
                user.getLanguage() != null ? user.getLanguage().getCode() : null,
                roleNames
        );
    }

    public boolean validateToken(String token) { return jwtTokenProvider.validateToken(token); }
    public Integer getUserIdFromToken(String token) { return jwtTokenProvider.getUserIdFromToken(token); }
    public java.util.List<String> getUserRolesFromToken(String token) { return jwtTokenProvider.getRolesFromToken(token); }

    public LoginResponse refreshAccessToken(Integer userId) {
        User user = findById(userId);
        java.util.List<String> roleNames = user.getRoles().stream()
                .map(r -> r.getName())
                .toList();

        String newAccessToken = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), roleNames);
        return new LoginResponse(newAccessToken, null, jwtTokenProvider.getExpirationTimeInSeconds(), convertUserToResponse(user));
    }

    @Transactional
    public void updateRegistrationStatus(boolean isEnabled) {
        GlobalSetting setting = settingsRepository.findByName("registration_enabled")
                .orElse(new GlobalSetting(null, "registration_enabled", "true"));

        setting.setValue(String.valueOf(isEnabled));
        settingsRepository.save(setting);
    }


}
