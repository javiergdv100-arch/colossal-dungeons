# Colossal Dungeons Enhanced

**Documento maestro de diseno y plan tecnico completo** para el mod **Colossal Dungeons Enhanced** - Minecraft 1.21.1 (NeoForge).

Un mod de mazmorras colosales con mecanicas profundas, mas de 70 criaturas unicas, un sistema completo de interacciones con items vanilla, y un plan tecnico listo para implementar desde cero.

---

## Contenido del proyecto

Este repositorio contiene un sitio web estatico con dos paginas principales:

### 1. Documento de Diseno (`index.html`)

La web principal del documento de diseno, con navegacion lateral, busqueda global, animaciones scroll-reveal y modales detallados:

| Seccion | Contenido |
|---------|-----------|
| **7 Dungeons** | Fichas completas: identidad, regla exclusiva, sectores, jefe final, semijefes, puzles, trampas, loot, eventos, secretos e items vanilla clave |
| **Bestiario (70 criaturas)** | Filtrable por dungeon y rango. Ficha detallada: vida, armadura, dano, comportamiento, botin e interacciones vanilla |
| **24 Trampas** | Con mecanicas detalladas, contrajuego y badges de items vanilla que las contrarrestan |
| **13 Puzles** | Sistemas logicos con soluciones e interacciones vanilla alternativas |
| **11 Mecanismos** | Activables por items vanilla, con comportamiento dinamico por dungeon |
| **Interacciones Vanilla (24 items)** | Seccion dedicada con 24 items de Minecraft vanilla, cada uno con multiples usos contextuales en las mazmorras |
| **NPCs y Campamento** | Incluyendo Valdris el Eterno y el sistema de companeros |
| **Dependencias** | Motor de VFX con AAA Particles (Effekseer), GeckoLib 4.x, SmartBrainLib, Curios |
| **Rendimiento y Roadmap** | Plan de produccion y optimizaciones |

### 2. Plan Tecnico de Implementacion (`technical-plan.html`)

Una pagina independiente (107 KB, autocontenida) con el **plan tecnico completo** para implementar el mod en Java/NeoForge. Suficientemente detallado para servir como guia de desarrollo sin ambiguedad:

| Seccion | Detalle |
|---------|---------|
| **Arquitectura General** | Vision global del framework, Java 21, NeoForge 1.21.1, Mojang mappings |
| **Estructura de Paquetes** | Arbol completo de paquetes Java con responsabilidades por modulo |
| **Registros (Registries)** | Todos los DeferredRegister: bloques, items, entidades, efectos, adjuntos, sonidos, particulas |
| **12 Sistemas Core** | Dungeon State Machine, Room Activation, Trap Framework, Puzzle Engine, Mechanism System, Vanilla Item Interaction, Resonance, Combat, VFX (AAA Particles), Dialogue/NPC, Companion, Addon API |
| **Guia de Integracion de APIs** | GeckoLib 4.x (animaciones), AAA Particles (VFX), SmartBrainLib (IA de jefes), Curios (accesorios), NeoForge (eventos, red, datos) |
| **14+ Ejemplos de Codigo Java** | Compilables y con sintaxis correcta de NeoForge 1.21.1 |
| **Protocolo de Red** | Payloads con record + StreamCodec, sincronizacion cliente-servidor |
| **Configuracion Data-Driven** | Esquemas JSON para trampas, puzles y dungeons cargados desde datapack |
| **Sistema de Efectos** | Arquitectura de MobEffects custom (OiledEffect, Fear, Resonance, etc.) |
| **Mecanica del Aceite** | Implementacion detallada: como el polvo de hueso limpia el aceite, interaccion con fuego, timer de 30 min |
| **Room Activation System** | Diagrama de estados: UNLOADED, PREPARED, ACTIVE, COMPLETED, DORMANT, SUSPENDED |
| **Principios de Framework** | Patrones de diseno: Template Method, Strategy, Observer, State Machine |

#### Ejemplos de codigo incluidos

Los 14 ejemplos de Java usan la API actual de NeoForge 1.21.1 con Mojang mappings (no MCP/Yarn):

1. Clase principal `@Mod` con inicializacion
2. `DeferredRegister` para bloques, items y entidades
3. `AttachmentType` (reemplazo moderno de Capabilities)
4. Entidad custom con `GeoEntity` (GeckoLib 4.x)
5. `AnimationController` con transiciones de fase
6. `AbstractTrap` - clase base con Template Method
7. `VanillaInteractionHandler` - escucha de `PlayerInteractEvent`
8. Payloads de red con `record` + `StreamCodec`
9. `DungeonManager` - maquina de estados de sala
10. IA de jefe con SmartBrainLib (arboles de comportamiento)
11. `ICurioItem` para accesorios Curios
12. Spawn de efectos AAA Particles
13. Cargador data-driven de trampas desde JSON
14. Mecanica de aceite completa (OiledEffect + Bone Meal cleanse)

---

## Sistema de Interacciones Vanilla

Una de las mecanicas centrales del mod: **24 items vanilla de Minecraft** tienen usos significativos y contextuales dentro de las mazmorras. No se trata de items decorativos, sino de herramientas con impacto mecanico real:

| Item Vanilla | Ejemplos de Uso |
|---|---|
| **Polvo de hueso** | Limpia aceite de superficies y jugadores, neutraliza trampas organicas, revela caminos ocultos |
| **Cubo de agua** | Apaga trampas de fuego, crea plataformas de hielo en The Veiled Peak, lava puzzles de presion |
| **Pedernal y acero** | Enciende mecanismos, ignita aceite estrategicamente, activa braseros rituales |
| **Bolas de nieve** | Distraccion de criaturas, activacion a distancia de placas de presion, ruptura de cristales fragiles |
| **Perla de ender** | Atraviesa barreras especificas, acceso a zonas secretas, escape de arenas de combate |
| **Escudo** | Bloqueo de proyectiles de trampas, reflejo de ataques especificos de jefes |
| **Cizallas** | Cortan telaranas trampa, desactivan sensores organicos, cosechan materiales de dungeon |
| **Honey Bottles** | Ralentizan enemigos, lubrican mecanismos, curan efectos de estado especificos |
| **Polvo de nieve** | Congela mecanismos temporalmente, crea puentes sobre lava, trampas para criaturas |
| **Leche** | Limpieza de debuffs de dungeon, purifica fuentes contaminadas |
| **Cana de pescar** | Atrae items a distancia, activa palancas remotas, desactiva trampas con gancho |
| **Catalejo** | Revela trampas ocultas a distancia, identifica puntos debiles de jefes |
| **TNT** | Destruye paredes falsas, activa mecanismos de presion masiva |
| **Sculk Sensors** | Detectan sonido para puzzles, crean sistemas de alarma, sincronizan mecanismos |
| **Note Blocks** | Resuelven puzzles musicales, calman criaturas, abren puertas de resonancia |
| **Items de oro** | Soborno de NPCs, activacion de altares dorados, acceso a areas exclusivas |
| **Componentes de Redstone** | Circuitos para puzzles, activacion de maquinaria, puentes de energia |
| **Pociones splash/lingering** | Efectos de area en combate, activacion quimica de mecanismos |
| **Brujula de Recuperacion** | Localiza salas secretas, guia hacia loot perdido, navega laberintos |
| Y mas... | Antorchas, comida, riendas, huevos, ballesta |

Cada dungeon tiene su propio set de **items vanilla clave** que desbloquean rutas alternativas y secretos.

---

## Las 7 Mazmorras

| # | Dungeon | Tema | Regla Exclusiva |
|---|---------|------|-----------------|
| 1 | **The Mirror Castle** | Reflejos y dualidad | Los espejos replican al jugador como enemigo |
| 2 | **The Worldbearer** | Titan geologico vivo | El dungeon se mueve y reconfigura en tiempo real |
| 3 | **The Hollow Leviathan** | Interior de criatura cosmica | Acido digestivo como hazard constante |
| 4 | **The Veiled Peak** | Montaña mistica, hielo y viento | Tormentas que alteran visibilidad y mecanicas |
| 5 | **The Descent into Madness** | Horror psicologico | Efectos de cordura que distorsionan la realidad |
| 6 | **The Primordial Tower** | Torre ancestral vertical | Gravedad alterada por sectores |
| 7 | **The Solar Palace** | Palacio solar, luz extrema | Mecanicas de luz/sombra como sistema central |

---

## Como ver el proyecto

### Opcion A: Archivo unico (recomendado para compartir)

Abre **`colossal-dungeons-enhanced.html`** (225 KB) en cualquier navegador. Contiene todo incrustado: CSS, JavaScript, datos y estructura. No necesita servidor ni dependencias externas.

### Opcion B: Version modular (para desarrollo)

Abre **`index.html`** con un servidor local o directamente en el navegador. Usa los archivos separados:

```
index.html          -- Estructura de la pagina
styles.css          -- Estilos (700+ lineas, tema oscuro, responsive)
data.js             -- Todos los datos del mod (1500+ lineas)
app.js              -- Logica de renderizado (443 lineas)
```

### Opcion C: Plan tecnico

Abre **`technical-plan.html`** directamente. Es completamente independiente (107 KB, autocontenido con estilos propios).

---

## Despliegue en GitHub Pages

En **Settings > Pages**, selecciona la rama a servir (por ejemplo `main` tras fusionar) y la raiz `/`. La web quedara disponible en:

```
https://javiergdv100-arch.github.io/colossal-dungeons/
```

El archivo `.nojekyll` ya esta incluido para evitar que GitHub intente procesar el sitio con Jekyll.

---

## Estructura del repositorio

```
colossal-dungeons/
|-- index.html                         -- Pagina principal del documento de diseno
|-- technical-plan.html                -- Plan tecnico completo (autocontenido, 107 KB)
|-- colossal-dungeons-enhanced.html    -- Bundle todo-en-uno (225 KB)
|-- styles.css                         -- Estilos del tema oscuro
|-- data.js                            -- Base de datos del mod (70 criaturas, 24 trampas, etc.)
|-- app.js                             -- Motor de renderizado de la web
|-- README.md                          -- Este archivo
|-- LICENSE.md                         -- Licencia del proyecto
|-- .nojekyll                          -- Desactiva Jekyll en GitHub Pages
```

---

## Stack tecnico del mod (documentado en el plan)

| Componente | Tecnologia | Version |
|---|---|---|
| Minecraft | Java Edition | 1.21.1 |
| Mod Loader | NeoForge | 21.1.x |
| Lenguaje | Java | 21 |
| Mappings | Mojang Official | -- |
| Animaciones | GeckoLib | 4.x |
| VFX / Particulas | AAA Particles (Effekseer) | -- |
| IA de Jefes | SmartBrainLib | -- |
| Accesorios | Curios API | -- |
| Configuracion | Data-driven (JSON datapacks) | -- |

---

## Uso del plan tecnico

El `technical-plan.html` esta disenado para ser una referencia directa de implementacion:

1. **Para programadores**: Contiene codigo Java compilable con la API correcta de NeoForge 1.21.1. Cada ejemplo usa `DeferredRegister`, `AttachmentTypes` (no el viejo sistema de Capabilities), y `StreamCodec` para networking.

2. **Para disenadores**: La estructura de paquetes y los diagramas de estado permiten entender el flujo del mod sin leer codigo.

3. **Para el equipo**: Los 12 sistemas documentados cubren todas las mecanicas del mod. El Addon API permite extensibilidad por terceros.

4. **Orden de implementacion sugerido**: Framework base > Registries > Dungeon State Machine > Room Activation > Trap Framework > Vanilla Interactions > Criaturas > VFX > Polish.

---

## Estado del proyecto

- **Fase**: Pre-produccion / Diseno completo
- **Documento de diseno**: Finalizado (web interactiva)
- **Plan tecnico**: Finalizado (listo para implementar)
- **Assets**: Pendientes (modelos, texturas, sonidos)
- **Codigo**: Pendiente (el plan tecnico cubre la implementacion completa)

---

## Licencia

Ver [LICENSE.md](LICENSE.md).
