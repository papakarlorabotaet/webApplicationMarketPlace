package ru.urfu.service;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import ru.urfu.entity.Role;
import ru.urfu.entity.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.urfu.dto.UserDto;
import ru.urfu.repository.RoleRepository;
import ru.urfu.repository.UserRepository;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }


    @Override
    public void saveUser(UserDto userDto) {
        User user = new User();
        user.setName(userDto.getName());
        user.setSurname(userDto.getSurname());
        user.setEmail(userDto.getEmail());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));// Обязательно шифруем пароль!
        // Начальные значения
        user.setAccountBalance(BigDecimal.valueOf(0));
        user.setAccountAmount(0);

        user.setCity(userDto.getCity());
        user.setRegistrationDate(LocalDateTime.now());

        // Логика назначения ролей

        Role role = roleRepository.findByName(userDto.getRole());
        if (role == null) {
            role = new Role();
            role.setName(userDto.getRole());
            roleRepository.save(role);
        }

        user.setRoles(Collections.singleton(role));
        userRepository.save(user);
    }

    @Override
    public void deleteUser(Long userId) {

    }

    @Override
    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public void deleteUserById(Long id) {

    }

    private Role checkRoleExist(String roleName) {
        Role role = new Role();
        role.setName(roleName);
        return roleRepository.save(role);
    }


    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Ищем нашего общего User
        User user = userRepository.findByEmail(email);

        if (user != null) {
            // Превращаем Set<Role> в список SimpleGrantedAuthority
            return new org.springframework.security.core.userdetails.User(
                    user.getEmail(),
                    user.getPassword(),
                    user.getRoles().stream()
                            .map(role -> new SimpleGrantedAuthority(role.getName()))
                            .collect(Collectors.toList())
            );
        } else {
            throw new UsernameNotFoundException("Invalid username or password.");
        }
    }
}