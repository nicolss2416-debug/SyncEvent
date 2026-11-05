# Frontend SyncEvent

Cliente web React 18 con Vite 5 y TypeScript. **Usa exclusivamente pnpm** para instalar
dependencias, iniciar y compilar el frontend.

## Flujos conectados

- Registro, inicio y cierre de sesión mediante la API REST y tokens JWT.
- Navegación limitada a los roles devueltos por el backend.
- Consulta y administración del ciclo de vida de eventos.
- Administración de cuentas, roles, multas y política de inasistencias.
- Perfil vinculado, autinscripción y cancelación de inscripciones del Invitado.
- Lista y descarga privada de certificados; verificación pública.
- Check-in QR/manual, búsqueda de participantes y estadísticas GraphQL para Monitor.

## Pendiente

- Importación masiva CSV con persistencia.
- Consulta de bitácora desde backend.
- Asignación de monitores cargados desde el servidor.
- Suscripción de estadísticas en vivo desde el navegador.
- Emisión masiva/automática de certificados y notificaciones.

## Comandos

- Instalar dependencias: `pnpm install`
- Desarrollo: `pnpm dev`
- Compilación: `pnpm run build`

El backend local debe estar disponible en `http://localhost:8080`; la configuración de CORS
permite por defecto el origen de Vite `http://localhost:5173` y admite configurar otros orígenes
por entorno.
