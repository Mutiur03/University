package org.example.university.controller;

import java.util.List;
import org.example.university.model.Admin;
import org.example.university.model.Role;
import org.example.university.repository.AdminRepository;
import org.example.university.security.AccountProvisioningService;
import org.example.university.security.AccountProvisioningService.Credentials;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
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

@RestController
@RequestMapping("/api/admins")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminRepository adminRepository;
    private final AccountProvisioningService accounts;

    public AdminController(AdminRepository adminRepository, AccountProvisioningService accounts) {
        this.adminRepository = adminRepository;
        this.accounts = accounts;
    }

    @GetMapping
    public List<Admin> findAll() {
        return adminRepository.findAll();
    }

    @GetMapping("/{id}")
    public Admin findById(@PathVariable Long id) {
        return adminRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public CreateAdminResponse create(@RequestBody Admin admin) {
        Credentials credentials = accounts.create(admin.getEmail(), Role.ROLE_ADMIN);
        try {
            Admin saved = adminRepository.save(admin);
            return new CreateAdminResponse(saved, credentials);
        } catch (RuntimeException exception) {
            accounts.delete(admin.getEmail());
            throw exception;
        }
    }

    @PutMapping("/{id}")
    @Transactional
    public Admin update(@PathVariable Long id, @RequestBody Admin admin) {
        Admin existing = adminRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        accounts.rename(existing.getEmail(), admin.getEmail());
        admin.setId(id);
        return adminRepository.save(admin);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable Long id) {
        Admin existing = adminRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        adminRepository.deleteById(id);
        accounts.delete(existing.getEmail());
    }

    public record CreateAdminResponse(Admin admin, Credentials credentials) {}
}
