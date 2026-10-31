package org.example.university.controller;

import java.util.List;
import org.example.university.model.Student;
import org.example.university.model.Role;
import org.example.university.repository.StudentRepository;
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
@RequestMapping("/api/students")
public class StudentController {

    private final StudentRepository studentRepository;
    private final AccountProvisioningService accounts;

    public StudentController(StudentRepository studentRepository, AccountProvisioningService accounts) {
        this.studentRepository = studentRepository;
        this.accounts = accounts;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public List<Student> findAll(Authentication authentication) {
        if (isStudent(authentication)) {
            return studentRepository.findByEmail(authentication.getName()).stream().toList();
        }
        return studentRepository.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public Student findById(@PathVariable Long id, Authentication authentication) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (isStudent(authentication) && !student.getEmail().equals(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return student;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public CreateStudentResponse create(@RequestBody Student student) {
        Credentials credentials = accounts.create(student.getEmail(), Role.ROLE_STUDENT);
        try {
            Student saved = studentRepository.save(student);
            return new CreateStudentResponse(saved, credentials);
        } catch (RuntimeException exception) {
            accounts.delete(student.getEmail());
            throw exception;
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Student update(@PathVariable Long id, @RequestBody Student student) {
        Student existing = studentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        accounts.rename(existing.getEmail(), student.getEmail());
        student.setId(id);
        return studentRepository.save(student);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable Long id) {
        Student existing = studentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        studentRepository.deleteById(id);
        accounts.delete(existing.getEmail());
    }

    public record CreateStudentResponse(Student student, Credentials credentials) {}

    private boolean isStudent(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_STUDENT"));
    }
}
