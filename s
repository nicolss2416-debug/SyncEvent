[1mdiff --git a/backend/src/main/java/com/syncevent/audit/api/AuditRequestFilter.java b/backend/src/main/java/com/syncevent/audit/api/AuditRequestFilter.java[m
[1mindex 3ce6f32..3f88e51 100644[m
[1m--- a/backend/src/main/java/com/syncevent/audit/api/AuditRequestFilter.java[m
[1m+++ b/backend/src/main/java/com/syncevent/audit/api/AuditRequestFilter.java[m
[36m@@ -49,6 +49,7 @@[m [mpublic class AuditRequestFilter extends OncePerRequestFilter {[m
             new Rule("POST", "^/api/admin/users$", "USUARIO_CREADO", "USUARIO", false),[m
             new Rule("PUT", "^/api/admin/users/[^/]+$", "USUARIO_ACTUALIZADO", "USUARIO", false),[m
             new Rule("POST", "^/api/admin/users/[^/]+/roles$", "ROL_ASIGNADO", "USUARIO", false),[m
[32m+[m[32m            new Rule("PUT", "^/api/admin/users/[^/]+/roles$", "ROL_ASIGNADO", "USUARIO", false),[m
             new Rule("POST", "^/api/admin/users/[^/]+/participant$", "PARTICIPANTE_VINCULADO", "USUARIO", false),[m
             new Rule("PATCH", "^/api/admin/users/[^/]+/active$", "ESTADO_CUENTA_CAMBIADO", "USUARIO", false),[m
             new Rule("PUT", "^/api/admin/attendance-policy$", "POLITICA_ASISTENCIA_ACTUALIZADA", "POLITICA", false),[m
[1mdiff --git a/backend/src/main/java/com/syncevent/users/api/AdminUserController.java b/backend/src/main/java/com/syncevent/users/api/AdminUserController.java[m
[1mindex fc263f1..6363f0f 100644[m
[1m--- a/backend/src/main/java/com/syncevent/users/api/AdminUserController.java[m
[1m+++ b/backend/src/main/java/com/syncevent/users/api/AdminUserController.java[m
[36m@@ -91,6 +91,17 @@[m [mpublic class AdminUserController {[m
                         userId, request.role(), UUID.fromString(currentToken.getClaimAsString("uid"))));[m
     }[m
 [m
[32m+[m[32m    @PutMapping("/{userId}/roles")[m
[32m+[m[32m    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMINISTRADOR')")[m
[32m+[m[32m    public UserAccountResponse replaceRoles([m
[32m+[m[32m            @PathVariable UUID userId,[m
[32m+[m[32m            @Valid @RequestBody UpdateUserRolesRequest request,[m
[32m+[m[32m            @AuthenticationPrincipal Jwt currentToken) {[m
[32m+[m[32m        return UserAccountResponse.from([m
[32m+[m[32m                adminUserService.replaceRoles([m
[32m+[m[32m                        userId, request.roles(), UUID.fromString(currentToken.getClaimAsString("uid"))));[m
[32m+[m[32m    }[m
[32m+[m
     @PutMapping("/{userId}/password")[m
     @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMINISTRADOR')")[m
     public UserAccountResponse resetPassword([m
[1mdiff --git a/backend/src/main/java/com/syncevent/users/application/AdminUserService.java b/backend/src/main/java/com/syncevent/users/application/AdminUserService.java[m
[1mindex aa98f14..45fa413 100644[m
[1m--- a/backend/src/main/java/com/syncevent/users/application/AdminUserService.java[m
[1m+++ b/backend/src/main/java/com/syncevent/users/application/AdminUserService.java[m
[36m@@ -102,6 +102,23 @@[m [mpublic class AdminUserService {[m
         return userPersistencePort.findById(id).orElseThrow(() -> new UserAccountNotFoundException(id));[m
     }[m
 [m
[32m+[m[32m    private UserAccount findByIdForUpdate(UUID id) {[m
[32m+[m[32m        return userPersistencePort.findByIdForUpdate(id)[m
[32m+[m[32m                .orElseThrow(() -> new UserAccountNotFoundException(id));[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    private void validateRoles(Set<UserRole> roles) {[m
[32m+[m[32m        if (roles == null || roles.isEmpty()) {[m
[32m+[m[32m            throw new IllegalArgumentException("Debe asignarse al menos un rol.");[m
[32m+[m[32m        }[m
[32m+[m[32m        for (UserRole role : roles) {[m
[32m+[m[32m            if (role == null || !rolePersistencePort.existsByName(role.name())) {[m
[32m+[m[32m                throw new IllegalArgumentException([m
[32m+[m[32m                        "El rol '" + role + "' no existe previamente en la base de datos.");[m
[32m+[m[32m            }[m
[32m+[m[32m        }[m
[32m+[m[32m    }[m
[32m+[m
     @Transactional[m
     public UserAccount updateEmail(UUID id, String email, UUID actorId) {[m
         UserAccount account = findById(id);[m
[36m@@ -127,21 +144,38 @@[m [mpublic class AdminUserService {[m
             throw new IllegalArgumentException("El rol especificado no existe previamente en la base de datos.");[m
         }[m
         UserAccount account = findById(id);[m
[31m-        if (account.getRoles().size() == 1 && account.getRoles().contains(role)) {[m
[32m+[m[32m        if (account.getRoles().contains(role)) {[m
[32m+[m[32m            return account;[m
[32m+[m[32m        }[m
[32m+[m
[32m+[m[32m        UserAccount updated = userPersistencePort.save(account.assignRole(role));[m
[32m+[m[32m        LocalDateTime now = LocalDateTime.now(clock);[m
[32m+[m[32m        // Los roles cambian de inmediato: las sesiones abiertas dejan de ser válidas.[m
[32m+[m[32m        sessionPersistencePort.revokeAllForUser(id, now);[m
[32m+[m[32m        activityRecorder.record(actorId, updated.getEmail(), "USER_ROLE_ADDED", now);[m
[32m+[m[32m        return updated;[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    @Transactional[m
[32m+[m[32m    public UserAccount replaceRoles(UUID id, Set<UserRole> roles, UUID actorId) {[m
[32m+[m[32m        validateRoles(roles);[m
[32m+[m[32m        Set<UserRole> newRoles = Set.copyOf(roles);[m
[32m+[m[32m        UserAccount account = findByIdForUpdate(id);[m
[32m+[m[32m        if (account.getRoles().equals(newRoles)) {[m
             return account;[m
         }[m
         if (account.isActive()[m
                 && account.getRoles().contains(UserRole.ADMINISTRADOR)[m
[31m-                && role != UserRole.ADMINISTRADOR[m
[32m+[m[32m                && !newRoles.contains(UserRole.ADMINISTRADOR)[m
                 && userPersistencePort.countActiveAdministrators() <= 1) {[m
             throw new LastAdministratorException();[m
         }[m
 [m
[31m-        UserAccount updated = userPersistencePort.save(account.assignRole(role));[m
[32m+[m[32m        UserAccount updated = userPersistencePort.save(account.withRoles(newRoles));[m
         LocalDateTime now = LocalDateTime.now(clock);[m
[31m-        // El rol cambia de inmediato: las sesiones abiertas con el rol anterior dejan de ser válidas.[m
[32m+[m[32m        // Los roles cambian de inmediato: las sesiones abiertas dejan de ser válidas.[m
         sessionPersistencePort.revokeAllForUser(id, now);[m
[31m-        activityRecorder.record(actorId, updated.getEmail(), "USER_ROLE_ADDED", now);[m
[32m+[m[32m        activityRecorder.record(actorId, updated.getEmail(), "USER_ROLES_UPDATED", now);[m
         return updated;[m
     }[m
 [m
[1mdiff --git a/backend/src/main/java/com/syncevent/users/domain/UserAccount.java b/backend/src/main/java/com/syncevent/users/domain/UserAccount.java[m
[1mindex 7fde95e..74ca3c8 100644[m
[1m--- a/backend/src/main/java/com/syncevent/users/domain/UserAccount.java[m
[1m+++ b/backend/src/main/java/com/syncevent/users/domain/UserAccount.java[m
[36m@@ -1,6 +1,7 @@[m
 package com.syncevent.users.domain;[m
 [m
 import java.time.LocalDateTime;[m
[32m+[m[32mimport java.util.HashSet;[m
 import java.util.Set;[m
 import java.util.UUID;[m
 import java.util.Locale;[m
[36m@@ -135,7 +136,24 @@[m [mpublic final class UserAccount {[m
         if (role == null) {[m
             throw new IllegalArgumentException("El rol es obligatorio.");[m
         }[m
[31m-        return new UserAccount(id, email, passwordHash, Set.of(role), active, createdAt, passwordChangedAt, participantId);[m
[32m+[m[32m        Set<UserRole> updatedRoles = new HashSet<>(roles);[m
[32m+[m[32m        updatedRoles.add(role);[m
[32m+[m[32m        return withRoles(updatedRoles);[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    public UserAccount withRoles(Set<UserRole> newRoles) {[m
[32m+[m[32m        if (newRoles == null || newRoles.isEmpty()) {[m
[32m+[m[32m            throw new IllegalArgumentException("Debe asignarse al menos un rol.");[m
[32m+[m[32m        }[m
[32m+[m[32m        return new UserAccount([m
[32m+[m[32m                id,[m
[32m+[m[32m                email,[m
[32m+[m[32m                passwordHash,[m
[32m+[m[32m                Set.copyOf(newRoles),[m
[32m+[m[32m                active,[m
[32m+[m[32m                createdAt,[m
[32m+[m[32m                passwordChangedAt,[m
[32m+[m[32m                participantId);[m
     }[m
 [m
     public UserAccount withActive(boolean newActive) {[m
[1mdiff --git a/backend/src/test/java/com/syncevent/users/api/AdminUserControllerTest.java b/backend/src/test/java/com/syncevent/users/api/AdminUserControllerTest.java[m
[1mindex 130c843..e71835a 100644[m
[1m--- a/backend/src/test/java/com/syncevent/users/api/AdminUserControllerTest.java[m
[1m+++ b/backend/src/test/java/com/syncevent/users/api/AdminUserControllerTest.java[m
[36m@@ -130,9 +130,9 @@[m [mclass AdminUserControllerTest {[m
     }[m
 [m
     @Test[m
[31m-    void asignaUnicoRolYRevocaLaSesionParaQueElNuevoRolSeApliqueAlReingresar() throws Exception {[m
[31m-        UUID userId = createUser("operator@example.com", "ADMINISTRADOR");[m
[31m-        String secondAdminToken = login("operator@example.com", "guest-password-123");[m
[32m+[m[32m    void asignaRolesSinQuitarLosQueYaTeniaYRevocaLaSesionParaQueElNuevoRolSeApliqueAlReingresar() throws Exception {[m
[32m+[m[32m        UUID userId = createUser("operator@example.com", "OPERADOR");[m
[32m+[m[32m        String oldOperatorToken = login("operator@example.com", "guest-password-123");[m
 [m
         mockMvc.perform(post("/api/admin/users/{userId}/roles", userId)[m
                         .header("Authorization", bearer())[m
[36m@@ -141,32 +141,62 @@[m [mclass AdminUserControllerTest {[m
                                 {"role":"MONITOR"}[m
                                 """))[m
                 .andExpect(status().isOk())[m
[31m-                .andExpect(jsonPath("$.roles.length()").value(1))[m
[31m-                .andExpect(jsonPath("$.roles[0]").value("MONITOR"));[m
[32m+[m[32m                .andExpect(jsonPath("$.roles.length()").value(2));[m
 [m
         org.junit.jupiter.api.Assertions.assertEquals([m
[31m-                1,[m
[32m+[m[32m                2,[m
                 jdbcTemplate.queryForObject([m
                         "SELECT COUNT(*) FROM user_account_roles WHERE user_id = ?",[m
                         Integer.class,[m
                         userId));[m
         org.junit.jupiter.api.Assertions.assertEquals(1, countActivity("USER_ROLE_ADDED"));[m
 [m
[31m-        // La sesión abierta con el rol anterior queda revocada inmediatamente.[m
[31m-        mockMvc.perform(get("/api/admin/users")[m
[31m-                        .header("Authorization", "Bearer " + secondAdminToken))[m
[32m+[m[32m        // La sesión abierta con el token anterior queda revocada inmediatamente.[m
[32m+[m[32m        mockMvc.perform(get("/api/auth/me")[m
[32m+[m[32m                        .header("Authorization", "Bearer " + oldOperatorToken))[m
                 .andExpect(status().isUnauthorized());[m
 [m
[31m-        // Al reingresar ya recibe el rol nuevo y el anterior deja de autorizar.[m
[31m-        String monitorToken = login("operator@example.com", "guest-password-123");[m
[32m+[m[32m        // Al reingresar ya recibe ambos roles asignados.[m
[32m+[m[32m        String newDualToken = login("operator@example.com", "guest-password-123");[m
         mockMvc.perform(get("/api/auth/me")[m
[31m-                        .header("Authorization", "Bearer " + monitorToken))[m
[32m+[m[32m                        .header("Authorization", "Bearer " + newDualToken))[m
                 .andExpect(status().isOk())[m
[31m-                .andExpect(jsonPath("$.roles.length()").value(1))[m
[31m-                .andExpect(jsonPath("$.roles[0]").value("MONITOR"));[m
[31m-        mockMvc.perform(get("/api/admin/users")[m
[31m-                        .header("Authorization", "Bearer " + monitorToken))[m
[31m-                .andExpect(status().isForbidden());[m
[32m+[m[32m                .andExpect(jsonPath("$.roles.length()").value(2));[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    @Test[m
[32m+[m[32m    void reemplazaMultiplesRolesYRevocaSesiones() throws Exception {[m
[32m+[m[32m        UUID userId = createUser("multi@example.com", "INVITADO");[m
[32m+[m[32m        String oldToken = login("multi@example.com", "guest-password-123");[m
[32m+[m
[32m+[m[32m        mockMvc.perform(put("/api/admin/users/{userId}/roles", userId)[m
[32m+[m[32m                        .header("Authorization", bearer())[m
[32m+[m[32m                        .contentType(MediaType.APPLICATION_JSON)[m
[32m+[m[32m                        .content("""[m
[32m+[m[32m                                {"roles":["OPERADOR","MONITOR"]}[m
[32m+[m[32m                                """))[m
[32m+[m[32m                .andExpect(status().isOk())[m
[32m+[m[32m                .andExpect(jsonPath("$.roles.length()").value(2));[m
[32m+[m
[32m+[m[32m        org.junit.jupiter.api.Assertions.assertEquals([m
[32m+[m[32m                2,[m
[32m+[m[32m                jdbcTemplate.queryForObject([m
[32m+[m[32m                        "SELECT COUNT(*) FROM user_account_roles WHERE user_id = ?",[m
[32m+[m[32m                        Integer.class,[m
[32m+[m[32m                        userId));[m
[32m+[m[32m        org.junit.jupiter.api.Assertions.assertEquals(1, countActivity("USER_ROLES_UPDATED"));[m
[32m+[m
[32m+[m[32m        // La sesión anterior queda revocada[m
[32m+[m[32m        mockMvc.perform(get("/api/auth/me")[m
[32m+[m[32m                        .header("Authorization", "Bearer " + oldToken))[m
[32m+[m[32m                .andExpect(status().isUnauthorized());[m
[32m+[m
[32m+[m[32m        // Al reingresar tiene los múltiples roles asignados[m
[32m+[m[32m        String newToken = login("multi@example.com", "guest-password-123");[m
[32m+[m[32m        mockMvc.perform(get("/api/auth/me")[m
[32m+[m[32m                        .header("Authorization", "Bearer " + newToken))[m
[32m+[m[32m                .andExpect(status().isOk())[m
[32m+[m[32m                .andExpect(jsonPath("$.roles.length()").value(2));[m
     }[m
 [m
     @Test[m
[1mdiff --git a/frontend/src/App.tsx b/frontend/src/App.tsx[m
[1mindex 3d04ae2..5f9778b 100644[m
[1m--- a/frontend/src/App.tsx[m
[1m+++ b/frontend/src/App.tsx[m
[36m@@ -696,20 +696,27 @@[m [mexport default function App() {[m
         });[m
         updated = mapBackendUser(result);[m
       }[m
[31m-      const nextRole = updates.roles && updates.roles.length > 0 ? updates.roles[updates.roles.length - 1] : undefined;[m
[31m-      if (nextRole && (updated.roles.length !== 1 || updated.roles[0] !== nextRole)) {[m
[31m-        const result = await apiRequest<BackendUserAccount>(`/api/admin/users/${id}/roles`, {[m
[31m-          method: 'POST', body: JSON.stringify({ role: nextRole }),[m
[31m-        });[m
[31m-        updated = mapBackendUser(result);[m
[32m+[m[32m      if (updates.roles && updates.roles.length > 0) {[m
[32m+[m[32m        const nextRoles = [...new Set(updates.roles)].sort();[m
[32m+[m[32m        const currentRoles = [...updated.roles].sort();[m
[32m+[m[32m        const rolesChanged = nextRoles.length !== currentRoles.length[m
[32m+[m[32m          || nextRoles.some((role, index) => role !== currentRoles[index]);[m
[32m+[m[32m        if (rolesChanged) {[m
[32m+[m[32m          const result = await apiRequest<BackendUserAccount>(`/api/admin/users/${id}/roles`, {[m
[32m+[m[32m            method: 'PUT', body: JSON.stringify({ roles: nextRoles }),[m
[32m+[m[32m          });[m
[32m+[m[32m          updated = mapBackendUser(result);[m
[32m+[m[32m        }[m
       }[m
       setUsers(previous => previous.map(user => user.id === id ? updated! : user));[m
       if (authUser && updated && updated.email.toLowerCase() === authUser.email.toLowerCase()) {[m
[31m-        const liveRole = updated.roles[0] || 'INVITADO';[m
[32m+[m[32m        const liveRole = updated.roles.includes(authUser.role)[m
[32m+[m[32m          ? authUser.role[m
[32m+[m[32m          : (updated.roles[0] || 'INVITADO');[m
         const nextSession: AuthUser = {[m
           ...authUser,[m
           role: liveRole,[m
[31m-          allRoles: [liveRole],[m
[32m+[m[32m          allRoles: updated.roles,[m
         };[m
         setAuthUser(nextSession);[m
         localStorage.setItem('syncevent_session', JSON.stringify(nextSession));[m
[36m@@ -1382,9 +1389,12 @@[m [mexport default function App() {[m
         <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>[m
           <ThemeToggle />[m
           {/* Backend Status indicator */}[m
[31m-          <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: '0.76rem', color: backendHealth === 'online' ? 'var(--success-400)' : 'var(--error-400)', background: 'var(--surface-2)', padding: '5px 12px', borderRadius: 20, border: '1px solid var(--glass-border)' }}>[m
[31m-            <span className="pulse-dot" style={{ width: 8, height: 8, borderRadius: '50%', background: backendHealth === 'online' ? 'var(--success-400)' : 'var(--error-400)' }} />[m
[31m-            {backendHealth === 'online' ? 'Servidor conectado' : backendHealth === 'checking' ? 'Conectando…' : 'Servidor no disponible'}[m
[32m+[m[32m          <div[m
[32m+[m[32m            title={`Estado de la conexión: ${backendHealth === 'online' ? 'Servidor conectado' : backendHealth === 'checking' ? 'Conectando…' : 'Servidor no disponible'}`}[m
[32m+[m[32m            style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: '0.74rem', fontWeight: 500, color: backendHealth === 'online' ? 'var(--success-400)' : 'var(--error-400)', background: 'var(--surface-2)', padding: '4px 10px', borderRadius: 20, border: '1px solid var(--glass-border)' }}[m
[32m+[m[32m          >[m
[32m+[m[32m            <span className="pulse-dot" style={{ width: 7, height: 7, borderRadius: '50%', background: backendHealth === 'online' ? 'var(--success-400)' : 'var(--error-400)' }} />[m
[32m+[m[32m            {backendHealth === 'online' ? 'Conectado' : backendHealth === 'checking' ? 'Conectando…' : 'Desconectado'}[m
           </div>[m
 [m
           {/* User card info */}[m
[1mdiff --git a/frontend/src/screens/AdminScreen.tsx b/frontend/src/screens/AdminScreen.tsx[m
[1mindex 30b676e..55bc4ec 100644[m
[1m--- a/frontend/src/screens/AdminScreen.tsx[m
[1m+++ b/frontend/src/screens/AdminScreen.tsx[m
[36m@@ -1,4 +1,4 @@[m
[31m-import React, { useEffect, useState } from 'react';[m
[32m+[m[32mimport React, { useEffect, useRef, useState } from 'react';[m
 import { Event, UserAccount, AuditLog, FineRecord, CertificateRecord, UserRole } from '../types/types';[m
 import { Icon } from '../components/icons';[m
 [m
[36m@@ -45,6 +45,8 @@[m [mexport function AdminScreen({[m
   const [userFormEmail, setUserFormEmail] = useState('');[m
   const [userFormPassword, setUserFormPassword] = useState('');[m
   const [userFormRoles, setUserFormRoles] = useState<UserRole[]>(['INVITADO']);[m
[32m+[m[32m  const [showRoleDropdown, setShowRoleDropdown] = useState(false);[m
[32m+[m[32m  const roleDropdownRef = useRef<HTMLDivElement>(null);[m
 [m
   // User filter & dynamic roles state[m
   const [userSearch, setUserSearch] = useState('');[m
[36m@@ -70,6 +72,20 @@[m [mexport function AdminScreen({[m
     setDisplayUsers(users);[m
   }, [users]);[m
 [m
[32m+[m[32m  useEffect(() => {[m
[32m+[m[32m    const handleClickOutside = (event: MouseEvent) => {[m
[32m+[m[32m      if (roleDropdownRef.current && !roleDropdownRef.current.contains(event.target as Node)) {[m
[32m+[m[32m        setShowRoleDropdown(false);[m
[32m+[m[32m      }[m
[32m+[m[32m    };[m
[32m+[m[32m    if (showRoleDropdown) {[m
[32m+[m[32m      document.addEventListener('mousedown', handleClickOutside);[m
[32m+[m[32m    }[m
[32m+[m[32m    return () => {[m
[32m+[m[32m      document.removeEventListener('mousedown', handleClickOutside);[m
[32m+[m[32m    };[m
[32m+[m[32m  }, [showRoleDropdown]);[m
[32m+[m
   // Backend live filter connection (debounced)[m
   useEffect(() => {[m
     const timer = setTimeout(async () => {[m
[36m@@ -198,6 +214,7 @@[m [mexport function AdminScreen({[m
     setUserFormEmail('');[m
     setUserFormPassword('');[m
     setUserFormRoles(['INVITADO']);[m
[32m+[m[32m    setShowRoleDropdown(false);[m
     setShowUserModal(true);[m
   };[m
 [m
[36m@@ -206,9 +223,24 @@[m [mexport function AdminScreen({[m
     setUserFormEmail(u.email);[m
     setUserFormPassword('');[m
     setUserFormRoles([...u.roles]);[m
[32m+[m[32m    setShowRoleDropdown(false);[m
     setShowUserModal(true);[m
   };[m
 [m
[32m+[m[32m  const handleToggleFormRole = (role: UserRole) => {[m
[32m+[m[32m    setUserFormRoles(prev => {[m
[32m+[m[32m      if (prev.includes(role)) {[m
[32m+[m[32m        if (prev.length === 1) {[m
[32m+[m[32m          addToast('El usuario debe conservar al menos un rol asignado.', 'warning');[m
[32m+[m[32m          return prev;[m
[32m+[m[32m        }[m
[32m+[m[32m        return prev.filter(r => r !== role);[m
[32m+[m[32m      } else {[m
[32m+[m[32m        return [...prev, role];[m
[32m+[m[32m      }[m
[32m+[m[32m    });[m
[32m+[m[32m  };[m
[32m+[m
   const handleSaveUser = async (e: React.FormEvent) => {[m
     e.preventDefault();[m
     if (!userFormEmail.trim() || (!editingUser && !userFormPassword.trim())) {[m
[36m@@ -240,7 +272,7 @@[m [mexport function AdminScreen({[m
         if (!await onResetPassword(editingUser.id, userFormPassword)) return;[m
         addToast(`Cuenta "${userFormEmail}" actualizada y contraseña restablecida. El usuario deberá iniciar sesión de nuevo.`, 'success');[m
       } else {[m
[31m-        addToast(`Cuenta "${userFormEmail}" actualizada correctamente. Si cambiaste el rol, el usuario deberá iniciar sesión de nuevo.`, 'success');[m
[32m+[m[32m        addToast(`Cuenta "${userFormEmail}" actualizada correctamente. Si cambiaste los roles, el usuario deberá iniciar sesión de nuevo.`, 'success');[m
       }[m
     } else {[m
       // Check duplicate email[m
[36m@@ -256,7 +288,7 @@[m [mexport function AdminScreen({[m
         password: userFormPassword,[m
       });[m
       if (!saved) return;[m
[31m-      addToast(`Cuenta "${userFormEmail}" creada correctamente.`, 'success');[m
[32m+[m[32m      addToast(`Cuenta "${userFormEmail}" creada correctamente con los roles asignados.`, 'success');[m
     }[m
     setShowUserModal(false);[m
   };[m
[36m@@ -942,45 +974,112 @@[m [mexport function AdminScreen({[m
                     </p>[m
                   </div>[m
                 )}[m
[31m-                <div className="form-group">[m
[31m-                  <label className="form-label">[m
[31m-                    Rol asignado (único por cuenta)[m
[31m-                  </label>[m
[31m-                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, marginTop: 6 }}>[m
[31m-                    {availableRoles.map(item => {[m
[31m-                      const isChecked = userFormRoles[0] === item.role;[m
[31m-                      return ([m
[31m-                        <label[m
[31m-                          key={item.role}[m
[31m-                          title={item.description}[m
[31m-                          style={{[m
[31m-                            display: 'flex',[m
[31m-                            alignItems: 'center',[m
[31m-                            gap: 10,[m
[31m-                            padding: '10px 14px',[m
[31m-                            background: isChecked ? 'rgba(90,143,179,0.15)' : 'var(--surface-2)',[m
[31m-                            border: `1px solid ${isChecked ? 'var(--accent-400)' : 'var(--glass-border)'}`,[m
[31m-                            borderRadius: 'var(--radius-sm)',[m
[31m-                            cursor: 'pointer',[m
[31m-                            fontSize: '0.84rem',[m
[31m-                            color: isChecked ? 'var(--sand-100)' : 'var(--stone-400)',[m
[31m-                            fontWeight: isChecked ? 600 : 400[m
[31m-                          }}[m
[31m-                        >[m
[31m-                          <input[m
[31m-                            type="radio"[m
[31m-                            name="userRole"[m
[31m-                            checked={isChecked}[m
[31m-                            onChange={() => setUserFormRoles([item.role])}[m
[31m-                          />[m
[31m-                          <div>[m
[31m-                            <div>{item.name}</div>[m
[31m-                            <div style={{ fontSize: '0.72rem', color: 'var(--stone-500)', fontWeight: 400 }}>{item.role}</div>[m
[31m-                          </div>[m
[31m-                        </label>[m
[31m-                      );[m
[31m-                    })}[m
[32m+[m[32m                <div className="form-group" ref={roleDropdownRef} style={{ position: 'relative' }}>[m
[32m+[m[32m                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>[m
[32m+[m[32m                    <label className="form-label" style={{ marginBottom: 4 }}>[m
[32m+[m[32m                      Roles asignados[m
[32m+[m[32m                    </label>[m
[32m+[m[32m                    <span style={{ fontSize: '0.74rem', color: 'var(--stone-400)' }}>[m
[32m+[m[32m                      {userFormRoles.length} seleccionado{userFormRoles.length === 1 ? '' : 's'}[m
[32m+[m[32m                    </span>[m
                   </div>[m
[32m+[m[32m                  <button[m
[32m+[m[32m                    type="button"[m
[32m+[m[32m                    className="select"[m
[32m+[m[32m                    onClick={() => setShowRoleDropdown(prev => !prev)}[m
[32m+[m[32m                    style={{[m
[32m+[m[32m                      width: '100%',[m
[32m+[m[32m                      display: 'flex',[m
[32m+[m[32m                      alignItems: 'center',[m
[32m+[m[32m                      justifyContent: 'space-between',[m
[32m+[m[32m                      minHeight: 44,[m
[32m+[m[32m                      padding: '8px 12px',[m
[32m+[m[32m                      background: 'var(--surface-2)',[m
[32m+[m[32m                      border: '1px solid var(--glass-border)',[m
[32m+[m[32m                      borderRadius: 'var(--radius-sm)',[m
[32m+[m[32m                      cursor: 'pointer',[m
[32m+[m[32m                      textAlign: 'left'[m
[32m+[m[32m                    }}[m
[32m+[m[32m                  >[m
[32m+[m[32m                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, alignItems: 'center', flex: 1, minWidth: 0 }}>[m
[32m+[m[32m                      {userFormRoles.length === 0 ? ([m
[32m+[m[32m                        <span style={{ color: 'var(--stone-400)', fontSize: '0.84rem' }}>Selecciona los roles...</span>[m
[32m+[m[32m                      ) : ([m
[32m+[m[32m                        userFormRoles.map(r => ([m
[32m+[m[32m                          <span[m
[32m+[m[32m                            key={r}[m
[32m+[m[32m                            className={`badge ${r === 'ADMINISTRADOR' ? 'badge-accent' : r === 'OPERADOR' ? 'badge-amber' : r === 'MONITOR' ? 'badge-emerald' : 'badge-slate'}`}[m
[32m+[m[32m                            style={{ fontSize: '0.74rem', pointerEvents: 'none' }}[m
[32m+[m[32m                          >[m
[32m+[m[32m                            {availableRoles.find(ar => ar.role === r)?.name || r}[m
[32m+[m[32m                          </span>[m
[32m+[m[32m                        ))[m
[32m+[m[32m                      )}[m
[32m+[m[32m                    </div>[m
[32m+[m[32m                    <span style={{ fontSize: '0.75rem', color: 'var(--stone-400)', marginLeft: 8 }}>[m
[32m+[m[32m                      {showRoleDropdown ? '▲' : '▼'}[m
[32m+[m[32m                    </span>[m
[32m+[m[32m                  </button>[m
[32m+[m
[32m+[m[32m                  {showRoleDropdown && ([m
[32m+[m[32m                    <div[m
[32m+[m[32m                      style={{[m
[32m+[m[32m                        position: 'absolute',[m
[32m+[m[32m                        top: '100%',[m
[32m+[m[32m                        left: 0,[m
[32m+[m[32m                        right: 0,[m
[32m+[m[32m                        zIndex: 60,[m
[32m+[m[32m                        marginTop: 4,[m
[32m+[m[32m                        background: 'var(--surface-1, #1e2430)',[m
[32m+[m[32m                        border: '1px solid var(--glass-border)',[m
[32m+[m[32m                        borderRadius: 'var(--radius-sm)',[m
[32m+[m[32m                        boxShadow: '0 10px 25px rgba(0,0,0,0.5)',[m
[32m+[m[32m                        padding: 6,[m
[32m+[m[32m                        display: 'flex',[m
[32m+[m[32m                        flexDirection: 'column',[m
[32m+[m[32m                        gap: 4[m
[32m+[m[32m                      }}[m
[32m+[m[32m                    >[m
[32m+[m[32m                      {availableRoles.map(item => {[m
[32m+[m[32m                        const isChecked = userFormRoles.includes(item.role);[m
[32m+[m[32m                        return ([m
[32m+[m[32m                          <div[m
[32m+[m[32m                            key={item.role}[m
[32m+[m[32m                            onClick={() => handleToggleFormRole(item.role)}[m
[32m+[m[32m                            style={{[m
[32m+[m[32m                              display: 'flex',[m
[32m+[m[32m                              alignItems: 'center',[m
[32m+[m[32m                              gap: 10,[m
[32m+[m[32m                              padding: '8px 12px',[m
[32m+[m[32m                              borderRadius: 'var(--radius-xs)',[m
[32m+[m[32m                              cursor: 'pointer',[m
[32m+[m[32m                              background: isChecked ? 'rgba(90,143,179,0.18)' : 'transparent',[m
[32m+[m[32m                              border: isChecked ? '1px solid var(--accent-400)' : '1px solid transparent',[m
[32m+[m[32m                              transition: 'all 0.15s ease'[m
[32m+[m[32m                            }}[m
[32m+[m[32m                          >[m
[32m+[m[32m                            <input[m
[32m+[m[32m                              type="checkbox"[m
[32m+[m[32m                              checked={isChecked}[m
[32m+[m[32m                              readOnly[m
[32m+[m[32m                              style={{ accentColor: 'var(--accent-400)', width: 16, height: 16, cursor: 'pointer' }}[m
[32m+[m[32m                            />[m
[32m+[m[32m                            <div style={{ flex: 1, minWidth: 0 }}>[m
[32m+[m[32m                              <div style={{ fontSize: '0.84rem', fontWeight: isChecked ? 600 : 400, color: isChecked ? 'var(--sand-100)' : 'var(--stone-300)' }}>[m
[32m+[m[32m                                {item.name}[m
[32m+[m[32m                              </div>[m
[32m+[m[32m                              <div style={{ fontSize: '0.72rem', color: 'var(--stone-500)' }}>[m
[32m+[m[32m                                {item.description}[m
[32m+[m[32m                              </div>[m
[32m+[m[32m                            </div>[m
[32m+[m[32m                          </div>[m
[32m+[m[32m                        );[m
[32m+[m[32m                      })}[m
[32m+[m[32m                    </div>[m
[32m+[m[32m                  )}[m
[32m+[m[32m                  <p style={{ margin: '6px 0 0', fontSize: '0.74rem', color: 'var(--stone-400)' }}>[m
[32m+[m[32m                    Haz clic directamente en cada rol para agregarlo o quitarlo (sin necesidad de Ctrl).[m
[32m+[m[32m                  </p>[m
                 </div>[m
               </div>[m
               <div className="modal-footer">[m
[1mdiff --git a/frontend/src/screens/LandingScreen.tsx b/frontend/src/screens/LandingScreen.tsx[m
[1mindex 9e1b017..917826a 100644[m
[1m--- a/frontend/src/screens/LandingScreen.tsx[m
[1m+++ b/frontend/src/screens/LandingScreen.tsx[m
[36m@@ -27,7 +27,7 @@[m [mexport function LandingScreen({[m
         <div className="landing-nav-actions">[m
           <ThemeToggle />[m
           <button className="btn btn-ghost" onClick={onLogin}>Iniciar sesión</button>[m
[31m-          <button className="btn btn-primary" onClick={onRegister}>Crear cuenta gratis</button>[m
[32m+[m[32m          <button className="btn btn-primary" onClick={onRegister}>Crear cuenta</button>[m
         </div>[m
       </nav>[m
 [m
[1mdiff --git a/frontend/src/screens/LoginScreen.tsx b/frontend/src/screens/LoginScreen.tsx[m
[1mindex 321d6b6..2d98eec 100644[m
[1m--- a/frontend/src/screens/LoginScreen.tsx[m
[1m+++ b/frontend/src/screens/LoginScreen.tsx[m
[36m@@ -251,7 +251,7 @@[m [mexport function LoginScreen({[m
           </div>[m
 [m
           <button type="submit" className="btn btn-primary w-full btn-lg" disabled={isLoading} style={{ marginTop: 8 }}>[m
[31m-            {isLoading ? <span style={{ width: 18, height: 18 }}><Icon.Loader /></span> : 'Ingresar a mi Pantalla'}[m
[32m+[m[32m            {isLoading ? <span style={{ width: 18, height: 18 }}><Icon.Loader /></span> : 'Ingresar'}[m
           </button>[m
         </form>[m
 [m
[1mdiff --git a/frontend/src/screens/RegisterScreen.tsx b/frontend/src/screens/RegisterScreen.tsx[m
[1mindex 4b10703..10aec92 100644[m
[1m--- a/frontend/src/screens/RegisterScreen.tsx[m
[1m+++ b/frontend/src/screens/RegisterScreen.tsx[m
[36m@@ -225,7 +225,7 @@[m [mexport function RegisterScreen({[m
           </div>[m
 [m
           <button type="submit" className="btn btn-primary w-full btn-lg" disabled={isLoading} style={{ marginTop: 8 }}>[m
[31m-            {isLoading ? 'Creando cuenta…' : 'Registrarme'}[m
[32m+[m[32m            {isLoading ? 'Creando cuenta…' : 'Crear cuenta'}[m
           </button>[m
         </form>[m
 [m
