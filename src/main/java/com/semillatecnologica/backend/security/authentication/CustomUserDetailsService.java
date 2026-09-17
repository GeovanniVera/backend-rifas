package com.semillatecnologica.backend.security.authentication;

import com.semillatecnologica.backend.modules.auth.model.User;
import com.semillatecnologica.backend.modules.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de UserDetailsService que carga usuarios desde la base de datos.
 *
 * <p>Se utiliza para autenticar usuarios por credenciales y para
 * cargar el contexto de seguridad desde el JWT.</p>
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Carga un usuario por su email para autenticación por credenciales.
     *
     * @param email Email del usuario
     * @return UserDetails con credenciales y autoridades
     * @throws UsernameNotFoundException si el usuario no existe
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailWithRolesAndPermissions(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));

        return buildUserDetails(user);
    }

    /**
     * Carga un usuario por su ID para autenticación desde JWT.
     *
     * @param userId ID del usuario
     * @return UserDetails con credenciales y autoridades
     * @throws UsernameNotFoundException si el usuario no existe
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(String userId) {
        User user = userRepository.findByIdWithRolesAndPermissions(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + userId));

        if (user.getSuspended() != null && user.getSuspended()) {
            throw new com.semillatecnologica.backend.shared.exception.SuspendedAccountException(
                    "Cuenta suspendida");
        }

        return buildUserDetails(user);
    }

    /**
     * Construye UserDetails a partir de una entidad User.
     *
     * <p>Las autoridades se derivan de los permisos efectivos del usuario
     * (unión de permisos de todos sus roles).</p>
     *
     * @param user Entidad User con roles y permistros cargados
     * @return UserDetails para Spring Security
     */
    private UserDetails buildUserDetails(User user) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        user.getRoles().forEach(role -> {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
            role.getPermissions().forEach(permission ->
                authorities.add(new SimpleGrantedAuthority(permission.getName()))
            );
        });

        return new org.springframework.security.core.userdetails.User(
                user.getId(),
                user.getPassword(),
                user.getIsVerified(),
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                authorities
        );
    }
}
