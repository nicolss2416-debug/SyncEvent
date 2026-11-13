import React, { useEffect, useRef, useState } from 'react';
import { Event, UserAccount, AuditLog, FineRecord, CertificateRecord, UserRole } from '../types/types';
import { Icon } from '../components/icons';

interface AdminScreenProps {
  events: Event[];
  users: UserAccount[];
  auditLogs: AuditLog[];
  onRefreshAudit?: (lastHours?: number) => Promise<void>;
  fines: FineRecord[];
  attendancePolicy: { absenceThreshold: number; windowDays: number };
  certificates: CertificateRecord[];
  onAddUser: (user: Omit<UserAccount, 'id' | 'createdAt'> & { password: string }) => Promise<boolean>;
  onUpdateUser: (id: string, updates: Partial<UserAccount>) => Promise<boolean>;
  onResetPassword: (id: string, password: string) => Promise<boolean>;
  onToggleUserStatus: (id: string) => Promise<boolean>;
  onResolveFine: (fineId: string) => Promise<boolean>;
  onUpdateAttendancePolicy: (policy: { absenceThreshold: number; windowDays: number }) => Promise<boolean>;
  onRevokeCertificate: (certId: string, reason: string) => Promise<boolean>;
  addToast: (msg: string, type?: 'success' | 'info' | 'error' | 'warning') => void;
}

export function AdminScreen({
  events,
  users,
  auditLogs,
  onRefreshAudit,
  fines,
  attendancePolicy,
  certificates,
  onAddUser,
  onUpdateUser,
  onResetPassword,
  onToggleUserStatus,
  onResolveFine,
  onUpdateAttendancePolicy,
  onRevokeCertificate,
  addToast,
}: AdminScreenProps) {
  const [activeTab, setActiveTab] = useState<'dashboard' | 'users' | 'audit' | 'fines' | 'certificates'>('dashboard');

  // User modal state
  const [showUserModal, setShowUserModal] = useState(false);
  const [editingUser, setEditingUser] = useState<UserAccount | null>(null);
  const [userFormEmail, setUserFormEmail] = useState('');
  const [userFormPassword, setUserFormPassword] = useState('');
  const [userFormRoles, setUserFormRoles] = useState<UserRole[]>(['INVITADO']);
  const [showRoleDropdown, setShowRoleDropdown] = useState(false);
  const roleDropdownRef = useRef<HTMLDivElement>(null);

  // User filter & dynamic roles state
  const [userSearch, setUserSearch] = useState('');
  const [userRoleFilter, setUserRoleFilter] = useState<string>('ALL');
  const [userStatusFilter, setUserStatusFilter] = useState<string>('ALL');
  const [displayUsers, setDisplayUsers] = useState<UserAccount[]>(users);
  const [loadingUsers, setLoadingUsers] = useState(false);
  const [availableRoles, setAvailableRoles] = useState<Array<{ role: UserRole; name: string; description: string }>>([
    { role: 'ADMINISTRADOR', name: 'Administrador', description: 'Gestión total del sistema, configuración, cuentas y auditoría.' },
    { role: 'OPERADOR', name: 'Operador', description: 'Creación y administración de eventos, inscripciones y reportes.' },
    { role: 'MONITOR', name: 'Monitor', description: 'Control de asistencia y verificación en campo.' },
    { role: 'INVITADO', name: 'Invitado', description: 'Visualización del catálogo de eventos y gestión de su propia inscripción.' },
  ]);

  // Dedicated reset password modal state
  const [resetUser, setResetUser] = useState<UserAccount | null>(null);
  const [resetNewPassword, setResetNewPassword] = useState('');
  const [resetConfirmPassword, setResetConfirmPassword] = useState('');
  const [showPasswordText, setShowPasswordText] = useState(false);
  const [isResetting, setIsResetting] = useState(false);

  useEffect(() => {
    setDisplayUsers(users);
  }, [users]);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (roleDropdownRef.current && !roleDropdownRef.current.contains(event.target as Node)) {
        setShowRoleDropdown(false);
      }
    };
    if (showRoleDropdown) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [showRoleDropdown]);

  // Backend live filter connection (debounced)
  useEffect(() => {
    const timer = setTimeout(async () => {
      setLoadingUsers(true);
      try {
        const raw = localStorage.getItem('syncevent_session');
        const token = raw ? JSON.parse(raw)?.token : null;
        const queryParams = new URLSearchParams();
        if (userSearch.trim()) queryParams.set('q', userSearch.trim());
        if (userRoleFilter !== 'ALL') queryParams.set('role', userRoleFilter);
        if (userStatusFilter !== 'ALL') queryParams.set('active', userStatusFilter === 'ACTIVE' ? 'true' : 'false');
        queryParams.set('page', '0');
        queryParams.set('size', '100');

        const response = await fetch(`/api/admin/users?${queryParams.toString()}`, {
          headers: token ? { Authorization: `Bearer ${token}` } : {},
        });
        if (response.ok) {
          const data = await response.json();
          if (Array.isArray(data.content)) {
            setDisplayUsers(data.content.map((u: any) => ({ ...u, name: u.email })));
          }
        }
      } catch (err) {
        console.error('Error fetching filtered users:', err);
      } finally {
        setLoadingUsers(false);
      }
    }, 250);

    return () => clearTimeout(timer);
  }, [userSearch, userRoleFilter, userStatusFilter]);

  const handleOpenResetModal = (user: UserAccount) => {
    setResetUser(user);
    setResetNewPassword('');
    setResetConfirmPassword('');
    setShowPasswordText(false);
  };

  const handleGeneratePassword = () => {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%';
    let generated = 'Sync!';
    for (let i = 0; i < 8; i++) {
      generated += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    setResetNewPassword(generated);
    setResetConfirmPassword(generated);
    setShowPasswordText(true);
  };

  const handleConfirmResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!resetUser) return;
    if (resetNewPassword.length < 8 || resetNewPassword.length > 128) {
      addToast('La nueva contraseña debe tener entre 8 y 128 caracteres.', 'warning');
      return;
    }
    if (resetNewPassword !== resetConfirmPassword) {
      addToast('Las contraseñas no coinciden.', 'warning');
      return;
    }

    setIsResetting(true);
    try {
      const ok = await onResetPassword(resetUser.id, resetNewPassword);
      if (ok) {
        addToast(`Contraseña de "${resetUser.email}" restablecida exitosamente. Todas sus sesiones fueron cerradas.`, 'success');
        setResetUser(null);
      }
    } finally {
      setIsResetting(false);
    }
  };

  useEffect(() => {
    try {
      const raw = localStorage.getItem('syncevent_session');
      const token = raw ? JSON.parse(raw)?.token : null;
      if (token) {
        fetch('/api/admin/users/roles', {
          headers: { Authorization: `Bearer ${token}` },
        })
          .then(res => res.ok ? res.json() : null)
          .then(data => {
            if (Array.isArray(data) && data.length > 0) {
              setAvailableRoles(data);
            }
          })
          .catch(() => {});
      }
    } catch {
      // Usar lista por defecto si falla
    }
  }, []);

  // Revoke modal state
  const [showRevokeModal, setShowRevokeModal] = useState<CertificateRecord | null>(null);
  const [revokeReason, setRevokeReason] = useState('');

  // Fine resolve modal
  const [resolvingFine, setResolvingFine] = useState<FineRecord | null>(null);

  // Audit filter state
  const [auditFilterEntity, setAuditFilterEntity] = useState<string>('ALL');
  const [auditSearch, setAuditSearch] = useState<string>('');

  // Attendance policy settings state
  const [absenceThreshold, setAbsenceThreshold] = useState(attendancePolicy.absenceThreshold);
  const [absenceWindowDays, setAbsenceWindowDays] = useState(attendancePolicy.windowDays);

  useEffect(() => {
    setAbsenceThreshold(attendancePolicy.absenceThreshold);
    setAbsenceWindowDays(attendancePolicy.windowDays);
  }, [attendancePolicy]);

  const handleSaveAttendancePolicy = async () => {
    if (!await onUpdateAttendancePolicy({ absenceThreshold, windowDays: absenceWindowDays })) return;
    addToast('Política de inasistencias actualizada.', 'success');
  };

  const activeAdminsCount = users.filter(u => u.roles.includes('ADMINISTRADOR') && u.active).length;

  const handleOpenCreateUser = () => {
    setEditingUser(null);
    setUserFormEmail('');
    setUserFormPassword('');
    setUserFormRoles(['INVITADO']);
    setShowRoleDropdown(false);
    setShowUserModal(true);
  };

  const handleOpenEditUser = (u: UserAccount) => {
    setEditingUser(u);
    setUserFormEmail(u.email);
    setUserFormPassword('');
    setUserFormRoles([...u.roles]);
    setShowRoleDropdown(false);
    setShowUserModal(true);
  };

  const handleToggleFormRole = (role: UserRole) => {
    setUserFormRoles(prev => {
      if (prev.includes(role)) {
        if (prev.length === 1) {
          addToast('El usuario debe conservar al menos un rol asignado.', 'warning');
          return prev;
        }
        return prev.filter(r => r !== role);
      } else {
        return [...prev, role];
      }
    });
  };

  const handleSaveUser = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!userFormEmail.trim() || (!editingUser && !userFormPassword.trim())) {
      addToast('El correo y la contraseña inicial son obligatorios.', 'warning');
      return;
    }
    if (userFormRoles.length === 0) {
      addToast('Debe asignar al menos un rol al usuario', 'warning');
      return;
    }

    if (editingUser) {
      // RN-18 validation: cannot remove admin role if only 1 active admin
      const isRemovingAdmin = editingUser.roles.includes('ADMINISTRADOR') && !userFormRoles.includes('ADMINISTRADOR');
      if (isRemovingAdmin && activeAdminsCount <= 1) {
        addToast('No se puede quitar el rol de Administrador a la única cuenta administradora activa.', 'error');
        return;
      }
      if (userFormPassword.trim() && (userFormPassword.length < 8 || userFormPassword.length > 128)) {
        addToast('La nueva contraseña debe tener entre 8 y 128 caracteres.', 'warning');
        return;
      }
      const saved = await onUpdateUser(editingUser.id, {
        email: userFormEmail,
        roles: userFormRoles,
      });
      if (!saved) return;
      if (userFormPassword.trim()) {
        if (!await onResetPassword(editingUser.id, userFormPassword)) return;
        addToast(`Cuenta "${userFormEmail}" actualizada y contraseña restablecida. El usuario deberá iniciar sesión de nuevo.`, 'success');
      } else {
        addToast(`Cuenta "${userFormEmail}" actualizada correctamente. Si cambiaste los roles, el usuario deberá iniciar sesión de nuevo.`, 'success');
      }
    } else {
      // Check duplicate email
      if (users.some(u => u.email.toLowerCase() === userFormEmail.toLowerCase())) {
        addToast(`El correo "${userFormEmail}" ya está registrado en la plataforma`, 'error');
        return;
      }
      const saved = await onAddUser({
        name: userFormEmail,
        email: userFormEmail,
        roles: userFormRoles,
        active: true,
        password: userFormPassword,
      });
      if (!saved) return;
      addToast(`Cuenta "${userFormEmail}" creada correctamente con los roles asignados.`, 'success');
    }
    setShowUserModal(false);
  };

  const handleToggleStatusWithCheck = async (user: UserAccount) => {
    // RN-18: Cannot deactivate last admin
    if (user.active && user.roles.includes('ADMINISTRADOR') && activeAdminsCount <= 1) {
      addToast('Debe permanecer al menos una cuenta administradora activa.', 'error');
      return;
    }
    if (!await onToggleUserStatus(user.id)) return;
    addToast(`Estado de ${user.name} actualizado a ${user.active ? 'Inactivo' : 'Activo'}`, 'info');
  };

  const filteredUsers = users.filter(u => {
    const matchRole = userRoleFilter === 'ALL' || u.roles.includes(userRoleFilter as UserRole);
    const matchStatus = userStatusFilter === 'ALL' || (userStatusFilter === 'ACTIVE' ? u.active : !u.active);
    const matchText = userSearch === '' || 
      `${u.name} ${u.email} ${u.id}`.toLowerCase().includes(userSearch.toLowerCase());
    return matchRole && matchStatus && matchText;
  });

  const filteredAuditLogs = auditLogs.filter(log => {
    const matchEntity = auditFilterEntity === 'ALL' || log.entityType === auditFilterEntity;
    const matchText = auditSearch === '' || 
      `${log.action} ${log.userEmail} ${log.details || ''} ${log.reason || ''}`.toLowerCase().includes(auditSearch.toLowerCase());
    return matchEntity && matchText;
  });

  return (
    <div className="actor-screen admin-screen animate-fade-in">
      {/* ── Actor Badge & Header ── */}
      <div className="actor-header-banner" style={{ background: 'var(--banner-bg)', border: '1px solid rgba(186,160,130,0.35)', borderRadius: 'var(--radius-lg)', padding: '24px 28px', marginBottom: 28, boxShadow: 'var(--shadow-sm)' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
            <div style={{ width: 52, height: 52, borderRadius: 14, background: 'linear-gradient(135deg,#3d7399,#1f4261)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', boxShadow: '0 8px 24px rgba(31,66,97,0.4)' }}>
              <span style={{ width: 26, height: 26 }}><Icon.Shield /></span>
            </div>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <h2 style={{ margin: 0, fontSize: '1.4rem', color: 'var(--sand-100)', fontFamily: 'var(--font-display)' }}>Pantalla del Administrador</h2>
                <span className="badge badge-accent" style={{ textTransform: 'uppercase', letterSpacing: '0.05em', fontSize: '0.72rem' }}>Control Maestro</span>
              </div>
              <p style={{ margin: '4px 0 0', fontSize: '0.86rem', color: 'var(--stone-400)' }}>
                Administra las cuentas, revisa la actividad, resuelve multas y supervisa los certificados.
              </p>
            </div>
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <button className={`btn btn-sm ${activeTab === 'dashboard' ? 'btn-primary' : 'btn-ghost'}`} onClick={() => setActiveTab('dashboard')}>
              <span style={{ width: 14, height: 14 }}><Icon.LayoutDashboard /></span> Tablero
            </button>
            <button className={`btn btn-sm ${activeTab === 'users' ? 'btn-primary' : 'btn-ghost'}`} onClick={() => setActiveTab('users')}>
              <span style={{ width: 14, height: 14 }}><Icon.Users /></span> Usuarios ({users.length})
            </button>
            <button className={`btn btn-sm ${activeTab === 'audit' ? 'btn-primary' : 'btn-ghost'}`} onClick={() => setActiveTab('audit')}>
              <span style={{ width: 14, height: 14 }}><Icon.Activity /></span> Auditoría ({auditLogs.length})
            </button>
            <button className={`btn btn-sm ${activeTab === 'fines' ? 'btn-primary' : 'btn-ghost'}`} onClick={() => setActiveTab('fines')}>
              <span style={{ width: 14, height: 14 }}><Icon.ShieldAlert /></span> Multas ({fines.filter(f=>f.status==='ACTIVA').length})
            </button>
            <button className={`btn btn-sm ${activeTab === 'certificates' ? 'btn-primary' : 'btn-ghost'}`} onClick={() => setActiveTab('certificates')}>
              <span style={{ width: 14, height: 14 }}><Icon.Award /></span> Certificados
            </button>
          </div>
        </div>
      </div>

      {/* ── TAB 1: DASHBOARD MAESTRO ── */}
      {activeTab === 'dashboard' && (
        <div className="tab-pane animate-fade-in">
          <div className="stats-grid" style={{ marginBottom: 28 }}>
            <div className="stat-card" style={{ border: '1px solid var(--glass-border)', borderRadius: 'var(--radius-lg)', background: 'var(--surface-1)', padding: '22px 24px', boxShadow: 'var(--shadow-sm)' }}>
              <div className="stat-card-accent" style={{ background: 'linear-gradient(90deg, var(--accent-400), var(--accent-600))' }} />
              <div className="stat-card-top">
                <span className="stat-card-label">Usuarios del Sistema</span>
                <div className="stat-card-icon" style={{ background: 'rgba(166,99,57,0.12)', color: 'var(--accent-500)' }}>
                  <span style={{ width: 20, height: 20 }}><Icon.Users /></span>
                </div>
              </div>
              <div className="stat-card-value" style={{ fontFamily: 'var(--font-display)', color: 'var(--sand-100)' }}>{users.length}</div>
              <div className="stat-card-change" style={{ color: 'var(--stone-400)', display: 'flex', alignItems: 'center', gap: 6 }}>
                <span style={{ width: 6, height: 6, borderRadius: '50%', background: 'var(--success-400)' }} />
                <span><strong>{activeAdminsCount}</strong> administradores activos</span>
              </div>
            </div>

            <div className="stat-card" style={{ border: '1px solid var(--glass-border)', borderRadius: 'var(--radius-lg)', background: 'var(--surface-1)', padding: '22px 24px', boxShadow: 'var(--shadow-sm)' }}>
              <div className="stat-card-accent" style={{ background: 'linear-gradient(90deg, #D97706, #B45309)' }} />
              <div className="stat-card-top">
                <span className="stat-card-label">Multas por Inasistencia</span>
                <div className="stat-card-icon" style={{ background: 'rgba(217,119,6,0.12)', color: 'var(--warning-400)' }}>
                  <span style={{ width: 20, height: 20 }}><Icon.ShieldAlert /></span>
                </div>
              </div>
              <div className="stat-card-value" style={{ fontFamily: 'var(--font-display)', color: 'var(--sand-100)' }}>{fines.filter(f => f.status === 'ACTIVA').length}</div>
              <div className="stat-card-change" style={{ color: 'var(--stone-400)' }}>
                Umbral: <strong>{absenceThreshold}</strong> inasistencias en <strong>{absenceWindowDays}</strong> días
              </div>
            </div>

            <div className="stat-card" style={{ border: '1px solid var(--glass-border)', borderRadius: 'var(--radius-lg)', background: 'var(--surface-1)', padding: '22px 24px', boxShadow: 'var(--shadow-sm)' }}>
              <div className="stat-card-accent" style={{ background: 'linear-gradient(90deg, #16A34A, #15803D)' }} />
              <div className="stat-card-top">
                <span className="stat-card-label">Eventos en la Red</span>
                <div className="stat-card-icon" style={{ background: 'rgba(22,163,74,0.12)', color: 'var(--success-500)' }}>
                  <span style={{ width: 20, height: 20 }}><Icon.Calendar /></span>
                </div>
              </div>
              <div className="stat-card-value" style={{ fontFamily: 'var(--font-display)', color: 'var(--sand-100)' }}>{events.length}</div>
              <div className="stat-card-change" style={{ color: 'var(--stone-400)', display: 'flex', alignItems: 'center', gap: 6 }}>
                <span style={{ width: 6, height: 6, borderRadius: '50%', background: 'var(--success-400)' }} />
                <span><strong>{events.filter(e => e.status === 'PUBLICADO' || e.status === 'EN_CURSO').length}</strong> activos para el público</span>
              </div>
            </div>

            <div className="stat-card" style={{ border: '1px solid var(--glass-border)', borderRadius: 'var(--radius-lg)', background: 'var(--surface-1)', padding: '22px 24px', boxShadow: 'var(--shadow-sm)' }}>
              <div className="stat-card-accent" style={{ background: 'linear-gradient(90deg, #7E6952, #4B3C2D)' }} />
              <div className="stat-card-top">
                <span className="stat-card-label">Registros de Auditoría</span>
                <div className="stat-card-icon" style={{ background: 'rgba(126,105,82,0.14)', color: 'var(--sand-300)' }}>
                  <span style={{ width: 20, height: 20 }}><Icon.Activity /></span>
                </div>
              </div>
              <div className="stat-card-value" style={{ fontFamily: 'var(--font-display)', color: 'var(--sand-100)' }}>{auditLogs.length}</div>
              <div className="stat-card-change" style={{ color: 'var(--stone-400)' }}>
                Bitácora de seguridad activa
              </div>
            </div>
          </div>

          {/* Quick status & audit sneak-peek */}
          <div className="grid-2-1" style={{ display: 'grid', gridTemplateColumns: '1.7fr 1fr', gap: 24, alignItems: 'start' }}>
            <div className="card" style={{ boxShadow: 'var(--shadow-sm)' }}>
              <div className="card-header" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '18px 24px' }}>
                <h3 className="card-title" style={{ margin: 0, fontSize: '1.02rem', gap: 10 }}>
                  <span style={{ width: 18, height: 18, color: 'var(--accent-500)' }}><Icon.Activity /></span>
                  Eventos Recientes de Auditoría
                </h3>
                <button className="btn btn-ghost btn-sm" onClick={() => setActiveTab('audit')} style={{ fontSize: '0.8rem' }}>
                  Ver bitácora completa →
                </button>
              </div>
              <div className="table-wrapper">
                <table>
                  <thead>
                    <tr>
                      <th>Fecha / Hora</th>
                      <th>Usuario</th>
                      <th>Acción</th>
                      <th>Entidad</th>
                      <th>Resultado</th>
                    </tr>
                  </thead>
                  <tbody>
                    {auditLogs.length === 0 ? (
                      <tr>
                        <td colSpan={5} style={{ textAlign: 'center', padding: '48px 20px', color: 'var(--stone-400)' }}>
                          <span style={{ width: 32, height: 32, display: 'inline-block', opacity: 0.45, marginBottom: 8 }}><Icon.Activity /></span>
                          <div style={{ fontSize: '0.88rem', fontWeight: 500 }}>No hay eventos de auditoría registrados en esta sesión</div>
                          <div style={{ fontSize: '0.78rem', color: 'var(--stone-500)', marginTop: 4 }}>Las acciones administrativas y accesos se registrarán automáticamente aquí</div>
                        </td>
                      </tr>
                    ) : (
                      auditLogs.slice(0, 5).map(log => (
                        <tr key={log.id}>
                          <td style={{ fontSize: '0.8rem', color: 'var(--stone-400)' }}>{log.timestamp}</td>
                          <td style={{ fontWeight: 500, color: 'var(--sand-200)', fontSize: '0.84rem' }}>{log.userEmail}</td>
                          <td><span className="badge badge-accent" style={{ fontSize: '0.74rem' }}>{log.action}</span></td>
                          <td><span className="badge badge-slate" style={{ fontSize: '0.74rem' }}>{log.entityType}</span></td>
                          <td>
                            <span className={`badge ${log.result === 'EXITO' ? 'badge-emerald' : 'badge-rose'}`} style={{ fontSize: '0.72rem' }}>
                              {log.result}
                            </span>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>

            <div className="card" style={{ boxShadow: 'var(--shadow-sm)' }}>
              <div className="card-header" style={{ padding: '18px 24px' }}>
                <h3 className="card-title" style={{ margin: 0, fontSize: '1.02rem', gap: 10 }}>
                  <span style={{ width: 18, height: 18, color: 'var(--accent-500)' }}><Icon.Settings /></span>
                  Política de Inasistencias
                </h3>
              </div>
              <div className="card-body" style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: 16 }}>
                <p style={{ margin: 0, fontSize: '0.84rem', color: 'var(--stone-400)', lineHeight: 1.5 }}>
                  Las inasistencias reiteradas generan una multa y bloquean nuevas inscripciones hasta que sea resuelta.
                </p>
                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label className="form-label" style={{ fontSize: '0.82rem', fontWeight: 600, color: 'var(--sand-200)', marginBottom: 6 }}>
                    Inasistencias máximas permitidas:
                  </label>
                  <input
                    type="number"
                    min="1"
                    max="10"
                    className="input"
                    value={absenceThreshold}
                    onChange={e => setAbsenceThreshold(parseInt(e.target.value) || 3)}
                  />
                </div>
                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label className="form-label" style={{ fontSize: '0.82rem', fontWeight: 600, color: 'var(--sand-200)', marginBottom: 6 }}>
                    Ventana de evaluación (días):
                  </label>
                  <input
                    type="number"
                    min="30"
                    max="365"
                    className="input"
                    value={absenceWindowDays}
                    onChange={e => setAbsenceWindowDays(parseInt(e.target.value) || 90)}
                  />
                </div>
                <button
                  className="btn btn-primary btn-sm"
                  onClick={handleSaveAttendancePolicy}
                  style={{ marginTop: 4, height: 38, fontWeight: 600 }}
                >
                  <span style={{ width: 14, height: 14 }}><Icon.Check /></span>
                  Guardar Parámetros
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ── TAB 2: GESTIÓN DE USUARIOS Y ROLES (HU-002, RF-003, RF-004) ── */}
      {activeTab === 'users' && (
        <div className="tab-pane animate-fade-in">
          <div className="card">
            <div className="card-header" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 12 }}>
              <div>
                <h3 className="card-title" style={{ margin: 0 }}>Gestión de Cuentas y Asignación de Roles</h3>
                <p style={{ margin: '4px 0 0', fontSize: '0.84rem', color: 'var(--stone-400)' }}>
                  Crea y actualiza cuentas, asigna un rol único, restablece contraseñas y protege la
                  disponibilidad de una cuenta administradora.
                </p>
              </div>
              <button className="btn btn-primary" onClick={handleOpenCreateUser}>
                <span style={{ width: 16, height: 16 }}><Icon.Plus /></span> Crear Nuevo Usuario
              </button>
            </div>

            {/* Filtros de búsqueda y rol */}
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, padding: '12px 20px', borderBottom: '1px solid var(--border-color)', alignItems: 'center' }}>
              <div style={{ position: 'relative', flex: '1 1 240px' }}>
                <input
                  type="text"
                  placeholder="Buscar por correo o nombre..."
                  className="input input-sm"
                  style={{ width: '100%', paddingLeft: 32 }}
                  value={userSearch}
                  onChange={e => setUserSearch(e.target.value)}
                />
                <span style={{ position: 'absolute', left: 10, top: '50%', transform: 'translateY(-50%)', width: 14, height: 14, color: 'var(--stone-400)' }}>
                  <Icon.Search />
                </span>
              </div>
              <select
                className="select select-sm"
                style={{ width: 'auto', fontSize: '0.82rem' }}
                value={userRoleFilter}
                onChange={e => setUserRoleFilter(e.target.value)}
              >
                <option value="ALL">Todos los roles</option>
                {availableRoles.map(r => (
                  <option key={r.role} value={r.role}>{r.name}</option>
                ))}
              </select>
              <select
                className="select select-sm"
                style={{ width: 'auto', fontSize: '0.82rem' }}
                value={userStatusFilter}
                onChange={e => setUserStatusFilter(e.target.value)}
              >
                <option value="ALL">Todos los estados</option>
                <option value="ACTIVE">Activa</option>
                <option value="INACTIVE">Desactivada</option>
              </select>
              {(userSearch || userRoleFilter !== 'ALL' || userStatusFilter !== 'ALL') && (
                <button
                  className="btn btn-ghost btn-sm"
                  style={{ fontSize: '0.8rem' }}
                  onClick={() => {
                    setUserSearch('');
                    setUserRoleFilter('ALL');
                    setUserStatusFilter('ALL');
                  }}
                >
                  Limpiar filtros
                </button>
              )}
              <span style={{ fontSize: '0.8rem', color: 'var(--stone-400)', marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: 6 }}>
                {loadingUsers && <span style={{ width: 14, height: 14 }}><Icon.Loader /></span>}
                Mostrando {displayUsers.length} de {users.length} usuarios
              </span>
            </div>

            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>Nombre / Identificación</th>
                    <th>Correo Electrónico</th>
                    <th>Roles Asignados</th>
                    <th>Estado de Cuenta</th>
                    <th>Creado</th>
                    <th style={{ textAlign: 'right' }}>Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {displayUsers.map(u => (
                    <tr key={u.id}>
                      <td>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                          <div className="avatar" style={{ width: 34, height: 34, fontSize: '0.8rem' }}>{u.name.slice(0, 2).toUpperCase()}</div>
                          <div>
                            <div style={{ fontWeight: 600, color: 'var(--sand-100)' }}>{u.name}</div>
                            <div style={{ fontSize: '0.74rem', color: 'var(--stone-500)' }}>ID: {u.id}</div>
                          </div>
                        </div>
                      </td>
                      <td style={{ color: 'var(--sand-300)' }}>{u.email}</td>
                      <td>
                        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4 }}>
                          {u.roles.map(r => (
                            <span
                              key={r}
                              className={`badge ${r === 'ADMINISTRADOR' ? 'badge-accent' : r === 'OPERADOR' ? 'badge-amber' : r === 'MONITOR' ? 'badge-emerald' : 'badge-slate'}`}
                              style={{ fontSize: '0.72rem' }}
                            >
                              {r}
                            </span>
                          ))}
                        </div>
                      </td>
                      <td>
                        <span className={`badge ${u.active ? 'badge-emerald' : 'badge-rose'}`} style={{ fontSize: '0.74rem' }}>
                          {u.active ? 'Activa' : 'Desactivada'}
                        </span>
                      </td>
                      <td style={{ fontSize: '0.8rem', color: 'var(--stone-400)' }}>{u.createdAt}</td>
                      <td style={{ textAlign: 'right' }}>
                        <div style={{ display: 'inline-flex', gap: 6 }}>
                          <button className="btn btn-secondary btn-sm" title="Editar roles y datos" onClick={() => handleOpenEditUser(u)}>
                            <span style={{ width: 14, height: 14 }}><Icon.Edit /></span>
                          </button>
                          <button
                            className="btn btn-secondary btn-sm"
                            title="Restablecer contraseña"
                            onClick={() => handleOpenResetModal(u)}
                          >
                            <span style={{ width: 14, height: 14 }}><Icon.Key /></span>
                          </button>
                          <button
                            className={`btn btn-sm ${u.active ? 'btn-ghost' : 'btn-primary'}`}
                            title={u.active ? 'Desactivar cuenta' : 'Activar cuenta'}
                            onClick={() => handleToggleStatusWithCheck(u)}
                          >
                            <span style={{ width: 14, height: 14 }}>{u.active ? <Icon.Ban /> : <Icon.Check />}</span>
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* ── TAB 3: BITÁCORA DE AUDITORÍA INALTERABLE (HU-023, HU-024) ── */}
      {activeTab === 'audit' && (
        <div className="tab-pane animate-fade-in">
          <div className="card">
            <div className="card-header" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 12 }}>
              <div>
                <h3 className="card-title">Registro de actividad</h3>
                <p style={{ margin: '4px 0 0', fontSize: '0.84rem', color: 'var(--stone-400)' }}>
                  Registro automático e inalterable: quién, qué hizo, entidad, fecha/hora, IP, resultado y motivo en cancelaciones/revocaciones.
                </p>
              </div>
              <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                {onRefreshAudit && (
                  <button
                    className="btn btn-secondary btn-sm"
                    onClick={() => { void onRefreshAudit(); }}
                    title="Actualizar la bitácora desde el servidor"
                  >
                    <span style={{ width: 14, height: 14 }}><Icon.RefreshCw /></span> Actualizar
                  </button>
                )}
                <select
                  className="select"
                  style={{ width: 'auto', fontSize: '0.82rem' }}
                  value={auditFilterEntity}
                  onChange={e => setAuditFilterEntity(e.target.value)}
                >
                  <option value="ALL">Todas las entidades</option>
                  <option value="USUARIO">Usuarios</option>
                  <option value="EVENTO">Eventos</option>
                  <option value="INSCRIPCION">Inscripciones</option>
                  <option value="ASISTENCIA">Asistencia</option>
                  <option value="CERTIFICADO">Certificados</option>
                  <option value="POLITICA">Políticas</option>
                </select>
                <div style={{ position: 'relative' }}>
                  <input
                    type="text"
                    placeholder="Filtrar por texto..."
                    className="input"
                    style={{ fontSize: '0.82rem', paddingLeft: 30, width: 200 }}
                    value={auditSearch}
                    onChange={e => setAuditSearch(e.target.value)}
                  />
                  <span style={{ position: 'absolute', left: 10, top: 10, width: 14, height: 14, color: 'var(--stone-500)' }}><Icon.Search /></span>
                </div>
              </div>
            </div>

            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>Marca de Tiempo</th>
                    <th>Usuario Responsable</th>
                    <th>Acción</th>
                    <th>Entidad / ID</th>
                    <th>Dirección IP</th>
                    <th>Resultado</th>
                    <th>Detalles / Motivo Justificado</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredAuditLogs.map(log => (
                    <tr key={log.id}>
                      <td style={{ fontSize: '0.78rem', color: 'var(--stone-400)', whiteSpace: 'nowrap' }}>{log.timestamp}</td>
                      <td style={{ fontWeight: 600, color: 'var(--sand-200)', fontSize: '0.85rem' }}>{log.userEmail}</td>
                      <td><span className="badge badge-accent" style={{ fontSize: '0.74rem' }}>{log.action}</span></td>
                      <td>
                        <span className="badge badge-slate" style={{ fontSize: '0.74rem' }}>{log.entityType}: {log.entityId}</span>
                      </td>
                      <td style={{ fontSize: '0.78rem', color: 'var(--stone-500)', fontFamily: 'monospace' }}>{log.ipAddress}</td>
                      <td>
                        <span className={`badge ${log.result === 'EXITO' ? 'badge-emerald' : 'badge-rose'}`} style={{ fontSize: '0.72rem' }}>
                          {log.result}
                        </span>
                      </td>
                      <td style={{ fontSize: '0.82rem', color: 'var(--stone-300)' }}>
                        {log.reason && (
                          <div style={{ color: 'var(--warning-400)', fontWeight: 500, marginBottom: 2 }}>
                            Motivo: "{log.reason}"
                          </div>
                        )}
                        {log.details || '—'}
                      </td>
                    </tr>
                  ))}
                  {filteredAuditLogs.length === 0 && (
                    <tr>
                      <td colSpan={7} style={{ textAlign: 'center', padding: 30, color: 'var(--stone-500)' }}>
                        No se encontraron registros de auditoría para los criterios seleccionados.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* ── TAB 4: SANCIONES Y GESTIÓN DE MULTAS (RN-24) ── */}
      {activeTab === 'fines' && (
        <div className="tab-pane animate-fade-in">
          <div className="card">
            <div className="card-header" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 12 }}>
              <div>
                <h3 className="card-title">Participantes Sancionados con Multas Activas</h3>
                <p style={{ margin: '4px 0 0', fontSize: '0.84rem', color: 'var(--stone-400)' }}>
                  Las personas con multas pendientes no pueden inscribirse a nuevos eventos hasta que se resuelva su caso.
                </p>
              </div>
              <div style={{ fontSize: '0.82rem', color: 'var(--warning-400)', background: 'rgba(234,179,8,0.1)', padding: '6px 14px', borderRadius: 'var(--radius-sm)', border: '1px solid rgba(234,179,8,0.25)' }}>
                Políticas activas: {absenceThreshold} inasistencias en {absenceWindowDays} días
              </div>
            </div>

            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>Participante</th>
                    <th>Documento</th>
                    <th>Correo</th>
                    <th>Inasistencias</th>
                    <th>Fecha Sanción</th>
                    <th>Estado Multa</th>
                    <th>Notas / Resolución</th>
                    <th style={{ textAlign: 'right' }}>Acción</th>
                  </tr>
                </thead>
                <tbody>
                  {fines.map(fine => (
                    <tr key={fine.id}>
                      <td style={{ fontWeight: 600, color: 'var(--sand-100)' }}>{fine.participantName}</td>
                      <td style={{ color: 'var(--stone-400)', fontSize: '0.82rem' }}>{fine.documentNumber}</td>
                      <td style={{ color: 'var(--stone-300)' }}>{fine.participantEmail}</td>
                      <td>
                        <span className="badge badge-rose" style={{ fontSize: '0.74rem' }}>
                          {fine.missedEventsCount} faltas en {fine.periodDays}d
                        </span>
                      </td>
                      <td style={{ fontSize: '0.8rem', color: 'var(--stone-400)' }}>{fine.issuedAt}</td>
                      <td>
                        <span className={`badge ${fine.status === 'ACTIVA' ? 'badge-rose' : 'badge-emerald'}`}>
                          {fine.status === 'ACTIVA' ? 'Sanción Vigente' : 'Resuelta'}
                        </span>
                      </td>
                      <td style={{ fontSize: '0.8rem', color: 'var(--stone-400)' }}>
                        {fine.status === 'RESUELTA' ? `Resuelto por: ${fine.resolvedBy || 'Admin'} (${fine.notes || 'Rehabilitado'})` : 'Inhabilitado para inscribirse'}
                      </td>
                      <td style={{ textAlign: 'right' }}>
                        {fine.status === 'ACTIVA' ? (
                          <button
                            className="btn btn-primary btn-sm"
                            onClick={() => setResolvingFine(fine)}
                          >
                            Resolver Sanción
                          </button>
                        ) : (
                          <span style={{ fontSize: '0.78rem', color: 'var(--success-400)' }}>Rehabilitado</span>
                        )}
                      </td>
                    </tr>
                  ))}
                  {fines.length === 0 && (
                    <tr>
                      <td colSpan={8} style={{ textAlign: 'center', padding: 30, color: 'var(--stone-500)' }}>
                        No hay participantes sancionados con multas en este momento.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* ── TAB 5: SUPERVISIÓN Y REVOCACIÓN DE CERTIFICADOS (RN-10, RN-19) ── */}
      {activeTab === 'certificates' && (
        <div className="tab-pane animate-fade-in">
          <div className="card">
            <div className="card-header">
              <h3 className="card-title">Supervisión y Revocación Oficial de Certificados</h3>
              <p style={{ margin: '4px 0 0', fontSize: '0.84rem', color: 'var(--stone-400)' }}>
                Revoca certificados con un motivo y conserva el registro de la decisión.
              </p>
            </div>

            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>Código Único</th>
                    <th>Participante</th>
                    <th>Evento / Curso</th>
                    <th>Horas</th>
                    <th>Fecha Emisión</th>
                    <th>Estado Actual</th>
                    <th>Motivo de Revocación</th>
                    <th style={{ textAlign: 'right' }}>Acción</th>
                  </tr>
                </thead>
                <tbody>
                  {certificates.map(cert => (
                    <tr key={cert.id}>
                      <td style={{ fontFamily: 'monospace', fontWeight: 600, color: 'var(--accent-400)' }}>{cert.code}</td>
                      <td style={{ color: 'var(--sand-100)', fontWeight: 500 }}>{cert.participantName}</td>
                      <td style={{ color: 'var(--sand-300)' }}>{cert.eventName}</td>
                      <td>{cert.durationHours}h</td>
                      <td style={{ fontSize: '0.8rem', color: 'var(--stone-400)' }}>{cert.issuedAt}</td>
                      <td>
                        <span className={`badge ${cert.status === 'VALIDO' ? 'badge-emerald' : 'badge-rose'}`}>
                          {cert.status === 'VALIDO' ? 'Válido y Verificable' : 'Revocado'}
                        </span>
                      </td>
                      <td style={{ fontSize: '0.82rem', color: 'var(--stone-400)' }}>
                        {cert.status === 'REVOCADO' ? (
                          <span style={{ color: 'var(--error-400)' }}>{cert.revocationReason} (por {cert.revokedBy})</span>
                        ) : '—'}
                      </td>
                      <td style={{ textAlign: 'right' }}>
                        {cert.status === 'VALIDO' ? (
                          <button
                            className="btn btn-danger btn-sm"
                            onClick={() => {
                              setShowRevokeModal(cert);
                              setRevokeReason('');
                            }}
                          >
                            <span style={{ width: 14, height: 14 }}><Icon.Ban /></span> Revocar
                          </button>
                        ) : (
                          <span className="badge badge-slate" style={{ fontSize: '0.72rem' }}>Inactivo</span>
                        )}
                      </td>
                    </tr>
                  ))}
                  {certificates.length === 0 && (
                    <tr>
                      <td colSpan={8} style={{ textAlign: 'center', padding: 30, color: 'var(--stone-500)' }}>
                        No hay certificados registrados para supervisión.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* ── MODAL: USUARIO (Crear / Editar roles) ── */}
      {showUserModal && (
        <div className="modal-backdrop">
          <div className="modal-card animate-modal" style={{ maxWidth: 520 }}>
            <div className="modal-header">
              <h3 className="modal-title">{editingUser ? 'Editar Cuenta y Roles' : 'Crear Nueva Cuenta'}</h3>
              <button className="btn-close" onClick={() => setShowUserModal(false)}><span style={{ width: 16, height: 16 }}><Icon.X /></span></button>
            </div>
            <form onSubmit={handleSaveUser}>
              <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                <div className="form-group">
                  <label className="form-label">Correo Electrónico Institucional</label>
                  <input
                    type="email"
                    required
                    className="input"
                    placeholder="carlos@syncevent.com"
                    value={userFormEmail}
                    onChange={e => setUserFormEmail(e.target.value)}
                  />
                </div>
                {!editingUser && (
                  <div className="form-group">
                    <label className="form-label">Contraseña inicial</label>
                    <input
                      type="password"
                      required
                      minLength={8}
                      maxLength={128}
                      className="input"
                      value={userFormPassword}
                      onChange={e => setUserFormPassword(e.target.value)}
                    />
                  </div>
                )}
                {editingUser && (
                  <div className="form-group">
                    <label className="form-label">Nueva contraseña (opcional)</label>
                    <input
                      type="password"
                      minLength={8}
                      maxLength={128}
                      className="input"
                      placeholder="Déjala vacía para conservar la contraseña actual"
                      value={userFormPassword}
                      onChange={e => setUserFormPassword(e.target.value)}
                    />
                    <p style={{ margin: '6px 0 0', fontSize: '0.76rem', color: 'var(--stone-400)' }}>
                      Si escribes una contraseña nueva, sus sesiones se cierran y deberá volver a iniciar sesión.
                    </p>
                  </div>
                )}
                <div className="form-group" ref={roleDropdownRef} style={{ position: 'relative' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <label className="form-label" style={{ marginBottom: 4 }}>
                      Roles asignados
                    </label>
                    <span style={{ fontSize: '0.74rem', color: 'var(--stone-400)' }}>
                      {userFormRoles.length} seleccionado{userFormRoles.length === 1 ? '' : 's'}
                    </span>
                  </div>
                  <button
                    type="button"
                    className="select"
                    onClick={() => setShowRoleDropdown(prev => !prev)}
                    style={{
                      width: '100%',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      minHeight: 44,
                      padding: '8px 12px',
                      background: 'var(--surface-2)',
                      border: '1px solid var(--glass-border)',
                      borderRadius: 'var(--radius-sm)',
                      cursor: 'pointer',
                      textAlign: 'left'
                    }}
                  >
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, alignItems: 'center', flex: 1, minWidth: 0 }}>
                      {userFormRoles.length === 0 ? (
                        <span style={{ color: 'var(--stone-400)', fontSize: '0.84rem' }}>Selecciona los roles...</span>
                      ) : (
                        userFormRoles.map(r => (
                          <span
                            key={r}
                            className={`badge ${r === 'ADMINISTRADOR' ? 'badge-accent' : r === 'OPERADOR' ? 'badge-amber' : r === 'MONITOR' ? 'badge-emerald' : 'badge-slate'}`}
                            style={{ fontSize: '0.74rem', pointerEvents: 'none' }}
                          >
                            {availableRoles.find(ar => ar.role === r)?.name || r}
                          </span>
                        ))
                      )}
                    </div>
                    <span style={{ fontSize: '0.75rem', color: 'var(--stone-400)', marginLeft: 8 }}>
                      {showRoleDropdown ? '▲' : '▼'}
                    </span>
                  </button>

                  {showRoleDropdown && (
                    <div
                      style={{
                        position: 'absolute',
                        top: '100%',
                        left: 0,
                        right: 0,
                        zIndex: 60,
                        marginTop: 4,
                        background: 'var(--surface-1, #1e2430)',
                        border: '1px solid var(--glass-border)',
                        borderRadius: 'var(--radius-sm)',
                        boxShadow: '0 10px 25px rgba(0,0,0,0.5)',
                        padding: 6,
                        display: 'flex',
                        flexDirection: 'column',
                        gap: 4
                      }}
                    >
                      {availableRoles.map(item => {
                        const isChecked = userFormRoles.includes(item.role);
                        return (
                          <div
                            key={item.role}
                            onClick={() => handleToggleFormRole(item.role)}
                            style={{
                              display: 'flex',
                              alignItems: 'center',
                              gap: 10,
                              padding: '8px 12px',
                              borderRadius: 'var(--radius-xs)',
                              cursor: 'pointer',
                              background: isChecked ? 'rgba(90,143,179,0.18)' : 'transparent',
                              border: isChecked ? '1px solid var(--accent-400)' : '1px solid transparent',
                              transition: 'all 0.15s ease'
                            }}
                          >
                            <input
                              type="checkbox"
                              checked={isChecked}
                              readOnly
                              style={{ accentColor: 'var(--accent-400)', width: 16, height: 16, cursor: 'pointer' }}
                            />
                            <div style={{ flex: 1, minWidth: 0 }}>
                              <div style={{ fontSize: '0.84rem', fontWeight: isChecked ? 600 : 400, color: isChecked ? 'var(--sand-100)' : 'var(--stone-300)' }}>
                                {item.name}
                              </div>
                              <div style={{ fontSize: '0.72rem', color: 'var(--stone-500)' }}>
                                {item.description}
                              </div>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  )}
                  <p style={{ margin: '6px 0 0', fontSize: '0.74rem', color: 'var(--stone-400)' }}>
                    Haz clic directamente en cada rol para agregarlo o quitarlo (sin necesidad de Ctrl).
                  </p>
                </div>
              </div>
              <div className="modal-footer">
                <button type="button" className="btn btn-ghost" onClick={() => setShowUserModal(false)}>Cancelar</button>
                <button type="submit" className="btn btn-primary">{editingUser ? 'Guardar Cambios' : 'Crear Usuario'}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ── MODAL: RESTABLECER CONTRASEÑA CONTROLADO POR ADMINISTRADOR (HU-002) ── */}
      {resetUser && (
        <div className="modal-backdrop">
          <div className="modal-card animate-modal" style={{ maxWidth: 480 }}>
            <div className="modal-header">
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <span style={{ width: 20, height: 20, color: 'var(--accent-400)' }}><Icon.Key /></span>
                <h3 className="modal-title" style={{ margin: 0 }}>Restablecer Contraseña</h3>
              </div>
              <button className="btn-close" onClick={() => setResetUser(null)}>
                <span style={{ width: 16, height: 16 }}><Icon.X /></span>
              </button>
            </div>

            <form onSubmit={handleConfirmResetPassword}>
              <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                <div style={{ padding: '12px 16px', background: 'var(--surface-2)', borderRadius: 'var(--radius-sm)', border: '1px solid var(--glass-border)' }}>
                  <div style={{ fontSize: '0.8rem', color: 'var(--stone-400)' }}>Cuenta de usuario:</div>
                  <div style={{ fontWeight: 600, color: 'var(--sand-100)', marginTop: 2 }}>{resetUser.email}</div>
                  <div style={{ display: 'flex', gap: 6, marginTop: 6 }}>
                    {resetUser.roles.map(r => (
                      <span key={r} className="badge badge-accent" style={{ fontSize: '0.7rem' }}>{r}</span>
                    ))}
                    <span className={`badge ${resetUser.active ? 'badge-emerald' : 'badge-rose'}`} style={{ fontSize: '0.7rem' }}>
                      {resetUser.active ? 'Activa' : 'Desactivada'}
                    </span>
                  </div>
                </div>

                {/* Banner de seguridad */}
                <div style={{ display: 'flex', gap: 10, padding: '10px 14px', background: 'rgba(234,179,8,0.1)', border: '1px solid rgba(234,179,8,0.3)', borderRadius: 'var(--radius-sm)', color: 'var(--sand-200)', fontSize: '0.82rem' }}>
                  <span style={{ width: 18, height: 18, color: '#eab308', flexShrink: 0 }}><Icon.AlertTriangle /></span>
                  <div>
                    <strong>Medida de seguridad:</strong> Al confirmar el cambio, todas las sesiones abiertas de esta cuenta se cerrarán de inmediato en el servidor.
                  </div>
                </div>

                <div className="form-group">
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <label className="form-label">Nueva Contraseña</label>
                    <button
                      type="button"
                      className="btn btn-ghost btn-sm"
                      style={{ fontSize: '0.74rem', padding: '2px 8px', height: 'auto', color: 'var(--accent-400)' }}
                      onClick={handleGeneratePassword}
                    >
                      Generar aleatoria
                    </button>
                  </div>
                  <div style={{ position: 'relative' }}>
                    <input
                      type={showPasswordText ? 'text' : 'password'}
                      required
                      minLength={8}
                      maxLength={128}
                      className="input"
                      placeholder="Mínimo 8 caracteres"
                      value={resetNewPassword}
                      onChange={e => setResetNewPassword(e.target.value)}
                    />
                    <button
                      type="button"
                      tabIndex={-1}
                      onClick={() => setShowPasswordText(!showPasswordText)}
                      style={{ position: 'absolute', right: 10, top: '50%', transform: 'translateY(-50%)', background: 'none', border: 'none', cursor: 'pointer', color: 'var(--stone-400)' }}
                      title={showPasswordText ? 'Ocultar' : 'Mostrar'}
                    >
                      <span style={{ width: 16, height: 16 }}><Icon.Eye /></span>
                    </button>
                  </div>
                </div>

                <div className="form-group">
                  <label className="form-label">Confirmar Contraseña</label>
                  <input
                    type={showPasswordText ? 'text' : 'password'}
                    required
                    minLength={8}
                    maxLength={128}
                    className="input"
                    placeholder="Repite la nueva contraseña"
                    value={resetConfirmPassword}
                    onChange={e => setResetConfirmPassword(e.target.value)}
                  />
                </div>

                {/* Checklist de validación visual */}
                <div style={{ fontSize: '0.78rem', display: 'flex', flexDirection: 'column', gap: 4 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6, color: resetNewPassword.length >= 8 ? 'var(--success-400)' : 'var(--stone-500)' }}>
                    <span style={{ width: 14, height: 14 }}>
                      {resetNewPassword.length >= 8 ? <Icon.Check /> : <Icon.Ban />}
                    </span>
                    Longitud entre 8 y 128 caracteres
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6, color: resetNewPassword && resetNewPassword === resetConfirmPassword ? 'var(--success-400)' : 'var(--stone-500)' }}>
                    <span style={{ width: 14, height: 14 }}>
                      {resetNewPassword && resetNewPassword === resetConfirmPassword ? <Icon.Check /> : <Icon.Ban />}
                    </span>
                    Las contraseñas coinciden
                  </div>
                </div>
              </div>

              <div className="modal-footer">
                <button type="button" className="btn btn-ghost" onClick={() => setResetUser(null)}>
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={isResetting || resetNewPassword.length < 8 || resetNewPassword !== resetConfirmPassword}
                >
                  {isResetting ? (
                    <>
                      <span style={{ width: 14, height: 14, marginRight: 6 }}><Icon.Loader /></span>
                      Restableciendo...
                    </>
                  ) : (
                    <>
                      <span style={{ width: 14, height: 14, marginRight: 6 }}><Icon.Key /></span>
                      Confirmar Restablecimiento
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ── MODAL: RESOLVER MULTA (RN-24) ── */}
      {resolvingFine && (
        <div className="modal-backdrop">
          <div className="modal-card animate-modal" style={{ maxWidth: 480 }}>
            <div className="modal-header">
              <h3 className="modal-title">Resolver Multa y Rehabilitar Participante</h3>
              <button className="btn-close" onClick={() => setResolvingFine(null)}><span style={{ width: 16, height: 16 }}><Icon.X /></span></button>
            </div>
            <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
              <p style={{ margin: 0, fontSize: '0.88rem', color: 'var(--sand-200)' }}>
                ¿Deseas levantar la sanción aplicada a <strong>{resolvingFine.participantName}</strong> ({resolvingFine.documentNumber})?
              </p>
              <p style={{ margin: 0, fontSize: '0.82rem', color: 'var(--stone-400)' }}>
                La resolución quedará registrada con tu cuenta y la fecha de la operación.
              </p>
            </div>
            <div className="modal-footer">
              <button className="btn btn-ghost" onClick={() => setResolvingFine(null)}>Cancelar</button>
              <button
                className="btn btn-primary"
                onClick={async () => {
                  if (!await onResolveFine(resolvingFine.id)) return;
                  addToast(`Multa resuelta para ${resolvingFine.participantName}. Participante rehabilitado.`, 'success');
                  setResolvingFine(null);
                }}
              >
                Confirmar y Rehabilitar
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ── MODAL: REVOCAR CERTIFICADO (RN-10) ── */}
      {showRevokeModal && (
        <div className="modal-backdrop">
          <div className="modal-card animate-modal" style={{ maxWidth: 480 }}>
            <div className="modal-header">
              <h3 className="modal-title" style={{ color: 'var(--error-400)' }}>Revocar Certificado Oficial</h3>
              <button className="btn-close" onClick={() => setShowRevokeModal(null)}><span style={{ width: 16, height: 16 }}><Icon.X /></span></button>
            </div>
            <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
              <p style={{ margin: 0, fontSize: '0.88rem', color: 'var(--sand-200)' }}>
                Se revocará el certificado <strong>{showRevokeModal.code}</strong> expedido a <strong>{showRevokeModal.participantName}</strong> por el evento <em>{showRevokeModal.eventName}</em>.
              </p>
              <div className="form-group">
                <label className="form-label">Motivo de revocación</label>
                <textarea
                  required
                  className="input"
                  rows={3}
                  value={revokeReason}
                  onChange={e => setRevokeReason(e.target.value)}
                  placeholder="Ej. Incumplimiento de horas mínimas de práctica o error en documento de identidad."
                />
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn btn-ghost" onClick={() => setShowRevokeModal(null)}>Cancelar</button>
              <button
                className="btn btn-danger"
                disabled={!revokeReason.trim()}
                onClick={async () => {
                  if (!await onRevokeCertificate(showRevokeModal.id, revokeReason)) return;
                  addToast(`Certificado ${showRevokeModal.code} revocado exitosamente`, 'warning');
                  setShowRevokeModal(null);
                }}
              >
                Confirmar Revocación
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
