# ObdScanner 🚗📡

App Android de **telemetría OBD-II** con detección automática de viajes, sincronización a la nube (Supabase), análisis con IA vía **MCP** y dashboard web de comportamiento de conducción.

<p>
  <a href="https://github.com/ogarciabrena/ObdScanner/releases/latest">
    <img alt="Descargar APK" src="https://img.shields.io/github/v/release/ogarciabrena/ObdScanner?label=%F0%9F%93%A5%20Descargar%20APK&style=for-the-badge">
  </a>
</p>

**📥 [Descargar la última versión del APK](https://github.com/ogarciabrena/ObdScanner/releases/latest)** — instálalo en Android 7+ y sigue el [tutorial](#tutorial-de-instalación).

```
Adaptador OBD ──Bluetooth──> App Android (tableta/celular)
                              │ 1. Detecta el adaptador al conectarse → "¿Iniciar viaje?"
                              │ 2. Graba telemetría localmente (offline-first)
                              │ 3. Sube a Supabase cuando hay internet
                              ▼
                          Supabase (Postgres gratis)
                              │
                 ┌────────────┴────────────┐
                 ▼                         ▼
          Servidor MCP              Dashboard web
          (análisis con la IA       (gráficas, score de
           que prefieras)            conducción, eventos)
```

## Características

- **Detección automática**: al conectarse el adaptador OBD por Bluetooth aparece una notificación para iniciar el viaje, aunque la app esté cerrada.
- **Grabación de viaje**: servicio en primer plano que registra ~14 sensores (RPM, velocidad, temperaturas, fuel trims, voltaje, MAF, etc.) con velocidad y RPM en vivo en la notificación.
- **Offline-first**: cada viaje se guarda local como JSONL; se sube a Supabase automáticamente al recuperar internet y se elimina del dispositivo.
- **Lectura y borrado de códigos de falla (DTC)**.
- **Detección temprana de fallas**: los fuel trims, temperatura de refrigerante, voltaje del módulo y sensores O2 grabados en cada viaje permiten detectar anomalías comparando contra el histórico (análisis vía MCP/SQL).
- **Dashboard web**: score de conducción, eventos bruscos (frenadas, aceleraciones, RPM sostenidas, excesos de velocidad), gráficas interactivas. Un solo archivo HTML, sin dependencias.
- **Privacidad por diseño**: cada usuario usa SU propia base de datos. La clave que va en la app solo puede insertar y leer — nunca borrar.

## Tutorial de instalación

### 1. Crea tu proyecto en Supabase (gratis)

1. Regístrate en [supabase.com](https://supabase.com) y crea un proyecto nuevo.
2. Ve a **SQL Editor** → **New query**, pega el contenido de [`supabase_schema.sql`](supabase_schema.sql) y presiona **Run**. Esto crea las tablas `trips` y `telemetry` con sus políticas de seguridad.
3. Ve a **Settings → API Keys** y copia:
   - **Project URL** (ej. `https://abcdefgh.supabase.co`)
   - **Publishable key** (`sb_publishable_...`)

### 2. Instala y configura la app

**Opción A — APK precompilado** (Releases de este repo):
1. Instala el APK en tu Android (7.0+, con Bluetooth).
2. Abre la app → pestaña **Settings** → sección *Sincronización en la nube*.
3. Pega tu Project URL y tu publishable key → **Guardar**.

**Opción B — compilar desde código**:
```bash
# Requisitos: JDK 17, Android SDK (platform 35)
# Opcional: deja tus credenciales como default de compilación
echo "supabase.url=https://TU-PROYECTO.supabase.co" >> local.properties
echo "supabase.key=sb_publishable_TU_CLAVE" >> local.properties

./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

> Sin configurar Supabase la app funciona igual, solo que los viajes se quedan en el dispositivo.

### 3. Empareja tu adaptador OBD

1. Conecta el adaptador (ELM327 Bluetooth) al puerto OBD-II del vehículo.
2. Empareja el adaptador en los ajustes Bluetooth de Android.
3. A partir de ahí, cada vez que el adaptador se conecte verás la notificación **"¿Iniciar viaje?"**.

### 4. Dashboard web

1. Abre [`dashboard/index.html`](dashboard/index.html) en cualquier navegador (doble clic basta).
2. La primera vez te pedirá tu Project URL y publishable key (se guardan solo en tu navegador).
3. Listo: selecciona un viaje y explora tus datos.

### 5. Análisis con IA (MCP)

Conecta tu base de telemetría a la IA que prefieras mediante el [servidor MCP oficial de Supabase](https://github.com/supabase-community/supabase-mcp).

Necesitas un **personal access token** de Supabase: Dashboard → Account → Access Tokens → Generate new token.

**Claude Code:**
```bash
claude mcp add supabase -s user \
  -e SUPABASE_ACCESS_TOKEN=sbp_TU_TOKEN \
  -- npx -y @supabase/mcp-server-supabase@latest --read-only --project-ref=TU_PROJECT_REF
```

**Cualquier otro cliente MCP** (Cursor, Windsurf, Claude Desktop, etc.) — configuración JSON equivalente:
```json
{
  "mcpServers": {
    "supabase": {
      "command": "npx",
      "args": ["-y", "@supabase/mcp-server-supabase@latest",
               "--read-only", "--project-ref=TU_PROJECT_REF"],
      "env": { "SUPABASE_ACCESS_TOKEN": "sbp_TU_TOKEN" }
    }
  }
}
```

> `TU_PROJECT_REF` es el subdominio de tu Project URL (lo que va antes de `.supabase.co`). Se recomienda `--read-only` para que el análisis nunca modifique datos.

Ejemplos de preguntas una vez conectado:
- *"¿Cuántas frenadas bruscas tuve esta semana?"*
- *"Grafica la temperatura del refrigerante de mis últimos 5 viajes, ¿ves tendencia al alza?"*
- *"¿Mi long fuel trim está subiendo con el tiempo?"* (indicador temprano de fugas de vacío o inyectores sucios)

## Esquema de datos

| Tabla | Contenido |
|---|---|
| `trips` | Un registro por viaje: `id`, `device`, `start_ts`, `end_ts` (epoch ms), `sample_count` |
| `telemetry` | Cada lectura: `trip_id`, `ts` (epoch ms), `pid`, `name`, `value`, `unit` |
| `telemetry_readable` | Vista con timestamps legibles para análisis SQL |

## Estructura del proyecto

```
app/src/main/java/com/obd/scanner/
├── data/obd/            # Bluetooth, protocolo ELM327, servicio de viaje, auto-detección
├── data/trip/           # Grabación de viajes (JSONL, cola offline)
├── data/sync/           # Subida a Supabase (WorkManager) y configuración
├── domain/              # Modelos y casos de uso (PIDs, DTCs, fórmulas)
└── ui/                  # Pantallas Compose (dashboard, conexión, DTCs, logs, settings)
dashboard/index.html     # Dashboard web autocontenido
supabase_schema.sql      # Esquema de la base de datos
```

## Seguridad

- La **publishable key** que va en la app/dashboard solo permite `INSERT` y `SELECT` (las políticas RLS del esquema no conceden `DELETE`/`UPDATE` destructivos).
- El **personal access token** del MCP no se pone nunca en la app ni en el dashboard; vive solo en la configuración de tu cliente de IA, y con `--read-only` no puede escribir.

## Licencia

MIT — ver [LICENSE](LICENSE).
