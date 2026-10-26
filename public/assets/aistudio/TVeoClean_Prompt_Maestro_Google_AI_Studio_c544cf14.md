# TVeoClean — Prompt maestro para Google AI Studio

**Versión del documento:** 1.0 · **Fecha:** 26 de octubre de 2026  
**Objetivo:** crear una app Android nativa para administrar, limpiar y personalizar televisores compatibles, con una experiencia visual premium y un Style Studio capaz de importar paquetes comunitarios desde GitHub con controles de seguridad estrictos.

## Cómo usar este documento

1. Adjunta a Google AI Studio el código fuente real de la app de referencia siempre que la plataforma lo permita. Si no puede acceder al repositorio, descarga el ZIP de GitHub y adjúntalo. No le pidas que invente la arquitectura basándose únicamente en el README.
2. Pega el bloque **Instrucciones de sistema** del archivo `TVeoClean_System_Instructions.txt` en el campo *System instructions / Custom instructions*.
3. Pega la sección **Prompt maestro** de este documento como primer mensaje del proyecto.
4. Trabaja con los prompts de fase al final. Pide que complete una fase, revise compilación y pruebas, y documente el resultado antes de pasar a la siguiente.

> **Regla técnica:** una PWA o página web no puede sustituir por sí sola a una app Android nativa para abrir conexiones ADB TCP y gestionar su autenticación. Un prototipo web sirve para validar el diseño y el flujo con datos simulados; la conexión real con el televisor, el almacenamiento seguro de claves, la instalación de APK y las operaciones sobre el sistema deben implementarse y probarse en Android nativo.

---

# PROMPT MAESTRO — COPIAR DESDE AQUÍ

## 1. Rol y forma de trabajo

Actúa como un equipo senior compuesto por: arquitecto Android, ingeniero Kotlin, especialista en ADB y redes, experto en seguridad de aplicaciones, diseñador de producto/UI-UX, ingeniero de pruebas y mantenedor de proyectos open source.

El objetivo no es generar una maqueta convincente pero vacía. Debes construir **TVeoClean**, una app mantenible, modular, comprobable y segura. Inspecciona los archivos reales que se te proporcionen antes de editar. Distingue en todo momento entre: (a) comportamiento ya comprobado en el código, (b) cambios que vas a implementar y (c) limitaciones que todavía no se han podido probar.

No afirmes que una función está implementada, que una TV es compatible, que una compilación funciona o que una prueba pasó si no hay evidencia. No simules ADB, GitHub, seguridad, restauraciones ni compatibilidad multiplataforma en la versión de producción. Si una función no puede verificarse en el entorno disponible, marca el estado como **Pendiente de prueba real**.

Trabaja por fases. Al comenzar, inspecciona el proyecto y entrega un plan de arquitectura, dependencias, riesgos, módulos, modelo de datos y pruebas. No reescribas todo de cero sin comparar la base existente. Al terminar cada fase, muestra archivos modificados, comandos ejecutados, resultados reales de pruebas, riesgos restantes e instrucciones exactas para probar. No continúes a la siguiente fase si la anterior no compila, salvo que expliques claramente el bloqueo y entregues el mejor resultado parcial posible.

## 2. Producto e identidad

- **Nombre:** TVeoClean.
- **Package/application ID propuesto:** `app.tveoclean` (verifica que no se reutilice por accidente el identificador de los proyectos de referencia).
- **Idioma inicial:** español. Arquitectura preparada desde el día uno para inglés y otros idiomas, incluida compatibilidad RTL.
- **Plataforma primaria:** app Android para teléfono, que administra un televisor compatible por la red local.
- **Objetivo:** ayudar a detectar y desactivar de forma reversible aplicaciones no deseadas, recuperar el estado anterior, gestionar el launcher cuando sea compatible, mejorar privacidad sin afirmaciones no demostradas y personalizar la experiencia mediante temas, ajustes y complementos de la comunidad.
- **Público objetivo:** usuarios normales que no programan, pero también usuarios avanzados que quieren revisar cada operación.
- **Privacidad:** sin cuenta obligatoria, anuncios, analítica de terceros ni envío automático de datos personales. Las conexiones a GitHub solo suceden cuando el usuario importa un paquete, abre el catálogo, comprueba actualizaciones o envía manualmente un informe.

La app debe tener marca, icono, ilustraciones, textos, composición, componentes y sistema visual propios. Puede aprender del flujo funcional y de la claridad de Clean Your TV, pero **no copies píxel por píxel** sus pantallas, capturas, ilustraciones, nombre, iconos, logotipo, textos distintivos ni recursos gráficos. Conserva atribuciones y licencias de todo código o recurso reutilizado. No sugieras que TVeoClean es una aplicación oficial de Clean Your TV.

## 3. Proyectos de referencia que debes inspeccionar

Repositorios y web de referencia:

- App Android: https://github.com/udhaykumarbala/cleanyourtv-app
- Proyecto principal, launcher, perfiles y temas: https://github.com/Gathin23/cleanyourtv
- Web y catálogo: https://cleanyour.tv/
- Documentación de temas heredados: https://github.com/Gathin23/cleanyourtv/blob/main/themes/README.md
- Documentación de módulos/tarjetas y demás contratos presentes en el repositorio: localízala y léela en el código real antes de cambiar el sistema.

Primero comprueba qué rama, commit, versión, módulos, estructura Gradle, minSdk, firmas, licencias, pruebas y capacidades están realmente presentes en los archivos entregados. Lee, según existan, `README`, `CHANGELOG`, `CONTRIBUTING`, `SECURITY`, `PRIVACY`, `THIRD-PARTY-NOTICES`, reglas de contribución, especificaciones de tarjetas y temas, módulo ADB y configuración de compilación. Si un archivo no existe, indícalo; no inventes su contenido.

La base de referencia ya utiliza Kotlin y Jetpack Compose para una app Android que habla ADB con la TV. El proyecto de launcher usa temas basados en `config.json` y fondos. La app de referencia tiene un flujo conectar → analizar → limpiar, prioriza desactivar antes que desinstalar, incorpora restauración y protege paquetes críticos. Debes verificar estos comportamientos en el código de la versión que te entreguen y preservar los que funcionen.

Si el proyecto se distribuye bajo MIT, conserva los avisos obligatorios y añade los avisos de terceros correspondientes. Respeta también licencias independientes como Apache-2.0 u OFL cuando corresponda. La licencia del repositorio no concede derechos automáticos sobre todos los fondos, iconos, marcas o paquetes aportados por terceros: valida cada recurso por separado.

## 4. Visión general de la aplicación

La experiencia principal debe poder explicarse en tres pasos:

1. **Conectar:** descubrir una TV compatible en la red local o introducir su dirección de forma manual; guiar al usuario para activar y autorizar ADB.
2. **Analizar:** leer estado, modelo, versión de Android, aplicaciones instaladas y capacidades que realmente se puedan verificar. Mostrar un plan legible, sin cambiar nada todavía.
3. **Aplicar de forma segura:** crear snapshot, mostrar los cambios, solicitar confirmación explícita, ejecutar en lotes pequeños, comprobar el resultado, conservar un historial y ofrecer restauración.

La navegación principal puede usar una barra inferior con **Inicio**, **Mi TV**, **Style Studio**, **Historial** y **Ajustes**. En pantallas amplias, usa navegación adaptable. No sobrecargues la pantalla inicial con tablas técnicas; el detalle ADB debe vivir en una vista avanzada.

### Pantallas mínimas

- Bienvenida y onboarding ilustrado de tres pasos.
- Inicio/resumen: estado de TV, marca/modelo detectados, estado de protección, tema activo, última revisión y acciones rápidas.
- Descubrimiento y conexión a TV.
- Asistente de depuración y emparejamiento.
- Análisis del dispositivo y plan de cambios.
- Personalizar plan: categorías y acciones individuales con riesgo explicado.
- Progreso de operación con etapas, registro humano y opción segura de detener cuando sea posible.
- Resultado final, lista de cambios verificados y errores recuperables.
- Gestor de aplicaciones: estado, ocultar/restaurar cuando esté soportado, búsqueda y filtros.
- Centro de restauración y snapshots.
- Style Studio con catálogo, importador GitHub, ficha de paquete, comparación de cambios, vista previa, instalación/aplicación, actualización y reversión.
- Mando a distancia y herramientas compatibles con el dispositivo detectado.
- Informe de privacidad basado en señales verificables.
- Ajustes, privacidad, licencias, soporte e información de diagnóstico.

## 5. Diseño visual: parecido en propósito, diferente en ejecución

Diseña una interfaz premium para un producto de hogar conectado, con personalidad visual de **“centro de control tranquilo”**: tecnológica pero cálida, ordenada, accesible y elegante. No copies la estética exacta del sitio de referencia ni su launcher.

### Dirección artística

- **Tema oscuro predeterminado:** fondo tinta azulado `#0B1020`, superficies `#121A2B` y `#182238`; texto principal `#F3F6FC`, texto secundario `#AAB5C8`.
- **Acento primario:** turquesa/menta `#55E6C1`; acento secundario índigo suave `#8585FF`; el ámbar se reserva para advertencias y el rojo para errores reales.
- **Tema claro:** blanco cálido `#F7F8FC`, superficies `#FFFFFF`, texto oscuro `#182033`, conservando una versión accesible de los acentos.
- **Gradientes:** leves, usados en cabeceras, foco activo o ilustraciones abstractas; nunca detrás de cada tarjeta. Evita los neones intensos, el exceso de glassmorphism y las sombras pesadas.
- **Tipografía:** jerarquía clara de títulos, encabezados, cuerpo y etiquetas. Usa fuentes de Android o una tipografía con licencia compatible; verifica la licencia antes de incluir archivos de fuente.
- **Formas:** tarjetas de esquinas redondeadas moderadas (16–24 dp), chips compactos, iconografía lineal coherente y botones con estados visibles.
- **Animación:** transiciones breves y funcionales, foco suave, progreso comprensible y microinteracciones discretas. Ofrece reducción de movimiento; no uses animaciones que bloqueen operaciones ni comprometan dispositivos modestos.
- **Ilustraciones:** crea recursos originales: una TV estilizada, señales de conexión, capas de personalización, escudos abstractos y previews propias. No descargues capturas ajenas para reutilizarlas como si fueran propias.

### Principios de composición

- Prioriza jerarquía, aire y lectura por encima de mostrar muchos datos.
- Cada pantalla debe tener un título claro, una frase que explique su utilidad, un contenido principal y una acción primaria reconocible.
- Mantén tamaños táctiles apropiados, contraste WCAG cuando sea aplicable, soporte TalkBack, etiquetas semánticas, escalado del texto y orientación adaptable.
- Usa iconos consistentes; no dependas solo del color para indicar éxito, advertencia o peligro.
- Los errores deben explicar qué pasó, qué no se modificó y qué puede hacer el usuario ahora.
- En las operaciones delicadas, separa claramente acciones seguras, potencialmente disruptivas y no disponibles.

### Inicio

- Cabecera con saludo, estado de conexión y avatar/icono TVeoClean, sin perfil obligatorio.
- Tarjeta hero de la TV con modelo, sistema, señal de conexión y una representación gráfica original. Si no se ha conectado, muestra una ilustración y un CTA para añadir TV.
- Acciones rápidas: **Analizar mi TV**, **Personalizar**, **Restaurar** y **Control remoto**; deshabilita o explica las que todavía no apliquen.
- Sección “Estado de mi TV” basada únicamente en datos obtenidos de la conexión, sin indicadores inventados.
- Sección de actividad reciente y tema activo.

### Style Studio

Esta área debe sentirse como una pequeña galería curada y un gestor de paquetes, no como una pantalla de administrador de archivos.

- Encabezado con buscador, botón **Importar desde GitHub**, filtros y tabs: **Descubrir**, **Temas**, **Ajustes**, **Complementos**, **Perfiles de limpieza**, **Instalados** y **Actualizaciones**.
- Tarjetas con miniatura o preview original, nombre, autor, descripción corta, compatibilidad, versión, licencia y una etiqueta de confianza claramente visible.
- Categorías y filtros por compatibilidad, tipo, popularidad solo si hay datos legítimos, fecha de actualización, autor y licencia.
- Vista previa de tema dentro de un marco de TV 16:9 con navegación por imágenes y controles visuales de color, dock, columnas, fondo y movimiento según lo que soporte el tema.
- Permite comparar el tema actual con el propuesto antes de aplicar.
- Distingue los temas que cambian la app móvil de los que cambian el launcher de la TV. Nunca presentes una personalización local del teléfono como si hubiese sido aplicada al televisor.

## 6. Arquitectura y stack

Usa Android nativo con Kotlin, Jetpack Compose y Material 3. Mantén el stack real existente si es razonable; antes de cambiar versiones de Gradle, Android Gradle Plugin, Kotlin, JDK o dependencias, verifica compatibilidad y no inventes números de versión. Usa las versiones estables compatibles que el proyecto pueda resolver en la fecha de trabajo y documenta qué comprobaste.

Arquitectura propuesta, adaptada a la estructura real después de inspeccionarla:

- `app`: composición de dependencias, navegación y entry point.
- `core:adb`: transporte ADB, autenticación, descubrimiento, emparejamiento, sesión y timeouts.
- `core:device`: identidad del dispositivo, capacidades, detección de SO y compatibilidad.
- `core:engine`: plan de cambios, ejecución por transacciones, comprobación, reintentos acotados y rollback.
- `core:profiles`: perfiles de TV, procedencia, compatibilidad, índice y validación de firma.
- `core:packs`: `tveo.json`, importación de GitHub, hashes, parser heredado, límites y almacenamiento privado.
- `core:restore`: snapshots, historial, restauración parcial/completa y exportación de un paquete de recuperación seguro.
- `core:platform`: contrato `TvDriver` y modelo de capacidades por plataforma.
- `core:privacy`: verificaciones de privacidad con evidencia y límites.
- `feature:home`, `connect`, `analyze`, `clean`, `restore`, `apps`, `remote`, `studio`, `privacy`, `settings`, `about`.
- `ui:design`: tokens, componentes reutilizables, temas y estados accesibles.

Usa Coroutines/Flow y una arquitectura clara (por ejemplo, MVVM con repositorios). Usa Room para historial y snapshots estructurados cuando corresponda, DataStore para preferencias, Android Keystore para material criptográfico y almacenamiento interno privado para archivos importados. Usa el gestor de dependencias actual del proyecto si existe; evita añadir librerías duplicadas sin razón.

No añadas permisos innecesarios. La ubicación, contactos, cámara y almacenamiento público no deben pedirse por conveniencia. Si Android exige un permiso de red local o multicast en una API específica, verifica el requisito según la versión y explica por qué es necesario. Nunca almacenes claves ADB en texto claro ni las registres en logs.

## 7. ADB, conexión y protección de la TV

### Conexión

- Descubre dispositivos mediante el mecanismo compatible presente en el proyecto (por ejemplo mDNS/servicios de red local); permite añadir manualmente una IP/host y puerto cuando sea útil.
- Explica cómo activar Opciones de desarrollador y depuración de red usando instrucciones genéricas y notas específicas por marca solo si están verificadas.
- Implementa soporte de emparejamiento inalámbrico con código en dispositivos Android que lo permitan. No afirmes que todas las TVs lo soportan; detecta capacidad y muestra alternativas seguras.
- Vincula la autorización a la clave del dispositivo y guarda las credenciales en Android Keystore. Permite revocar la TV enlazada.
- Usa timeouts, cancelación, reconexión controlada, validación de host y estados de conexión claros.
- No sugieras abrir el puerto ADB de la TV hacia Internet ni automatices cambios en el router.
- Explica qué significa mantener o desactivar la depuración al terminar. Si la desactivación puede interrumpir la sesión o no es compatible, informa con claridad y deja la decisión al usuario.

### Análisis de solo lectura

Antes de proponer cambios, detecta lo que se pueda obtener de forma fiable: marca/modelo, versión, paquetes disponibles, estado habilitado/deshabilitado, launcher actual, conectividad y capacidades disponibles. El análisis no debe modificar nada.

Cada recomendación debe incluir:

- nombre y paquete;
- estado actual observado;
- motivo de la recomendación;
- impacto esperado y riesgo;
- evidencia (comando de lectura, dato o perfil y versión de perfil);
- si se puede restaurar y cómo;
- advertencias de compatibilidad.

No confundas un nombre de paquete sospechoso con prueba de que sea bloatware o telemetría. Si falta un perfil fiable para un modelo, usa un modo conservador y marca la incertidumbre.

### Aplicación y restauración

- Nunca desinstales paquetes como opción predeterminada. Prioriza la desactivación reversible para el usuario Android correspondiente.
- Antes de modificar: genera un snapshot suficiente para revertir exactamente todas las propiedades que administrarás, guarda inventario previo, configuración del launcher, tema y lista de archivos asociados.
- Muestra el plan final y exige confirmación explícita. En modo avanzado, permite revisar los comandos y sus efectos.
- Aplica cambios en lotes pequeños; después de cada lote, vuelve a consultar el estado real. No confíes únicamente en el código de salida de un comando.
- Usa operaciones idempotentes, estados explícitos y límites de reintentos. No repitas indefinidamente un comando que falla.
- El rollback debe calcularse con el snapshot y el estado actual, no ejecutarse a ciegas. Si un paso falla, detén el resto, comunica qué se completó y ofrece revertir lo ya aplicado.
- Comprueba los resultados después de aplicar. No muestres “Completado” si la comprobación no lo confirma.
- Conserva un mecanismo de seguridad para HOME. No desactives el launcher original hasta comprobar que el launcher alternativo se instaló, se puede abrir y es posible regresar.
- Si se incorpora un watchdog en la TV, implementa y prueba el mecanismo de recuperación real; no lo simules solo en la app del móvil.
- Conserva registros locales con fecha, operación, dispositivo, paquetes o claves afectados, resultado y referencia al snapshot. Redacta IPs, claves, códigos de emparejamiento y datos sensibles en informes exportados.
- Permite restaurar un solo paquete, un grupo de cambios, el launcher/tema o toda la sesión.
- Exporta, cuando esté implementado, instrucciones de recuperación reproducibles para ADB que no contengan secretos ni operaciones destructivas ajenas a los cambios registrados.

## 8. Compatibilidad por plataforma, sin promesas falsas

Diseña una arquitectura de adaptadores (`TvDriver`) que describa capacidades reales, pero no supongas que todas las plataformas permiten lo mismo.

- **Android TV / Google TV:** capacidad principal cuando hay acceso ADB autorizado y el modelo permite la operación. Aun dentro de esta familia, el launcher, las funciones de limpieza y los ajustes disponibles varían por fabricante y firmware.
- **Amazon Fire TV / Fire OS:** implementa solo funciones demostradas para el modelo y su versión. Si cambiar el launcher o deshabilitar aplicaciones presenta riesgos o no está soportado, ofrece modo de limpieza limitado y explica las restricciones.
- **Chromecast con Google TV, Google TV Streamer y otros dispositivos especiales:** detecta casos donde deshabilitar el launcher afecta casting, configuración inicial u otras funciones. No cambies el Home cuando el perfil no garantice recuperación.
- **Samsung Tizen, LG webOS, Roku y otros sistemas sin ADB Android:** no afirmes que pueden controlarse con comandos ADB. Prepara interfaces de adaptadores para el futuro y usa únicamente métodos oficiales o documentados y capacidades de usuario disponibles. En ausencia de un método probado, ofrece solo una ficha informativa y etiqueta las funciones como **No disponible en esta plataforma**.
- Para cada familia de TV muestra una matriz de capacidades: analizar, gestionar apps, cambiar launcher, aplicar temas, restaurar, mando remoto, y limitaciones.

Nunca uses root, exploits, bypass de restricciones del fabricante ni instrucciones para saltarte mecanismos de autorización. Nunca bloquees actualizaciones del sistema por defecto. No desactives Wi-Fi, Play Store, servicios básicos del mando, entradas HDMI, casting, accesibilidad, servicios de voz o componentes del sistema sin una razón verificada y una aprobación individual y comprensible. Los componentes esenciales se protegen siempre.

## 9. Style Studio: formato de paquetes `tveo.json`

Implementa un sistema de paquetes extensible que permita compartir personalizaciones mediante repositorios públicos de GitHub sin ejecutar código arbitrario. Tipos iniciales:

- `theme`: colores, layout, dock, columnas, movimiento, fondos, iconos e imágenes permitidos.
- `tweak`: cambio limitado de valores de configuración soportados.
- `addon`: complemento; puede ser un paquete de datos o, si es un APK real, debe pasar controles adicionales y confirmación reforzada.
- `debloat-profile`: recomendación declarativa de nombres de paquetes y razones, jamás comandos arbitrarios.
- `profile`: reglas de compatibilidad/capacidades para una marca/modelo, sujeto a firma y política de confianza.

Distingue en el esquema el destino del paquete, por ejemplo `mobile-ui`, `tv-launcher`, `tv-profile` o `tv-addon`, para no aplicar una configuración al dispositivo equivocado.

### Campos mínimos del manifiesto

Define y documenta una versión estable del esquema. El formato debe incluir como mínimo:

- `schema`: versión del esquema.
- `type`: tipo del paquete.
- `id`: identificador único y estable con espacio de nombres del autor.
- `name`, `version`, `description`, `author`.
- `license` y `credits`/atribuciones obligatorios cuando corresponda.
- `compat`: rangos de versión de TVeoClean, launcher, Android, plataforma y modelo compatibles.
- `target`: destino de aplicación.
- `files`: ruta relativa, rol, MIME esperado, tamaño y SHA-256 para cada archivo.
- `permissions` o `capabilitiesRequested`: capacidades que requiere y explicación de cada una.
- `preview`: imágenes de vista previa locales al paquete o referencias aprobadas.
- `changelog`: novedades y riesgos de esta versión.
- Para perfiles de limpieza: lista de paquetes sugeridos, razón y nivel de riesgo por cada uno; nunca un campo de comandos shell.
- Para APK: nombre de paquete, versión, SHA-256 del APK, huella SHA-256 del certificado de firma, permisos Android observados y requisitos de compatibilidad.

Valida el manifiesto con un esquema formal, por ejemplo kotlinx.serialization más validaciones de negocio. Rechaza campos obligatorios ausentes, tipos incorrectos, versiones imposibles, rutas absolutas, `..`, rutas duplicadas, enlaces simbólicos, archivos que salgan del directorio del paquete y claves de configuración no permitidas. No ignores silenciosamente errores de seguridad.

### Compatibilidad heredada con Clean Your TV

Permite importar temas existentes que contengan `config.json`, `wallpaper.jpg` y/o la carpeta `wallpapers/`, sin obligar al autor a editar su repositorio. Crea un adaptador de importación que:

1. identifique el formato heredado;
2. lea solamente las claves conocidas que implemente el launcher objetivo;
3. transforme los datos a un modelo interno normalizado compatible con `tveo.json`;
4. registre las claves ignoradas o no compatibles;
5. genere una vista previa del resultado y advierta si el tema utiliza funciones que la versión del launcher no soporta.

Nunca escribas ni modifiques el repositorio remoto del creador durante la importación. La conversión es local y queda dentro del almacenamiento privado de TVeoClean. No cambies el significado original de valores válidos. Si hay una clave desconocida o un tipo incorrecto, no la ejecutes ni la fuerces: informa que se omitió.

### Lista blanca de configuración

Los temas y tweaks de datos solo pueden ajustar claves expresamente autorizadas. Las claves deben tener tipo, límites y valor por defecto definidos en código. Valida colores, números, intervalos, listas de paquetes y dimensiones. Limita el número de imágenes y la cantidad total de bytes. Re-codifica o decodifica de forma aislada imágenes a formatos admitidos y dimensiones razonables; rechaza archivos malformados. No aceptes SVG activo, HTML, enlaces remotos arbitrarios ni contenido que pueda ejecutar scripts.

Límites iniciales configurables y testeados: hasta 10 MB por imagen; hasta 100 imágenes por paquete; tamaño máximo de manifiesto 256 KB; tamaño total del paquete de datos limitado (valor inicial 50 MB); profundidad de ruta acotada; y límites de memoria durante la decodificación. Los límites deben ser constantes centralizadas y documentadas, no números repetidos por el código.

## 10. Importador de GitHub: flujo obligatorio de resolución, validación y aplicación

La pantalla **Importar desde GitHub** debe permitir pegar un enlace a un repositorio, a una carpeta, a un archivo o a una rama/tag concreta. No basta con mostrar un formulario: implementa el flujo real con estados, manejo de errores, cancelación y pruebas.

### Paso 1 — Normalizar el enlace

- Acepta URLs HTTPS válidas de `github.com`; las consultas a API deben usar `api.github.com` y los contenidos sin procesar deben obtenerse desde `raw.githubusercontent.com` cuando corresponda.
- Rechaza esquemas distintos de HTTPS, URL malformadas, credenciales embebidas, puertos inesperados y hosts parecidos (por ejemplo, dominios que solo contengan la cadena “github.com”).
- Extrae owner, repository, ref y ruta del paquete con un parser estricto. Un enlace de repo, `tree`, `blob`, rama, tag o carpeta debe interpretarse explícitamente; si no se puede resolver sin ambigüedad, pide que el usuario proporcione otro enlace.
- Rechaza o controla redirecciones fuera de la lista permitida. No sigas enlaces arbitrarios presentes en README, manifiestos o metadatos.

### Paso 2 — Fijar una versión inmutable

- Consulta la API oficial de GitHub para resolver la rama/tag/ref a un commit SHA completo.
- Guarda el URL original, owner/repo, ruta, ref original, commit SHA completo y fecha de importación.
- Después de resolverlo, todas las lecturas deben estar fijadas a ese commit SHA; no continúes descargando de `main`, `master` u otra rama móvil.
- Si no se puede resolver el commit, hay límite de API, el repositorio fue eliminado o el contenido está inaccesible, detente y muestra un error recuperable. No sustituyas automáticamente el repositorio por otro ni pidas un token personal como primera solución.
- La resolución a SHA garantiza reproducibilidad del contenido seleccionado, pero **no verifica la identidad ni la honradez del autor**.

### Paso 3 — Descargar solo los archivos necesarios

- Obtén árbol/contenido de forma limitada. No descargues el repositorio completo si solo hace falta una carpeta o conjunto pequeño de archivos.
- Descarga únicamente los archivos declarados y permitidos, con límites de bytes, cantidad, tiempo y redirecciones.
- Nunca hagas checkout, compile, ejecutes scripts, cargues librerías del repositorio ni ejecutes GitHub Actions u otros workflows de terceros.
- Rechaza archivos `.sh`, `.bat`, `.cmd`, `.ps1`, `.py`, `.js`, `.jar`, `.so`, ejecutables, binarios desconocidos y cualquier instrucción ejecutable dentro de paquetes de tipo `theme`, `tweak`, `profile` o `debloat-profile`. No necesitan scripts para aplicar datos.
- Un APK puede admitirse exclusivamente como `addon` cuando el usuario lo solicite explícitamente y se cumplan los controles especiales descritos más adelante. No es una excepción para otros tipos de paquete.
- Aplica límites de tiempo, cancelación y reintentos acotados; respeta `Retry-After` y los límites comunicados por GitHub.

### Paso 4 — Validar estructura, integridad y contenido

- Localiza `tveo.json` en la carpeta seleccionada. Si no existe, intenta reconocer un tema heredado válido mediante `config.json` y sus recursos de fondos.
- Verifica el esquema, rutas relativas, compatibilidad, tipos MIME, tamaño y hash SHA-256 de cada archivo.
- Calcula el SHA-256 de los bytes recibidos y compáralo con el declarado. Si no coincide, cancela la importación.
- Aclara en UI que un hash ubicado en el mismo repositorio no acredita la identidad del autor; sirve para detectar cambios respecto al manifiesto fijado, no para demostrar confianza.
- Descomprime contenido, si el formato lo requiere, en un espacio privado aislado. Prevén zip-slip, rutas duplicadas, expansión desproporcionada, archivos anidados y agotamiento de memoria.
- No previsualices SVG, HTML ni formatos activos en un WebView. Las vistas previas se decodifican como imágenes de formatos permitidos.

### Paso 5 — Clasificar confianza

Usa etiquetas claramente diferenciadas:

- **Oficial:** paquete publicado en un catálogo oficial cuya clave verificadora está fijada dentro de la app.
- **Verificado:** paquete aceptado por un índice comunitario curado y firmado, con firma comprobada contra una clave de confianza instalada por un canal seguro.
- **Enlace sin verificar:** importación directa de un repositorio sin firma/curación reconocida.

Un repositorio público, un número alto de estrellas, un autor conocido, un commit SHA o un manifiesto válido **no son por sí solos prueba de que sea seguro**. No asciendas automáticamente un paquete al estado “Verificado”. Si la firma, versión del esquema o cadena de confianza no puede comprobarse, degrada el estado y explica por qué.

### Paso 6 — Pantalla previa y comparación de cambios

Antes de cambiar el teléfono o la TV, presenta una ficha con:

- nombre, tipo, versión, descripción, autor, repositorio, ruta y commit SHA;
- licencia, créditos, fecha de publicación si está disponible, changelog y compatibilidad detectada;
- nivel de confianza y su significado;
- lista de archivos, tamaños, hashes y permisos/capacidades solicitados;
- miniaturas y vista previa dentro de un marco de TV cuando sea tema;
- comparación **actual → propuesto** de colores, fondo, columnas, dock, animaciones, ajustes o lista de paquetes;
- lista de cada paquete que un perfil de limpieza pretende sugerir, con razón, nivel de riesgo y estado de protección;
- advertencias por claves ignoradas, incompatibilidades y metadatos sin verificar.

Para un enlace sin verificar, muestra un aviso destacado y exige una confirmación expresa adicional. No uses una casilla premarcada.

### Paso 7 — Snapshot, aplicación y verificación

Cuando el usuario pulse **Aplicar paquete**:

1. comprueba de nuevo compatibilidad, estado del dispositivo, hash y que la TV conectada sea la esperada;
2. guarda un snapshot antes de tocar la configuración activa: valores actuales, archivos de fondos, tema activo, opciones gestionadas y metadatos del paquete anterior;
3. guarda la versión importada y los recursos en el almacenamiento privado de la app, organizados por ID y commit;
4. escribe en una ubicación temporal; valida una última vez y cambia al estado activo de forma atómica cuando sea posible;
5. aplica solo las claves permitidas, usando el motor de la app, nunca comandos aportados por el repositorio;
6. vuelve a leer el estado y compara con el esperado;
7. si algo falla, detén el proceso y realiza rollback del paquete parcial según el snapshot; informa si la restauración fue completa o si necesita intervención;
8. registra resultado, cambios y hashes en el historial.

Tras finalizar, muestra **Ver aplicado**, **Revertir** y **Ver detalles**. El acceso a revertir debe estar siempre disponible desde el historial. La aplicación de un tema o ajuste nunca debe reemplazar el último snapshot válido hasta que las comprobaciones tengan éxito.

### Paso 8 — Revisar actualizaciones

Un paquete queda fijado a su commit importado. La app puede ofrecer **Buscar actualización**, pero no debe cambiar el paquete silenciosamente. Al encontrar una versión nueva, repite todo el pipeline: resolución SHA, descarga, validación, diferencias, snapshot y consentimiento. Permite volver a un commit anterior instalado.

## 11. Seguridad especial para perfiles de limpieza

Un `debloat-profile` solo declara una lista de nombres de paquetes y metadatos de recomendación. La app genera todos los comandos internamente a partir de un conjunto cerrado de operaciones admitidas. No existe ningún campo para shell, argumentos libres, comandos ADB personalizados ni scripts remotos.

Implementa una lista de paquetes protegidos, versionada y probada, que incluya como mínimo componentes necesarios de conectividad, Wi-Fi, interfaz de configuración, instalador/gestor de paquetes, Play Store cuando exista, servicios esenciales de Google cuando sean necesarios, mando/entradas, casting cuando se use, accesibilidad, componentes del sistema, configuración inicial y launcher de recuperación. La lista debe considerar alias y diferencias de fabricante. No permitas que un perfil de terceros reduzca o sobrescriba esa lista.

- Paquetes protegidos: bloqueo incondicional de acciones de desactivación.
- Paquetes desconocidos o sin evidencia suficiente: no se seleccionan por defecto; requieren explicación y revisión individual si la función permite gestionarlos.
- Perfiles comunitarios: aplican inicialmente en modo de simulación/plan, con cada paquete sin marcar hasta que la política de confianza y evidencia permitan otra decisión segura.
- Aplicar perfiles en lotes pequeños con validación después de cada lote.
- Prohíbe cambios que puedan cortar ADB o Wi-Fi antes de preparar un camino de recuperación.
- No digas “elimina toda la telemetría” si solo se desactivaron algunos paquetes. Usa una descripción precisa, limitada a lo comprobado.

## 12. Complementos APK

Los APK son código ejecutable y constituyen un riesgo diferente de un tema JSON. Trátalos como una categoría aislada con pantalla y consentimiento reforzado.

Antes de permitir la instalación en la TV, muestra y verifica:

- origen, repositorio y commit SHA fijado;
- nombre del paquete, versión, compatibilidad de Android/TV;
- SHA-256 calculado del archivo;
- huella del certificado de firma, extraída del APK y comparada contra la política del catálogo o un valor esperado confiable;
- permisos solicitados y motivo;
- tamaño y evidencia de procedencia disponible.

No aceptes una firma/hash como confiable solo porque el mismo repositorio los declare. Un APK con hash correcto pero firma desconocida debe seguir marcado como **sin verificar**. No instales de forma silenciosa ni reemplaces un APK con firma/certificado diferente. Exige consentimiento de instalación Android/TV cuando corresponda. No se admiten APK de origen opaco en las secciones `theme`, `tweak` o `debloat-profile`.

## 13. Snapshots, historial y botón Revertir

- Antes de cada cambio, registra de forma local qué se va a cambiar y el estado que debe restaurarse.
- Diferencia snapshot del teléfono, configuración del launcher y estado de paquetes de la TV.
- Un snapshot debe tener ID, fecha, dispositivo, versión de app/launcher, paquetes o claves administrados, hashes de archivos y estado de integridad.
- Cifra secretos cuando existan y evita guardar datos innecesarios.
- Permite renombrar, exportar, restaurar, verificar y eliminar snapshots con un diálogo comprensible. Nunca elimines la última copia de recuperación de forma automática.
- En restauración, vuelve a consultar el estado real; no asumas que todo sigue igual. Calcula las operaciones mínimas necesarias para volver al estado guardado.
- Si el dispositivo ya no está conectado, no declares que se ha restaurado: muestra **Restauración preparada; falta conectar la TV**.

## 14. Mando a distancia y utilidades

Cuando ADB y el sistema soporten una acción, implementa mando básico con navegación, OK, atrás, inicio, volumen si es posible, envío de texto donde sea seguro, y captura de pantalla únicamente con una operación real y autorizada. Las funciones que el sistema no permita deben mostrarse como no disponibles, no como botones falsos.

La captura de pantalla debe requerir acción explícita, guardarse en el almacenamiento privado salvo que el usuario exporte, e informar si no está soportada. No envíes las capturas a ningún servidor. El envío de texto debe evitar campos de contraseña o contenido marcado como seguro si el sistema permite detectarlo.

## 15. Informe de privacidad

Genera un informe de privacidad con evidencia visible, fecha, fuente y nivel de certeza. Separa claramente:

- hechos consultados en el dispositivo;
- observaciones de la configuración;
- inferencias con incertidumbre;
- funciones no comprobables mediante ADB.

No afirmes que el micrófono, ACR, tracking o telemetría están apagados sin una comprobación fiable. No envíes el informe fuera del dispositivo automáticamente. Antes de exportarlo, permite revisar el contenido y elimina por defecto IPs, números de serie, identificadores personales, códigos y claves.

## 16. Modelo de datos mínimo

Diseña entidades con migraciones y relaciones claras, por ejemplo:

- `TvDevice`: id local, alias, marca/modelo, versión detectada, capacidades, huella de sesión no secreta, última conexión.
- `TvCapability`: función, estado (soportado/no soportado/desconocido), evidencia y versión.
- `AnalysisRun`: dispositivo, fecha, inventario, evidencia, versión del perfil y resultado.
- `ChangePlan` / `ChangeItem`: operación propuesta, valores antes/después, riesgo, razón, paquete protegido y decisión del usuario.
- `Snapshot`: estado, hash/integridad, recursos relacionados y resultado de prueba de restauración.
- `Pack`: ID, tipo, target, versión, origen, commit SHA, confianza, licencia, compatibilidad, hashes, archivos y estado.
- `PackInstall`: paquete, dispositivo destino, snapshot, diff aplicado, resultados, logs y referencia de rollback.
- `AuditEvent`: actor local, hora, operación, objetivo, resultado y códigos de error sin secretos.
- `UserPreferences`: tema, idioma, accesibilidad, reducción de movimiento y preferencias de UI.

No guardes inventarios completos indefinidamente si ya no hacen falta. Documenta políticas de retención y borrado local.

## 17. Manejo de errores y estados

Usa estados explícitos como `Idle`, `Connecting`, `Connected`, `Analyzing`, `PlanReady`, `Applying`, `Verifying`, `Succeeded`, `PartiallySucceeded`, `RollingBack`, `RolledBack`, `WaitingForDevice`, `Unsupported` y `Failed`. No dejes que una excepción convierta una operación parcial en un éxito.

Los mensajes deben ser comprensibles. Ejemplos:

- “No pudimos verificar el enlace de GitHub. No se cambió nada. Comprueba la URL o vuelve a intentarlo cuando GitHub esté disponible”.
- “El paquete no coincide con su SHA-256 declarado. Se canceló la importación”.
- “Esta TV no permite cambiar el launcher con el método disponible. Puedes continuar con el análisis y las funciones compatibles”.
- “La operación se detuvo tras un error. Restauramos 3 cambios; 1 elemento necesita revisión. Ver detalles”.

No muestres stack traces a usuarios normales. En modo diagnóstico, guarda detalle técnico sin claves, tokens ni códigos privados.

## 18. Pruebas y calidad obligatorias

No basta con compilar. Añade pruebas de unitarias, integración y UI en la medida que la infraestructura permita:

### ADB y motor de cambios

- fixtures de respuestas ADB reales anonimizadas;
- servidor ADB falso para éxito, timeout, desconexión y autorización denegada;
- reconexión, cancelación, comando repetido e idempotencia;
- snapshot, rollback parcial, doble rollback y TV que cambia de estado a mitad del proceso;
- validación de lista protegida y perfiles malformados;
- prueba que impida ejecutar operaciones de desactivación para paquetes protegidos.

### Importador y paquetes

- repositorio válido, inexistente, privado, borrado, ref inválido y error de rate limit;
- link a repo, rama, tag, carpeta y archivo;
- cambios de `main` después de fijar el commit;
- hash incorrecto, manifiesto malformado, versión desconocida, claves desconocidas y archivos ausentes;
- ruta `../`, rutas absolutas, symlinks, nombres duplicados y archivos anidados;
- imagen gigante, imagen malformada, formatos no permitidos, límite total y número de imágenes;
- contenido ejecutable disfrazado, archivo HTML/SVG activo, APK no declarado y archivo que supera límites;
- redirección a host no permitido, certificado TLS inválido, timeout y cancelación;
- paquete sin firma, firma inválida o huella de certificado que no coincide;
- conversión de varios temas heredados reales y verificación de que se preservan las claves soportadas.

### UI y accesibilidad

- capturas de referencia propias para pantallas clave y estados vacíos/errores;
- pruebas de navegación y de estados de conexión;
- tema claro/oscuro, escalado de fuente, TalkBack, contraste y reducción de movimiento;
- preview de tema con la misma configuración normalizada que se aplicaría;
- prueba que impida aplicar cambios antes de confirmación explícita.

Añade CI en GitHub Actions para lint, tests, verificación de formato y compilación. Usa build reproducible cuando sea viable. No firmes lanzamientos con claves privadas dentro del repositorio. Documenta cómo obtener un APK de depuración y cómo verificar una release firmada. No inventes resultados de CI.

## 19. Entregables y criterios de aceptación

La implementación se considera lista para revisión cuando:

1. el proyecto compila con instrucciones reproducibles en un entorno Android documentado;
2. se completa una conexión ADB real en, al menos, un dispositivo de prueba compatible;
3. el análisis solo de lectura no modifica el estado de la TV;
4. el plan es visible antes de aplicar y cada operación muestra su riesgo;
5. snapshots y restauración están probados, incluidos fallos a mitad de operación;
6. un tema heredado real se importa sin modificar el repositorio original;
7. un repositorio válido se fija a commit SHA, se verifica y produce un diff antes de aplicar;
8. una URL maliciosa, archivo no permitido o hash incorrecto se rechaza sin aplicar cambios;
9. los perfiles no pueden sobreescribir la lista de paquetes protegidos ni aportar comandos shell;
10. los cambios de tema se verifican y existe un botón de revertir disponible desde el historial;
11. el diseño visual es consistente, adaptable, accesible y tiene identidad propia;
12. las capacidades de plataformas no Android se muestran honestamente como limitadas o no disponibles hasta disponer de un driver comprobado;
13. las licencias, atribuciones, documentación de arquitectura, limitaciones y pasos de pruebas están completos.

## 20. Reglas finales de implementación

- No hagas una migración grande y destructiva antes de entregar un análisis de diferencias con la arquitectura actual.
- No reemplaces archivos completos con pseudocódigo ni entregues fragmentos con “resto igual”, puntos suspensivos o `TODO` como sustituto de la implementación solicitada.
- No inventes clases, APIs, comandos, versiones o nombres de paquetes. Verifica contra el código, la documentación oficial o las dependencias instaladas.
- Mantén las funciones separadas y testeables; usa código legible y comentarios específicos para decisiones de seguridad.
- No metas datos de prueba en la compilación de producción ni dejes botones decorativos que aparenten hacer algo real.
- Cuando falte información, elige la alternativa conservadora, deja constancia y sigue con lo que sí puede hacerse. Pregunta solo si una decisión cambia de forma importante la arquitectura o el comportamiento seguro.
- Al final de cada fase informa: **Qué quedó implementado**, **Qué se probó realmente**, **Qué falta**, **Riesgos conocidos**, **Archivos modificados** y **Pasos exactos para probarlo**.

Empieza ahora con la Fase 0: inspecciona el código fuente adjunto/referenciado, verifica licencia y arquitectura, identifica qué ya existe, propone el plan de evolución y presenta un sistema visual original de TVeoClean con wireframes textuales de Inicio, Conectar, Analizar y Style Studio. No escribas todavía una reimplementación completa sin esa inspección.

# FIN DEL PROMPT MAESTRO

---

# Prompts de continuación por fase

Pega un solo prompt de fase cada vez y no avances hasta comprobar los entregables.

## Fase 0 — Auditoría, diseño y plan

Inspecciona a fondo el código real adjunto de `cleanyourtv-app` y, si está disponible, el repositorio principal `cleanyourtv`. Presenta un inventario de módulos, arquitectura, dependencias, versión/commit, licencia, comportamiento implementado, pruebas existentes, deuda técnica y riesgos. Distingue lo confirmado del README de lo confirmado en código. Propón un plan incremental para TVeoClean, incluyendo `app.tveoclean`, arquitectura de adaptadores y el modelo de paquetes `tveo.json`. Luego diseña un sistema visual original y muestra wireframes textuales de Inicio, Conectar, Analizar y Style Studio en modo oscuro y claro. No modifiques código todavía. No inventes archivos o capacidades que no has encontrado.

## Fase 1 — Sistema visual y navegación funcional

Implementa el design system de TVeoClean con Material 3, tokens de color, tipografía, componentes, tema oscuro/claro, accesibilidad, estados vacíos, carga, error y reducción de movimiento. Implementa navegación real entre Inicio, Mi TV, Style Studio, Historial y Ajustes. Usa repositorios falsos únicamente en un módulo de preview/testing claramente separado; marca cada dato simulado como demostración. No simules conectividad real en producción. Añade pruebas de UI y capturas base de las pantallas principales. Devuelve los archivos completos modificados y resultados reales de compilación/pruebas.

## Fase 2 — Conexión ADB y análisis de solo lectura

Implementa o adapta `core:adb` sobre el código existente. Añade descubrimiento, conexión, autorización segura, gestión de sesión, timeouts, cancelación y flujo de emparejamiento por código cuando el dispositivo lo soporte. Mantén las claves en Android Keystore. Implementa análisis de solo lectura y el modelo de capacidades por dispositivo. Usa un servidor ADB falso para pruebas. No desactives ni instales nada en esta fase. Documenta las limitaciones de las TVs que solo ofrecen emparejamiento inalámbrico o que bloquean ADB.

## Fase 3 — Plan, limpieza reversible y recuperación

Implementa el plan de cambios, perfiles conservadores, lista de protección inalterable por los paquetes de terceros, ejecución por lotes, verificación del estado real, snapshots, restauración por diferencia, auditoría y mensajes de operación parcial. Nunca ejecutes acciones antes de mostrar y confirmar el plan. No desinstales por defecto. Prueba fallos a mitad de un lote, pérdida de conexión, timeout y rollback. Si el launcher alternativo no puede probarse como recuperable en una TV, no deshabilites el Home original.

## Fase 4 — Style Studio y manifiesto

Implementa el modelo `tveo.json`, validación formal, almacenamiento privado, listado de paquetes instalados, actualización manual, vista previa y diferencias entre estado actual y propuesto. Separa `mobile-ui`, `tv-launcher`, `tv-profile` y `tv-addon`. Añade importación heredada de `config.json`, `wallpaper.jpg` y `wallpapers/`, usando fixtures reales del proyecto principal. No apliques cambios aún hasta que las pruebas de importación, validación y preview pasen.

## Fase 5 — Importación segura de GitHub

Implementa el asistente completo: parseo estricto del enlace, resolución a commit SHA, acceso solo a dominios permitidos, descarga limitada, validación de esquema y hashes, etiquetas de confianza, preview, diff, permisos y compatibilidad. Incluye pruebas de URLs válidas e inválidas, rate limit, redirección prohibida, cambios de rama, hash erróneo, archivos ejecutables, rutas maliciosas y límites de tamaño. La importación debe poder cancelarse. No ejecutes scripts o código del repositorio. Deja los paquetes fijados al commit importado y nunca actualices silenciosamente.

## Fase 6 — Aplicación, snapshot y revertir de paquetes

Integra el resultado de Style Studio con el motor transaccional. Antes de cada aplicación, crea snapshot; aplica solo valores permitidos desde la app; verifica leyendo el estado real y revierte si falla. Añade historial, comparar versiones, reinstalar un commit anterior y recuperar el tema precedente. Ejecuta pruebas con pérdida de red y fallos de almacenamiento. No declares éxito hasta verificar el resultado.

## Fase 7 — Complementos APK, mando y privacidad

Implementa únicamente capacidades que el dispositivo permita y que puedan probarse. Para APK, verifica el archivo, hash calculado, certificado, permisos, paquete y compatibilidad; compara la huella con una fuente de confianza independiente del propio paquete; exige confirmación reforzada y nunca instales silenciosamente. Implementa mando, texto y captura solo donde funcionen de verdad. El informe de privacidad debe basarse en evidencia, señalar incertidumbres y permanecer local salvo exportación explícita.

## Fase 8 — Endurecimiento y publicación

Ejecuta auditoría de permisos, revisión de secretos, pruebas unitarias/integración/UI, análisis estático, compilación release y CI. Verifica licencias y avisos de terceros, documentación, migraciones, accesibilidad, rendimiento en dispositivos modestos y comportamiento con desconexión. Produce instrucciones reproducibles para compilar y probar, guía de firma sin incluir claves privadas, changelog y matriz honesta de compatibilidad. No declares listo para publicar ningún componente que no haya superado pruebas reales.

---

# Fuentes de referencia consultadas

- App Android: https://github.com/udhaykumarbala/cleanyourtv-app
- Proyecto Clean Your TV: https://github.com/Gathin23/cleanyourtv
- Web: https://cleanyour.tv/
- Guía de temas: https://github.com/Gathin23/cleanyourtv/blob/main/themes/README.md

Revisa siempre las versiones y el contenido actual del repositorio antes de codificar; las funciones disponibles y la compatibilidad pueden cambiar.
