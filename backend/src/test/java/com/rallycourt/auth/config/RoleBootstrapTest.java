package com.rallycourt.auth.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rallycourt.auth.entity.Role;
import com.rallycourt.auth.repository.RoleRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoleBootstrapTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private RoleBootstrap roleBootstrap;

    @Test
    void roleBootstrapRunnerCreatesAllMissingRoles() throws Exception {
        when(roleRepository.findByCode(any())).thenReturn(Optional.empty());

        roleBootstrap.roleBootstrapRunner().run(null);

        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository, org.mockito.Mockito.times(3)).save(captor.capture());
        assertEquals("ADMIN", captor.getAllValues().get(0).getCode());
        assertEquals("PLAYER", captor.getAllValues().get(1).getCode());
        assertEquals("COURT_OWNER", captor.getAllValues().get(2).getCode());
    }

    @Test
    void roleBootstrapRunnerSkipsExistingRoles() throws Exception {
        Role existingRole = new Role();
        existingRole.setCode("ADMIN");
        existingRole.setName("Administrator");

        when(roleRepository.findByCode("ADMIN")).thenReturn(Optional.of(existingRole));
        when(roleRepository.findByCode("PLAYER")).thenReturn(Optional.of(existingRole));
        when(roleRepository.findByCode("COURT_OWNER")).thenReturn(Optional.of(existingRole));

        roleBootstrap.roleBootstrapRunner().run(null);

        verify(roleRepository, never()).save(any(Role.class));
        assertSame(existingRole, roleRepository.findByCode("ADMIN").orElseThrow());
    }
}
