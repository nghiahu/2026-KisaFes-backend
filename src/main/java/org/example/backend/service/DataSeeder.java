package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.entity.Role;
import org.example.backend.repository.IRoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final IRoleRepository roleRepository;

    @Override
    public void run(String... args) {

        if (!roleRepository.existsByName("USER")) {
            roleRepository.save(
                    Role.builder()
                            .name("USER")
                            .permissions(Set.of())
                            .isSystemRole(true)
                            .build()
            );
        }
    }
}