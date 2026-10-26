# TVeoClean — Plan de Arquitectura, Seguridad y Diseño (Fase 0)

Aplicación Android nativa (Kotlin, Jetpack Compose y Material 3) para analizar en solo lectura, limpiar de forma reversible (`pm disable-user`), proteger, restaurar y personalizar televisores compatibles con Android TV / Google TV mediante ADB autorizado en red local, incorporando un **Style Studio** con validación criptográfica e importación segura desde GitHub fijada por commit SHA.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> Estas decisiones recogen tus respuestas en la etapa de clarificación y definen los límites técnicos verificados antes de tocar una sola línea de código.

- **Decisión confirmada 1 — Origen del código y honestidad de auditoría**: En el espacio de trabajo actual solo están presentes la plantilla Android base de Google AI Studio y el documento adjunto `TVeoClean_Prompt_Maestro_Google_AI_Studio.md` (no se adjuntaron los ZIP de `udhaykumarbala/cleanyourtv-app` ni de `Gathin23/cleanyourtv`). Procederemos construyendo los paquetes modulares limpios dentro de este proyecto, documentando con total transparencia qué proviene de la plantilla actual, qué se contrastó de la documentación pública de referencia y qué se desarrollará desde cero con atribuciones MIT/OFL intactas.
- **Decisión confirmada 2 — Arquitectura modular de compilación rápida**: Toda la separación de dominios (`core.adb`, `core.device`, `core.engine`, `core.packs`, `core.profiles`, `core.restore`, `core.platform`, `core.privacy`, `ui.design` y `feature.*`) se estructurará mediante paquetes desacoplados con interfaces estrictas e inyección por constructor dentro de `:app`, garantizando compilaciones incrementales rápidas y testabilidad unitaria/Robolectric aislada.
- **Decisión confirmada 3 — Experiencia inicial de Style Studio**: Style Studio incluirá un catálogo local curado y verificable de paquetes declarativos de muestra (claramente etiquetados por destino `mobile-ui`, `tv-launcher` y `tv-profile`) junto con el importador real desde GitHub en vivo fijado a commit SHA, sin simular conexiones ADB cuando no haya un televisor real conectado.
- **Nota técnica sobre Identificador de Aplicación (`applicationId`)**: El Prompt Maestro sugiere `app.tveoclean`, mientras que el entorno de compilación de la plataforma exige el formato `com.aistudio.tveoclean.<id>` para evitar colisiones de despliegue en el emulador en streaming. Mantendremos el namespace interno limpio y libre de cualquier identificador de `com.gathin.tvhome` o `cleanyourtv`, configurando `com.aistudio.tveoclean.kxmprq` para asegurar compatibilidad total con el entorno de compilación y empaquetado.

---

## 0. Diagnóstico Técnico y Auditoría Inicial (Fase 0)

Siguiendo la regla estricta de no inventar auditorías sobre archivos inexistentes, se distingue con precisión quirúrgica entre los tres niveles de evidencia:

### A. Qué existe realmente hoy en el espacio de trabajo entregado
- **Estado del proyecto local**: Plantilla inicial virgen de Android con Kotlin `2.2.10`, Android Gradle Plugin `9.1.1`, `compileSdk = 36`, `minSdk = 24`, `targetSdk = 36` y Jetpack Compose BOM `2024.09.00`.
- **Dependencias ya configuradas y resolubles**: Material 3, Lifecycle/ViewModel Compose, Navigation Compose (`2.8.9`), Room (`2.7.0` con KSP `2.3.5`), DataStore Preferences (`1.1.7`), OkHttp (`4.10.0`), Retrofit + Moshi (`2.12.0` / `1.15.2`), Coil (`2.7.0`), Coroutines (`1.10.2`), JUnit4 y Robolectric (`4.16.1`).
- **Funcionalidad actual en código local**: Únicamente un `MainActivity` de plantilla (`Greeting("Android")`). **0% de lógica ADB, limpieza, restauración o Style Studio está implementada todavía.**

### B. Qué se verificó públicamente sobre los proyectos de referencia (sin acceso al ZIP completo)
- **`Gathin23/cleanyourtv` y `cleanyour.tv` (Licencia MIT + fuente Outfit bajo SIL OFL 1.1)**:
  - Utiliza desactivación reversible a nivel de usuario (`pm disable-user --user 0 <pkg>`) sobre ADB TCP (puerto 5555) en lugar de borrado destructivo o root.
  - Mantiene listas curadas por marca/modelo (Sony, TCL, Hisense, Xiaomi, Chromecast con Google TV) protegiendo servicios críticos del sistema.
  - Su launcher ligero (`com.gathin.tvhome`) admite superposición declarativa de temas en `/sdcard/Android/data/com.gathin.tvhome/files` mediante `config.json` (claves documentadas: `dock_apps`, `fill_dock`, `hidden_apps`, `background_color`, densidad de columnas y ajustes de movimiento/glow) acompañado de `wallpaper.jpg` o carpeta `wallpapers/`.
- **`udhaykumarbala/cleanyourtv-app` (Licencia MIT)**:
  - Compañero móvil en Kotlin + Jetpack Compose que descubre televisores por mDNS en la LAN, abre sesiones ADB sobre Wi-Fi, ejecuta el flujo *Conectar → Analizar → Limpiar* y ofrece restauración de paquetes.
  - **Limitación auditada**: Al no haberse adjuntado el código fuente de `cleanyourtv-app`, no reutilizaremos código copiado a ciegas; implementaremos nuestro propio transporte protocolo ADB v1.0 (`CNXN`, `AUTH`, `OPEN`, `WRTE`, `CLSE` + TLS/Pairing Android 11+) y conservaremos los avisos de atribución inspiracional y compatibilidad de formato heredado en la pantalla de Licencias.

### C. Clasificación de capacidades para TVeoClean

| Área funcional | Estado actual | Acción requerida en TVeoClean |
| :--- | :--- | :--- |
| **Sistema de diseño M3 + Navegación adaptable** | Solo plantilla genérica | **Desarrollar desde cero** con paleta Tinta/Turquesa/Índigo, tema claro cálido, soporte RTL, TalkBack y reducción de movimiento. |
| **Transporte ADB TCP + Keystore RSA + mDNS** | Inexistente en workspace | **Desarrollar desde cero** con par de claves RSA resguardado en Android Keystore, descubrimiento NsdManager (`_adb._tcp`, `_adb-tls-connect._tcp`, `_adb-tls-pairing._tcp`), timeouts y servidor ADB falso para tests deterministas. |
| **Análisis de solo lectura + Matriz `TvDriver`** | Inexistente en workspace | **Desarrollar desde cero** separando Android TV / Google TV, Fire OS (modo conservador), Chromecast/Streamer (bloqueo de reemplazo inseguro de Home) y plataformas sin ADB (Tizen, webOS, Roku marcadas honestamente como *No disponible*). |
| **Motor transaccional, Lista Protegida y Snapshots** | Inexistente en workspace | **Desarrollar desde cero** con lista inmutable de paquetes críticos, snapshots previos en Room, ejecución en lotes pequeños, relectura real post-cambio y rollback diferencial. |
| **Style Studio (`tveo.json` + temas heredados)** | Inexistente en workspace | **Desarrollar desde cero** con validador formal, lista blanca de claves, decodificación aislada de imágenes y adaptador local para `config.json` + `wallpaper.jpg` / `wallpapers/`. |
| **Importador GitHub fijado a Commit SHA** | Inexistente en workspace | **Desarrollar desde cero** con resolución obligatoria de ref a SHA de 40 caracteres vía `api.github.com`, descargas acotadas desde `raw.githubusercontent.com`, verificación SHA-256 y etiquetas honestas de confianza. |

---

## 1. Overview & Core Concept

- **Qué hace TVeoClean**: Permite a cualquier propietario de un televisor Android TV o Google TV conectarse por su red local Wi-Fi con autorización explícita, auditar qué aplicaciones y servicios están activos sin alterar nada, aplicar planes de limpieza 100% reversibles con copias de restauración automáticas y personalizar tanto la app como launchers compatibles desde **Style Studio**.
- **Público objetivo**: Desde usuarios domésticos que buscan recuperar fluidez y orden visual sin usar la terminal ni arriesgar su televisor, hasta usuarios avanzados que desean inspeccionar cada comando ADB, hash SHA-256, permiso y diferencia (`diff`) antes de confirmar un cambio.
- **Valor diferencial**: Seguridad por diseño ("Centro de control tranquilo"). Ninguna acción ocurre sin snapshot previo y confirmación expresa; ningún paquete comunitario de GitHub puede inyectar comandos shell ni desactivar paquetes críticos del sistema.

---

## 2. User Experience & Visual Design

### Identidad Visual: "Centro de Control Tranquilo"
- **Dirección estética**: Sobria, serena, técnica pero cálida, inspirada en equipamiento audiovisual de alta gama para el salón. Cero neones agresivos, cero sombras pesadas y cero copias de Clean Your TV.
- **Paleta cromática semántica (Material 3)**:
  - **Modo Oscuro (Predeterminado)**:
    - Fondo base (`background`): Tinta azulada profunda `#0B1020`
    - Superficie primaria (`surface` / tarjetas): Azul noche `#121A2B`
    - Superficie elevada / activa (`surfaceContainerHigh`): Pizarra marina `#182238`
    - Texto principal (`onBackground` / `onSurface`): Blanco glacial `#F3F6FC`
    - Texto secundario (`onSurfaceVariant`): Gris bruma `#AAB5C8`
    - Acento primario (`primary`): Turquesa menta `#55E6C1` (texto sobre primario `#062922`)
    - Acento secundario (`secondary`): Índigo suave `#8585FF`
    - Advertencia verificada: Ámbar `#F5B041` · Error real / bloqueo: Coral sobrio `#FF6B6B`
  - **Modo Claro**:
    - Fondo base (`background`): Blanco cálido `#F7F8FC`
    - Superficie primaria (`surface`): Blanco puro `#FFFFFF` y contenedor suave `#EEF2F9`
    - Texto principal (`onSurface`): Tinta oscura `#182033` · Secundario: `#4E5B73`
    - Acento primario accesible (contraste WCAG AA ≥ 4.5:1): Turquesa profundo `#0D7A63` · Secundario: Índigo `#4F4FD9`
- **Tipografía (Google Fonts locales vía `font-util` con licencia SIL OFL verificada)**:
  - Títulos y cabeceras: **Plus Jakarta Sans** (geométrica, limpia, excelente legibilidad en paneles de control).
  - Cuerpo y etiquetas: **Inter** (claridad óptima en listas densas de paquetes y diagnósticos).
  - Hashes SHA-256, IPs y evidencia ADB: **JetBrains Mono** (alineación monoespaciada clara para auditoría).
- **Geometría, Accesibilidad y Movimiento**:
  - Tarjetas con radio `20.dp`, chips compactos de `10.dp`, áreas táctiles mínimas de `48.dp x 48.dp`, soporte completo de escalado `sp`, etiquetas semánticas TalkBack e iconos emparejados siempre con texto (nunca depender solo del color).
  - Preferencia global **Reducir movimiento** (detecta ajuste del sistema Android + interruptor propio en Ajustes) que sustituye transiciones elásticas por fundidos instantáneos o sin animación.

### Wireframes Textuales de Pantallas Clave (Oscuro y Claro)

#### 1) Inicio y Resumen (`Home`)
```text
┌──────────────────────────────────────────────────────────────┐
│ [Icono TVeo]  TVeoClean                  [Chip: Sin conexión]│
│               Centro de control seguro para tu televisor     │
├──────────────────────────────────────────────────────────────┤
│ ┌── TARJETA HERO (Sup: #121A2B oscuro / #FFFFFF claro) ────┐ │
│ │  ┌────────────────────────┐  Sala Principal (No enlazada)│ │
│ │  │   [Marco TV 16:9 con   │  Android TV / Google TV      │ │
│ │  │    ondas LAN sutiles]  │  Ninguna modificación hecha  │ │
│ │  └────────────────────────┘                              │ │
│ │  [ Botón Primario #55E6C1: Vincular mi televisor ]       │ │
│ └──────────────────────────────────────────────────────────┘ │
│                                                              │
│ ACCIONES RÁPIDAS (Cuadrícula 2x2 adaptable)                  │
│ ┌─────────────────────────┐  ┌─────────────────────────────┐ │
│ │ [🔍] Analizar mi TV     │  │ [🎨] Style Studio           │ │
│ │ Solo lectura · Requiere │  │ Galería curada e importador │ │
│ │ TV conectada            │  │ GitHub disponible ahora     │ │
│ └─────────────────────────┘  └─────────────────────────────┘ │
│ ┌─────────────────────────┐  ┌─────────────────────────────┐ │
│ │ [↺] Centro Restauración │  │ [🎮] Control y Utilidades   │ │
│ │ 0 snapshots guardados   │  │ Disponible al conectar ADB  │ │
│ └─────────────────────────┘  └─────────────────────────────┘ │
│                                                              │
│ ESTADO VERIFICADO Y ACTIVIDAD RECIENTE                       │
│ ┌──────────────────────────────────────────────────────────┐ │
│ │ ⓘ Sin datos inventados: conecta tu TV en red local para  │ │
│ │   leer modelo real, versión de Android y paquetes.       │ │
│ └──────────────────────────────────────────────────────────┘ │
├──────────────────────────────────────────────────────────────┤
│ [🏠 Inicio] [📺 Mi TV] [✨ Studio] [🕒 Historial] [⚙️ Ajustes]│
└──────────────────────────────────────────────────────────────┘
```

#### 2) Descubrimiento, Emparejamiento y Matriz de Plataforma (`Conectar`)
```text
┌──────────────────────────────────────────────────────────────┐
│ [←] Conectar televisor                     [? Guía paso a paso]│
│     Red local Wi-Fi · Tus claves ADB se guardan en Keystore  │
├──────────────────────────────────────────────────────────────┤
│ DISPOSITIVOS DETECTADOS EN TU RED (mDNS / NSD)               │
│ ┌──────────────────────────────────────────────────────────┐ │
│ │ 📺 Google TV Sala · 192.168.1.44:5555    [Emparejar/Abrir]│ │
│ │    Servicio: _adb-tls-pairing._tcp · Requiere código PIN │ │
│ └──────────────────────────────────────────────────────────┘ │
│                                                              │
│ CONEXIÓN MANUAL                                              │
│ ┌───────────────────────────────┐ ┌──────────┐ ┌───────────┐ │
│ │ IP / Host (ej. 192.168.1.50)  │ │ 5555     │ │ Conectar  │ │
│ └───────────────────────────────┘ └──────────┘ └───────────┘ │
│ [ Emparejar con código de 6 dígitos (Android 11+) ]          │
│                                                              │
│ ¿QUÉ TELEVISORES SON COMPATIBLES? (Honestidad de plataforma) │
│ • Android TV / Google TV: Soporte completo con ADB autorizado│
│ • Fire OS: Modo conservador (launcher bloqueado si hay riesgo)│
│ • Samsung Tizen / LG webOS / Roku: NO soportan ADB (Ficha    │
│   informativa · funciones marcadas como No disponibles)      │
│                                                              │
│ 🛡️ Seguridad LAN: Nunca abras el puerto 5555 en tu router.  │
└──────────────────────────────────────────────────────────────┘
```

#### 3) Análisis de Solo Lectura y Plan Explicado (`Analizar`)
```text
┌──────────────────────────────────────────────────────────────┐
│ [←] Análisis de solo lectura          [Modo Avanzado: OFF ○] │
│     Sony Bravia 4K (Android 12) · 0 cambios realizados aún   │
├──────────────────────────────────────────────────────────────┤
│ RESUMEN OBSERVADO (Evidencia: pm list packages + getprop)    │
│ [ 142 Paquetes ]  [ 6 Recomendados ]  [ 38 Protegidos 🔒 ]   │
│                                                              │
│ RECOMENDACIONES REVERSIBLES (Desactivar con pm disable-user) │
│ ┌──────────────────────────────────────────────────────────┐ │
│ │ [✓] Recomendaciones promocionales                        │ │
│ │     com.google.android.tungsten.setupwraith              │ │
│ │     Riesgo: Bajo · Impacto: Reduce sugerencias en inicio │ │
│ │     Evidencia: Perfil base v1.0 · Reversible con 1 toque │ │
│ └──────────────────────────────────────────────────────────┘ │
│ ┌──────────────────────────────────────────────────────────┐ │
│ │ [ ] Servicio de fabricante desconocido                   │ │
│ │     com.vendor.custom.agent (Sin marcar por defecto)     │ │
│ │     ⚠ Incertidumbre: No figura en perfil verificado      │ │
│ └──────────────────────────────────────────────────────────┘ │
│ ┌──────────────────────────────────────────────────────────┐ │
│ │ [🔒] Google Play Services / Wi-Fi / Launcher Stock       │ │
│ │     BLOQUEADO · Protegido para evitar pérdida de HOME/red│ │
│ └──────────────────────────────────────────────────────────┘ │
│                                                              │
│ [ Crear Snapshot y Revisar Confirmación (1 cambio) ]         │
└──────────────────────────────────────────────────────────────┘
```

#### 4) Style Studio, Preview 16:9 e Importador GitHub (`Style Studio`)
```text
┌──────────────────────────────────────────────────────────────┐
│ Style Studio                        [+ Importar de GitHub]   │
│ Galería declarativa · Temas, perfiles y ajustes verificados  │
├──────────────────────────────────────────────────────────────┤
│ [🔍 Buscar paquetes...]                                      │
│ (Descubrir) (Temas) (Ajustes) (Perfiles) (Addons) (Instalados)│
├──────────────────────────────────────────────────────────────┤
│ VISTA PREVIA EN MARCO TV 16:9 (Normalizada con tveo.json)    │
│ ┌──────────────────────────────────────────────────────────┐ │
│ │  ╔════════════════════════════════════════════════════╗  │ │
│ │  ║  21:40                           Wi-Fi · Bravia    ║  │ │
│ │  ║   Fondo: #0E1729 · Columnas: 5 · Movimiento: Suave  ║  │ │
│ │  ║  ┌────┐  ┌────┐  ┌────┐  ┌────┐  ┌────┐            ║  │ │
│ │  ║  │App1│  │App2│  │App3│  │App4│  │App5│  (Dock)    ║  │ │
│ │  ║  └────┘  └────┘  └────┘  └────┘  └────┘            ║  │ │
│ │  ╚════════════════════════════════════════════════════╝  │ │
│ │  Destino: [🎯 Launcher TV]   Confianza: [🛡️ Oficial]     │ │
│ │  Comparador: [ Actual ] ◄───► [ Propuesto (Diff: 4) ]    │ │
│ └──────────────────────────────────────────────────────────┘ │
│                                                              │
│ PAQUETES DESTACADOS E IMPORTADOS                             │
│ ┌──────────────────────────────────────────────────────────┐ │
│ │ Brisa Nocturna · v1.2.0 · por TVeoClean Team             │ │
│ │ Licencia: MIT · SHA fijado: 8f3d9a1... · [Ver Diff/Aplicar]│
│ └──────────────────────────────────────────────────────────┘ │
│ ┌──────────────────────────────────────────────────────────┐ │
│ │ Tema Heredado Aurora (config.json convertido localmente) │ │
│ │ [⚠ Enlace sin verificar] · 1 clave omitida · [Inspeccionar]│
│ └──────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────┘
```

> **Comportamiento en Modo Claro (`#F7F8FC` / `#FFFFFF`)**: El marco de vista previa 16:9 mantiene fielmente los colores reales que tendrá el televisor (para no falsear la previsualización del tema), mientras que toda la interfaz envolvente de la app móvil (barras, tarjetas, chips, comparadores de diferencias y avisos de seguridad) utiliza superficies blancas limpias `#FFFFFF`, bordes sutiles `#DCE3F0`, texto tinta `#182033` y acentos de alto contraste `#0D7A63`.

---

## 3. Key Product Decisions & Trade-Offs

### Decisión 1: Transporte ADB nativo + Almacenamiento Criptográfico en Android Keystore
- **Enfoque elegido**: Implementar un cliente de protocolo ADB TCP directo en Kotlin con soporte de par de claves RSA 2048 protegido por `AndroidKeyStore` (o cifrado envolvente AES-GCM respaldado por Keystore cuando el handshake ADB requiere firma raw del digest RSA) y descubrimiento en red local mediante `NsdManager`.
- **Por qué**: Evita depender de binarios externos no auditables, jamás expone claves privadas en texto plano ni en logs y permite inyectar un `FakeAdbTransport` determinista para probar timeouts, desconexiones a mitad de lote y denegaciones de autorización.

### Decisión 2: Paquetes Declarativos (`tveo.json`) con Prohibición Total de Ejecución de Scripts
- **Enfoque elegido**: Ningún paquete `theme`, `tweak`, `profile` o `debloat-profile` puede incluir ni sugerir comandos shell, scripts (`.sh`, `.py`, `.js`, `.bat`), HTML/SVG activos ni binarios. El motor interno de TVeoClean traduce únicamente propiedades de una **lista blanca tipada** y nombres de paquete Android validados por regex (`^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z][a-zA-Z0-9_]*)+$`) a operaciones cerradas.
- **Por qué**: Elimina de raíz la ejecución remota de código (RCE) al importar repositorios comunitarios desde GitHub. Los `addon` de tipo APK se aíslan en un flujo separado que extrae el certificado X.509 real del archivo descargado, compara su huella SHA-256 contra fuentes de confianza independientes y exige confirmación reforzada del usuario.

### Decisión 3: Rollback Diferencial Basado en Estado Real (Nunca "A Ciegas")
- **Enfoque elegido**: Antes de aplicar cualquier lote o paquete, `RestoreManager` guarda un `Snapshot` verificable en Room + almacenamiento interno privado (`filesDir/packs/<id>/<commitSha>/`). Al aplicar o revertir, `Engine` vuelve a consultar el estado real en la TV lote a lote; si algo falla, detiene la cola restante y calcula el diff exacto entre el estado observado actual y el snapshot.
- **Por qué**: Si el usuario cambió algo manualmente en el televisor entre sesiones o si el Wi-Fi cae en el paquete 2 de 5, un script ciego causaría inconsistencias. Además, **jamás se desactiva el launcher original** sin verificar antes que un launcher alternativo compatible está instalado, responde al intent `HOME` y dispone de ruta de retorno comprobada.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Diagrama de Arquitectura Modular (Dentro de `:app`)

```text
┌────────────────────────────────────────────────────────────────────────────┐
│                        UI LAYER (Jetpack Compose + M3)                     │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐ ┌──────────────┐  │
│  │  Inicio  │ │  Mi TV   │ │  Style   │ │  Historial & │ │  Privacidad, │  │
│  │  & Onb.  │ │(Conectar,│ │  Studio  │ │  Snapshots / │ │  Control Rem.│  │
│  │          │ │ Analizar,│ │ (Catálogo│ │  Revertir    │ │  Ajustes &   │  │
│  │          │ │ Apps)    │ │ +GitHub) │ │              │ │  Licencias   │  │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └──────┬───────┘ └──────┬───────┘  │
│       └────────────┴────────────┴──────────────┴────────────────┘          │
│                                 │ StateFlow (Sealed UI States)             │
│                         ┌───────▼────────┐                                 │
│                         │ ui.design      │ Tokens, TV 16:9 Preview Frame,  │
│                         │ Design System  │ Diff Viewer, Trust Badges       │
│                         └───────┬────────┘                                 │
└─────────────────────────────────┼──────────────────────────────────────────┘
                                  ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                     DOMAIN & ENGINE LAYER (Pure Kotlin)                    │
│  ┌────────────────────┐  ┌────────────────────┐  ┌──────────────────────┐  │
│  │ core.platform      │  │ core.engine        │  │ core.packs           │  │
│  │ • TvDriver Contract│  │ • ReadOnlyAnalyzer │  │ • TveoManifest v1    │  │
│  │ • AndroidTvDriver  │  │ • ProtectedPkgGuard│  │ • LegacyConfigAdapter│  │
│  │ • FireOsDriver     │  │ • BatchTxExecutor  │  │ • WhitelistValidator │  │
│  │ • NonAdbInfoDriver │  │ • DiffRollbackCalc │  │ • ApkCertInspector   │  │
│  └─────────┬──────────┘  └─────────┬──────────┘  └──────────┬───────────┘  │
│            │                       │                        │              │
│  ┌─────────▼──────────┐  ┌─────────▼──────────┐  ┌──────────▼───────────┐  │
│  │ core.device        │  │ core.restore       │  │ core.privacy &       │  │
│  │ • CapabilityMatrix │  │ • SnapshotManager  │  │ core.profiles        │  │
│  │ • HomeSafetyGuard  │  │ • SafeExportRedact │  │ • EvidenceVerifier   │  │
│  └─────────┬──────────┘  └─────────┬──────────┘  └──────────┬───────────┘  │
└────────────┼───────────────────────┼────────────────────────┼──────────────┘
             ▼                       ▼                        ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                    INFRASTRUCTURE & SECURITY LAYER                         │
│  ┌────────────────────────┐ ┌────────────────────┐ ┌────────────────────┐  │
│  │ core.adb               │ │ Local Persistence  │ │ GitHub Importer    │  │
│  │ • AdbSocketTransport   │ │ • Room Database    │ │ • StrictUrlParser  │  │
│  │ • NsdTvDiscovery       │ │ • DataStore Prefs  │ │ • ShaCommitPinning │  │
│  │ • KeystoreAdbAuth      │ │ • Isolated Private │ │ • HostAllowlist    │  │
│  │ • WirelessPairingTls   │ │   Pack Storage     │ │ • Size/MIME/SHA256 │  │
│  └────────────────────────┘ └────────────────────┘ └────────────────────┘  │
└────────────────────────────────────────────────────────────────────────────┘
```

### Contrato `tveo.json` (Esquema v1.0) y Límites Centralizados
- **Campos obligatorios validados**: `schema` (`"1.0"`), `type` (`theme` | `tweak` | `addon` | `debloat-profile` | `profile`), `id` (`namespace.slug`), `name`, `version` (SemVer), `description`, `author`, `license`, `credits`, `target` (`mobile-ui` | `tv-launcher` | `tv-profile` | `tv-addon`), `compat` (rangos de app, Android SDK y familia de TV), `files` (ruta relativa normalizada, rol, MIME permitido, bytes y `sha256`), `capabilitiesRequested`, `preview` y `changelog`.
- **Constantes de seguridad inmutables (`PackSecurityLimits`)**:
  - Manifiesto máximo: `256 KB` · Imagen individual máxima: `10 MB` · Máximo imágenes por paquete: `100` · Tamaño total paquete de datos: `50 MB` · Profundidad máxima de ruta relativa: `4` niveles.
  - Dominios HTTPS exclusivos permitidos en importación: `github.com` (entrada), `api.github.com` (resolución de árbol/commit SHA) y `raw.githubusercontent.com` (descarga fijada por `<owner>/<repo>/<40-hex-sha>/<path>`), bloqueando redirecciones cruzadas a dominios externos.
  - Extensiones bloqueadas en paquetes declarativos: `.sh`, `.bat`, `.cmd`, `.ps1`, `.py`, `.js`, `.jar`, `.so`, `.dex`, `.elf`, `.html`, `.svg`.

### Modelo de Datos Local (Room + DataStore)
- `TvDeviceEntity` y `TvCapabilityEntity`: Dispositivos autorizados, huella pública (sin secretos), familia detectada y capacidades con su evidencia.
- `AnalysisRunEntity` y `ChangePlanEntity`: Inventario leído en modo solo lectura, recomendaciones justificadas y decisiones explícitas del usuario.
- `SnapshotEntity` y `AuditEventEntity`: Estado exacto previo (`pm`, propiedades de tema/launcher, hashes de fondos), integridad SHA-256, historial auditable y redacción automática de IPs/códigos al exportar diagnósticos.
- `InstalledPackEntity`: Paquetes instalados fijados por `commitSha`, origen, clasificación de confianza (`OFFICIAL`, `VERIFIED`, `UNVERIFIED_LINK`), archivos verificados y puntero a su snapshot de reversión.
- `UserPreferences` (DataStore): Tema visual (`DARK`, `LIGHT`, `SYSTEM`), `reduceMotion`, `advancedAdbView` e idioma/accesibilidad.

---

## 5. Análisis de Riesgos de Seguridad y Mitigaciones

| Riesgo identificado | Nivel | Mitigación arquitectónica en TVeoClean |
| :--- | :--- | :--- |
| **Pérdida de pantalla de inicio (Soft-brick de HOME)** | Crítico | `HomeSafetyGuard` bloquea de forma incondicional desactivar el launcher activo salvo que exista otro launcher verificado instalado, habilitado y probado. |
| **Desactivación de Wi-Fi, ADB, Play Store o entradas HDMI** | Crítico | `ProtectedPackageRegistry` compilado en código y versionado; ningún `debloat-profile` externo puede sobreescribirlo ni preseleccionar paquetes desconocidos. |
| **Mutación maliciosa de rama `main` en GitHub (Supply-chain)** | Alto | El importador resuelve primero la rama/tag a un commit SHA inmutable de 40 caracteres y descarga todos los archivos exclusivamente desde ese SHA, verificando el SHA-256 byte a byte. |
| **Falsa sensación de confianza ("Hash válido = Autor seguro")** | Alto | La UI distingue claramente *Integridad de descarga* de *Confianza de origen*. Todo enlace directo sin firma criptográfica en catálogo curado se etiqueta como **Enlace sin verificar** con confirmación expresa sin premarcar. |
| **Path Traversal (`../`), Zip-Slip o desbordamiento de memoria** | Alto | Normalización canónica de rutas, rechazo de enlaces simbólicos/rutas absolutas, lectura por streams con corte inmediato al superar `PackSecurityLimits` y decodificación de bitmaps con `inJustDecodeBounds` antes de alojar memoria. |
| **Exposición de clave privada ADB o puerto 5555 a Internet** | Alto | Claves gestionadas con `AndroidKeyStore`, prohibición de loggear payloads de autenticación y avisos claros en la guía de conexión para operar exclusivamente en LAN privada. |

---

## 6. Hoja de Ruta Incremental por Fases

1. **Fase 1 — Sistema Visual, Identidad y Navegación Funcional**: Configuración de `TVeoClean`, icono adaptativo original, tipografías locales OFL, tema oscuro/claro (`#0B1020` / `#F7F8FC`), componentes accesibles con `testTag`, reducción de movimiento, pantallas principales navegables (`Inicio`, `Mi TV`, `Style Studio`, `Historial`, `Ajustes`) con estados reales (sin simular una TV falsa conectada en producción) y primeras pruebas Robolectric/UI.
2. **Fase 2 — Conexión ADB, Keystore y Análisis de Solo Lectura**: Transporte ADB TCP + par RSA en Android Keystore, descubrimiento mDNS (`NsdManager`), emparejamiento inalámbrico por código cuando aplique, adaptadores `TvDriver` honestos por plataforma, analizador 100% solo lectura y pruebas contra servidor ADB simulado (`FakeAdbServer`).
3. **Fase 3 — Plan de Cambios, Lista Protegida, Snapshots y Rollback**: Motor transaccional por lotes con Room, bloqueo duro de paquetes críticos, confirmación obligatoria, verificación post-cambio leyendo estado real, restauración diferencial y exportación segura redactada.
4. **Fase 4 — Style Studio, Esquema `tveo.json` y Adaptador Heredado**: Parser y validador estricto `tveo.json`, lista blanca de claves, convertidor local no destructivo de temas heredados (`config.json` + `wallpaper.jpg` / `wallpapers/`), visor de diferencias (`Actual → Propuesto`) y marco interactivo TV 16:9.
5. **Fase 5 — Importador Seguro desde GitHub**: Resolución a commit SHA vía API oficial, allowlist estricta de hosts, límites de tamaño/MIME, cálculo SHA-256, etiquetas de confianza y batería exhaustiva de pruebas contra repositorios/archivos maliciosos.
6. **Fase 6 — Aplicación Transaccional de Paquetes y Botón Revertir**: Integración de Style Studio con `SnapshotManager` y `BatchTxExecutor`, historial de versiones fijadas por commit y reversión en un toque.
7. **Fase 7 — Complementos APK Verificados, Control Remoto e Informe de Privacidad**: Inspector X.509 de certificados APK con consentimiento reforzado, mando remoto / captura según capacidades reales de la TV e informe de privacidad basado en evidencia comprobable.
8. **Fase 8 — Endurecimiento, Accesibilidad, Licencias y Documentación Reproducible**: Auditoría final de permisos, pruebas completas unitarias/Robolectric, avisos MIT/OFL y guía reproducible de compilación y prueba en TV real.
