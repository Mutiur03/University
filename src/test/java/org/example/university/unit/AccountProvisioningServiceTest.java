package org.example.university.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.example.university.model.Role;
import org.example.university.model.UserAccount;
import org.example.university.repository.UserAccountRepository;
import org.example.university.security.AccountProvisioningService;
import org.example.university.security.AccountProvisioningService.Credentials;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AccountProvisioningServiceTest {

    private final UserAccountRepository users = mock(UserAccountRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final AccountProvisioningService service = new AccountProvisioningService(users, encoder);

    @Test
    void createSavesHashedPasswordAndReturnsTemporaryOne() {
        when(users.findByUsername("a@x.com")).thenReturn(Optional.empty());

        Credentials credentials = service.create("a@x.com", Role.ROLE_STUDENT);

        ArgumentCaptor<UserAccount> saved = ArgumentCaptor.forClass(UserAccount.class);
        verify(users).save(saved.capture());
        assertEquals(12, credentials.temporaryPassword().length());
        assertNotEquals(credentials.temporaryPassword(), saved.getValue().getPassword());
        assertTrue(encoder.matches(credentials.temporaryPassword(), saved.getValue().getPassword()));
        assertEquals(Role.ROLE_STUDENT, saved.getValue().getRole());
    }

    @Test
    void createRejectsDuplicateEmail() {
        when(users.findByUsername("a@x.com")).thenReturn(Optional.of(new UserAccount()));

        assertThrows(DataIntegrityViolationException.class, () -> service.create("a@x.com", Role.ROLE_TEACHER));
        verify(users, never()).save(any());
    }

    @Test
    void renameChangesUsername() {
        UserAccount account = new UserAccount();
        account.setUsername("old@x.com");
        when(users.findByUsername("old@x.com")).thenReturn(Optional.of(account));
        when(users.findByUsername("new@x.com")).thenReturn(Optional.empty());

        service.rename("old@x.com", "new@x.com");

        assertEquals("new@x.com", account.getUsername());
        verify(users).save(account);
    }
}
