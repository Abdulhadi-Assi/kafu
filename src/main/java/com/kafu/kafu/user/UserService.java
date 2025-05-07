package com.kafu.kafu.user;

import com.kafu.kafu.address.AddressService;
import com.kafu.kafu.exception.ApplicationErrorEnum;
import com.kafu.kafu.exception.BusinessException;
import com.kafu.kafu.gov.Gov;
import com.kafu.kafu.gov.GovService;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final AddressService addressService;
    private final GovService govService;
    private final Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }


    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApplicationErrorEnum.USER_NOT_FOUND));
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public User create(UserDTO userDTO) {
        if (userDTO.getId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A new user cannot already have an ID");
        }
        if (userDTO.getKeycloakId() == null || userDTO.getKeycloakId().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Keycloak ID is required");
        }
        if (userRepository.existsByEmail(userDTO.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        if (userRepository.existsByKeycloakId(userDTO.getKeycloakId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Keycloak ID already exists");
        }
        User user = UserMapper.toEntity(userDTO);
        // Set gov if provided
        if (userDTO.getGovId() != null) {
            user.setGov(govService.findById(userDTO.getGovId()));
        }
        // Set address if provided
        if (userDTO.getAddressId() != null) {
            user.setAddress(addressService.findById(userDTO.getAddressId()));
        }

        user = userRepository.save(user);
        return user;
    }

    @Transactional
    public User update(Long id, UserDTO userDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApplicationErrorEnum.USER_NOT_FOUND));

        // Extract keycloak id from security context
        String tokenKeycloakId = getKeycloakIdFromSecurityContext();
        if (tokenKeycloakId == null || !tokenKeycloakId.equals(user.getKeycloakId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only update your own user");
        }

        if (userDTO.getEmail() != null &&
                !userDTO.getEmail().equals(user.getEmail()) && userRepository.existsByEmail(userDTO.getEmail())
        ) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        if (userDTO.getKeycloakId() != null &&
                !userDTO.getKeycloakId().equals(user.getKeycloakId())
        ) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Keycloak ID is wrong");
        }
        UserMapper.updateEntity(user, userDTO);

        // Update gov if provided
        if (userDTO.getGovId() != null) {
            user.setGov(govService.findById(userDTO.getGovId()));
        }
        // Update address if provided
        if (userDTO.getAddressId() != null) {
            user.setAddress(addressService.findById(userDTO.getAddressId()));
        }
        user = userRepository.save(user);
        return user;
    }

    @Transactional
    public void delete(Long id) {
        User user =
        userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApplicationErrorEnum.USER_NOT_FOUND));
        String keycloakId = user.getKeycloakId();

        try {
            var userResource = keycloak.realm(realm).users().get(keycloakId);
            if (userResource != null) {
                userResource.logout();
                userResource.remove();
            }
        } catch (Exception e) {
            throw new RuntimeException("User could not be deleted");
        }

        user.setDeleted(true);
        user.setEmail("deleted_" + user.getEmail());
        user.setKeycloakId("deleted_" + user.getKeycloakId());
        userRepository.save(user);
    }

    public String getKeycloakIdFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if ((authentication != null) && (authentication.getPrincipal() instanceof Jwt jwt)) {
            return jwt.getClaimAsString("sub");
        }
        return null;
    }
    public User findByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new BusinessException(ApplicationErrorEnum.USER_NOT_FOUND));
    }

    public User getCurrentUser()
    {
        String keycloakId = getKeycloakIdFromSecurityContext();
        if (keycloakId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authenticated user");
        }
        // Find user by keycloak id
        var user = findByKeycloakId(keycloakId);
        if (user == null) {
            throw new BusinessException(ApplicationErrorEnum.USER_NOT_FOUND);
        }
        return user;
    }

    public void associateUser(Long govId, Long userId) {
        Gov gov = govService.findById(govId);
        User user = findById(userId);
        user.setGov(gov);
        userRepository.save(user);
    }
}
