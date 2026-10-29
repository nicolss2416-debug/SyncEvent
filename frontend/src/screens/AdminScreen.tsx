import React, { useState } from 'react';
import { Event, UserAccount, AuditLog, FineRecord, CertificateRecord, UserRole } from '../types';
import { Icon } from '../icons';

interface AdminScreenProps {
  events: Event[];
  users: UserAccount[];
  auditLogs: AuditLog[];
  fines: FineRecord[];
  certificates: CertificateRecord[];
  onAddUser: (user: Omit<UserAccount, 'id' | 'createdAt'>) => void;
  onUpdateUser: (id: string, updates: Partial<UserAccount>) => void;
  onToggleUserStatus: (id: string) => void;
  onResolveFine: (fineId: string, notes: string) => void;
  onRevokeCertificate: (certId: string, reason: string) => void;
  addToast: (msg: string, type?: 'success' | 'info' | 'error' | 'warning') => void;
}

export function AdminScreen({
  events,
  users,
  auditLogs,
  fines,
  certificates,
  onAddUser,
  onUpdateUser,
  onToggleUserStatus,
  onResolveFine,
  onRevokeCertificate,
  addToast,
}: AdminScreenProps) {
  const [activeTab, setActiveTab] = useState<'dashboard' | 'users' | 'audit' | 'fines' | 'certificates'>('dashboard');

  // User modal state
  const [showUserModal, setShowUserModal] = useState(false);
  const [editingUser, setEditingUser] = useState<UserAccount | null>(null);
  const [userFormName, setUserFormName] = useState('');
  const [userFormEmail, setUserFormEmail] = useState('');
  const [userFormRoles, setUserFormRoles] = useState<UserRole[]>(['INVITADO']);

  // Revoke modal state
  const [showRevokeModal, setShowRevokeModal] = useState<CertificateRecord | null>(null);
  const [revokeReason, setRevokeReason] = useState('');

  // Fine resolve modal
  const [resolvingFine, setResolvingFine] = useState<FineRecord | null>(null);
  const [resolveNotes, setResolveNotes] = useState('');

  // Audit filter state
  const [auditFilterEntity, setAuditFilterEntity] = useState<string>('ALL');
  const [auditSearch, setAuditSearch] = useState<string>('');

  // Attendance policy settings state
  const [absenceThreshold, setAbsenceThreshold] = useState(3);
  const [absenceWindowDays, setAbsenceWindowDays] = useState(90);

  const activeAdminsCount = users.filter(u => u.roles.includes('ADMINISTRADOR') && u.active).length;

  const handleOpenCreateUser = () => {
    setEditingUser(null);
    setUserFormName('');
    setUserFormEmail('');
    setUserFormRoles(['INVITADO']);
    setShowUserModal(true);
  };

  const handleOpenEditUser = (u: UserAccount) => {
    setEditingUser(u);
    setUserFormName(u.name);
    setUserFormEmail(u.email);
    setUserFormRoles([...u.roles]);
    setShowUserModal(true);
  };

  const handleSaveUser = (e: React.FormEvent) => {
    e.preventDefault();
    if (!userFormName.trim() || !userFormEmail.trim()) {
      addToast('Nombre y correo son requeridos', 'warning');
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
        addToast('Regla RN-18: No se puede quitar el rol de Administrador al único administrador activo del sistema', 'error');
        return;
      }
      onUpdateUser(editingUser.id, {
        name: userFormName,
        email: userFormEmail,
        roles: userFormRoles,
      });
      addToast(`Usuario "${userFormName}" actualizado correctamente`, 'success');
    } else {
      // Check duplicate email
      if (users.some(u => u.email.toLowerCase() === userFormEmail.toLowerCase())) {
        addToast(`El correo "${userFormEmail}" ya está registrado en la plataforma`, 'error');
        return;
      }
      onAddUser({
        name: userFormName,
        email: userFormEmail,
        roles: userFormRoles,
        active: true,
      });
      addToast(`Usuario "${userFormName}" creado con éxito`, 'success');
    }
    setShowUserModal(false);
  };

  const handleToggleStatusWithCheck = (user: UserAccount) => {
    // RN-18: Cannot deactivate last admin
    if (user.active && user.roles.includes('ADMINISTRADOR') && activeAdminsCount <= 1) {
      addToast('Regla RN-18: El sistema no puede quedarse sin Administrador activo. Operación denegada.', 'error');
      return;
    }
    onToggleUserStatus(user.id);
    addToast(`Estado de ${user.name} actualizado a ${user.active ? 'Inactivo' : 'Activo'}`, 'info');
  };

  const filteredAuditLogs = auditLogs.filter(log => {
    const matchEntity = auditFilterEntity === 'ALL' || log.entityType === auditFilterEntity;
    const matchText = auditSearch === '' || 
      `${log.action} ${log.userEmail} ${log.details || ''} ${log.reason || ''}`.toLowerCase().includes(auditSearch.toLowerCase());
    return matchEntity && matchText;
  });

  return (
    <div className="actor-screen admin-screen animate-fade-in">
      {/* ── Actor Badge & Header ── */}
      <div className="actor-header-banner" style={{ background: 'linear-gradient(135deg, #FFFFFF 0%, #FAF5ED 100%)', border: '1px solid rgba(186,160,130,0.35)', borderRadius: 'var(--radius-lg)', padding: '24px 28px', marginBottom: 28, boxShadow: 'var(--shadow-sm)' }}>
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
                Gestión de cuentas y roles (HU-002), auditoría inalterable (HU-023), políticas de inasistencias/multas (RN-24) y revocación de certificados.
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
            <div className="stat-card">
              <div className="stat-card-accent" style={{ background: 'linear-gradient(90deg, #3d7399, #1f4261)' }} />
              <div className="stat-card-top">
                <span className="stat-card-label">Usuarios del Sistema</span>
                <div className="stat-card-icon" style={{ background: 'rgba(61,115,153,0.15)', color: '#5a8fb3' }}>
                  <span style={{ width: 20, height: 20 }}><Icon.Users /></span>
                </div>
              </div>
              <div className="stat-card-value">{users.length}</div>
              <div className="stat-card-change" style={{ color: 'var(--success-400)' }}>{activeAdminsCount} Administradores activos</div>
            </div>

            <div className="stat-card">
              <div className="stat-card-accent" style={{ background: 'linear-gradient(90deg, #eab308, #ca8a04)' }} />
              <div className="stat-card-top">
                <span className="stat-card-label">Multas por Inasistencia</span>
                <div className="stat-card-icon" style={{ background: 'rgba(234,179,8,0.15)', color: '#facc15' }}>
                  <span style={{ width: 20, height: 20 }}><Icon.ShieldAlert /></span>
                </div>
              </div>
              <div className="stat-card-value">{fines.filter(f => f.status === 'ACTIVA').length}</div>
              <div className="stat-card-change" style={{ color: 'var(--warning-400)' }}>Umbral: {absenceThreshold} inasistencias en {absenceWindowDays} días</div>
            </div>

            <div className="stat-card">
              <div className="stat-card-accent" style={{ background: 'linear-gradient(90deg, #22c55e, #16a34a)' }} />
              <div className="stat-card-top">
                <span className="stat-card-label">Eventos en la Red</span>
                <div className="stat-card-icon" style={{ background: 'rgba(34,197,94,0.15)', color: '#4ade80' }}>
                  <span style={{ width: 20, height: 20 }}><Icon.Calendar /></span>
                </div>
              </div>
              <div className="stat-card-value">{events.length}</div>
              <div className="stat-card-change" style={{ color: 'var(--success-400)' }}>{events.filter(e => e.status === 'PUBLICADO' || e.status === 'EN_CURSO').length} activos para el público</div>
            </div>

            <div className="stat-card">
              <div className="stat-card-accent" style={{ background: 'linear-gradient(90deg, #80accc, #3d7399)' }} />
              <div className="stat-card-top">
                <span className="stat-card-label">Registros de Auditoría</span>
                <div className="stat-card-icon" style={{ background: 'rgba(90,143,179,0.15)', color: '#80accc' }}>
                  <span style={{ width: 20, height: 20 }}><Icon.Activity /></span>
                </div>
              </div>
              <div className="stat-card-value">{auditLogs.length}</div>
              <div className="stat-card-change" style={{ color: 'var(--info-400)' }}>Bitácora inalterable (RN-12)</div>
            </div>
          </div>

          {/* Quick status & audit sneak-peek */}
          <div className="grid-2-1" style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: 24 }}>
            <div className="card">
              <div className="card-header" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <h3 className="card-title"><span style={{ width: 18, height: 18, color: 'var(--accent-400)' }}><Icon.Activity /></span> Eventos Recientes de Auditoría</h3>
                <button className="btn btn-ghost btn-sm" onClick={() => setActiveTab('audit')}>Ver bitácora completa →</button>
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
                    {auditLogs.slice(0, 5).map(log => (
                      <tr key={log.id}>
                        <td style={{ fontSize: '0.8rem', color: 'var(--stone-400)' }}>{log.timestamp}</td>
                        <td style={{ fontWeight: 500, color: 'var(--sand-200)' }}>{log.userEmail}</td>
                        <td><span className="badge badge-accent" style={{ fontSize: '0.74rem' }}>{log.action}</span></td>
                        <td><span className="badge badge-slate" style={{ fontSize: '0.74rem' }}>{log.entityType}</span></td>
                        <td>
                          <span className={`badge ${log.result === 'EXITO' ? 'badge-emerald' : 'badge-rose'}`} style={{ fontSize: '0.72rem' }}>
                            {log.result}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            <div className="card">
              <div className="card-header">
                <h3 className="card-title"><span style={{ width: 18, height: 18, color: 'var(--accent-400)' }}><Icon.Settings /></span> Política de Inasistencias</h3>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                <p style={{ margin: 0, fontSize: '0.85rem', color: 'var(--stone-300)' }}>
                  Regla <strong>RN-24</strong>: Los participantes que acumulen inasistencias no justificadas reciben una multa automática y quedan inhabilitados para nuevas inscripciones.
                </p>
                <div className="form-group">
                  <label className="form-label">Inasistencias máximas permitidas:</label>
                  <input
                    type="number"
                    min="1"
                    max="10"
                    className="input"
                    value={absenceThreshold}
                    onChange={e => setAbsenceThreshold(parseInt(e.target.value) || 3)}
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">Ventana de evaluación (días):</label>
                  <input
                    type="number"
                    min="30"
                    max="365"
                    className="input"
                    value={absenceWindowDays}
                    onChange={e => setAbsenceWindowDays(parseInt(e.target.value) || 90)}
                  />
                </div>
                <button className="btn btn-secondary btn-sm" onClick={() => addToast('Parámetros de política de inasistencia actualizados', 'success')}>
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
                  Control exclusivo del Administrador (HU-002). Permite múltiples roles (RN-14) y protege al último Administrador (RN-18).
                </p>
              </div>
              <button className="btn btn-primary" onClick={handleOpenCreateUser}>
                <span style={{ width: 16, height: 16 }}><Icon.Plus /></span> Crear Nuevo Usuario
              </button>
            </div>

            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>Nombre / Identificación</th>
                    <th>Correo Electrónico</th>
                    <th>Roles Asignados (RN-14)</th>
                    <th>Estado de Cuenta</th>
                    <th>Creado</th>
                    <th style={{ textAlign: 'right' }}>Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {users.map(u => (
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
                <h3 className="card-title">Bitácora de Auditoría Inalterable (RN-11, RN-12)</h3>
                <p style={{ margin: '4px 0 0', fontSize: '0.84rem', color: 'var(--stone-400)' }}>
                  Registro automático e inalterable: quién, qué hizo, entidad, fecha/hora, IP, resultado y motivo en cancelaciones/revocaciones.
                </p>
              </div>
              <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
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
                  Regla RN-24: Participantes con inasistencias consecutivas bloqueados para nuevas inscripciones hasta que el Administrador resuelva la sanción.
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
                            onClick={() => {
                              setResolvingFine(fine);
                              setResolveNotes('Justificación médica / académica verificada');
                            }}
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
                Regla RN-10: Solo el Administrador puede revocar certificados emitidos indicando el motivo justificado. El certificado conserva trazabilidad histórica.
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
                  <label className="form-label">Nombre Completo</label>
                  <input
                    type="text"
                    required
                    className="input"
                    placeholder="Ej. Carlos Mendoza"
                    value={userFormName}
                    onChange={e => setUserFormName(e.target.value)}
                  />
                </div>
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
                <div className="form-group">
                  <label className="form-label">Roles Asignados (RN-14: Soporta Roles Múltiples)</label>
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, marginTop: 6 }}>
                    {(['ADMINISTRADOR', 'OPERADOR', 'MONITOR', 'INVITADO'] as UserRole[]).map(r => {
                      const isChecked = userFormRoles.includes(r);
                      return (
                        <label
                          key={r}
                          style={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: 10,
                            padding: '10px 14px',
                            background: isChecked ? 'rgba(90,143,179,0.15)' : 'var(--surface-2)',
                            border: `1px solid ${isChecked ? 'var(--accent-400)' : 'var(--glass-border)'}`,
                            borderRadius: 'var(--radius-sm)',
                            cursor: 'pointer',
                            fontSize: '0.84rem',
                            color: isChecked ? 'var(--sand-100)' : 'var(--stone-400)',
                            fontWeight: isChecked ? 600 : 400
                          }}
                        >
                          <input
                            type="checkbox"
                            checked={isChecked}
                            onChange={() => {
                              if (isChecked) {
                                setUserFormRoles(prev => prev.filter(x => x !== r));
                              } else {
                                setUserFormRoles(prev => [...prev, r]);
                              }
                            }}
                          />
                          {r}
                        </label>
                      );
                    })}
                  </div>
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
              <div className="form-group">
                <label className="form-label">Justificación / Motivo de Resolución:</label>
                <textarea
                  className="input"
                  rows={3}
                  value={resolveNotes}
                  onChange={e => setResolveNotes(e.target.value)}
                  placeholder="Ej. Incapacidad médica validada o reprogramación autorizada."
                />
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn btn-ghost" onClick={() => setResolvingFine(null)}>Cancelar</button>
              <button
                className="btn btn-primary"
                onClick={() => {
                  onResolveFine(resolvingFine.id, resolveNotes);
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
                <label className="form-label">Motivo Obligatorio de Revocación (RN-10):</label>
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
                onClick={() => {
                  onRevokeCertificate(showRevokeModal.id, revokeReason);
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
