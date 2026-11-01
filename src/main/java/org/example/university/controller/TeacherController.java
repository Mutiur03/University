package org.example.university.controller;

import java.util.List;
import org.example.university.model.Teacher;
import org.example.university.model.Role;
import org.example.university.repository.TeacherRepository;
import org.example.university.security.AccountProvisioningService;
import org.example.university.security.AccountProvisioningService.Credentials;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/teachers")
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
public class TeacherController {

    private final TeacherRepository teacherRepository;
    private final AccountProvisioningService accounts;

    public TeacherController(TeacherRepository teacherRepository, AccountProvisioningService accounts) {
        this.teacherRepository = teacherRepository;
        this.accounts = accounts;
    }

    @GetMapping
    public List<Teacher> findAll(Authentication authentication) {
        if (isTeacher(authentication)) {
            return teacherRepository.findByEmail(authentication.getName()).stream().toList();
        }
        return teacherRepository.findAll();
    }

    @GetMapping("/{id}")
    public Teacher findById(@PathVariable Long id, Authentication authentication) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (isTeacher(authentication) && !teacher.getEmail().equals(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return teacher;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public CreateTeacherResponse create(@RequestBody Teacher teacher) {
        Credentials credentials = accounts.create(teacher.getEmail(), Role.ROLE_TEACHER);
        try {
            Teacher saved = teacherRepository.save(teacher);
            return new CreateTeacherResponse(saved, credentials);
        } catch (RuntimeException exception) {
            accounts.delete(teacher.getEmail());
            throw exception;
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Teacher update(@PathVariable Long id, @RequestBody Teacher teacher) {
        Teacher existing = teacherRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        accounts.rename(existing.getEmail(), teacher.getEmail());
        teacher.setId(id);
        return teacherRepository.save(teacher);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable Long id) {
        Teacher existing = teacherRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        teacherRepository.deleteById(id);
        accounts.delete(existing.getEmail());
    }

    public record CreateTeacherResponse(Teacher teacher, Credentials credentials) {}

    private boolean isTeacher(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_TEACHER"));
    }
}
