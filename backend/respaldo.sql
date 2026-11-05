--
-- PostgreSQL database dump
--

\restrict Fn3dyGdJDUqXN6YhVTAlIuESCXcKE31sBFHbUdxwIqePdad7NylXnslSS04RPhq

-- Dumped from database version 18.6
-- Dumped by pg_dump version 18.6

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: public; Type: SCHEMA; Schema: -; Owner: postgres
--

-- *not* creating schema, since initdb creates it


ALTER SCHEMA public OWNER TO postgres;

--
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: postgres
--

COMMENT ON SCHEMA public IS '';


--
-- Name: impedir_mutacion_auditoria(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.impedir_mutacion_auditoria() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    RAISE EXCEPTION 'Los registros de auditoría son inmutables'
      USING ERRCODE = '55000';
END;
$$;


ALTER FUNCTION public.impedir_mutacion_auditoria() OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: app_role; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.app_role (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    nombre character varying(30) NOT NULL,
    descripcion character varying(200),
    CONSTRAINT ck_app_role_nombre CHECK (((nombre)::text = ANY (ARRAY[('ADMINISTRADOR'::character varying)::text, ('OPERADOR'::character varying)::text, ('MONITOR'::character varying)::text, ('INVITADO'::character varying)::text])))
);


ALTER TABLE public.app_role OWNER TO postgres;

--
-- Name: app_user; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.app_user (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    nombres character varying(120) NOT NULL,
    correo character varying(254) NOT NULL,
    password_hash character varying(255) NOT NULL,
    activo boolean DEFAULT true NOT NULL,
    intentos_fallidos integer DEFAULT 0 NOT NULL,
    bloqueado_hasta timestamp with time zone,
    ultimo_acceso timestamp with time zone,
    creado_en timestamp with time zone DEFAULT now() NOT NULL,
    actualizado_en timestamp with time zone DEFAULT now() NOT NULL,
    version integer DEFAULT 0 NOT NULL,
    CONSTRAINT app_user_intentos_fallidos_check CHECK ((intentos_fallidos >= 0)),
    CONSTRAINT app_user_nombres_check CHECK ((length(btrim((nombres)::text)) > 0)),
    CONSTRAINT app_user_version_check CHECK ((version >= 0)),
    CONSTRAINT ck_app_user_correo CHECK (((correo)::text = lower(btrim((correo)::text))))
);


ALTER TABLE public.app_user OWNER TO postgres;

--
-- Name: COLUMN app_user.password_hash; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.app_user.password_hash IS 'Guardar únicamente hash seguro generado por el backend; nunca contraseña en texto plano.';


--
-- Name: asistencia; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.asistencia (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    inscripcion_id uuid NOT NULL,
    registrada_en timestamp with time zone DEFAULT now() NOT NULL,
    metodo character varying(10) NOT NULL,
    registrada_por uuid NOT NULL,
    observacion character varying(500),
    CONSTRAINT asistencia_metodo_check CHECK (((metodo)::text = ANY (ARRAY[('QR'::character varying)::text, ('MANUAL'::character varying)::text])))
);


ALTER TABLE public.asistencia OWNER TO postgres;

--
-- Name: attendance_absences; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.attendance_absences (
    id uuid NOT NULL,
    event_id uuid NOT NULL,
    participant_id uuid NOT NULL,
    recorded_at timestamp(6) without time zone NOT NULL,
    registration_id uuid NOT NULL
);


ALTER TABLE public.attendance_absences OWNER TO postgres;

--
-- Name: attendance_policy; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.attendance_policy (
    id integer NOT NULL,
    absence_threshold integer NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    updated_by uuid,
    window_days integer NOT NULL
);


ALTER TABLE public.attendance_policy OWNER TO postgres;

--
-- Name: auditoria; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.auditoria (
    id bigint NOT NULL,
    usuario_id uuid,
    usuario_correo character varying(254),
    accion character varying(80) NOT NULL,
    entidad character varying(80) NOT NULL,
    entidad_id character varying(100),
    resultado character varying(15) NOT NULL,
    ip inet,
    detalle jsonb DEFAULT '{}'::jsonb NOT NULL,
    fecha timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT auditoria_resultado_check CHECK (((resultado)::text = ANY (ARRAY[('OK'::character varying)::text, ('ERROR'::character varying)::text, ('DENEGADO'::character varying)::text])))
);


ALTER TABLE public.auditoria OWNER TO postgres;

--
-- Name: TABLE auditoria; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.auditoria IS 'Bitácora append-only de acciones importantes del sistema.';


--
-- Name: auditoria_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.auditoria ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.auditoria_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: auth_activity; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.auth_activity (
    id bigint NOT NULL,
    activity character varying(30) NOT NULL,
    email character varying(254) NOT NULL,
    occurred_at timestamp(6) without time zone NOT NULL,
    user_id uuid
);


ALTER TABLE public.auth_activity OWNER TO postgres;

--
-- Name: auth_activity_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.auth_activity ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.auth_activity_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: auth_sessions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.auth_sessions (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    last_activity_at timestamp(6) without time zone NOT NULL,
    revoked_at timestamp(6) without time zone,
    user_id uuid NOT NULL
);


ALTER TABLE public.auth_sessions OWNER TO postgres;

--
-- Name: certificado; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.certificado (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    inscripcion_id uuid NOT NULL,
    codigo_verificacion character varying(100) NOT NULL,
    hash_contenido character varying(128) NOT NULL,
    estado character varying(10) DEFAULT 'VIGENTE'::character varying NOT NULL,
    emitido_en timestamp with time zone DEFAULT now() NOT NULL,
    emitido_por uuid,
    revocado_en timestamp with time zone,
    revocado_por uuid,
    motivo_revocacion text,
    observaciones text,
    nombre_participante_snapshot character varying(201) NOT NULL,
    evento_nombre_snapshot character varying(150) NOT NULL,
    duracion_horas_snapshot numeric(7,2) NOT NULL,
    evento_fecha_inicio_snapshot timestamp with time zone NOT NULL,
    evento_fecha_fin_snapshot timestamp with time zone NOT NULL,
    pdf_uri text,
    CONSTRAINT certificado_duracion_horas_snapshot_check CHECK ((duracion_horas_snapshot > (0)::numeric)),
    CONSTRAINT certificado_estado_check CHECK (((estado)::text = ANY (ARRAY[('VIGENTE'::character varying)::text, ('REVOCADO'::character varying)::text]))),
    CONSTRAINT ck_certificado_fechas CHECK ((evento_fecha_fin_snapshot > evento_fecha_inicio_snapshot)),
    CONSTRAINT ck_certificado_revocacion CHECK (((((estado)::text = 'VIGENTE'::text) AND (revocado_en IS NULL) AND (revocado_por IS NULL) AND (motivo_revocacion IS NULL)) OR (((estado)::text = 'REVOCADO'::text) AND (revocado_en IS NOT NULL) AND (motivo_revocacion IS NOT NULL) AND (length(btrim(motivo_revocacion)) > 0))))
);


ALTER TABLE public.certificado OWNER TO postgres;

--
-- Name: COLUMN certificado.nombre_participante_snapshot; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.certificado.nombre_participante_snapshot IS 'Nombre al momento de emisión, conservado como evidencia histórica.';


--
-- Name: certificado_verificacion; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.certificado_verificacion (
    id bigint NOT NULL,
    certificado_id uuid NOT NULL,
    verificado_en timestamp with time zone DEFAULT now() NOT NULL,
    resultado character varying(15) NOT NULL,
    hash_consultado character varying(128),
    ip inet,
    CONSTRAINT certificado_verificacion_resultado_check CHECK (((resultado)::text = ANY (ARRAY[('VALIDO'::character varying)::text, ('REVOCADO'::character varying)::text, ('NO_COINCIDE'::character varying)::text])))
);


ALTER TABLE public.certificado_verificacion OWNER TO postgres;

--
-- Name: TABLE certificado_verificacion; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.certificado_verificacion IS 'Historial de verificaciones; totalVerificaciones se obtiene con COUNT(*).';


--
-- Name: certificado_verificacion_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.certificado_verificacion ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.certificado_verificacion_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: certificate_documents; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.certificate_documents (
    id uuid NOT NULL,
    content_hash character varying(128) NOT NULL,
    event_end_at timestamp(6) without time zone NOT NULL,
    event_id uuid NOT NULL,
    event_name character varying(255) NOT NULL,
    event_start_at timestamp(6) without time zone NOT NULL,
    issued_at timestamp(6) without time zone NOT NULL,
    issued_by uuid NOT NULL,
    participant_id uuid NOT NULL,
    participant_name character varying(255) NOT NULL,
    pdf_hash character varying(128) NOT NULL,
    registration_id uuid NOT NULL,
    revocation_reason character varying(255),
    revoked_at timestamp(6) without time zone,
    status character varying(20) NOT NULL,
    verification_code character varying(64) NOT NULL,
    CONSTRAINT certificate_documents_status_check CHECK (((status)::text = ANY ((ARRAY['VIGENTE'::character varying, 'REVOCADO'::character varying])::text[])))
);


ALTER TABLE public.certificate_documents OWNER TO postgres;

--
-- Name: event_attendance; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.event_attendance (
    registration_id uuid NOT NULL,
    checked_in_at timestamp(6) without time zone NOT NULL,
    checked_in_by uuid NOT NULL,
    method character varying(10) NOT NULL,
    CONSTRAINT event_attendance_method_check CHECK (((method)::text = ANY ((ARRAY['QR'::character varying, 'MANUAL'::character varying])::text[])))
);


ALTER TABLE public.event_attendance OWNER TO postgres;

--
-- Name: event_cancellation_history; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.event_cancellation_history (
    id uuid NOT NULL,
    canceled_at timestamp(6) without time zone NOT NULL,
    event_id uuid NOT NULL,
    previous_status character varying(20) NOT NULL,
    reason character varying(2000) NOT NULL,
    CONSTRAINT event_cancellation_history_previous_status_check CHECK (((previous_status)::text = ANY ((ARRAY['BORRADOR'::character varying, 'PUBLICADO'::character varying, 'EN_CURSO'::character varying, 'FINALIZADO'::character varying, 'CANCELADO'::character varying, 'APLAZADO'::character varying])::text[])))
);


ALTER TABLE public.event_cancellation_history OWNER TO postgres;

--
-- Name: event_monitor_assignments; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.event_monitor_assignments (
    event_id uuid NOT NULL,
    monitor_id uuid NOT NULL,
    assigned_at timestamp(6) without time zone NOT NULL,
    assigned_by uuid NOT NULL
);


ALTER TABLE public.event_monitor_assignments OWNER TO postgres;

--
-- Name: event_postponement_history; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.event_postponement_history (
    id bigint NOT NULL,
    action character varying(30) NOT NULL,
    event_id uuid NOT NULL,
    new_end_at timestamp(6) without time zone NOT NULL,
    new_start_at timestamp(6) without time zone NOT NULL,
    previous_end_at timestamp(6) without time zone NOT NULL,
    previous_start_at timestamp(6) without time zone NOT NULL,
    reason character varying(2000) NOT NULL,
    recorded_at timestamp(6) without time zone NOT NULL
);


ALTER TABLE public.event_postponement_history OWNER TO postgres;

--
-- Name: event_postponement_history_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.event_postponement_history ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.event_postponement_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: event_registrations; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.event_registrations (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    created_by uuid NOT NULL,
    qr_token character varying(64) NOT NULL,
    status character varying(20) NOT NULL,
    event_id uuid NOT NULL,
    participant_id uuid NOT NULL,
    CONSTRAINT event_registrations_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVA'::character varying, 'CANCELADA'::character varying])::text[])))
);


ALTER TABLE public.event_registrations OWNER TO postgres;

--
-- Name: evento; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.evento (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    nombre character varying(150) NOT NULL,
    descripcion text,
    tipo character varying(20) NOT NULL,
    modalidad character varying(15) NOT NULL,
    estado character varying(15) DEFAULT 'BORRADOR'::character varying NOT NULL,
    fecha_inicio timestamp with time zone NOT NULL,
    fecha_fin timestamp with time zone NOT NULL,
    lugar character varying(250),
    enlace text,
    aforo integer NOT NULL,
    duracion_horas numeric(7,2) NOT NULL,
    creado_por uuid,
    creado_en timestamp with time zone DEFAULT now() NOT NULL,
    actualizado_en timestamp with time zone DEFAULT now() NOT NULL,
    version integer DEFAULT 0 NOT NULL,
    CONSTRAINT ck_evento_fechas CHECK ((fecha_fin > fecha_inicio)),
    CONSTRAINT ck_evento_modalidad CHECK (((((modalidad)::text = 'PRESENCIAL'::text) AND (NULLIF(btrim((lugar)::text), ''::text) IS NOT NULL)) OR (((modalidad)::text = 'VIRTUAL'::text) AND (NULLIF(btrim(enlace), ''::text) IS NOT NULL)) OR (((modalidad)::text = 'HIBRIDA'::text) AND (NULLIF(btrim((lugar)::text), ''::text) IS NOT NULL) AND (NULLIF(btrim(enlace), ''::text) IS NOT NULL)))),
    CONSTRAINT evento_aforo_check CHECK ((aforo > 0)),
    CONSTRAINT evento_duracion_horas_check CHECK ((duracion_horas > (0)::numeric)),
    CONSTRAINT evento_estado_check CHECK (((estado)::text = ANY (ARRAY[('BORRADOR'::character varying)::text, ('PUBLICADO'::character varying)::text, ('EN_CURSO'::character varying)::text, ('FINALIZADO'::character varying)::text, ('CANCELADO'::character varying)::text]))),
    CONSTRAINT evento_modalidad_check CHECK (((modalidad)::text = ANY (ARRAY[('PRESENCIAL'::character varying)::text, ('VIRTUAL'::character varying)::text, ('HIBRIDA'::character varying)::text]))),
    CONSTRAINT evento_nombre_check CHECK ((length(btrim((nombre)::text)) > 0)),
    CONSTRAINT evento_tipo_check CHECK (((tipo)::text = ANY (ARRAY[('CAPACITACION'::character varying)::text, ('TALLER'::character varying)::text, ('TORNEO'::character varying)::text]))),
    CONSTRAINT evento_version_check CHECK ((version >= 0))
);


ALTER TABLE public.evento OWNER TO postgres;

--
-- Name: events; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.events (
    id uuid NOT NULL,
    capacity integer NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    description text,
    duration_hours numeric(8,2) NOT NULL,
    end_at timestamp(6) without time zone NOT NULL,
    location character varying(250),
    meeting_url character varying(2048),
    modality character varying(20) NOT NULL,
    name character varying(150) NOT NULL,
    start_at timestamp(6) without time zone NOT NULL,
    status character varying(20) NOT NULL,
    type character varying(20) NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    CONSTRAINT events_modality_check CHECK (((modality)::text = ANY ((ARRAY['PRESENCIAL'::character varying, 'VIRTUAL'::character varying, 'HIBRIDA'::character varying])::text[]))),
    CONSTRAINT events_status_check CHECK (((status)::text = ANY ((ARRAY['BORRADOR'::character varying, 'PUBLICADO'::character varying, 'EN_CURSO'::character varying, 'FINALIZADO'::character varying, 'CANCELADO'::character varying, 'APLAZADO'::character varying])::text[]))),
    CONSTRAINT events_type_check CHECK (((type)::text = ANY ((ARRAY['CAPACITACION'::character varying, 'TALLER'::character varying, 'TORNEO'::character varying])::text[])))
);


ALTER TABLE public.events OWNER TO postgres;

--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


ALTER TABLE public.flyway_schema_history OWNER TO postgres;

--
-- Name: importacion_csv; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.importacion_csv (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    evento_id uuid NOT NULL,
    archivo_nombre character varying(255) NOT NULL,
    estado character varying(15) DEFAULT 'CARGADA'::character varying NOT NULL,
    total_filas integer DEFAULT 0 NOT NULL,
    filas_validas integer DEFAULT 0 NOT NULL,
    filas_invalidas integer DEFAULT 0 NOT NULL,
    creado_por uuid,
    creado_en timestamp with time zone DEFAULT now() NOT NULL,
    confirmada_en timestamp with time zone,
    CONSTRAINT ck_importacion_totales CHECK (((filas_validas + filas_invalidas) <= total_filas)),
    CONSTRAINT importacion_csv_estado_check CHECK (((estado)::text = ANY (ARRAY[('CARGADA'::character varying)::text, ('VALIDADA'::character varying)::text, ('CONFIRMADA'::character varying)::text, ('FALLIDA'::character varying)::text]))),
    CONSTRAINT importacion_csv_filas_invalidas_check CHECK ((filas_invalidas >= 0)),
    CONSTRAINT importacion_csv_filas_validas_check CHECK ((filas_validas >= 0)),
    CONSTRAINT importacion_csv_total_filas_check CHECK ((total_filas >= 0))
);


ALTER TABLE public.importacion_csv OWNER TO postgres;

--
-- Name: importacion_csv_fila; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.importacion_csv_fila (
    id bigint NOT NULL,
    importacion_id uuid NOT NULL,
    numero_fila integer NOT NULL,
    datos jsonb NOT NULL,
    valida boolean DEFAULT false NOT NULL,
    errores jsonb DEFAULT '[]'::jsonb NOT NULL,
    participante_id uuid,
    inscripcion_id uuid,
    CONSTRAINT importacion_csv_fila_numero_fila_check CHECK ((numero_fila > 0))
);


ALTER TABLE public.importacion_csv_fila OWNER TO postgres;

--
-- Name: importacion_csv_fila_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.importacion_csv_fila ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.importacion_csv_fila_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: inscripcion; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.inscripcion (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    evento_id uuid NOT NULL,
    participante_id uuid NOT NULL,
    fecha timestamp with time zone DEFAULT now() NOT NULL,
    estado character varying(12) DEFAULT 'ACTIVA'::character varying NOT NULL,
    token_qr uuid DEFAULT gen_random_uuid() NOT NULL,
    creada_por uuid,
    cancelada_en timestamp with time zone,
    CONSTRAINT ck_inscripcion_cancelacion CHECK (((((estado)::text = 'ACTIVA'::text) AND (cancelada_en IS NULL)) OR (((estado)::text = 'CANCELADA'::text) AND (cancelada_en IS NOT NULL)))),
    CONSTRAINT inscripcion_estado_check CHECK (((estado)::text = ANY (ARRAY[('ACTIVA'::character varying)::text, ('CANCELADA'::character varying)::text])))
);


ALTER TABLE public.inscripcion OWNER TO postgres;

--
-- Name: TABLE inscripcion; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.inscripcion IS 'Relación entre evento y participante; token_qr identifica la inscripción.';


--
-- Name: participant_change_history; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.participant_change_history (
    id bigint NOT NULL,
    changed_at timestamp(6) without time zone NOT NULL,
    changed_by character varying(150) NOT NULL,
    participant_id uuid NOT NULL
);


ALTER TABLE public.participant_change_history OWNER TO postgres;

--
-- Name: participant_change_history_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.participant_change_history ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.participant_change_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: participant_fines; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.participant_fines (
    id uuid NOT NULL,
    absence_threshold integer NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    participant_id uuid NOT NULL,
    resolved_at timestamp(6) without time zone,
    resolved_by uuid,
    status character varying(20) NOT NULL,
    triggered_by_absence_id uuid NOT NULL,
    window_days integer NOT NULL,
    CONSTRAINT participant_fines_status_check CHECK (((status)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'RESUELTA'::character varying])::text[])))
);


ALTER TABLE public.participant_fines OWNER TO postgres;

--
-- Name: participante; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.participante (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tipo_documento character varying(20) NOT NULL,
    documento character varying(40) NOT NULL,
    nombres character varying(100) NOT NULL,
    apellidos character varying(100) NOT NULL,
    correo character varying(254),
    telefono character varying(30),
    creado_en timestamp with time zone DEFAULT now() NOT NULL,
    actualizado_en timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT ck_participante_correo CHECK (((correo IS NULL) OR ((correo)::text = lower(btrim((correo)::text))))),
    CONSTRAINT ck_participante_documento CHECK (((documento)::text = btrim((documento)::text))),
    CONSTRAINT participante_apellidos_check CHECK ((length(btrim((apellidos)::text)) > 0)),
    CONSTRAINT participante_nombres_check CHECK ((length(btrim((nombres)::text)) > 0)),
    CONSTRAINT participante_tipo_documento_check CHECK (((tipo_documento)::text = ANY (ARRAY[('CC'::character varying)::text, ('CE'::character varying)::text, ('TI'::character varying)::text, ('PASAPORTE'::character varying)::text, ('OTRO'::character varying)::text])))
);


ALTER TABLE public.participante OWNER TO postgres;

--
-- Name: participants; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.participants (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    created_by character varying(150) NOT NULL,
    document_number character varying(50) NOT NULL,
    document_type character varying(30) NOT NULL,
    email character varying(254) NOT NULL,
    first_name character varying(100) NOT NULL,
    last_name character varying(100) NOT NULL,
    phone character varying(30) NOT NULL,
    updated_at timestamp(6) without time zone,
    updated_by character varying(150)
);


ALTER TABLE public.participants OWNER TO postgres;

--
-- Name: permission; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.permission (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    codigo character varying(80) NOT NULL,
    descripcion character varying(200) NOT NULL
);


ALTER TABLE public.permission OWNER TO postgres;

--
-- Name: registration_cancellation_history; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.registration_cancellation_history (
    id uuid NOT NULL,
    canceled_at timestamp(6) without time zone NOT NULL,
    canceled_by uuid NOT NULL,
    reason character varying(2000) NOT NULL,
    registration_id uuid NOT NULL
);


ALTER TABLE public.registration_cancellation_history OWNER TO postgres;

--
-- Name: role_permission; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.role_permission (
    role_id uuid NOT NULL,
    permission_id uuid NOT NULL
);


ALTER TABLE public.role_permission OWNER TO postgres;

--
-- Name: TABLE role_permission; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.role_permission IS 'Permisos asignados a cada rol; evita almacenar permisos repetidos en texto.';


--
-- Name: user_account_roles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_account_roles (
    user_id uuid NOT NULL,
    role character varying(30) NOT NULL,
    CONSTRAINT user_account_roles_role_check CHECK (((role)::text = ANY ((ARRAY['ADMINISTRADOR'::character varying, 'OPERADOR'::character varying, 'MONITOR'::character varying, 'INVITADO'::character varying])::text[])))
);


ALTER TABLE public.user_account_roles OWNER TO postgres;

--
-- Name: user_accounts; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_accounts (
    id uuid NOT NULL,
    active boolean NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    email character varying(254) NOT NULL,
    password_hash character varying(255) NOT NULL,
    participant_id uuid
);


ALTER TABLE public.user_accounts OWNER TO postgres;

--
-- Name: user_role; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_role (
    user_id uuid NOT NULL,
    role_id uuid NOT NULL,
    asignado_por uuid,
    asignado_en timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.user_role OWNER TO postgres;

--
-- Name: TABLE user_role; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.user_role IS 'Relación muchos-a-muchos entre usuarios y roles.';


--
-- Name: usuario_evento; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.usuario_evento (
    user_id uuid NOT NULL,
    evento_id uuid NOT NULL,
    funcion character varying(15) NOT NULL,
    asignado_por uuid,
    asignado_en timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT usuario_evento_funcion_check CHECK (((funcion)::text = ANY (ARRAY[('OPERADOR'::character varying)::text, ('MONITOR'::character varying)::text])))
);


ALTER TABLE public.usuario_evento OWNER TO postgres;

--
-- Name: TABLE usuario_evento; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.usuario_evento IS 'Asignaciones de operadores y monitores a eventos.';


--
-- Data for Name: app_role; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.app_role (id, nombre, descripcion) FROM stdin;
90000000-0000-4000-8000-000000000001	ADMINISTRADOR	Administración de usuarios, roles y auditoría
90000000-0000-4000-8000-000000000002	OPERADOR	Gestión de eventos, participantes, certificados e importaciones
90000000-0000-4000-8000-000000000003	MONITOR	Control de asistencia por QR o manual
90000000-0000-4000-8000-000000000004	INVITADO	Consulta de eventos publicados
\.


--
-- Data for Name: app_user; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.app_user (id, nombres, correo, password_hash, activo, intentos_fallidos, bloqueado_hasta, ultimo_acceso, creado_en, actualizado_en, version) FROM stdin;
10000000-0000-4000-8000-000000000001	Administrador Demo	admin@syncevent.local	{REEMPLAZAR_POR_HASH_BCRYPT}	t	0	\N	\N	2026-10-20 13:00:15.564084-05	2026-10-20 13:00:15.564084-05	0
10000000-0000-4000-8000-000000000002	Operador Demo	operador@syncevent.local	{REEMPLAZAR_POR_HASH_BCRYPT}	t	0	\N	\N	2026-10-20 13:00:15.564084-05	2026-10-20 13:00:15.564084-05	0
10000000-0000-4000-8000-000000000003	Monitor Demo	monitor@syncevent.local	{REEMPLAZAR_POR_HASH_BCRYPT}	t	0	\N	\N	2026-10-20 13:00:15.564084-05	2026-10-20 13:00:15.564084-05	0
10000000-0000-4000-8000-000000000004	Invitado Demo	invitado@syncevent.local	{REEMPLAZAR_POR_HASH_BCRYPT}	t	0	\N	\N	2026-10-20 13:00:15.564084-05	2026-10-20 13:00:15.564084-05	0
\.


--
-- Data for Name: asistencia; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.asistencia (id, inscripcion_id, registrada_en, metodo, registrada_por, observacion) FROM stdin;
\.


--
-- Data for Name: attendance_absences; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.attendance_absences (id, event_id, participant_id, recorded_at, registration_id) FROM stdin;
\.


--
-- Data for Name: attendance_policy; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.attendance_policy (id, absence_threshold, updated_at, updated_by, window_days) FROM stdin;
\.


--
-- Data for Name: auditoria; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.auditoria (id, usuario_id, usuario_correo, accion, entidad, entidad_id, resultado, ip, detalle, fecha) FROM stdin;
1	\N	admin@syncevent.local	PRUEBA	evento	\N	OK	\N	{}	2026-10-20 13:00:45.896358-05
\.


--
-- Data for Name: auth_activity; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.auth_activity (id, activity, email, occurred_at, user_id) FROM stdin;
1	ADMIN_BOOTSTRAP	admin@syncevent.com	2026-10-27 19:10:40.242706	aac78257-e845-4071-a6c9-b7866dfee865
2	REGISTER	nicolss2416@gmail.com	2026-10-29 19:52:38.9791	82dde2ab-10f4-4a53-899d-4ae59349da2e
3	LOGIN_FAILED	operador@syncevent.com	2026-10-29 19:52:53.106238	\N
4	LOGIN_FAILED	monitor@syncevent.com	2026-10-29 19:53:14.7982	\N
5	REGISTER	gaviriaevelynnicole@gamil.com	2026-10-29 19:54:38.47455	143fe9c6-9328-41a1-968a-51a2be1d1997
6	LOGIN_FAILED	gaviriaevelynnicole@gmail.com	2026-10-29 19:55:20.983679	\N
7	LOGIN_FAILED	gaviriaevelynnicole@gmail.com	2026-10-29 19:56:04.304824	\N
8	REGISTER	nicolssu2416@gmail.com	2026-10-29 19:56:54.790158	2be98d8d-9b26-4ec6-8f00-639c810bef08
9	LOGIN_SUCCESS	nicolssu2416@gmail.com	2026-10-29 19:57:17.993221	2be98d8d-9b26-4ec6-8f00-639c810bef08
10	LOGIN_SUCCESS	admin@syncevent.com	2026-10-29 20:24:51.386976	aac78257-e845-4071-a6c9-b7866dfee865
11	LOGIN_FAILED	monitor@syncevent.com	2026-10-29 20:50:00.633252	\N
12	LOGIN_SUCCESS	admin@syncevent.com	2026-10-29 21:02:17.683159	aac78257-e845-4071-a6c9-b7866dfee865
13	LOGIN_FAILED	laura.gomez@example.test	2026-10-29 21:10:54.305536	\N
14	LOGIN_SUCCESS	admin@syncevent.com	2026-10-29 21:24:49.561054	aac78257-e845-4071-a6c9-b7866dfee865
\.


--
-- Data for Name: auth_sessions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.auth_sessions (id, created_at, last_activity_at, revoked_at, user_id) FROM stdin;
e68b8fcf-8c3b-47a7-b19a-ac3d1b6ed464	2026-10-29 19:57:17.993221	2026-10-29 19:57:18.151088	\N	2be98d8d-9b26-4ec6-8f00-639c810bef08
4c819312-4725-473c-bb43-761879c915c6	2026-10-29 20:24:51.386976	2026-10-29 20:24:51.386976	\N	aac78257-e845-4071-a6c9-b7866dfee865
2d28e25b-5e0b-4064-936a-8e488bb8ca14	2026-10-29 21:02:17.683159	2026-10-29 21:02:17.683159	\N	aac78257-e845-4071-a6c9-b7866dfee865
31dd4782-62e6-4270-bd8b-5b8eca56e2c1	2026-10-29 21:24:49.561054	2026-10-29 21:24:49.561054	\N	aac78257-e845-4071-a6c9-b7866dfee865
\.


--
-- Data for Name: certificado; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.certificado (id, inscripcion_id, codigo_verificacion, hash_contenido, estado, emitido_en, emitido_por, revocado_en, revocado_por, motivo_revocacion, observaciones, nombre_participante_snapshot, evento_nombre_snapshot, duracion_horas_snapshot, evento_fecha_inicio_snapshot, evento_fecha_fin_snapshot, pdf_uri) FROM stdin;
0cddf71f-7293-49a7-90ce-d0f6d1cd5435	40000000-0000-4000-8000-000000000001	CERT-SYNC-2026	e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855	VIGENTE	2026-10-27 09:28:03.420028-05	\N	\N	\N	\N	\N	Laura Gómez	Congreso Nacional de Tecnología 2026	16.00	2026-11-15 04:00:00-05	2026-11-16 13:00:00-05	\N
\.


--
-- Data for Name: certificado_verificacion; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.certificado_verificacion (id, certificado_id, verificado_en, resultado, hash_consultado, ip) FROM stdin;
\.


--
-- Data for Name: certificate_documents; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.certificate_documents (id, content_hash, event_end_at, event_id, event_name, event_start_at, issued_at, issued_by, participant_id, participant_name, pdf_hash, registration_id, revocation_reason, revoked_at, status, verification_code) FROM stdin;
11111111-1111-1111-1111-111111111111	36f7c6aee187b99ed125791f57c27c009a4b73340d8b50633e23c554bb4f8a08	2026-11-16 18:00:00	20000000-0000-4000-8000-000000000001	Congreso Nacional de Tecnología 2026	2026-11-15 09:00:00	2026-10-27 09:00:00	10000000-0000-4000-8000-000000000001	30000000-0000-4000-8000-000000000001	Laura Gómez	pdfhashsample	40000000-0000-4000-8000-000000000001	\N	\N	VIGENTE	CERT-SYNC-2026
\.


--
-- Data for Name: event_attendance; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.event_attendance (registration_id, checked_in_at, checked_in_by, method) FROM stdin;
\.


--
-- Data for Name: event_cancellation_history; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.event_cancellation_history (id, canceled_at, event_id, previous_status, reason) FROM stdin;
\.


--
-- Data for Name: event_monitor_assignments; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.event_monitor_assignments (event_id, monitor_id, assigned_at, assigned_by) FROM stdin;
\.


--
-- Data for Name: event_postponement_history; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.event_postponement_history (id, action, event_id, new_end_at, new_start_at, previous_end_at, previous_start_at, reason, recorded_at) FROM stdin;
\.


--
-- Data for Name: event_registrations; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.event_registrations (id, created_at, created_by, qr_token, status, event_id, participant_id) FROM stdin;
\.


--
-- Data for Name: evento; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.evento (id, nombre, descripcion, tipo, modalidad, estado, fecha_inicio, fecha_fin, lugar, enlace, aforo, duracion_horas, creado_por, creado_en, actualizado_en, version) FROM stdin;
20000000-0000-4000-8000-000000000001	Taller de PostgreSQL	Evento demostrativo para validar el modelo de SyncEvent.	TALLER	PRESENCIAL	PUBLICADO	2026-10-27 13:00:15.655917-05	2026-10-27 17:00:15.655917-05	Sala de formación 1	\N	30	4.00	10000000-0000-4000-8000-000000000001	2026-10-20 13:00:15.655917-05	2026-10-20 13:00:15.655917-05	0
\.


--
-- Data for Name: events; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.events (id, capacity, created_at, description, duration_hours, end_at, location, meeting_url, modality, name, start_at, status, type, updated_at) FROM stdin;
a39c34e2-a928-4a38-877f-b31ec4928fe5	150	2026-10-27 14:16:29.35324	Evento oficial de tecnología y arquitectura hexagonal	8.00	2026-11-10 22:00:00	Auditorio Central	\N	PRESENCIAL	Congreso de Innovación Tecnológica 2026	2026-11-10 14:00:00	PUBLICADO	CAPACITACION	2026-10-27 14:17:25.077122
662fee67-c353-474d-ac96-1c0bd1db7c43	5	2026-10-27 14:58:45.606572	prueba 	1.00	2026-10-28 13:00:00	prueba	\N	PRESENCIAL	taller habilidades blandas	2026-10-28 12:00:00	PUBLICADO	TALLER	2026-10-27 15:00:04.530272
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	<< Flyway Baseline >>	BASELINE	<< Flyway Baseline >>	\N	postgres	2026-10-30 09:35:06.822129	0	t
\.


--
-- Data for Name: importacion_csv; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.importacion_csv (id, evento_id, archivo_nombre, estado, total_filas, filas_validas, filas_invalidas, creado_por, creado_en, confirmada_en) FROM stdin;
\.


--
-- Data for Name: importacion_csv_fila; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.importacion_csv_fila (id, importacion_id, numero_fila, datos, valida, errores, participante_id, inscripcion_id) FROM stdin;
\.


--
-- Data for Name: inscripcion; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.inscripcion (id, evento_id, participante_id, fecha, estado, token_qr, creada_por, cancelada_en) FROM stdin;
40000000-0000-4000-8000-000000000001	20000000-0000-4000-8000-000000000001	30000000-0000-4000-8000-000000000001	2026-10-20 13:00:15.789182-05	ACTIVA	f3b06420-0a7f-4e4b-b636-d34972ad77f5	10000000-0000-4000-8000-000000000002	\N
40000000-0000-4000-8000-000000000002	20000000-0000-4000-8000-000000000001	30000000-0000-4000-8000-000000000002	2026-10-20 13:00:15.789182-05	ACTIVA	d39879b9-a553-4eb6-9afa-33b362911138	10000000-0000-4000-8000-000000000002	\N
\.


--
-- Data for Name: participant_change_history; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.participant_change_history (id, changed_at, changed_by, participant_id) FROM stdin;
\.


--
-- Data for Name: participant_fines; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.participant_fines (id, absence_threshold, created_at, participant_id, resolved_at, resolved_by, status, triggered_by_absence_id, window_days) FROM stdin;
\.


--
-- Data for Name: participante; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.participante (id, tipo_documento, documento, nombres, apellidos, correo, telefono, creado_en, actualizado_en) FROM stdin;
30000000-0000-4000-8000-000000000001	CC	DEMO-1001	Laura	Gómez	laura.gomez@example.test	3000000001	2026-10-20 13:00:15.705856-05	2026-10-20 13:00:15.705856-05
30000000-0000-4000-8000-000000000002	CC	DEMO-1002	Mateo	Rodríguez	mateo.rodriguez@example.test	3000000002	2026-10-20 13:00:15.705856-05	2026-10-20 13:00:15.705856-05
\.


--
-- Data for Name: participants; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.participants (id, created_at, created_by, document_number, document_type, email, first_name, last_name, phone, updated_at, updated_by) FROM stdin;
\.


--
-- Data for Name: permission; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.permission (id, codigo, descripcion) FROM stdin;
5f82b729-6a1f-4542-bf13-b86259384867	USUARIO_GESTIONAR	Crear y administrar usuarios
9e1fd5b5-d14e-49f0-a36a-8459e70dece2	ROL_GESTIONAR	Administrar roles y permisos
75379859-2b7c-45eb-9f64-13b8c7833fef	EVENTO_CREAR_EDITAR	Crear y editar eventos
3723543d-d5ee-4205-827a-13f4d31f07f7	EVENTO_PUBLICAR	Publicar y cambiar estado de eventos
4fe5f5ef-9399-41c3-8652-120a35ccfdf4	PARTICIPANTE_GESTIONAR	Registrar, consultar y editar participantes
5b07512f-49c7-4630-99ab-b83750c67c2e	INSCRIPCION_GESTIONAR	Registrar y cancelar inscripciones
06a5bf64-d4d8-4ae0-85f6-68348541f3c3	CHECKIN_REGISTRAR	Registrar asistencia QR o manual
6aa36240-395d-47c9-b320-450cdcb73b84	CERTIFICADO_EMITIR	Emitir y descargar certificados
ce085d8b-9663-4168-8591-bddbf4d6ee09	CERTIFICADO_REVOCAR	Revocar certificados
2b5911f8-8d4e-41f9-ae65-e3de196bc724	AUDITORIA_CONSULTAR	Consultar la bitácora de auditoría
bb7279ad-e182-4996-a496-49d62e92f88e	CSV_IMPORTAR	Cargar y validar participantes desde CSV
5982079c-9dae-4334-ba81-c2b94260ab43	DASHBOARD_CONSULTAR	Consultar indicadores y filtrar dashboard
\.


--
-- Data for Name: registration_cancellation_history; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.registration_cancellation_history (id, canceled_at, canceled_by, reason, registration_id) FROM stdin;
\.


--
-- Data for Name: role_permission; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.role_permission (role_id, permission_id) FROM stdin;
90000000-0000-4000-8000-000000000001	5f82b729-6a1f-4542-bf13-b86259384867
90000000-0000-4000-8000-000000000001	9e1fd5b5-d14e-49f0-a36a-8459e70dece2
90000000-0000-4000-8000-000000000001	75379859-2b7c-45eb-9f64-13b8c7833fef
90000000-0000-4000-8000-000000000001	3723543d-d5ee-4205-827a-13f4d31f07f7
90000000-0000-4000-8000-000000000001	4fe5f5ef-9399-41c3-8652-120a35ccfdf4
90000000-0000-4000-8000-000000000001	5b07512f-49c7-4630-99ab-b83750c67c2e
90000000-0000-4000-8000-000000000001	06a5bf64-d4d8-4ae0-85f6-68348541f3c3
90000000-0000-4000-8000-000000000001	6aa36240-395d-47c9-b320-450cdcb73b84
90000000-0000-4000-8000-000000000001	ce085d8b-9663-4168-8591-bddbf4d6ee09
90000000-0000-4000-8000-000000000001	2b5911f8-8d4e-41f9-ae65-e3de196bc724
90000000-0000-4000-8000-000000000001	bb7279ad-e182-4996-a496-49d62e92f88e
90000000-0000-4000-8000-000000000001	5982079c-9dae-4334-ba81-c2b94260ab43
90000000-0000-4000-8000-000000000002	75379859-2b7c-45eb-9f64-13b8c7833fef
90000000-0000-4000-8000-000000000002	3723543d-d5ee-4205-827a-13f4d31f07f7
90000000-0000-4000-8000-000000000002	4fe5f5ef-9399-41c3-8652-120a35ccfdf4
90000000-0000-4000-8000-000000000002	5b07512f-49c7-4630-99ab-b83750c67c2e
90000000-0000-4000-8000-000000000002	6aa36240-395d-47c9-b320-450cdcb73b84
90000000-0000-4000-8000-000000000002	ce085d8b-9663-4168-8591-bddbf4d6ee09
90000000-0000-4000-8000-000000000002	bb7279ad-e182-4996-a496-49d62e92f88e
90000000-0000-4000-8000-000000000002	5982079c-9dae-4334-ba81-c2b94260ab43
90000000-0000-4000-8000-000000000003	06a5bf64-d4d8-4ae0-85f6-68348541f3c3
90000000-0000-4000-8000-000000000003	5982079c-9dae-4334-ba81-c2b94260ab43
90000000-0000-4000-8000-000000000004	5982079c-9dae-4334-ba81-c2b94260ab43
\.


--
-- Data for Name: user_account_roles; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_account_roles (user_id, role) FROM stdin;
aac78257-e845-4071-a6c9-b7866dfee865	ADMINISTRADOR
82dde2ab-10f4-4a53-899d-4ae59349da2e	INVITADO
143fe9c6-9328-41a1-968a-51a2be1d1997	INVITADO
2be98d8d-9b26-4ec6-8f00-639c810bef08	INVITADO
\.


--
-- Data for Name: user_accounts; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_accounts (id, active, created_at, email, password_hash, participant_id) FROM stdin;
aac78257-e845-4071-a6c9-b7866dfee865	t	2026-10-27 19:10:40.242706	admin@syncevent.com	b727c14475b38e6b8ce67c551ed1f307d56c86bf390a94055063fc6f71a51bd1489c496bea465ed8da2f608dfe31146c	\N
82dde2ab-10f4-4a53-899d-4ae59349da2e	t	2026-10-29 19:52:38.9791	nicolss2416@gmail.com	3fa49ca11447440591bca20862f2d447bde1ed55d986e064055c23b53054effaeb26d518c97c5b07493c47330043f365	\N
143fe9c6-9328-41a1-968a-51a2be1d1997	t	2026-10-29 19:54:38.47455	gaviriaevelynnicole@gamil.com	2a2274bbef0441c3de0157aec3c9ed436c744bdf7714bbd25ef96d4fd1505cda5952be648843f71c0403b882b7feabe3	\N
2be98d8d-9b26-4ec6-8f00-639c810bef08	t	2026-10-29 19:56:54.790158	nicolssu2416@gmail.com	be313b5b3b14c91c56a2487adf6434f4d33efe4d5122daaef43afba35130bf7139d62b7b457efc92f85a711b07bd83be	\N
\.


--
-- Data for Name: user_role; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_role (user_id, role_id, asignado_por, asignado_en) FROM stdin;
10000000-0000-4000-8000-000000000001	90000000-0000-4000-8000-000000000001	\N	2026-10-20 13:00:15.605839-05
10000000-0000-4000-8000-000000000002	90000000-0000-4000-8000-000000000002	\N	2026-10-20 13:00:15.605839-05
10000000-0000-4000-8000-000000000003	90000000-0000-4000-8000-000000000003	\N	2026-10-20 13:00:15.605839-05
10000000-0000-4000-8000-000000000004	90000000-0000-4000-8000-000000000004	\N	2026-10-20 13:00:15.605839-05
\.


--
-- Data for Name: usuario_evento; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.usuario_evento (user_id, evento_id, funcion, asignado_por, asignado_en) FROM stdin;
10000000-0000-4000-8000-000000000003	20000000-0000-4000-8000-000000000001	MONITOR	10000000-0000-4000-8000-000000000001	2026-10-20 13:00:15.755813-05
\.


--
-- Name: auditoria_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.auditoria_id_seq', 1, true);


--
-- Name: auth_activity_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.auth_activity_id_seq', 14, true);


--
-- Name: certificado_verificacion_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.certificado_verificacion_id_seq', 1, false);


--
-- Name: event_postponement_history_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.event_postponement_history_id_seq', 1, false);


--
-- Name: importacion_csv_fila_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.importacion_csv_fila_id_seq', 1, false);


--
-- Name: participant_change_history_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.participant_change_history_id_seq', 1, false);


--
-- Name: app_role app_role_nombre_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.app_role
    ADD CONSTRAINT app_role_nombre_key UNIQUE (nombre);


--
-- Name: app_role app_role_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.app_role
    ADD CONSTRAINT app_role_pkey PRIMARY KEY (id);


--
-- Name: app_user app_user_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.app_user
    ADD CONSTRAINT app_user_pkey PRIMARY KEY (id);


--
-- Name: asistencia asistencia_inscripcion_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.asistencia
    ADD CONSTRAINT asistencia_inscripcion_id_key UNIQUE (inscripcion_id);


--
-- Name: asistencia asistencia_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.asistencia
    ADD CONSTRAINT asistencia_pkey PRIMARY KEY (id);


--
-- Name: attendance_absences attendance_absences_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.attendance_absences
    ADD CONSTRAINT attendance_absences_pkey PRIMARY KEY (id);


--
-- Name: attendance_policy attendance_policy_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.attendance_policy
    ADD CONSTRAINT attendance_policy_pkey PRIMARY KEY (id);


--
-- Name: auditoria auditoria_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.auditoria
    ADD CONSTRAINT auditoria_pkey PRIMARY KEY (id);


--
-- Name: auth_activity auth_activity_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.auth_activity
    ADD CONSTRAINT auth_activity_pkey PRIMARY KEY (id);


--
-- Name: auth_sessions auth_sessions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.auth_sessions
    ADD CONSTRAINT auth_sessions_pkey PRIMARY KEY (id);


--
-- Name: certificado certificado_codigo_verificacion_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.certificado
    ADD CONSTRAINT certificado_codigo_verificacion_key UNIQUE (codigo_verificacion);


--
-- Name: certificado certificado_inscripcion_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.certificado
    ADD CONSTRAINT certificado_inscripcion_id_key UNIQUE (inscripcion_id);


--
-- Name: certificado certificado_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.certificado
    ADD CONSTRAINT certificado_pkey PRIMARY KEY (id);


--
-- Name: certificado_verificacion certificado_verificacion_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.certificado_verificacion
    ADD CONSTRAINT certificado_verificacion_pkey PRIMARY KEY (id);


--
-- Name: certificate_documents certificate_documents_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.certificate_documents
    ADD CONSTRAINT certificate_documents_pkey PRIMARY KEY (id);


--
-- Name: event_attendance event_attendance_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_attendance
    ADD CONSTRAINT event_attendance_pkey PRIMARY KEY (registration_id);


--
-- Name: event_cancellation_history event_cancellation_history_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_cancellation_history
    ADD CONSTRAINT event_cancellation_history_pkey PRIMARY KEY (id);


--
-- Name: event_monitor_assignments event_monitor_assignments_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_monitor_assignments
    ADD CONSTRAINT event_monitor_assignments_pkey PRIMARY KEY (event_id, monitor_id);


--
-- Name: event_postponement_history event_postponement_history_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_postponement_history
    ADD CONSTRAINT event_postponement_history_pkey PRIMARY KEY (id);


--
-- Name: event_registrations event_registrations_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_registrations
    ADD CONSTRAINT event_registrations_pkey PRIMARY KEY (id);


--
-- Name: evento evento_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.evento
    ADD CONSTRAINT evento_pkey PRIMARY KEY (id);


--
-- Name: events events_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.events
    ADD CONSTRAINT events_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: importacion_csv_fila importacion_csv_fila_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.importacion_csv_fila
    ADD CONSTRAINT importacion_csv_fila_pkey PRIMARY KEY (id);


--
-- Name: importacion_csv importacion_csv_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.importacion_csv
    ADD CONSTRAINT importacion_csv_pkey PRIMARY KEY (id);


--
-- Name: inscripcion inscripcion_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.inscripcion
    ADD CONSTRAINT inscripcion_pkey PRIMARY KEY (id);


--
-- Name: inscripcion inscripcion_token_qr_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.inscripcion
    ADD CONSTRAINT inscripcion_token_qr_key UNIQUE (token_qr);


--
-- Name: participant_change_history participant_change_history_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.participant_change_history
    ADD CONSTRAINT participant_change_history_pkey PRIMARY KEY (id);


--
-- Name: participant_fines participant_fines_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.participant_fines
    ADD CONSTRAINT participant_fines_pkey PRIMARY KEY (id);


--
-- Name: participante participante_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.participante
    ADD CONSTRAINT participante_pkey PRIMARY KEY (id);


--
-- Name: participants participants_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.participants
    ADD CONSTRAINT participants_pkey PRIMARY KEY (id);


--
-- Name: permission permission_codigo_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.permission
    ADD CONSTRAINT permission_codigo_key UNIQUE (codigo);


--
-- Name: permission permission_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.permission
    ADD CONSTRAINT permission_pkey PRIMARY KEY (id);


--
-- Name: registration_cancellation_history registration_cancellation_history_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.registration_cancellation_history
    ADD CONSTRAINT registration_cancellation_history_pkey PRIMARY KEY (id);


--
-- Name: role_permission role_permission_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.role_permission
    ADD CONSTRAINT role_permission_pkey PRIMARY KEY (role_id, permission_id);


--
-- Name: event_registrations uk_event_registrations_event_participant; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_registrations
    ADD CONSTRAINT uk_event_registrations_event_participant UNIQUE (event_id, participant_id);


--
-- Name: event_registrations uk_event_registrations_qr_token; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_registrations
    ADD CONSTRAINT uk_event_registrations_qr_token UNIQUE (qr_token);


--
-- Name: participants uk_participants_document; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.participants
    ADD CONSTRAINT uk_participants_document UNIQUE (document_type, document_number);


--
-- Name: user_accounts uk_user_accounts_email; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_accounts
    ADD CONSTRAINT uk_user_accounts_email UNIQUE (email);


--
-- Name: user_accounts uk_user_accounts_participant; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_accounts
    ADD CONSTRAINT uk_user_accounts_participant UNIQUE (participant_id);


--
-- Name: certificate_documents ukj404g5vcwfbecc55w6kfinvfg; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.certificate_documents
    ADD CONSTRAINT ukj404g5vcwfbecc55w6kfinvfg UNIQUE (verification_code);


--
-- Name: certificate_documents ukl2i5e78y9n95xp8rwyc93jcjc; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.certificate_documents
    ADD CONSTRAINT ukl2i5e78y9n95xp8rwyc93jcjc UNIQUE (registration_id);


--
-- Name: app_user uq_app_user_correo; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.app_user
    ADD CONSTRAINT uq_app_user_correo UNIQUE (correo);


--
-- Name: importacion_csv_fila uq_importacion_fila; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.importacion_csv_fila
    ADD CONSTRAINT uq_importacion_fila UNIQUE (importacion_id, numero_fila);


--
-- Name: inscripcion uq_inscripcion_evento_participante; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.inscripcion
    ADD CONSTRAINT uq_inscripcion_evento_participante UNIQUE (evento_id, participante_id);


--
-- Name: participante uq_participante_documento; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.participante
    ADD CONSTRAINT uq_participante_documento UNIQUE (tipo_documento, documento);


--
-- Name: user_account_roles user_account_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_account_roles
    ADD CONSTRAINT user_account_roles_pkey PRIMARY KEY (user_id, role);


--
-- Name: user_accounts user_accounts_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_accounts
    ADD CONSTRAINT user_accounts_pkey PRIMARY KEY (id);


--
-- Name: user_role user_role_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_role
    ADD CONSTRAINT user_role_pkey PRIMARY KEY (user_id, role_id);


--
-- Name: usuario_evento usuario_evento_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.usuario_evento
    ADD CONSTRAINT usuario_evento_pkey PRIMARY KEY (user_id, evento_id, funcion);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_events_location_schedule; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_events_location_schedule ON public.events USING btree (location, start_at, end_at);


--
-- Name: idx_events_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_events_status ON public.events USING btree (status);


--
-- Name: ix_asistencia_fecha; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX ix_asistencia_fecha ON public.asistencia USING btree (registrada_en DESC);


--
-- Name: ix_auditoria_entidad_accion_fecha; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX ix_auditoria_entidad_accion_fecha ON public.auditoria USING btree (entidad, accion, fecha DESC);


--
-- Name: ix_auditoria_fecha; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX ix_auditoria_fecha ON public.auditoria USING btree (fecha DESC);


--
-- Name: ix_auditoria_usuario_fecha; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX ix_auditoria_usuario_fecha ON public.auditoria USING btree (usuario_id, fecha DESC);


--
-- Name: ix_certificado_estado_emitido; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX ix_certificado_estado_emitido ON public.certificado USING btree (estado, emitido_en DESC);


--
-- Name: ix_certificado_verificacion_fecha; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX ix_certificado_verificacion_fecha ON public.certificado_verificacion USING btree (certificado_id, verificado_en DESC);


--
-- Name: ix_evento_estado_fecha; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX ix_evento_estado_fecha ON public.evento USING btree (estado, fecha_inicio DESC);


--
-- Name: ix_evento_tipo_modalidad; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX ix_evento_tipo_modalidad ON public.evento USING btree (tipo, modalidad);


--
-- Name: ix_importacion_estado_fecha; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX ix_importacion_estado_fecha ON public.importacion_csv USING btree (estado, creado_en DESC);


--
-- Name: ix_importacion_fila_lote_valida; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX ix_importacion_fila_lote_valida ON public.importacion_csv_fila USING btree (importacion_id, valida);


--
-- Name: ix_inscripcion_evento_estado; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX ix_inscripcion_evento_estado ON public.inscripcion USING btree (evento_id, estado);


--
-- Name: ix_inscripcion_participante_fecha; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX ix_inscripcion_participante_fecha ON public.inscripcion USING btree (participante_id, fecha DESC);


--
-- Name: ix_participante_apellidos_nombres; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX ix_participante_apellidos_nombres ON public.participante USING btree (lower((apellidos)::text), lower((nombres)::text));


--
-- Name: ix_participante_correo; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX ix_participante_correo ON public.participante USING btree (correo) WHERE (correo IS NOT NULL);


--
-- Name: auditoria trg_auditoria_inmutable; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_auditoria_inmutable BEFORE DELETE OR UPDATE ON public.auditoria FOR EACH ROW EXECUTE FUNCTION public.impedir_mutacion_auditoria();


--
-- Name: asistencia asistencia_inscripcion_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.asistencia
    ADD CONSTRAINT asistencia_inscripcion_id_fkey FOREIGN KEY (inscripcion_id) REFERENCES public.inscripcion(id) ON DELETE RESTRICT;


--
-- Name: asistencia asistencia_registrada_por_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.asistencia
    ADD CONSTRAINT asistencia_registrada_por_fkey FOREIGN KEY (registrada_por) REFERENCES public.app_user(id) ON DELETE RESTRICT;


--
-- Name: auditoria auditoria_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.auditoria
    ADD CONSTRAINT auditoria_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.app_user(id) ON DELETE SET NULL;


--
-- Name: certificado certificado_emitido_por_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.certificado
    ADD CONSTRAINT certificado_emitido_por_fkey FOREIGN KEY (emitido_por) REFERENCES public.app_user(id) ON DELETE RESTRICT;


--
-- Name: certificado certificado_inscripcion_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.certificado
    ADD CONSTRAINT certificado_inscripcion_id_fkey FOREIGN KEY (inscripcion_id) REFERENCES public.inscripcion(id) ON DELETE RESTRICT;


--
-- Name: certificado certificado_revocado_por_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.certificado
    ADD CONSTRAINT certificado_revocado_por_fkey FOREIGN KEY (revocado_por) REFERENCES public.app_user(id) ON DELETE RESTRICT;


--
-- Name: certificado_verificacion certificado_verificacion_certificado_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.certificado_verificacion
    ADD CONSTRAINT certificado_verificacion_certificado_id_fkey FOREIGN KEY (certificado_id) REFERENCES public.certificado(id) ON DELETE RESTRICT;


--
-- Name: evento evento_creado_por_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.evento
    ADD CONSTRAINT evento_creado_por_fkey FOREIGN KEY (creado_por) REFERENCES public.app_user(id) ON DELETE RESTRICT;


--
-- Name: event_registrations fk6eykq6wu4n23qhn5vwb8kyut5; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_registrations
    ADD CONSTRAINT fk6eykq6wu4n23qhn5vwb8kyut5 FOREIGN KEY (event_id) REFERENCES public.events(id);


--
-- Name: user_accounts fk_user_accounts_participant; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_accounts
    ADD CONSTRAINT fk_user_accounts_participant FOREIGN KEY (participant_id) REFERENCES public.participants(id);


--
-- Name: user_account_roles fkm1jb33yny1o66uydeevk7sqwe; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_account_roles
    ADD CONSTRAINT fkm1jb33yny1o66uydeevk7sqwe FOREIGN KEY (user_id) REFERENCES public.user_accounts(id);


--
-- Name: event_registrations fkr249nu19r0w7nqbanwh4gbt39; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_registrations
    ADD CONSTRAINT fkr249nu19r0w7nqbanwh4gbt39 FOREIGN KEY (participant_id) REFERENCES public.participants(id);


--
-- Name: importacion_csv importacion_csv_creado_por_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.importacion_csv
    ADD CONSTRAINT importacion_csv_creado_por_fkey FOREIGN KEY (creado_por) REFERENCES public.app_user(id) ON DELETE RESTRICT;


--
-- Name: importacion_csv importacion_csv_evento_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.importacion_csv
    ADD CONSTRAINT importacion_csv_evento_id_fkey FOREIGN KEY (evento_id) REFERENCES public.evento(id) ON DELETE RESTRICT;


--
-- Name: importacion_csv_fila importacion_csv_fila_importacion_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.importacion_csv_fila
    ADD CONSTRAINT importacion_csv_fila_importacion_id_fkey FOREIGN KEY (importacion_id) REFERENCES public.importacion_csv(id) ON DELETE CASCADE;


--
-- Name: importacion_csv_fila importacion_csv_fila_inscripcion_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.importacion_csv_fila
    ADD CONSTRAINT importacion_csv_fila_inscripcion_id_fkey FOREIGN KEY (inscripcion_id) REFERENCES public.inscripcion(id) ON DELETE RESTRICT;


--
-- Name: importacion_csv_fila importacion_csv_fila_participante_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.importacion_csv_fila
    ADD CONSTRAINT importacion_csv_fila_participante_id_fkey FOREIGN KEY (participante_id) REFERENCES public.participante(id) ON DELETE RESTRICT;


--
-- Name: inscripcion inscripcion_creada_por_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.inscripcion
    ADD CONSTRAINT inscripcion_creada_por_fkey FOREIGN KEY (creada_por) REFERENCES public.app_user(id) ON DELETE SET NULL;


--
-- Name: inscripcion inscripcion_evento_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.inscripcion
    ADD CONSTRAINT inscripcion_evento_id_fkey FOREIGN KEY (evento_id) REFERENCES public.evento(id) ON DELETE RESTRICT;


--
-- Name: inscripcion inscripcion_participante_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.inscripcion
    ADD CONSTRAINT inscripcion_participante_id_fkey FOREIGN KEY (participante_id) REFERENCES public.participante(id) ON DELETE RESTRICT;


--
-- Name: role_permission role_permission_permission_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.role_permission
    ADD CONSTRAINT role_permission_permission_id_fkey FOREIGN KEY (permission_id) REFERENCES public.permission(id) ON DELETE CASCADE;


--
-- Name: role_permission role_permission_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.role_permission
    ADD CONSTRAINT role_permission_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.app_role(id) ON DELETE CASCADE;


--
-- Name: user_role user_role_asignado_por_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_role
    ADD CONSTRAINT user_role_asignado_por_fkey FOREIGN KEY (asignado_por) REFERENCES public.app_user(id) ON DELETE SET NULL;


--
-- Name: user_role user_role_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_role
    ADD CONSTRAINT user_role_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.app_role(id) ON DELETE RESTRICT;


--
-- Name: user_role user_role_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_role
    ADD CONSTRAINT user_role_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.app_user(id) ON DELETE RESTRICT;


--
-- Name: usuario_evento usuario_evento_asignado_por_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.usuario_evento
    ADD CONSTRAINT usuario_evento_asignado_por_fkey FOREIGN KEY (asignado_por) REFERENCES public.app_user(id) ON DELETE SET NULL;


--
-- Name: usuario_evento usuario_evento_evento_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.usuario_evento
    ADD CONSTRAINT usuario_evento_evento_id_fkey FOREIGN KEY (evento_id) REFERENCES public.evento(id) ON DELETE RESTRICT;


--
-- Name: usuario_evento usuario_evento_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.usuario_evento
    ADD CONSTRAINT usuario_evento_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.app_user(id) ON DELETE RESTRICT;


--
-- Name: SCHEMA public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE USAGE ON SCHEMA public FROM PUBLIC;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--

\unrestrict Fn3dyGdJDUqXN6YhVTAlIuESCXcKE31sBFHbUdxwIqePdad7NylXnslSS04RPhq

