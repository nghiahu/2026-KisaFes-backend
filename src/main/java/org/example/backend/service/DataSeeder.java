package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.entity.Category;
import org.example.backend.entity.Role;
import org.example.backend.repository.ICategoryRepository;
import org.example.backend.repository.IRoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final IRoleRepository roleRepository;
    private final ICategoryRepository categoryRepository;

    @Override
    public void run(String... args) {
        seedRoles();
        seedCategories();
    }

    private void seedRoles() {
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

    private void seedCategories() {
        if (categoryRepository.count() == 0) {
            // Software Category
            Category software = new Category();
            software.setName("Software");
            software.setDescription("Agile software development workflow");
            software.setDefaultStatuses(List.of(
                    createStatus("To Do", org.example.backend.entity.StatusCategory.TO_DO, "bg-slate-500"),
                    createStatus("In Progress", org.example.backend.entity.StatusCategory.IN_PROGRESS, "bg-blue-500"),
                    createStatus("In Review", org.example.backend.entity.StatusCategory.IN_PROGRESS, "bg-amber-500"),
                    createStatus("Done", org.example.backend.entity.StatusCategory.DONE, "bg-emerald-500")
            ));
            software.setDefaultRoles(List.of(
                    createRole("Scrum Master", Set.of("TASK_CREATE", "TASK_UPDATE", "TASK_DELETE", "BOARD_UPDATE", "MEMBER_INVITE", "ROLE_MANAGE")),
                    createRole("Developer", Set.of("TASK_CREATE", "TASK_UPDATE", "TASK_VIEW")),
                    createRole("Tester", Set.of("TASK_UPDATE", "TASK_VIEW"))
            ));
            categoryRepository.save(software);

            // Design Category
            Category design = new Category();
            design.setName("Design");
            design.setDescription("Creative design process workflow");
            design.setDefaultStatuses(List.of(
                    createStatus("Backlog", org.example.backend.entity.StatusCategory.TO_DO, "bg-slate-500"),
                    createStatus("Drafting", org.example.backend.entity.StatusCategory.IN_PROGRESS, "bg-purple-500"),
                    createStatus("Feedback", org.example.backend.entity.StatusCategory.IN_PROGRESS, "bg-rose-500"),
                    createStatus("Approved", org.example.backend.entity.StatusCategory.DONE, "bg-emerald-500")
            ));
            design.setDefaultRoles(List.of(
                    createRole("Art Director", Set.of("TASK_CREATE", "TASK_UPDATE", "TASK_DELETE", "BOARD_UPDATE", "MEMBER_INVITE")),
                    createRole("Designer", Set.of("TASK_CREATE", "TASK_UPDATE", "TASK_VIEW"))
            ));
            categoryRepository.save(design);

            // Business Category
            Category business = new Category();
            business.setName("Business");
            business.setDescription("General business project management");
            business.setDefaultStatuses(List.of(
                    createStatus("Planning", org.example.backend.entity.StatusCategory.TO_DO, "bg-blue-500"),
                    createStatus("Executing", org.example.backend.entity.StatusCategory.IN_PROGRESS, "bg-amber-500"),
                    createStatus("Completed", org.example.backend.entity.StatusCategory.DONE, "bg-emerald-500")
            ));
            business.setDefaultRoles(List.of(
                    createRole("Manager", Set.of("TASK_CREATE", "TASK_UPDATE", "TASK_DELETE", "MEMBER_INVITE")),
                    createRole("Analyst", Set.of("TASK_VIEW", "TASK_UPDATE"))
            ));
            categoryRepository.save(business);
        }
    }

    private Category.CategoryStatus createStatus(String label, org.example.backend.entity.StatusCategory category, String color) {
        Category.CategoryStatus status = new Category.CategoryStatus();
        status.setLabel(label);
        status.setCategory(category);
        status.setColor(color);
        return status;
    }

    private Category.CategoryRole createRole(String name, Set<String> permissions) {
        Category.CategoryRole role = new Category.CategoryRole();
        role.setName(name);
        role.setPermissions(permissions);
        return role;
    }
}