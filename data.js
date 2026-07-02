/* ============================================================
   Colossal Dungeons Enhanced — Documento de diseño (datos)
   Minecraft 1.21.1 · NeoForge
   Todo el contenido vive en window.CDE_DATA
   ============================================================ */
window.CDE_DATA = {};


/* ---------- META / PROYECTO ---------- */
CDE_DATA.meta = {
  title: "Colossal Dungeons Enhanced",
  tagline: "Siete mazmorras colosales. Un solo intento por mundo. Ningún atajo.",
  platform: "Minecraft 1.21.1 · NeoForge",
  status: "Preproducción",
  intro: "Un mod centrado casi por completo en siete mazmorras colosales hechas con calidad de juego completo. Cada dungeon tiene identidad visual, reglas propias, estructuras, mobs, jefes, trampas, puzles, eventos, loot y mecánicas únicas. El mundo exterior no se llena de mobs: su punto de entrada es un campamento de aventureros desde el que se descubren las dungeons mediante mapas, rumores, misiones y NPCs reclutables.",
  principles: [
    "Una sola copia de cada dungeon por mundo.",
    "Las siete tienen dificultad alta y aproximadamente equivalente.",
    "Se pueden intentar en cualquier orden.",
    "No hay escalado artificial según el equipo del jugador.",
    "El equipo ayuda, pero no sustituye la habilidad.",
    "La misión principal de cada dungeon es derrotar a su jefe.",
    "Las misiones del campamento son secundarias y opcionales.",
    "Las dungeons pueden restaurarse para ser rejugables.",
    "El soporte para addons es una parte central, no un extra.",
    "El rendimiento se diseña desde el framework, no al final."
  ]
};

/* ---------- ESTADÍSTICAS DE PANEL ---------- */
CDE_DATA.stats = [
  { label: "Dungeons diseñadas", value: "7", sub: "de 7 objetivo" },
  { label: "Criaturas únicas", value: "70", sub: "hasta 90 con variantes" },
  { label: "Jefes principales", value: "7", sub: "uno por dungeon" },
  { label: "Semijefes", value: "9", sub: "uno o dos por dungeon" },
  { label: "Familias de trampas", value: "24", sub: "configurables" },
  { label: "Puzles base", value: "10", sub: "ampliables por addons" }
];


/* ---------- DEPENDENCIAS (Photon -> AAA Particles) ---------- */
CDE_DATA.dependencies = [
  {
    name: "AAA Particles",
    role: "Motor de efectos visuales (VFX) — sustituye a Photon",
    provisional: false,
    highlight: true,
    desc: "Librería de efectos que integra el sistema Effekseer (archivos .efkefc) dentro de Minecraft. Reemplaza a Photon como motor de VFX del mod. Expone API para desarrolladores: emisores de partículas ligados a entidades, orientación del emisor a partir de un vector de dirección, metadatos de finalización cuando un efecto ligado a una entidad desaparece, y registro/precarga de efectos.",
    uses: [
      "Efectos visuales avanzados de armas, mobs y jefes.",
      "Auras, rastros de ataque, explosiones y ondas de choque.",
      "Portales, polvo, derrumbamientos, gas y niebla.",
      "Fragmentos de memoria, efectos psíquicos y transformaciones.",
      "Efectos ambientales por dungeon (reflejos, brasas solares, esporas orgánicas)."
    ],
    note: "Los efectos .efkefc se empaquetan como recursos del mod y se instancian mediante la API de AAA Particles, ligándolos a huesos de GeckoLib cuando se necesita sincronía con animaciones."
  },
  {
    name: "GeckoLib",
    role: "Modelos y animaciones",
    provisional: false,
    desc: "Motor de animación y renderizado 3D para entidades, bloques, objetos y armaduras, con animaciones por keyframes, keyframes de sonido y partículas, y eventos sincronizados.",
    uses: ["Animaciones de mobs, jefes, trampas y mecanismos.", "Bloques y objetos animados.", "Eventos sincronizados con animaciones."]
  },
  {
    name: "SmartBrainLib",
    role: "Inteligencia artificial avanzada",
    provisional: true,
    desc: "Se usa al principio para acelerar la creación de IA compleja: sensores, memorias, percepción, navegación, selección de ataques, estados y coordinación. Con el tiempo, sus comportamientos podrán migrarse al framework modular interno.",
    uses: ["Comportamientos de mobs, búsqueda de objetivos y detección por sonido.", "Cambio de fases y selección de ataques.", "Élites, semijefes y jefes."]
  },
  {
    name: "Curios API",
    role: "Accesorios equipables",
    provisional: false,
    desc: "Sistema oficial de accesorios del mod en NeoForge 1.21.1.",
    uses: ["Ranuras de accesorios: amuletos, anillos, collares, cinturones y reliquias.", "Objetos equipables con efectos especiales."]
  },
  {
    name: "FDLib",
    role: "Cinemáticas y presentación",
    provisional: true,
    desc: "Se mantiene mientras acelere el desarrollo. En el futuro podrá sustituirse por sistemas propios de cámaras, cinemáticas, bossbars e impact frames.",
    uses: ["Cámaras controladas y presentaciones de jefes.", "Bossbars personalizadas e impact frames.", "Escenas especiales de NPCs y transformaciones."]
  },
  {
    name: "More Hitboxes",
    role: "Entidades multiparte",
    provisional: true,
    desc: "Primera dependencia prevista para sustituir por un sistema multiparte propio (hitboxes vinculadas a huesos, daño localizado, partes rompibles/desprendibles, API pública para addons).",
    uses: ["Criaturas grandes y jefes con partes independientes.", "Trampas animadas, estatuas y pilares.", "Sincronización de hitboxes con animaciones."]
  }
];


/* ---------- DUNGEONS ---------- */
CDE_DATA.dungeons = [];

CDE_DATA.dungeons.push({
  id: "mirror-castle",
  name: "The Mirror Castle",
  subtitle: "El castillo de los reflejos",
  color: "#8fb7c9",
  themeWord: "Reflejo · Ilusión · Percepción",
  identity: "Un castillo señorial de plata, azogue y cristal donde cada superficie miente. Salones de espejos, ventanales pulidos y suelos de mercurio convierten la orientación en un acto de fe. La dungeon se juega leyendo el entorno: distinguir lo real de lo reflejado, y a uno mismo de sus copias.",
  rule: "Regla exclusiva — «Ley del Reflejo»: en las salas marcadas, atacar de frente a una imagen especular devuelve el daño al jugador. Muchos enemigos solo son vulnerables si se les golpea a través de su reflejo real, no de su cuerpo aparente. Romper la superficie especular correcta debilita o anula a los enemigos vinculados a ella.",
  objective: "Alcanzar el Salón del Trono Invertido y destruir al Soberano Añicos rompiendo, una a una, las superficies que sostienen su reflejo.",
  bossId: "shattered-sovereign",
  sectors: [
    { name: "Galería de Bienvenida", desc: "Pasillos duplicados por espejos; enseña la Ley del Reflejo con enemigos inofensivos y puertas falsas." },
    { name: "Salón de Azogue", desc: "Suelos de mercurio que reflejan el techo; el jugador debe caminar por las vigas reales, no por su reflejo." },
    { name: "Laberinto de Marcos", desc: "Marcos de espejo que se reorganizan cuando nadie mira (usa el sistema de salas intercambiables)." },
    { name: "Cámara de los Gemelos", desc: "Arena de los semijefes Los Gemelos Rotos." },
    { name: "Salón del Trono Invertido", desc: "Arena final, un salón simétrico donde arriba y abajo se confunden." }
  ],
  puzzles: ["Espejos de trayectoria", "Pasillo de memoria", "Puzle de luz y espejos"],
  traps: ["Sala de espejos giratorios", "Reflejo hostil", "Pasillo infinito de la lámpara mágica", "Bola rodante colosal (esfera reflectante)"],
  loot: ["Fragmentos de azogue (material de crafteo).", "Espejo de bolsillo: revela una ruta real durante unos segundos.", "Armadura de Plata Pulida: refleja una parte del daño a distancia."],
  weapons: ["Estoque de la Locura (posible, si el Duelista Hueco se ubica aquí).", "Filo Espejado: al parar un golpe, proyecta un reflejo cortante."],
  events: ["Apagón especular: todas las luces se invierten y las salas reales e ilusorias intercambian su iluminación.", "El Cortejo: una fila de reflejos del jugador desfila y uno de ellos es real y hostil."],
  secrets: ["Tras un espejo agrietado concreto hay una trastienda con loot de plata.", "Un reflejo que no imita al jugador señala un pasadizo."],
  entrance: "Un portón de plata que solo refleja a quien no lleva casco; entrar empaña todos los espejos durante unos segundos.",
  exit: "Salida normal disponible tras el jefe; existe un espejo-portal de retirada de un solo uso.",
  reset: "Reinicio por sectores: las superficies rotas se regeneran y los marcos se reorganizan.",
  multiplayer: "Los reflejos pueden separar al grupo en 'salas espejo' distintas hasta que resuelven un puzle común.",
  performance: "Los reflejos son planos renderizados con recorte de portal, no cámaras duplicadas; solo la sala activa refleja en tiempo real."
});


CDE_DATA.dungeons.push({
  id: "worldbearer",
  name: "The Worldbearer",
  subtitle: "El portador del mundo",
  color: "#c8a96a",
  themeWord: "Escala colosal · Peso · Gravedad",
  identity: "La dungeon está construida sobre, dentro y alrededor de un titán pétreo del tamaño de una montaña que sostiene una porción del mundo sobre sus hombros. Se explora ascendiendo por su columna, sus costillas huecas y las plataformas que penden de sus cadenas. Todo aquí pesa: la piedra, los contrapesos, las decisiones.",
  rule: "Regla exclusiva — «Ley del Peso»: puertas, ascensores y puentes funcionan por contrapeso real. Para avanzar hay que mover masa (bloques empujables, contrapesos, el propio Martillo de Contrapeso). Retirar peso de un soporte puede abrir una ruta y derrumbar otra a la vez.",
  objective: "Escalar hasta el corazón del titán y liberar/derrotar al Atlas Moribundo, la voluntad que lo mantiene erguido, sin quedar aplastado por lo que se derrumba.",
  bossId: "dying-atlas",
  sectors: [
    { name: "Los Talones", desc: "Base del titán; cabrestantes manuales y ascensores por cadenas dan acceso al interior." },
    { name: "La Columna", desc: "Ascenso vertical por vértebras huecas; plataformas basculantes según el peso del grupo." },
    { name: "La Caja Torácica", desc: "Salas amplias entre costillas; los Atlantes Menores sostienen techos que caen al morir." },
    { name: "Los Hombros", desc: "Exterior azotado por el viento, con la carga del mundo suspendida arriba." },
    { name: "El Corazón de Piedra", desc: "Arena del Atlas Moribundo." }
  ],
  puzzles: ["Puzle de peso y sustitución", "Puzle de bloques empujables", "Cooperación asimétrica (cabrestantes)"],
  traps: ["Paredes aplastantes", "Bola rodante colosal", "Trampa de derrumbamiento", "Placa basculante colosal", "Cadena tensada"],
  loot: ["Núcleo de contrapeso: material pesado para forja.", "Botas de Lastre: inmunidad al empuje a cambio de velocidad.", "Cincel del Atlas: rompe muros débiles marcados."],
  weapons: ["Martillo de Contrapeso: cuanto más se carga, más daño, empuje e interacción con mecanismos."],
  events: ["El Titán se mueve: toda una sección se inclina y hay que reubicar el peso antes del derrumbe.", "Lluvia de escombros: fragmentos del mundo suspendido caen por rutas concretas."],
  secrets: ["Una vértebra floja esconde un atajo vertical si se derriba con explosión.", "Dentro de una cadena hueca hay una cámara de expedicionarios perdidos."],
  entrance: "Una gran compuerta que solo se abre bajando un ascensor manual con suficiente peso encima.",
  exit: "Ascensor rápido de descenso tras el jefe; antes, la única salida es volver a bajar por la columna.",
  reset: "Los contrapesos vuelven a su posición y los derrumbes se reconstruyen por sectores.",
  multiplayer: "Muchos mecanismos requieren varios jugadores empujando cabrestantes o repartiendo peso simultáneamente.",
  performance: "El titán es estructura estática por plantillas; solo las secciones móviles cercanas simulan física."
});


CDE_DATA.dungeons.push({
  id: "hollow-leviathan",
  name: "The Hollow Leviathan",
  subtitle: "El leviatán hueco",
  color: "#9d6b8f",
  themeWord: "Biología · Órganos · Defensas vivas",
  identity: "Una mazmorra orgánica dentro de un leviatán colosal muerto —o casi muerto—. Conductos, órganos, tejidos, fluidos y defensas vivas. Las paredes respiran, los suelos digieren y las puertas muerden. El cuerpo reacciona a la intrusión como un organismo a una infección.",
  rule: "Regla exclusiva — «Ley de la Infección»: cada órgano dañado altera toda la dungeon (destruir un pulmón despeja el gas de una zona; reventar una glándula inunda otra de ácido). El jugador es tratado como patógeno: hacer ruido o daño acelera la respuesta inmune (oleadas de anticuerpos).",
  objective: "Alcanzar la cavidad central y destruir al Parásito Regente, el organismo que reanima al leviatán, decidiendo qué órganos sacrificar por el camino.",
  bossId: "sovereign-parasite",
  sectors: [
    { name: "Las Fauces", desc: "Entrada por la boca; primeras trampas vivas y suelos digestivos." },
    { name: "El Tracto", desc: "Conductos estrechos con fluidos, esfínteres-puerta y capullos que liberan mobs." },
    { name: "La Cámara Pulmonar", desc: "Grandes sacos que se inflan y desinflan; el nivel de gas depende de si respiran." },
    { name: "El Nido de Nervios", desc: "Arena del semijefe El Sistema Nervioso; paredes de ojos orgánicos." },
    { name: "La Cavidad del Corazón", desc: "Arena del Parásito Regente." }
  ],
  puzzles: ["Puzle biológico de órganos", "Secuencia de sonidos (latidos)", "Puzle de peso y sustitución (esfínteres)"],
  traps: ["Trampas vivas", "Puerta devoradora", "Suelos digestivos", "Paredes que respiran", "Capullos que liberan mobs"],
  loot: ["Muestras orgánicas: ingredientes alquímicos.", "Membrana Resiliente: armadura que absorbe veneno.", "Glándula de bilis: arma arrojadiza corrosiva."],
  weapons: ["Bisturí del Cirujano: hace daño extra a partes y órganos, poco a armadura."],
  events: ["Respuesta inmune: oleada masiva de anticuerpos tras hacer demasiado ruido.", "Espasmo: todo el sector se contrae, cerrando y abriendo conductos."],
  secrets: ["Una vena secundaria lleva a una perla del leviatán (loot raro).", "Un órgano vestigial esconde a un Vigía Cosido dormido y su recompensa."],
  entrance: "La boca del leviatán, que traga al jugador; al entrar, un esfínter-puerta se cierra detrás.",
  exit: "Reventar el corazón abre una brecha en el costado como salida; antes, hay que retroceder por el tracto.",
  reset: "Los órganos regeneran tejido y los fluidos se rellenan; los capullos vuelven a poblarse.",
  multiplayer: "El sistema de fluidos y gas afecta a todo el grupo; conviene coordinar qué órganos tocar.",
  performance: "Los fluidos son volúmenes lógicos por sala, no física de líquidos; la 'respiración' es animación de bloques."
});


CDE_DATA.dungeons.push({
  id: "veiled-peak",
  name: "The Veiled Peak",
  subtitle: "El pico velado",
  color: "#a9c4d6",
  themeWord: "Verticalidad · Clima · Espíritus",
  identity: "Un pico montañoso oculto tras un velo mágico de niebla, ventisca y espíritus. Se asciende por cornisas, puentes de cuerda y templos colgantes mientras el clima cambia y la visibilidad desaparece. La montaña juzga a quien sube: solo los que respetan sus reglas llegan a la cima.",
  rule: "Regla exclusiva — «Ley del Velo»: la niebla oculta el camino real y muestra rutas falsas. Encender los faros espirituales en orden disipa el velo por tramos. El viento empuja hacia los precipicios: moverse contra ráfagas mal calculadas es mortal.",
  objective: "Ascender a la cima disipando el velo y enfrentar a la Tormenta Coronada, el espíritu que protege el pico.",
  bossId: "crowned-tempest",
  sectors: [
    { name: "El Sendero Bajo", desc: "Bosque de piedra con almas veladas; primeras ráfagas de viento suaves." },
    { name: "Los Puentes de Cuerda", desc: "Travesías sobre el vacío que oscilan con el viento; cadenas tensadas cruzan la niebla." },
    { name: "El Templo de los Faros", desc: "Zona de puzles: encender faros espirituales en secuencia para abrir la subida." },
    { name: "La Cornisa del Oráculo", desc: "Arena del semijefe El Guardián de la Cima." },
    { name: "La Cima Coronada", desc: "Arena abierta de la Tormenta Coronada, azotada por el rayo." }
  ],
  puzzles: ["Secuencia de sonidos (campanas de niebla)", "Puzle de luz (faros espirituales)", "Cooperación asimétrica (sostener puentes)"],
  traps: ["Ventisca cegadora", "Placa de hielo deslizante", "Ráfaga de viento de precipicio", "Trampa de derrumbamiento (aludes)", "Cuchillas y púas retráctiles (estalactitas)"],
  loot: ["Cristal de faro: fuente de luz portátil que disipa niebla.", "Capa Cortavientos: reduce el empuje del viento.", "Reliquia del Oráculo: revela la ruta segura brevemente."],
  weapons: ["Arco de las Ventiscas: sus flechas ignoran el desvío del viento y empujan."],
  events: ["Tormenta súbita: el viento cambia de dirección y hay que reubicarse antes de ser lanzado.", "Procesión de almas: espíritus guían —o engañan— hacia una ruta."],
  secrets: ["Una cornisa oculta tras la niebla lleva a un santuario con loot espiritual.", "Un faro apagado adrede abre una gruta bajo el hielo."],
  entrance: "Un tótem en la base que solo deja pasar cuando se ofrenda una fuente de luz.",
  exit: "Desde la cima, un descenso rápido por corriente de aire ascendente; antes, hay que rehacer la subida.",
  reset: "El velo vuelve a cubrir la montaña y los faros se apagan; los puentes se rearman.",
  multiplayer: "Algunos puentes solo se mantienen si un jugador sostiene un mecanismo mientras otros cruzan.",
  performance: "La niebla es niebla volumétrica por sala con distancia de render adaptada; el viento es un campo de fuerzas por zona."
});


CDE_DATA.dungeons.push({
  id: "descent-madness",
  name: "The Descent into Madness",
  subtitle: "El descenso a la locura",
  color: "#b0605a",
  themeWord: "Terror psicológico · Percepción · Sin retorno",
  identity: "Un descenso interminable a la oscuridad donde la realidad se deforma. Voces humanas piden ayuda, los pasillos se repiten, los recuerdos se corrompen y no siempre se puede confiar en lo que se ve o se oye. La dungeon no ataca solo al cuerpo: ataca a la certeza.",
  rule: "Regla exclusiva confirmada — «Sin salida normal»: una vez dentro, solo se sale (1) completando el descenso, (2) muriendo o (3) usando un Cristal de Escape. Las puertas se cierran al entrar y en zonas protegidas no se pueden romper ni colocar bloques para huir.",
  objective: "Descender hasta el fondo y enfrentar a la Cordura Rota, aceptando que gran parte de lo que se percibe por el camino es mentira.",
  bossId: "broken-sanity",
  sectors: [
    { name: "El Umbral", desc: "La advertencia y el punto sin retorno; las puertas sellan detrás." },
    { name: "Los Pasillos que Repiten", desc: "Corredores en bucle con la lámpara mágica y voces de auxilio." },
    { name: "La Galería de Recuerdos", desc: "Salas que recrean escenas falsas de aventureros muertos." },
    { name: "El Coro", desc: "Arena del semijefe Semblante, entre voces superpuestas." },
    { name: "El Fondo", desc: "Arena de la Cordura Rota." }
  ],
  puzzles: ["Pasillo de memoria", "Secuencia de sonidos (voces reales vs. falsas)", "Puzle de rutas falsas y repetición"],
  traps: ["Pasillo infinito de la lámpara mágica", "Trampas vivas (variantes perturbadoras)", "Suelo que desaparece", "Voces falsas y engaño sensorial", "Bola rodante colosal (esfera deformada de carne y ojos)"],
  loot: ["Cristal de Escape: única salida de emergencia; drop raro y limitado.", "Vela de Cordura: revela qué es real dentro de su radio.", "Diario reconstruido: pistas y lore."],
  weapons: ["Estoque de la Locura: irrompible, ligerísimo, con Punzada rápida y Ráfaga de estocadas."],
  events: ["Falso rescate: una voz conocida guía a una emboscada del Duelista Hueco.", "Colapso de realidad: la sala se reordena y las salidas cambian de sitio."],
  secrets: ["Ignorar todas las voces en un tramo abre la ruta real.", "Un recuerdo completado sin errores revela un alijo lúcido."],
  entrance: "Un umbral con la advertencia grabada; no hay confirmación: se puede entrar por accidente y quedar sellado.",
  exit: "Solo completar, morir o Cristal de Escape. No hay retirada convencional.",
  reset: "Restauración total por sectores al vaciarse de jugadores; el sellado se rearma.",
  multiplayer: "Los bucles pueden separar al grupo en copias distintas; las voces pueden imitar a compañeros reales.",
  performance: "Los bucles reutilizan pocos segmentos con teletransporte imperceptible; las voces son audios ligeros pre-generados."
});


CDE_DATA.dungeons.push({
  id: "primordial-tower",
  name: "The Primordial Tower",
  subtitle: "La torre primordial",
  color: "#6ea27a",
  themeWord: "Verticalidad · Elementos · Conocimiento antiguo",
  identity: "Una torre ancestral cuyos pisos representan las fuerzas primordiales: fuego, agua, aire y tierra, coronadas por el rayo del génesis. Cada nivel impone su elemento y reescribe cómo se mueve, se lucha y se sobrevive. Subir es aprender el lenguaje del mundo antes de que existieran las reglas.",
  rule: "Regla exclusiva — «Ley de los Ciclos»: cada piso está regido por un elemento que altera la física local (el fuego prende, el agua arrastra, el aire empuja, la tierra atrapa). Llevar la afinidad equivocada castiga; usar el elemento del piso contra sus guardianes es la clave.",
  objective: "Ascender por los cuatro pisos elementales y derrotar al Concilio Primordial en la cúspide, dominando un elemento por fase.",
  bossId: "primordial-concord",
  sectors: [
    { name: "Piso de la Tierra", desc: "Suelos que atrapan, columnas que crecen; puzles de peso y bloques." },
    { name: "Piso del Agua", desc: "Corrientes que arrastran y niveles que suben y bajan." },
    { name: "Piso del Aire", desc: "Plataformas flotantes y ráfagas; verticalidad extrema." },
    { name: "Piso del Fuego", desc: "Lava alterna, braseros y aceite; el calor es constante." },
    { name: "La Cúspide del Génesis", desc: "Arena del Concilio Primordial, bañada por el rayo." }
  ],
  puzzles: ["Puzle de elementos (afinidad por piso)", "Puzle de peso y sustitución", "Secuencia de sonidos (resonancia elemental)", "Espejos de trayectoria (rayo)"],
  traps: ["Columnas elementales", "Suelo de lava/hielo alterno", "Estatua o pilar lanzallamas", "Bola rodante colosal (núcleo elemental)", "Ráfaga de viento de precipicio"],
  loot: ["Esencias elementales: reactivos de crafteo.", "Anillo de Afinidad: inmunidad parcial a un elemento a elegir.", "Tablilla del Génesis: lore y receta."],
  weapons: ["Cetro de los Ciclos: cambia de elemento según el piso y potencia el daño afín."],
  events: ["Cambio de ciclo: el elemento de un piso muta temporalmente al siguiente del ciclo.", "Marea primordial: el agua sube varios pisos y hay que ascender rápido."],
  secrets: ["Combinar dos elementos en un altar abre una cámara oculta.", "Un piso 'apagado' contiene la reliquia si se reactiva con el elemento correcto."],
  entrance: "Una puerta sellada por los cuatro sellos elementales; basta con activar el primero para entrar.",
  exit: "Un pozo de rayo de descenso directo tras el jefe; antes, hay que bajar piso a piso.",
  reset: "Cada piso restablece su elemento y sus mecanismos; los sellos se rearman.",
  multiplayer: "Algunos altares necesitan a varios jugadores aportando elementos distintos a la vez.",
  performance: "Cada piso es una sala aislada activa; los efectos elementales se limitan al piso ocupado."
});


CDE_DATA.dungeons.push({
  id: "solar-palace",
  name: "The Solar Palace",
  subtitle: "El palacio solar",
  color: "#e0b23c",
  themeWord: "Sol · Fuego · Luz · Calor",
  identity: "Un palacio majestuoso de oro, bronce y mármol consagrado al sol. Patios abrasadores, salones de espejos solares y braseros ceremoniales. La luz aquí no es decoración: es arma, llave y castigo. Un lugar deslumbrante donde el calor y el aceite convierten cualquier chispa en catástrofe.",
  rule: "Regla exclusiva — «Ley de la Luz»: la mayoría de puertas, ascensores y defensas se alimentan de luz solar redirigida con espejos. La oscuridad las apaga; la luz mal dirigida incinera. Muchos enemigos se fortalecen bajo la luz directa y son vulnerables en la sombra.",
  objective: "Atravesar el palacio redirigiendo la luz y liberar/derrotar a Helios Encadenado, la entidad solar cautiva en el corazón del trono.",
  bossId: "chained-helios",
  sectors: [
    { name: "El Atrio Dorado", desc: "Entrada ceremonial con braseros activables y guardias gilded." },
    { name: "La Sala de Espejos Solares", desc: "Puzles de luz: redirigir haces para abrir puertas sin incinerarse." },
    { name: "Los Jardines Ardientes", desc: "Patios con aceite, esferas solares rodantes y trampas ígneas." },
    { name: "El Observatorio del Alba", desc: "Arena del semijefe El Guardián del Mediodía." },
    { name: "El Trono del Sol", desc: "Arena de Helios Encadenado." }
  ],
  puzzles: ["Puzle de luz y espejos", "Secuencia de antorchas y braseros", "Puzle de peso y sustitución (compuertas de luz)"],
  traps: ["Rayo solar concentrado", "Esfera solar ardiente (bola rodante)", "Braseros en cadena", "Aceite y fuego", "Estatua o pilar lanzallamas", "Trampa oscilante ardiente"],
  loot: ["Fragmento solar: material luminoso de forja.", "Manto del Alba: reduce daño de fuego y luz.", "Lente ustoria: arma/herramienta que concentra luz."],
  weapons: ["Lanza del Mediodía: acumula calor con la luz y libera un tajo ígneo."],
  events: ["Cénit: toda la luz del palacio se intensifica; las sombras seguras se reducen.", "Eclipse: la luz se apaga, las defensas mueren y despiertan los enemigos de sombra."],
  secrets: ["Alinear tres espejos hacia un sello oculto abre la cámara del tesoro solar.", "Un brasero apagado adrede durante el Cénit revela un pasadizo en sombra."],
  entrance: "Un gran portón que solo se abre cuando un haz de luz solar redirigido incide en su cerradura.",
  exit: "Un pozo de luz de teletransporte tras el jefe; antes, hay que rehacer la ruta de espejos.",
  reset: "Los espejos vuelven a su ángulo inicial, los braseros se apagan y el aceite se repone.",
  multiplayer: "Los grandes puzles de luz requieren varios jugadores girando espejos coordinadamente.",
  performance: "Los haces de luz son rayos lógicos con VFX de AAA Particles; solo la sala activa calcula reflexiones."
});


/* ---------- CRIATURAS (70) ---------- */
CDE_DATA.creatures = [];
function mob(o){ CDE_DATA.creatures.push(o); }

/* ===== COMPARTIDAS ===== */
mob({ id:"gelatinous-cube", name:"Gelatinous Cube", en:"Gelatinous Cube", dungeon:"shared", rank:"rare",
  hp:"30 (15♥)", armor:"50% reducción", dmg:"< 1♥",
  desc:"Gran cubo gelatinoso translúcido que actúa como aspiradora viviente de la mazmorra: recoge drops, objetos perdidos y restos para evitar acumulación de entidades. Ciego, se guía por sonido, vibraciones y objetos tirados. Su cuerpo se deforma visualmente en el punto exacto del impacto (hundimiento, rebote y recuperación) sin recalcular la hitbox física.",
  behavior:["Lento y ciego; se mueve en las cuatro direcciones sin girar.","Bloquea pasillos: su peligro principal es cortar el paso.","Digiere cada objeto durante hasta 1 hora; si muere antes, suelta el 100% de lo no digerido.","Deja un rastro gelatinoso que ralentiza durante 20 s."],
  drops:["25%: Trozo de cubo gelatinoso — al usarlo sobre un bloque válido, lo 'digiere' y desaparece (no afecta a bedrock, bloques protegidos ni de progresión)."] });

mob({ id:"mimic", name:"Mimic", en:"Mimic", dungeon:"shared", rank:"normal",
  hp:"20 (10♥)", armor:"0", dmg:"5 (2.5♥)",
  desc:"Criatura codiciosa que adopta la forma de cofres, contenedores y objetos valiosos. Aunque esté disfrazada, se distingue por unos ojos visibles mediante una capa de textura superpuesta. Ataca por sorpresa y huye para volver a esconderse.",
  behavior:["Espera proximidad o interacción y ataca por sorpresa.","Siempre aplica Náusea I (5 s) y Ceguera (10 s) al 100%.","Tras atacar huye, busca otro cofre y puede sustituir uno real.","Si no encuentra dónde esconderse, sigue atacando hasta morir."],
  drops:["Siempre suelta algunas esmeraldas.","Posible loot valioso adicional por definir."] });

mob({ id:"stitched-watcher", name:"Vigía Cosido", en:"Stitched Watcher", dungeon:"shared", rank:"elite",
  hp:"—", armor:"—", dmg:"Indirecto",
  desc:"Criatura integrada en paredes, techos o estructuras que vigila la mazmorra. No combate directamente: percibe y coordina. Encaja especialmente en The Hollow Leviathan pero puede aparecer en varias dungeons.",
  behavior:["Sigue al jugador con varios ojos y detecta movimiento, luz o presencia.","Alerta mobs, activa trampas, cierra puertas y coordina emboscadas.","Se neutraliza con daño, ceguera, oscuridad, distracciones, puzles o destruyendo su órgano central."],
  drops:["Órgano óptico: componente de accesorios de detección."] });

mob({ id:"adventurer-wraith", name:"Espectro del Aventurero", en:"Adventurer's Wraith", dungeon:"shared", rank:"rare",
  hp:"90 (45♥)", armor:"2", dmg:"7 (3.5♥)",
  desc:"Aparición rara que surge en los puntos donde han muerto aventureros. Repite en bucle sus últimos instantes y ataca a quien perturba su recuerdo. No es hostil de inmediato: da tiempo a retirarse.",
  behavior:["Aparece cerca de restos de muertes previas.","Al derrotarlo o apaciguarlo revela pistas del loot perdido en la zona.","Puede señalar pasadizos o cofres olvidados."],
  drops:["Fragmento de recuerdo: pista de mapa o de secreto cercano."] });


/* Neotélido (con variantes -> cuentan hacia las 90) */
mob({ id:"neothelid-adult", name:"Neotélido adulto", en:"Neothelid (Adult)", dungeon:"shared", rank:"rare",
  hp:"320", armor:"6", dmg:"Cola 16 (8♥)",
  desc:"Mob raro colosal, ciego, serpentiforme y aberrante, nacido cuando una colonia de 60+ bebés pasa 10 minutos sin comer y se devora hasta dejar un único superviviente. Detecta sonidos, vibraciones y golpes hasta ~120 bloques.",
  behavior:["Golpe de cola (16, área amplia, gran empuje) y embestida corporal (12–18).","Devorar: atrapa, arrastra y puede curarse.","Furia ciega: al recibir un golpe que no localiza, ataca indiscriminadamente alrededor.","Vómito de crías: escupe 8–10 bebés que atacan a su objetivo.","Golpe corporal al suelo (14–18) con onda de choque y barrido descontrolado."],
  drops:["1 objeto garantizado (25% c/u): Cerebro neotélido, Cristal de recuerdo, Mapa incompleto o Diario reconstruido.","Huevos de bebé (30%:1 · 20%:2 · 50%:0).","~1/3 de la experiencia del Dragón del End."] });

mob({ id:"neothelid-baby", name:"Neotélido bebé", en:"Neothelid (Baby)", dungeon:"shared", rank:"normal",
  hp:"4", armor:"0", dmg:"Mordisco 2 (1♥)",
  desc:"Cría que se mueve en grupo buscando comida. Variante base del Neotélido; sin comer demasiado tiempo, las salvajes se vuelven caníbales y desencadenan la transformación adulta.",
  behavior:["Se desplazan en enjambre y atacan mordiendo.","Domesticadas siguen al jugador y aceptan órdenes básicas.","Colonia domesticada de 30 forma un adulto domesticado (160 HP, 4 armadura, cola 10)."],
  drops:["—"] });

/* ===== THE MIRROR CASTLE ===== */
mob({ id:"reflection-twin", name:"Gemelo de Reflejo", en:"Reflection Twin", dungeon:"mirror-castle", rank:"elite",
  hp:"Igual al jugador", armor:"Variable", dmg:"Copia del jugador",
  desc:"Copia parcialmente la apariencia, el equipo y los movimientos y ataques recientes del jugador. Se debilita destruyendo o alterando la superficie reflectante que lo mantiene.",
  behavior:["Imita los últimos movimientos memorizados del jugador.","Vinculado a una superficie especular concreta.","Romper o cubrir esa superficie lo debilita o lo destruye."],
  drops:["Astilla de azogue.","Posible pieza de equipo copiada de baja durabilidad."] });

mob({ id:"glass-servant", name:"Sirviente de Cristal", en:"Glass Servant", dungeon:"mirror-castle", rank:"normal",
  hp:"24 (12♥)", armor:"3", dmg:"6 (3♥)",
  desc:"Sirviente humanoide hecho de vidrio pulido y astillas. Camina con paso quebradizo por los salones y estalla en esquirlas al caer.",
  behavior:["Al morir explota en esquirlas que causan daño en área.","Se vuelve casi invisible cuando queda quieto frente a un espejo.","Vulnerable a ataques contundentes (martillo)."],
  drops:["Esquirlas de cristal.","Raro: vidrio pulido perfecto."] });

mob({ id:"quicksilver-maiden", name:"Doncella de Azogue", en:"Quicksilver Maiden", dungeon:"mirror-castle", rank:"normal",
  hp:"30 (15♥)", armor:"0", dmg:"5 (2.5♥)",
  desc:"Figura líquida de mercurio que se desliza por suelos y espejos. Al recibir daño se divide en gotas menores que vuelven a fusionarse.",
  behavior:["Se divide al ser golpeada y se recompone si se la deja reposar.","Puede viajar por superficies especulares para reaparecer detrás.","El fuego la endurece temporalmente (más lenta, más frágil)."],
  drops:["Gota de azogue.","Ingrediente alquímico."] });


mob({ id:"frame-sentinel", name:"Centinela de Marco", en:"Frame Sentinel", dungeon:"mirror-castle", rank:"normal",
  hp:"40 (20♥)", armor:"6", dmg:"7 (3.5♥)",
  desc:"Marco de espejo animado que patrulla los pasillos y proyecta puertas y ventanas falsas para desorientar. Su cristal es su punto débil.",
  behavior:["Proyecta aberturas ilusorias que llevan a callejones.","Bloquea el paso girando su marco como una hitbox sólida.","Romper su cristal lo inutiliza; el borde metálico resiste."],
  drops:["Marco de plata (decorativo crafteable).","Cristal intacto."] });

mob({ id:"specular-echo", name:"Eco Especular", en:"Specular Echo", dungeon:"mirror-castle", rank:"normal",
  hp:"18 (9♥)", armor:"0", dmg:"Repite el último ataque del jugador",
  desc:"Copia diferida que reproduce, con un segundo de retraso, la última acción del jugador. Enseña a controlar los propios ataques dentro de la dungeon.",
  behavior:["Reproduce con retraso el último golpe o habilidad del jugador.","No inicia acciones propias: solo repite.","Se anula quedándose quieto el tiempo suficiente."],
  drops:["Fragmento de eco: componente de crafteo sonoro."] });

mob({ id:"mirror-lantern-bearer", name:"Portador de Faroles", en:"Lantern-bearer", dungeon:"mirror-castle", rank:"normal",
  hp:"22 (11♥)", armor:"2", dmg:"5 (2.5♥) + ceguera",
  desc:"Sirviente encorvado que porta un farol cuya luz, rebotada en los espejos, ciega al jugador. Apagar su farol lo vuelve inofensivo y oscurece la sala.",
  behavior:["Su luz reflejada aplica Ceguera breve en línea de visión.","Busca espejos para amplificar su resplandor.","Apagar el farol (agua/flecha) lo debilita y cambia la iluminación de la sala."],
  drops:["Farol de plata: fuente de luz portátil."] });

mob({ id:"glass-harlequin", name:"Arlequín de Vidrio", en:"Glass Harlequin", dungeon:"mirror-castle", rank:"elite",
  hp:"70 (35♥)", armor:"4", dmg:"9 (4.5♥)",
  desc:"Bufón de cristal ágil y burlón que intercambia su posición con la del jugador o con sus propios reflejos, atacando desde ángulos imposibles.",
  behavior:["Teletransporte corto entre espejos y reflejos.","Lanza esquirlas giratorias en abanico.","Se ríe con voces reflejadas para confundir la dirección del ataque."],
  drops:["Cascabel de vidrio (accesorio).","Raro: máscara del arlequín."] });


mob({ id:"broken-twins", name:"Los Gemelos Rotos", en:"The Broken Twins", dungeon:"mirror-castle", rank:"miniboss",
  hp:"2 × 120 (comparten daño)", armor:"6", dmg:"12 (6♥)",
  desc:"Dos caballeros especulares que comparten una única reserva de vida repartida: dañar a uno hiere al otro, pero solo mueren si caen casi a la vez. Uno ataca de frente y el otro desde el reflejo.",
  behavior:["Comparten pool de vida; hay que igualar el daño de ambos.","Uno es el 'real' y el otro su reflejo, e intercambian roles al girar la sala.","Golpear al reflejo equivocado devuelve el daño (Ley del Reflejo)."],
  drops:["Par de hojas espejadas.","Fragmento de trono invertido."] });

mob({ id:"lady-quicksilver", name:"La Dama del Azogue", en:"The Lady of Quicksilver", dungeon:"mirror-castle", rank:"miniboss",
  hp:"180 (90♥)", armor:"3", dmg:"11 (5.5♥)",
  desc:"Bruja del castillo cuyo cuerpo es mercurio vivo. Convierte los suelos en espejos líquidos y hace emerger manos de azogue desde cada superficie reflectante.",
  behavior:["Convierte el suelo en espejo: caer en él teletransporta a otra sala.","Invoca manos de azogue que agarran e inmovilizan.","Es vulnerable solo cuando su reflejo real queda expuesto por una rotura."],
  drops:["Corazón de azogue (reliquia).","Receta de Armadura de Plata Pulida."] });

mob({ id:"shattered-sovereign", name:"El Soberano Añicos", en:"The Shattered Sovereign", dungeon:"mirror-castle", rank:"boss",
  hp:"1200 (fases)", armor:"8", dmg:"14–20",
  desc:"JEFE. Un rey compuesto de mil añicos de espejo suspendidos que se recomponen en distintas formas. Solo puede morir si se destruyen, una a una, las grandes superficies especulares que sostienen su reflejo en el Salón del Trono Invertido.",
  behavior:["Fase 1: combate como caballero espejado, devolviendo daño a los golpes frontales mal dirigidos.","Fase 2: se fragmenta y ataca desde múltiples reflejos simultáneos; hay que romper el espejo 'ancla'.","Fase 3: invierte la sala (techo/suelo) y proyecta reflejos hostiles del propio jugador.","Cada gran espejo destruido reduce su armadura y expone su forma real."],
  drops:["Corona Añicos (casco único).","Núcleo del Trono Invertido (material legendario).","Fragmento de azogue puro."] });


/* ===== THE WORLDBEARER ===== */
mob({ id:"walking-fragment", name:"Fragmento Andante", en:"Walking Fragment", dungeon:"worldbearer", rank:"normal",
  hp:"28 (14♥)", armor:"6", dmg:"6 (3♥)",
  desc:"Trozo de la piel pétrea del titán que ha cobrado vida y camina con pesadez. Lento pero muy resistente; ideal para bloquear pasos estrechos.",
  behavior:["Muy resistente al daño cortante; débil al contundente.","Al morir se desmorona en bloques que pueden usarse.","Se agrupa para taponar rutas."],
  drops:["Piedra viva (bloque de construcción)."] });

mob({ id:"ballast-pilgrim", name:"Peregrino de Lastre", en:"Ballast Pilgrim", dungeon:"worldbearer", rank:"normal",
  hp:"24 (12♥)", armor:"2", dmg:"7 (3.5♥)",
  desc:"Fanático que carga bloques de contrapeso a la espalda para 'aliviar' al titán. Cuando muere, suelta su lastre, que puede activar placas o aplastar.",
  behavior:["Al morir suelta un bloque pesado que cae y puede activar mecanismos.","Se sacrifican arrojándose sobre placas para bloquear rutas.","Lentos por el peso que cargan."],
  drops:["Bloque de contrapeso.","Reliquia del peregrino (rara)."] });

mob({ id:"counterweight-guardian", name:"Guardián de Contrapeso", en:"Counterweight Guardian", dungeon:"worldbearer", rank:"normal",
  hp:"46 (23♥)", armor:"6", dmg:"10 (5♥) + empuje",
  desc:"Autómata de piedra armado con una maza de contrapeso, hermano mecánico del Martillo de Contrapeso del jugador. Carga sus golpes para lanzar por los aires.",
  behavior:["Carga el golpe: más espera, más daño y empuje enorme.","Puede desplazar bloques empujables al golpear.","Vulnerable durante la larga recuperación de su golpe cargado."],
  drops:["Núcleo de contrapeso.","Raro: plano del Martillo de Contrapeso."] });

mob({ id:"lithic-swarm", name:"Colonia Lítica", en:"Lithic Swarm", dungeon:"worldbearer", rank:"normal",
  hp:"3 c/u (enjambre)", armor:"0", dmg:"2 (1♥)",
  desc:"Nube de guijarros vivos que fluye como arena por las grietas del titán. Individualmente inofensivos, en masa cubren el suelo y erosionan la armadura.",
  behavior:["Se mueven en enjambre y rodean al jugador.","El contacto continuo desgasta durabilidad de armadura.","Vulnerables a área, agua y explosiones."],
  drops:["Grava viva."] });


mob({ id:"lesser-atlas", name:"Atlante Menor", en:"Lesser Atlas", dungeon:"worldbearer", rank:"elite",
  hp:"140 (70♥)", armor:"8", dmg:"12 (6♥)",
  desc:"Coloso humanoide que sostiene con los brazos una sección del techo. Mientras vive, ese techo se mantiene; al morir, la estructura cae. Obliga a elegir entre matarlo o esquivarlo.",
  behavior:["Sostiene un techo o plataforma: matarlo provoca un derrumbe controlado.","Golpea con puños que agrietan el suelo.","Puede usarse su muerte para abrir o cerrar rutas."],
  drops:["Núcleo de atlante (material pesado).","Fragmento de mundo suspendido."] });

mob({ id:"foundation-wyrm", name:"Sierpe de Cimientos", en:"Foundation Wyrm", dungeon:"worldbearer", rank:"elite",
  hp:"120 (60♥)", armor:"5", dmg:"11 (5.5♥)",
  desc:"Gusano de roca que horada los huesos y cimientos del titán, emergiendo por donde menos se espera. Sus túneles pueden abrir atajos o precipicios.",
  behavior:["Se entierra y emerge por sorpresa (IA de emboscada).","Al emerger crea un agujero que puede usarse como ruta.","Débil justo después de emerger, mientras se reorienta."],
  drops:["Coraza de sierpe (armadura ligera pesada).","Diente perforante."] });

mob({ id:"living-vertebra", name:"El Vértebra Viva", en:"The Living Vertebra", dungeon:"worldbearer", rank:"miniboss",
  hp:"260 (130♥) por segmentos", armor:"7", dmg:"13 (6.5♥)",
  desc:"Un segmento de la columna del titán que se ha independizado y se enrosca por la sala como una serpiente de piedra multiparte. Cada vértebra es una hitbox separada.",
  behavior:["Entidad multiparte: destruir vértebras concretas la acorta y debilita.","Barre la sala con su cuerpo y aplasta con las secciones.","Protege su vértebra-núcleo, único punto letal."],
  drops:["Vértebra-núcleo (reliquia).","Módulos de piedra viva."] });

mob({ id:"core-custodian", name:"Custodio del Núcleo", en:"Core Custodian", dungeon:"worldbearer", rank:"miniboss",
  hp:"300 (150♥)", armor:"9", dmg:"14 (7♥) + empuje",
  desc:"Guardián colosal que vigila el ascenso al corazón. Usa contrapesos gigantes como armas y controla las placas basculantes de su arena.",
  behavior:["Manipula placas basculantes para desequilibrar al jugador.","Lanza y recoge una bola de contrapeso encadenada.","Se aturde si su propia bola impacta contra un muro."],
  drops:["Cadena del custodio (accesorio de anti-empuje).","Sello del corazón."] });

mob({ id:"dying-atlas", name:"Atlas Moribundo", en:"The Dying Atlas", dungeon:"worldbearer", rank:"boss",
  hp:"1500 (fases)", armor:"10", dmg:"16–22",
  desc:"JEFE. La voluntad viva del titán, un coloso agonizante que sostiene el mundo aun mientras lucha. Cada fase debilita un soporte de la arena, que empieza a derrumbarse: el combate es también una carrera contra la caída.",
  behavior:["Fase 1: puñetazos sísmicos y ondas de choque que agrietan el suelo.","Fase 2: arranca contrapesos de la arena y los lanza; el techo empieza a ceder.","Fase 3: la arena se derrumba por partes y hay que combatir reubicando el peso para no caer.","Sus manos y hombros son partes independientes que pueden inutilizarse."],
  drops:["Corazón del Titán (reliquia legendaria).","Martillo de Contrapeso mejorado (posible).","Núcleo de mundo."] });


/* ===== THE HOLLOW LEVIATHAN ===== */
mob({ id:"clinging-parasite", name:"Parásito Adherido", en:"Clinging Parasite", dungeon:"hollow-leviathan", rank:"normal",
  hp:"16 (8♥)", armor:"0", dmg:"4 (2♥)",
  desc:"Sanguijuela colosal adherida a las paredes de carne del leviatán. Se deja caer sobre los intrusos y se aferra, drenando salud lentamente.",
  behavior:["Espera adherido al techo o pared y cae sobre el objetivo.","Se aferra y drena vida hasta que se lo golpea.","Débil una vez despegado del muro."],
  drops:["Glándula de sanguijuela (alquimia)."] });

mob({ id:"antibody", name:"Anticuerpo", en:"Antibody", dungeon:"hollow-leviathan", rank:"normal",
  hp:"12 (6♥)", armor:"0", dmg:"5 (2.5♥)",
  desc:"Organismo defensivo blanco que el leviatán genera en masa para expulsar patógenos. Aparecen en oleadas proporcionales al ruido y al daño que causa el jugador.",
  behavior:["Aparecen en oleadas al activarse la 'respuesta inmune'.","Se lanzan en enjambre contra el intruso.","Menos ruido y daño = menos anticuerpos."],
  drops:["Plasma coagulado."] });

mob({ id:"devouring-larva", name:"Larva Devoradora", en:"Devouring Larva", dungeon:"hollow-leviathan", rank:"normal",
  hp:"20 (10♥)", armor:"1", dmg:"6 (3♥)",
  desc:"Larva voraz que repta por los conductos digestivos consumiendo todo a su paso. Crece si devora suficiente materia.",
  behavior:["Devora drops y restos; puede engordar y volverse más peligrosa.","Repta rápido por superficies húmedas.","Vulnerable al fuego."],
  drops:["Saco larval.","Raro: larva madura (invocable)."] });

mob({ id:"wall-maw", name:"Boca de Pared", en:"Wall Maw", dungeon:"hollow-leviathan", rank:"normal",
  hp:"36 (18♥)", armor:"4", dmg:"8 (4♥) + agarre",
  desc:"Boca oculta en el tejido de las paredes, entre trampa y criatura. Muerde a quien pasa cerca y puede tragar y escupir a otra zona.",
  behavior:["Camuflada hasta que el jugador se acerca.","Muerde, agarra y puede transportar a otra sala.","Su interior blando es su punto débil cuando está abierta."],
  drops:["Diente de pared.","Membrana."] });


mob({ id:"spitting-polyp", name:"Pólipo Escupidor", en:"Spitting Polyp", dungeon:"hollow-leviathan", rank:"normal",
  hp:"22 (11♥)", armor:"0", dmg:"Ácido 7 (3.5♥)",
  desc:"Pólipo fijo en suelos y techos que escupe bilis ácida a distancia. Su ácido daña la armadura y crea charcos peligrosos.",
  behavior:["Ataque a distancia con proyectiles ácidos.","Deja charcos de ácido que degradan armadura.","Inmóvil: se elimina fácil si se llega a corta distancia."],
  drops:["Vejiga ácida (arma arrojadiza)."] });

mob({ id:"membrane-weaver", name:"Tejedor de Membranas", en:"Membrane Weaver", dungeon:"hollow-leviathan", rank:"elite",
  hp:"110 (55♥)", armor:"3", dmg:"9 (4.5♥)",
  desc:"Criatura que sella pasajes con tejido vivo y teje capullos que liberan mobs. Controla el flujo de la dungeon cerrando y abriendo conductos.",
  behavior:["Sella salidas con membranas que hay que cortar o quemar.","Crea capullos que eclosionan en Larvas o Anticuerpos.","Huye y re-sella si se ve superado."],
  drops:["Seda de membrana (armadura anti-veneno).","Capullo intacto."] });

mob({ id:"false-heart", name:"Corazón Falso", en:"False Heart", dungeon:"hollow-leviathan", rank:"elite",
  hp:"150 (75♥)", armor:"5", dmg:"Onda 10 (5♥)",
  desc:"Órgano señuelo que late con fuerza para atraer al jugador y confundirlo respecto al corazón real. Cada latido emite una onda de choque y bombea enemigos a la sala.",
  behavior:["Late emitiendo ondas de empuje rítmicas (usa resonancia).","Bombea Anticuerpos con cada latido.","Destruirlo altera el sistema de fluidos... pero no mata al leviatán."],
  drops:["Válvula falsa.","Pista sobre la ubicación del corazón real."] });

mob({ id:"nerve-cluster", name:"El Sistema Nervioso", en:"The Nerve Cluster", dungeon:"hollow-leviathan", rank:"miniboss",
  hp:"320 (160♥) multiparte", armor:"4", dmg:"12 (6♥)",
  desc:"Maraña de ganglios y nervios luminosos que recorre toda la sala como un candelabro vivo. Coordina las defensas del leviatán; cortar sus ramas apaga trampas y ojos.",
  behavior:["Entidad multiparte: cada ganglio controla trampas u ojos de la sala.","Descargas eléctricas por los nervios que conectan los ganglios.","Cortar ramas concretas apaga defensas antes del núcleo."],
  drops:["Ganglio luminoso (accesorio).","Fibra nerviosa conductora."] });

mob({ id:"sovereign-parasite", name:"El Parásito Regente", en:"The Sovereign Parasite", dungeon:"hollow-leviathan", rank:"boss",
  hp:"1300 (fases)", armor:"6", dmg:"15–21",
  desc:"JEFE. El organismo que reanima al leviatán desde su cavidad central, un parásito colosal enraizado en el corazón real. Combatirlo altera todo el cuerpo: cada fase colapsa un sistema de la dungeon.",
  behavior:["Fase 1: golpea con tentáculos-raíz y escupe crías.","Fase 2: retrae el corazón tras costillas vivas que hay que abrir.","Fase 3: al verse morir, provoca un espasmo global del leviatán (la sala se contrae).","Sus raíces son partes independientes; cortarlas reduce su regeneración."],
  drops:["Corazón del Leviatán (reliquia legendaria).","Bisturí del Cirujano mejorado (posible).","Ámbar orgánico."] });


/* ===== THE VEILED PEAK ===== */
mob({ id:"veiled-soul", name:"Alma Velada", en:"Veiled Soul", dungeon:"veiled-peak", rank:"normal",
  hp:"18 (9♥)", armor:"0", dmg:"5 (2.5♥)",
  desc:"Espíritu de un escalador perdido, envuelto en jirones de tela y niebla. Casi invisible en la ventisca; se materializa al atacar.",
  behavior:["Se difumina en la niebla y reaparece cerca.","La luz de los faros la hace visible y vulnerable.","Atraviesa parcialmente obstáculos."],
  drops:["Jirón espectral (textil mágico)."] });

mob({ id:"petrified-climber", name:"Escalador Petrificado", en:"Petrified Climber", dungeon:"veiled-peak", rank:"normal",
  hp:"30 (15♥)", armor:"5", dmg:"6 (3♥)",
  desc:"Aventurero congelado y reanimado por el velo, aún aferrado a su piolet. Se mueve rígido pero golpea con fuerza sorprendente.",
  behavior:["Resistente al frío; frágil al calor/fuego.","Escala paredes verticales para flanquear.","Puede empujar hacia precipicios."],
  drops:["Piolet desgastado.","Provisiones congeladas."] });

mob({ id:"zephyr-wisp", name:"Céfiro", en:"Zephyr Wisp", dungeon:"veiled-peak", rank:"normal",
  hp:"14 (7♥)", armor:"0", dmg:"3 (1.5♥) + empuje fuerte",
  desc:"Elemental de viento que apenas hace daño pero empuja con violencia hacia el vacío. En las cornisas, un solo empujón puede ser mortal.",
  behavior:["Su ataque principal es empujar hacia precipicios.","Se mueve errático y rápido.","Se dispersa con proyectiles pesados o sin viento a favor."],
  drops:["Esencia de viento."] });

mob({ id:"fog-bellringer", name:"Campanero de Niebla", en:"Fog Bellringer", dungeon:"veiled-peak", rank:"normal",
  hp:"26 (13♥)", armor:"2", dmg:"5 (2.5♥)",
  desc:"Monje espectral que toca una campana para invocar niebla espesa y llamar a otras almas. Silenciar su campana despeja la zona.",
  behavior:["Toca la campana: genera niebla cegadora y atrae aliados.","Coordina emboscadas con Almas Veladas.","Romper la campana lo desarma y disipa la niebla local."],
  drops:["Campana de niebla (mecanismo sonoro)."] });


mob({ id:"spectral-yak", name:"Yak Espectral", en:"Spectral Yak", dungeon:"veiled-peak", rank:"normal",
  hp:"44 (22♥)", armor:"3", dmg:"9 (4.5♥) embestida",
  desc:"Bestia fantasmal de montaña que carga en línea recta desde la niebla. Su embestida puede arrojar al jugador por una cornisa.",
  behavior:["Carga telegrafiada de largo alcance con gran empuje.","Tras fallar la carga queda aturdido contra el muro.","Se mueve en pequeñas manadas."],
  drops:["Pelaje espectral (aislante del frío)."] });

mob({ id:"veil-warden", name:"Guardián del Velo", en:"Veil Warden", dungeon:"veiled-peak", rank:"elite",
  hp:"120 (60♥)", armor:"5", dmg:"11 (5.5♥)",
  desc:"Centinela encapuchado que se vuelve invisible dentro de la niebla y solo es visible bajo la luz de los faros. Custodia los tramos de subida.",
  behavior:["Invisible en niebla; visible y vulnerable bajo luz de faro.","Ataca desde ángulos muertos (IA de emboscada).","Apaga faros cercanos para recuperar su ventaja."],
  drops:["Manto del velo (sigilo).","Llave de cornisa."] });

mob({ id:"frozen-oracle", name:"Oráculo Congelado", en:"Frozen Oracle", dungeon:"veiled-peak", rank:"elite",
  hp:"130 (65♥)", armor:"4", dmg:"Escarcha 10 (5♥)",
  desc:"Vidente petrificada en hielo que lanza esquirlas de escarcha y 'profecías' que aplican debuffs de lentitud y miedo. Ralentiza y desorienta antes de rematar.",
  behavior:["Congela el suelo creando placas deslizantes.","Aplica Lentitud y visión reducida con sus profecías.","Vulnerable mientras 'profetiza' (animación larga)."],
  drops:["Reliquia del Oráculo (revela ruta segura).","Núcleo de escarcha."] });

mob({ id:"summit-keeper", name:"El Guardián de la Cima", en:"The Summit Keeper", dungeon:"veiled-peak", rank:"miniboss",
  hp:"340 (170♥)", armor:"7", dmg:"13 (6.5♥)",
  desc:"Coloso de hielo y viento que defiende la cornisa final antes de la cima. Controla el viento de la arena y desata pequeños aludes.",
  behavior:["Cambia la dirección del viento de la arena a voluntad.","Provoca aludes que hay que esquivar o cubrirse.","Se enfría y endurece; el fuego rompe su coraza temporalmente."],
  drops:["Corona de escarcha (accesorio).","Sello de la cima."] });

mob({ id:"crowned-tempest", name:"La Tormenta Coronada", en:"The Crowned Tempest", dungeon:"veiled-peak", rank:"boss",
  hp:"1250 (fases)", armor:"5", dmg:"14–20 + rayo",
  desc:"JEFE. El espíritu que corona el pico: una tormenta viva de viento, hielo y relámpago. Combate en una arena abierta y expuesta donde el clima es su mayor arma.",
  behavior:["Fase 1: ráfagas que empujan hacia los bordes y esquirlas de hielo.","Fase 2: convoca la niebla y ataca desde la invisibilidad, guiada por el trueno.","Fase 3: descarga rayos en zonas marcadas y desata la ventisca total.","El velo la protege; encender los faros de la arena la expone por turnos."],
  drops:["Corona de la Tempestad (yelmo único).","Corazón del Velo (reliquia legendaria).","Cristal de rayo."] });


/* ===== THE DESCENT INTO MADNESS ===== */
mob({ id:"hollow-fencer", name:"El Duelista Hueco", en:"The Hollow Fencer", dungeon:"descent-madness", rank:"rare",
  hp:"Por definir (ágil)", armor:"Ligera", dmg:"Moderado / muy rápido",
  desc:"Cadáver sin cabeza de un aventurero ágil, controlado por un parásito alojado en el pecho que usa la cavidad torácica como órgano de resonancia para imitar voces humanas y atraer a las víctimas. Combate con estoque y broquel a un ritmo técnico y veloz.",
  behavior:["Emite voces de auxilio pre-grabadas (ES/EN) desde el pecho antes de atacar.","Se desplaza lateralmente, flanquea y usa esquinas (IA de emboscada).","Punzada rápida (estocada casi instantánea) y Ráfaga de estocadas (puede deshabilitar escudos).","Postura de parry con 100% de éxito durante su animación reconocible.","Tras ~10 s ofensivos se fatiga 3 s; con poca vida intenta curarse con una poción interrumpible."],
  drops:["Buena cantidad de esmeraldas, armas y armadura encantadas.","10%: Estoque de la Locura (irrompible, ligerísimo, con Punzada rápida y Ráfaga de estocadas)."] });

mob({ id:"the-whisperer", name:"Susurrante", en:"The Whisperer", dungeon:"descent-madness", rank:"normal",
  hp:"20 (10♥)", armor:"0", dmg:"6 (3♥)",
  desc:"Presencia invisible que solo se delata al susurrar. Usa fragmentos de conversación para atraer al jugador a trampas o precipicios.",
  behavior:["Invisible hasta que habla; su voz proviene de una dirección falsa.","Guía hacia trampas con susurros.","La Vela de Cordura lo revela y lo hace vulnerable."],
  drops:["Eco susurrado (componente sonoro)."] });

mob({ id:"faceless-pilgrim", name:"Peregrino sin Rostro", en:"Faceless Pilgrim", dungeon:"descent-madness", rank:"normal",
  hp:"24 (12♥)", armor:"1", dmg:"6 (3♥)",
  desc:"Figura encapuchada que copia la apariencia de otros jugadores o NPCs conocidos, sembrando la duda sobre quién es real en multijugador.",
  behavior:["Adopta el aspecto de un compañero o NPC.","Se mezcla entre el grupo hasta atacar.","Al morir revela su rostro vacío."],
  drops:["Máscara vacía."] });

mob({ id:"the-crowd", name:"La Multitud", en:"The Crowd", dungeon:"descent-madness", rank:"normal",
  hp:"6 c/u (enjambre)", armor:"0", dmg:"3 (1.5♥)",
  desc:"Marea de manos y sombras que emergen del suelo y las paredes para agarrar y frenar. No matan, pero inmovilizan lo suficiente para que algo peor lo haga.",
  behavior:["Agarran y ralentizan en masa.","Surgen de superficies oscuras; la luz las reduce.","Vulnerables a área y fuego."],
  drops:["Sombra coagulada."] });


mob({ id:"rotten-memory", name:"Recuerdo Podrido", en:"Rotten Memory", dungeon:"descent-madness", rank:"normal",
  hp:"28 (14♥)", armor:"0", dmg:"7 (3.5♥) + náusea",
  desc:"Recreación distorsionada de un ser querido o aliado del jugador, deformada por la locura. Ataca con la culpa: dudar cuesta caro.",
  behavior:["Aparenta ser un NPC o rescate y muta al acercarse.","Aplica Náusea y visión deformada.","Se disipa si se le ignora el tiempo suficiente."],
  drops:["Fragmento de recuerdo corrompido."] });

mob({ id:"choir-of-cries", name:"Coro de Auxilio", en:"The Choir of Cries", dungeon:"descent-madness", rank:"elite",
  hp:"100 (50♥)", armor:"2", dmg:"Sónico 9 (4.5♥)",
  desc:"Amasijo de bocas que emite decenas de voces de auxilio superpuestas, saturando el sistema de resonancia y atrayendo a todo lo cercano. El ruido es su escudo y su arma.",
  behavior:["Sobrecarga el sistema de ruido: atrae mobs de otras salas.","Ataques sónicos en área que desorientan.","Silenciarlo (o el sigilo) reduce su poder."],
  drops:["Núcleo del coro (resonancia).","Fragmento resonante."] });

mob({ id:"the-one-who-repeats", name:"El Que Repite", en:"The One Who Repeats", dungeon:"descent-madness", rank:"elite",
  hp:"140 (70♥)", armor:"3", dmg:"10 (5♥)",
  desc:"Entidad ligada al bucle de los pasillos: cuando está presente, la sala se reinicia y todo lo hecho parece deshacerse. Romper su ciclo es la única forma de avanzar de verdad.",
  behavior:["Fuerza reinicios de la sala (usa el pasillo infinito).","Cada repetición cambia un detalle: la pista está en lo que difiere.","Al derrotarlo, el bucle se rompe y se abre la ruta real."],
  drops:["Ancla de bucle (evita un reinicio).","Cristal de Escape (posible)."] });

mob({ id:"semblance", name:"Semblante", en:"The Semblance", dungeon:"descent-madness", rank:"miniboss",
  hp:"280 (140♥)", armor:"4", dmg:"12 (6♥)",
  desc:"Un espejo viviente de las peores decisiones del jugador: combina sus errores, su equipo y sus tácticas más predecibles para castigarlo con ellas.",
  behavior:["Aprende y repite los patrones de ataque del jugador.","Cambia de forma según cómo lucha el jugador (más agresivo o más defensivo).","Solo es vulnerable cuando el jugador rompe su propia rutina."],
  drops:["Reflejo lúcido (accesorio).","Pieza de equipo espejada."] });

mob({ id:"broken-sanity", name:"La Cordura Rota", en:"The Broken Sanity", dungeon:"descent-madness", rank:"boss",
  hp:"1400 (fases)", armor:"5", dmg:"15–22",
  desc:"JEFE. La personificación de la locura del descenso: una entidad cambiante que ataca la percepción tanto como el cuerpo. Nada en su arena es del todo fiable, y la propia interfaz puede mentir.",
  behavior:["Fase 1: proyecta clones falsos del jefe; solo uno hace daño real.","Fase 2: distorsiona la sala y las voces guían hacia trampas del propio combate.","Fase 3: invierte controles percibidos y satura con recuerdos hostiles; la Vela de Cordura marca lo real.","Ignorar los engaños y confiar en patrones aprendidos es la clave."],
  drops:["Corazón de la Locura (reliquia legendaria).","Estoque de la Locura mejorado (posible).","Cristal de lucidez."] });


/* ===== THE PRIMORDIAL TOWER ===== */
mob({ id:"primal-ember", name:"Ascua Primigenia", en:"Primal Ember", dungeon:"primordial-tower", rank:"normal",
  hp:"16 (8♥)", armor:"0", dmg:"Fuego 6 (3♥)",
  desc:"Chispa viva del piso del fuego que prende todo lo que toca, incluido el aceite y al jugador aceitado. Frágil pero peligrosa en grupo.",
  behavior:["Incendia al contacto y prende charcos de aceite.","Se apaga con agua (muere).","Más rápida en el piso del fuego."],
  drops:["Brasa primordial (reactivo)."] });

mob({ id:"ancestral-droplet", name:"Gota Ancestral", en:"Ancestral Droplet", dungeon:"primordial-tower", rank:"normal",
  hp:"18 (9♥)", armor:"0", dmg:"5 (2.5♥) + arrastre",
  desc:"Masa de agua viva del piso acuático que arrastra al jugador con corrientes y puede apagar sus fuentes de luz y fuego.",
  behavior:["Genera corrientes que empujan y arrastran.","Apaga antorchas y al jugador ardiendo.","Se congela y se vuelve frágil con frío."],
  drops:["Esencia de agua."] });

mob({ id:"old-gust", name:"Ráfaga Antigua", en:"Old Gust", dungeon:"primordial-tower", rank:"normal",
  hp:"14 (7♥)", armor:"0", dmg:"3 (1.5♥) + empuje",
  desc:"Torbellino del piso del aire que empuja al jugador entre plataformas flotantes, buscando lanzarlo al vacío entre pisos.",
  behavior:["Empuja hacia los huecos entre plataformas.","Puede elevar y soltar al jugador.","Se disipa con proyectiles pesados."],
  drops:["Esencia de aire."] });

mob({ id:"primordial-pebble", name:"Guijarro Primordial", en:"Primordial Pebble", dungeon:"primordial-tower", rank:"normal",
  hp:"26 (13♥)", armor:"6", dmg:"6 (3♥)",
  desc:"Roca viva del piso de la tierra que atrapa los pies del jugador con raíces de piedra y bloquea rutas endureciéndose.",
  behavior:["Inmoviliza brevemente con roca emergente.","Muy resistente; débil al contundente.","Se endurece formando muros temporales."],
  drops:["Esencia de tierra."] });


mob({ id:"genesis-spark", name:"Chispa de Génesis", en:"Genesis Spark", dungeon:"primordial-tower", rank:"normal",
  hp:"20 (10♥)", armor:"0", dmg:"Rayo 8 (4♥)",
  desc:"Descarga viva del génesis que salta entre superficies metálicas y agua. Encadena rayos entre enemigos y charcos, castigando la mala posición.",
  behavior:["Sus rayos rebotan entre metal y agua.","Encadena daño si el jugador está mojado.","Estática lenta pero de reacción rápida."],
  drops:["Chispa de génesis (reactivo de rayo)."] });

mob({ id:"unstable-golem", name:"Golem Elemental Inestable", en:"Unstable Elemental Golem", dungeon:"primordial-tower", rank:"elite",
  hp:"150 (75♥)", armor:"6", dmg:"12 (6♥)",
  desc:"Coloso que cicla entre fuego, agua, aire y tierra, cambiando sus ataques y debilidades con cada mutación. Obliga a leer su elemento actual antes de golpear.",
  behavior:["Cambia de elemento cada pocos segundos (cambia debilidad y ataque).","Usar el elemento opuesto al activo lo daña de más.","En su cambio de fase queda vulnerable un instante."],
  drops:["Núcleo inestable (multi-elemento).","Fragmento de afinidad."] });

mob({ id:"cycle-custodian", name:"Custodio de los Ciclos", en:"Cycle Custodian", dungeon:"primordial-tower", rank:"elite",
  hp:"130 (65♥)", armor:"5", dmg:"11 (5.5♥)",
  desc:"Guardián que controla el orden de los elementos del piso y puede forzar un 'cambio de ciclo', alterando la física local en pleno combate.",
  behavior:["Fuerza el evento de cambio de ciclo del piso.","Protege los altares elementales.","Vulnerable cuando su ciclo queda desincronizado."],
  drops:["Engranaje del ciclo.","Sello elemental."] });

mob({ id:"first-warden", name:"El Primer Guardián", en:"The First Warden", dungeon:"primordial-tower", rank:"miniboss",
  hp:"330 (165♥)", armor:"7", dmg:"13 (6.5♥)",
  desc:"El más antiguo de los custodios, hecho de los cuatro elementos en equilibrio. Domina dos elementos a la vez y combina sus efectos en ataques compuestos.",
  behavior:["Combina dos elementos (p. ej. vapor: agua+fuego) en cada ataque.","Cambia el elemento dominante de la arena.","Solo es vulnerable al elemento que no está usando."],
  drops:["Anillo de Afinidad (elección de elemento).","Fragmento del génesis."] });

mob({ id:"primordial-concord", name:"El Concilio Primordial", en:"The Primordial Concord", dungeon:"primordial-tower", rank:"boss",
  hp:"1500 (4 fases)", armor:"8", dmg:"16–22",
  desc:"JEFE. Un ser único que encarna los cuatro elementos primordiales, combatido por fases en la cúspide bañada por el rayo. Cada fase impone un elemento distinto y su regla física correspondiente.",
  behavior:["Fase Tierra: atrapa y aplasta con columnas emergentes.","Fase Agua: inunda y arrastra la arena, subiendo el nivel.","Fase Aire: reduce la arena a plataformas flotantes y empuja.","Fase Fuego: prende el suelo y el aceite; culmina con el rayo del génesis. Cada fase se vence con su elemento afín."],
  drops:["Corazón del Génesis (reliquia legendaria).","Cetro de los Ciclos mejorado (posible).","Esencia primordial pura."] });


/* ===== THE SOLAR PALACE ===== */
mob({ id:"solar-dancer", name:"Danzante Solar", en:"Solar Dancer", dungeon:"solar-palace", rank:"normal",
  hp:"22 (11♥)", armor:"1", dmg:"6 (3♥)",
  desc:"Sacerdote ágil consagrado al sol que combate girando cintas ardientes. Se fortalece bajo la luz directa y flaquea en la sombra.",
  behavior:["Más rápido y fuerte bajo luz solar directa.","Deja estelas de fuego breves al girar.","Vulnerable si se lo arrastra a la sombra."],
  drops:["Cinta solar (textil ígneo)."] });

mob({ id:"gilded-guard", name:"Guardia Dorado", en:"Gilded Guard", dungeon:"solar-palace", rank:"normal",
  hp:"40 (20♥)", armor:"8", dmg:"8 (4♥)",
  desc:"Guardián de armadura dorada bruñida que refleja la luz y cega al atacante que golpea de frente bajo el sol. Sólido y disciplinado.",
  behavior:["Su armadura refleja luz: golpear de frente al sol aplica Ceguera al jugador.","Alta armadura frontal; débil por la espalda.","Forma muros de escudos con otros guardias."],
  drops:["Placa dorada (forja).","Escudo bruñido."] });

mob({ id:"brazier-igneous", name:"Ígneo de Brasero", en:"Brazier Igneous", dungeon:"solar-palace", rank:"normal",
  hp:"18 (9♥)", armor:"0", dmg:"Fuego 7 (3.5♥)",
  desc:"Criatura de fuego que nace de los braseros del palacio. Prende el aceite y al jugador aceitado; apagar su brasero de origen la extingue.",
  behavior:["Emerge de braseros encendidos; prende aceite y jugador.","Se apaga con agua o cerrando su brasero.","Explota en chispas al morir."],
  drops:["Brasa dorada."] });

mob({ id:"bronze-falcon", name:"Halcón de Bronce", en:"Bronze Falcon", dungeon:"solar-palace", rank:"normal",
  hp:"16 (8♥)", armor:"4", dmg:"7 (3.5♥) picado",
  desc:"Autómata alado de bronce que patrulla los patios y se lanza en picado concentrando luz solar en un destello cegador.",
  behavior:["Ataca en picado desde el aire.","Concentra luz para cegar antes de impactar.","Frágil una vez en tierra tras fallar el picado."],
  drops:["Engranaje de bronce.","Pluma metálica."] });


mob({ id:"burning-standard", name:"Portaestandarte Ardiente", en:"Burning Standard-bearer", dungeon:"solar-palace", rank:"normal",
  hp:"30 (15♥)", armor:"3", dmg:"6 (3♥)",
  desc:"Portador de un estandarte solar que potencia y enardece a los aliados cercanos. Eliminarlo primero desactiva los buffs del grupo enemigo.",
  behavior:["Otorga daño y velocidad extra a los enemigos cercanos.","No es muy agresivo; se protege detrás de aliados.","Su estandarte marca la prioridad de objetivo."],
  drops:["Estandarte solar (decorativo/funcional)."] });

mob({ id:"solar-mirror-colossus", name:"Coloso de Espejos Solares", en:"Solar Mirror Colossus", dungeon:"solar-palace", rank:"elite",
  hp:"150 (75♥)", armor:"7", dmg:"Haz 13 (6.5♥)",
  desc:"Autómata cubierto de espejos que redirige haces de luz solar mortales por la sala. Convierte el puzle de luz del palacio en un peligro activo.",
  behavior:["Dispara y redirige haces de luz que incineran en línea.","Los mismos haces pueden usarse para resolver puzles o dañarlo.","Sus espejos son partes rompibles que reducen su alcance."],
  drops:["Espejo ustorio (lente de arma).","Núcleo solar."] });

mob({ id:"dawn-priestess", name:"Sacerdotisa del Alba", en:"Dawn Priestess", dungeon:"solar-palace", rank:"elite",
  hp:"120 (60♥)", armor:"3", dmg:"Luz 10 (5♥)",
  desc:"Sanadora del culto solar que cura a sus aliados y purga la oscuridad. Mientras viva, los enemigos del palacio se regeneran bajo la luz.",
  behavior:["Cura a aliados y se cura bajo luz directa.","Lanza destellos que dañan y ciegan.","En la sombra pierde su curación y queda vulnerable."],
  drops:["Cáliz del alba (accesorio de curación).","Incienso solar."] });

mob({ id:"noon-guardian", name:"El Guardián del Mediodía", en:"The Noon Guardian", dungeon:"solar-palace", rank:"miniboss",
  hp:"340 (170♥)", armor:"8", dmg:"14 (7♥)",
  desc:"Coloso ceremonial que alcanza su máximo poder al mediodía simulado del observatorio. Controla la intensidad de la luz de su arena y la usa como arma.",
  behavior:["Aumenta la luz de la arena (Cénit) para fortalecerse.","Barre la sala con un haz solar giratorio.","Provocar un 'eclipse' (apagar focos) lo debilita drásticamente."],
  drops:["Corona del mediodía (accesorio).","Sello del trono solar."] });

mob({ id:"chained-helios", name:"Helios Encadenado", en:"The Chained Helios", dungeon:"solar-palace", rank:"boss",
  hp:"1450 (fases)", armor:"7", dmg:"16–22 + fuego",
  desc:"JEFE. Una entidad solar cautiva —un pequeño sol vivo— encadenada al trono del palacio y explotada como fuente de poder. Combatirla es liberarla o extinguirla: en ambos casos, el palacio arde.",
  behavior:["Fase 1: lanza llamaradas y esferas solares rodantes por la arena.","Fase 2: rompe cadenas y desata haces de luz que hay que bloquear con espejos.","Fase 3: entra en Cénit total incendiando el aceite; provocar un eclipse abre su ventana letal.","Sus cadenas y núcleo son partes independientes; romperlas altera sus ataques."],
  drops:["Corazón Solar (reliquia legendaria).","Lanza del Mediodía mejorada (posible).","Fragmento de sol."] });


/* ---------- TRAMPAS ---------- */
CDE_DATA.traps = [];
function trap(o){ CDE_DATA.traps.push(o); }

trap({ name:"Mecánica del aceite", category:"Superficie", dungeon:["solar-palace","primordial-tower","descent-madness"],
  desc:"Charco extremadamente plano que no fluye ni ralentiza: es una superficie peligrosa. Al pisarlo, el jugador queda 'aceitado'. Si toca fuego, arde el doble de tiempo; el agua no lo apaga (lo empeora y duplica el daño). El polvo de hueso limpia el aceite si aún no arde, o da 30 min de protección preventiva.",
  config:["Duración del fuego","Multiplicador con agua","Protección de polvo de hueso"] });

trap({ name:"Lámpara de aceite", category:"Ambiental", dungeon:["solar-palace","descent-madness"],
  desc:"Lámpara colgada del techo sobre un charco de aceite, con una zona de detección invisible de 3 bloques. Cada vez que un jugador entra hay un 10% (1/10) de que caiga, impacte y prenda el aceite. Una lámpara caída no se reactiva.",
  config:["Probabilidad de caída","Ancho de zona","Reinicio"] });

trap({ name:"Paredes aplastantes", category:"Aplastamiento", dungeon:["worldbearer","hollow-leviathan"],
  desc:"Dos paredes con hitbox física real avanzan lentamente hacia el centro para aplastar. Empujan entidades y bloquean proyectiles. Dan tiempo a reaccionar y suelen tener una vía de escape (palanca, hueco, puzle).",
  config:["Velocidad","Daño","Tiempo de reacción","Método de escape","Reinicio"] });

trap({ name:"Estatua o pilar lanzallamas", category:"Fuego a distancia", dungeon:["solar-palace","primordial-tower"],
  desc:"Estatua o pilar retráctil (suelo/techo/pared) que se despliega, se vuelve sólido y dispara ráfagas de fuego en hasta 4 direcciones. Prende al jugador y el aceite. Una variante funciona como interruptor: cada golpe alterna encendido/apagado.",
  config:["Direcciones","Daño","Duración de ráfaga","Interacción con aceite"] });


trap({ name:"Trampa de derrumbamiento", category:"Estructural", dungeon:["worldbearer","veiled-peak","hollow-leviathan"],
  desc:"El suelo (o el techo) se derrumba progresivamente. Da 3–6 s para escapar; afecta a un máximo de ~20 bloques. Genera polvo que reduce la visión 3–5 s y puede mandar al jugador a otra sala, separarlo del grupo o abrir/cerrar rutas. Los escombros desaparecen escalonadamente (13–20 s).",
  config:["Suelo o techo","Tiempo de aviso","Tamaño","Destino de caída","Permanencia de escombros"] });

trap({ name:"Skeever Trap (jaula que cae)", category:"Encierro", dungeon:["hollow-leviathan","descent-madness"],
  desc:"Jaula pesada que cae del techo y encierra al jugador, unida por una cadena. 500 HP y 50% de reducción de daño: romperla es lento. Un accionador la levanta (activable con la mano desde fuera o con proyectiles desde dentro). Tras 3 min se llena de gas: Veneno I→III (sube un nivel cada 10 s, no mata directamente).",
  config:["Tiempo antes del gas","Nivel máximo","Proyectiles válidos","Posición del accionador"] });

trap({ name:"Tablero de pinchos móvil", category:"Perforación", dungeon:["worldbearer","primordial-tower"],
  desc:"Plataforma de pinchos con hitbox física que se desplaza (techo/paredes/laterales) para aplastar y perforar. Hace 16 de daño (8♥). Activable por placa de presión especial, temporizador, palanca o detección. Tamaño y velocidad totalmente configurables.",
  config:["Tamaño","Orientación","Velocidad","Recorrido","Activador"] });

trap({ name:"Bola rodante colosal", category:"Persecución", dungeon:["solar-palace","mirror-castle","hollow-leviathan","descent-madness","primordial-tower"],
  desc:"Gran masa sólida que se libera, cae o rueda por gravedad siguiendo la pendiente, golpeando y aplastando. Apariencia temática por dungeon: esfera solar ardiente, esfera reflectante, masa orgánica, esfera deformada de carne y ojos, o núcleo elemental. Puede romper bloques, activar placas y provocar derrumbes.",
  config:["Modelo/tamaño","Peso","Velocidad/aceleración","Daño","Recorrido","Reinicio","Nº de bolas"] });


trap({ name:"Pasillo infinito de la lámpara mágica", category:"Espacial", dungeon:["descent-madness","mirror-castle"],
  desc:"Trampa espacial para pasillos: el jugador avanza pero regresa de forma imperceptible al mismo tramo, creando un corredor infinito, mantenido por una lámpara mágica en el techo. Se escapa rompiendo/apagando la lámpara, caminando hacia atrás, quedándose quieto o siguiendo pistas que cambian en cada repetición.",
  config:["Longitud/segmentos","Solución","Cambios por repetición","Jugadores afectados"] });

trap({ name:"Trampa oscilante modular", category:"Impacto", dungeon:["primordial-tower","solar-palace","worldbearer"],
  desc:"Sistema común para hachas gigantes, mazas, martillos, bolas de pinchos, cuchillas y troncos suspendidos. Daño y empuje se configuran por separado. Variante ardiente que prende al jugador, el aceite y elementos inflamables.",
  config:["Modelo","Movimiento","Daño/empuje","Fuego","Activador","Reinicio"] });

trap({ name:"Cuchillas y púas retráctiles de pared", category:"Corte", dungeon:["veiled-peak","primordial-tower","hollow-leviathan"],
  desc:"Familia de trampas que salen de las paredes: cuchillas deslizantes que barren la superficie, púas retráctiles en ciclos o púas fijas que cortan el paso. Daño y empuje independientes: una variante hace mucho daño y poco empuje, otra lanza hacia otra trampa.",
  config:["Tipo de filo","Recorrido","Tiempo de extensión/retracción","Daño/empuje","Activador"] });

trap({ name:"Cadena tensada", category:"Barrido", dungeon:["worldbearer","veiled-peak"],
  desc:"Una cadena atraviesa una zona a gran velocidad para cortar, derribar, empujar, arrastrar o enganchar, pudiendo lanzar al jugador hacia otra trampa o bloquear temporalmente un camino.",
  config:["Trayectoria","Velocidad","Daño/empuje","Tiempo de agarre","Activador"] });

trap({ name:"Cofre de peso muerto", category:"Engaño", dungeon:["worldbearer","solar-palace"],
  desc:"Parece un cofre valioso, pero al abrirlo inclina el suelo y hace deslizar entidades y objetos hacia un precipicio, obligando a elegir entre el loot y la seguridad (o soltar objetos para reducir peso).",
  config:["Ángulo","Velocidad","Peso necesario","Loot","Reinicio"] });


trap({ name:"Puerta devoradora", category:"Órgano vivo", dungeon:["hollow-leviathan","descent-madness"],
  desc:"Parece una puerta normal, pero es un organismo vivo: se abre como una boca, absorbe, muerde, sujeta, traga y escupe, pudiendo transportar al jugador a otra sala o lanzarlo hacia otra trampa o encuentro.",
  config:["Fuerza de absorción","Daño","Tiempo de agarre","Método de liberación","Destino"] });

trap({ name:"Trampas vivas", category:"Órgano vivo", dungeon:["hollow-leviathan","descent-madness"],
  desc:"Familia orgánica: paredes que respiran, raíces o tentáculos que agarran, bocas ocultas, suelos digestivos, capullos que liberan mobs y ojos orgánicos que detectan al jugador. Tienen vida, resistencias y debilidades propias.",
  config:["Área de detección","Daño/empuje","Duración del agarre","Regeneración","Reinicio"] });

/* ---- Trampas nuevas por dungeon ---- */
trap({ name:"Sala de espejos giratorios", category:"Ilusión", dungeon:["mirror-castle"], nuevo:true,
  desc:"NUEVA. Sala cuyos paneles de espejo giran periódicamente y reorganizan el reflejo del recorrido. Mientras giran, las salidas reales y falsas intercambian su posición; solo un reflejo no imita al jugador y marca la salida verdadera.",
  config:["Velocidad de giro","Nº de paneles","Salida real fija o cambiante"] });

trap({ name:"Reflejo hostil", category:"Ilusión", dungeon:["mirror-castle","descent-madness"], nuevo:true,
  desc:"NUEVA. Al pasar frente a ciertos espejos, el reflejo del jugador sale del cristal y ataca con su mismo equipo durante unos segundos. Solo se le hace daño real cubriendo o rompiendo su espejo de origen.",
  config:["Duración del reflejo","Daño","Espejo vinculado"] });

trap({ name:"Ventisca cegadora", category:"Clima", dungeon:["veiled-peak"], nuevo:true,
  desc:"NUEVA. Ráfaga súbita de nieve que reduce la visión casi a cero durante unos segundos y empuja en una dirección. Los faros espirituales encendidos crean burbujas de visibilidad seguras.",
  config:["Intensidad","Duración","Dirección del empuje","Radio de faro"] });


trap({ name:"Placa de hielo deslizante", category:"Movimiento", dungeon:["veiled-peak","primordial-tower"], nuevo:true,
  desc:"NUEVA. Tramos de hielo que anulan la fricción: el jugador se desliza sin control hacia precipicios, pinchos o vacíos entre plataformas. Requiere frenar con objetos, muros o tempo.",
  config:["Longitud","Fricción","Destino del deslizamiento"] });

trap({ name:"Ráfaga de viento de precipicio", category:"Empuje", dungeon:["veiled-peak","primordial-tower"], nuevo:true,
  desc:"NUEVA. Corrientes rítmicas junto a los bordes que empujan hacia el vacío. Hay que cruzar leyendo el patrón del viento; la Capa Cortavientos reduce el empuje.",
  config:["Fuerza","Cadencia","Dirección"] });

trap({ name:"Suelo que desaparece", category:"Percepción", dungeon:["descent-madness"], nuevo:true,
  desc:"NUEVA. En el descenso, secciones de suelo se vuelven ilusorias sin previo aviso: parecen sólidas pero no lo son. La Vela de Cordura revela cuáles son reales dentro de su radio.",
  config:["Patrón","Aviso (sí/no)","Radio de revelado"] });

trap({ name:"Voces falsas y engaño sensorial", category:"Sonido", dungeon:["descent-madness"], nuevo:true,
  desc:"NUEVA. El sistema reproduce voces de auxilio o de compañeros desde direcciones falsas para atraer a trampas o emboscadas del Duelista Hueco. Integra el sistema de resonancia y ruido.",
  config:["Idioma (ES/EN)","Dirección falsa","Frecuencia"] });

trap({ name:"Columnas elementales", category:"Elemental", dungeon:["primordial-tower"], nuevo:true,
  desc:"NUEVA. Columnas que emergen del suelo cargadas del elemento del piso (llamas, hielo, viento, roca) siguiendo patrones. Chocar con la columna equivocada según tu afinidad castiga más.",
  config:["Elemento","Patrón","Altura","Daño por afinidad"] });

trap({ name:"Rayo solar concentrado", category:"Luz", dungeon:["solar-palace"], nuevo:true,
  desc:"NUEVA. Haz de luz solar redirigido por espejos que incinera todo lo que cruza su línea. Es a la vez peligro y herramienta: los mismos haces resuelven puzles y dañan a enemigos y sellos.",
  config:["Recorrido del haz","Daño","Espejos móviles","Interacción con puzles"] });


/* ---------- PUZLES ---------- */
CDE_DATA.puzzles = [];
function puz(o){ CDE_DATA.puzzles.push(o); }

puz({ name:"Espejos de trayectoria", dungeon:["mirror-castle","primordial-tower"],
  desc:"Redirigir un haz (luz o proyectil) rebotándolo en espejos orientables hasta alcanzar un sello. En el Palacio Solar se convierte en un puzle de luz mortal; en la Torre, en un puzle de rayo del génesis." });
puz({ name:"Puzle de peso y sustitución", dungeon:["worldbearer","hollow-leviathan","primordial-tower","solar-palace"],
  desc:"Igualar o sustituir el peso en balanzas y placas usando bloques empujables, objetos o contrapesos para abrir puertas, levantar jaulas o revelar pasadizos." });
puz({ name:"Secuencia de sonidos", dungeon:["hollow-leviathan","veiled-peak","descent-madness","primordial-tower"],
  desc:"Reproducir o identificar una secuencia sonora (latidos, campanas de niebla, voces reales frente a falsas, resonancia elemental) para desbloquear el paso." });
puz({ name:"Puzle biológico de órganos", dungeon:["hollow-leviathan"],
  desc:"Manipular órganos del leviatán en el orden correcto (bombear, drenar, sellar) para alterar los fluidos, el gas y las rutas sin desencadenar la respuesta inmune." });
puz({ name:"Pasillo de memoria", dungeon:["mirror-castle","descent-madness"],
  desc:"Recordar y reproducir un recorrido, una secuencia o una escena que se muestra brevemente y luego se oculta; fallar reinicia o castiga." });
puz({ name:"Cooperación asimétrica", dungeon:["worldbearer","veiled-peak","solar-palace"],
  desc:"Puzle multijugador donde cada jugador ve o controla algo distinto (uno sostiene un cabrestante o puente mientras otros cruzan o resuelven) y deben coordinarse." });


puz({ name:"Puzle de luz y espejos", dungeon:["solar-palace","mirror-castle"], nuevo:true,
  desc:"NUEVO. Girar espejos y abrir/cerrar compuertas de luz para llevar un haz solar hasta una cerradura sin incinerar a nadie ni bloquear la ruta de otro jugador." });
puz({ name:"Puzle de elementos (afinidad por piso)", dungeon:["primordial-tower"], nuevo:true,
  desc:"NUEVO. Activar altares con el elemento correcto del piso (y a veces combinando dos) para avanzar; llevar la afinidad equivocada castiga y cambiar el ciclo altera la solución." });
puz({ name:"Puzle de rutas falsas y repetición", dungeon:["descent-madness"], nuevo:true,
  desc:"NUEVO. En los pasillos que se repiten, la salida real se descubre observando qué detalle cambia en cada vuelta (una estatua movida, una antorcha apagada) e ignorando las voces." });
puz({ name:"Secuencia de antorchas y braseros", dungeon:["solar-palace"], nuevo:true,
  desc:"NUEVO. Encender antorchas y braseros activables en el orden correcto (a veces durante el Cénit o el Eclipse) para abrir puertas y desactivar defensas solares." });

/* ---------- MECANISMOS Y ACTIVADORES ---------- */
CDE_DATA.mechanisms = [];
function mech(o){ CDE_DATA.mechanisms.push(o); }

mech({ name:"Activador hidráulico", desc:"Mecanismo tipo palanca que se activa con agua (botellas o cubo). Fuente, cuenco o altar con estados visuales de llenado y cambio de color al activarse. Puede vaciarse con cubo o botellas.", config:["Capacidad","Cantidad necesaria","Color","Señal emitida"] });
mech({ name:"Pasadizo secreto", desc:"Pared normal que pierde opacidad y hitbox hasta permitir el paso. Se abre por golpes (nº exacto, zona, tiempo entre golpes), palanca, placa, activador hidráulico o combinación.", config:["Material","Tamaño","Nº de golpes","Método de cierre"] });
mech({ name:"Bloques empujables", desc:"Bloques sólidos y desplazables para puzles físicos. Se empujan mirándolos y manteniendo presión (más lento, con animación y sonido). Flechas a máxima potencia, martillos, ondas y explosiones también los mueven según fuerza y dirección.", config:["Peso/resistencia","Fuerza mínima","Direcciones válidas","Explosiones"] });
mech({ name:"Activador de balanza", desc:"Compara el peso de dos lados (gárgola, estatua o diosa con balanza). Igualarlo abre puertas o revela pasadizos; equivocarse activa castigos (oleadas, trampas, gas, cierre de puertas).", config:["Peso exacto/margen","Tipo de objeto","Castigo por fallo"] });


mech({ name:"Pulsador oculto configurable", desc:"Convierte casi cualquier bloque en un botón camuflado, distinguible solo por detalles mínimos (runa, grieta, saliente). Modos: momentáneo, interruptor, uso único, secuencia, pulsación múltiple o condicionado.", config:["Apariencia","Método","Modo","Nº de pulsaciones"] });
mech({ name:"Cabrestante manual de barras", desc:"Eje con barras que el jugador empuja caminando en círculo para transmitir energía mecánica. Puede almacenar carga para puzles cooperativos: sube ascensores, abre puertas pesadas, levanta jaulas o mantiene trampas desactivadas.", config:["Nº de barras","Vueltas necesarias","Energía por vuelta","Nº de jugadores"] });
mech({ name:"Ascensor manual por cadenas", desc:"Plataforma elevadora pesada (base romboidal) accionada con cadenas, poleas y cabrestante. Sube, baja y se detiene en niveles; puede atascarse, retroceder o romperse. Depende del nº de jugadores y del peso.", config:["Tamaño","Altura máxima","Nº de paradas","Nº de jugadores"] });
mech({ name:"Antorcha o brasero activable", desc:"Antorcha o brasero que, al encenderse con un mechero, emite una señal: abre puertas, desactiva trampas, activa eventos o forma secuencias. Coexisten versiones puramente decorativas para que el jugador no sepa siempre cuáles son funcionales.", config:["Tipo","Color de llama","Secuencia","Reinicio"] });
mech({ name:"Sistema de Resonancia de la Mazmorra", desc:"Cristales, vetas y placas que absorben sonido y vibraciones (pasos, golpes, flechas, explosiones, derrumbes) y se cargan en 4 estados (dormido→sobrecargado). Al sobrecargarse pueden explotar, alertar mobs, abrir puertas secretas o cambiar conexiones. Producen Fragmentos resonantes.", config:["Sensibilidad","Capacidad","Propagación por vetas","Recompensas"] });


/* ---------- NPCs Y CAMPAMENTO ---------- */
CDE_DATA.camp = {
  title: "Campamento de aventureros",
  desc: "La principal estructura del mod fuera de las mazmorras y el punto de entrada natural al contenido. Menor que una dungeon colosal, pero detallada como centro de operaciones: tiendas, hogueras, mesas con mapas, almacenes, zona de entrenamiento y restos de expediciones.",
  functions: [
    "Descubrir la existencia de las siete mazmorras y obtener mapas, fragmentos y coordenadas.",
    "Hablar con aventureros, recibir encargos y misiones sencillas.",
    "Entregar y recibir objetos; comprar o intercambiar recursos.",
    "Conocer rumores y secretos; prepararse antes de entrar.",
    "Reclutar a ciertos aventureros como compañeros."
  ],
  companions: "Compañeros reclutables con sistema propio (adaptado a NeoForge 1.21.1): seguimiento, órdenes básicas, combate, equipamiento, inventario, estados de espera/seguimiento, reacción a trampas, diálogos y condiciones de reclutamiento. Migamigos se usa solo como inspiración, no como dependencia.",
  dialogue: "Sistema propio de NPCs y diálogos: árboles con varias respuestas, ramas, condiciones y consecuencias; memoria por jugador (promesas, mentiras, objetos entregados, reputación); definido por datos (JSON) con claves de idioma para traducción completa."
};

CDE_DATA.valdris = {
  name: "Valdris el Eterno",
  role: "Comerciante especial con IA, memoria y contratos invisibles",
  desc: "Un cráneo flotante rodeado de manos esqueléticas y gemas que cambian de color según su estado. No es un vendedor normal: su tienda es una mecánica completa de observación, riesgo y engaño. Todos los precios, tiempos y probabilidades son provisionales.",
  states: [
    { name:"Mirando al jugador", detail:"8–12 s · gemas azul frío · no se puede robar." },
    { name:"Girando la cabeza", detail:"2–3 s · gemas blancas · ventana de robo de 1,5 s." },
    { name:"Cogiendo algo detrás", detail:"4–5 s cada 45–60 s · da la espalda · ventana larga de robo." },
    { name:"Durmiendo", detail:"tras 90 s sin interacción · 6–8 s · la ventana de robo más larga." },
    { name:"Focus Total", detail:"permanente si te descubre robando · gemas rojas · rotación 360° · sin más ventanas para ese jugador." }
  ],
  contracts: "Los objetos llevan un contrato invisible que nunca se muestra: el Ojo del Abismo hace que los no-muertos te ignoren pero las trampas te dañan el doble; el Mapa del Piso Siguiente marca como vacía la sala del enemigo más fuerte; la Espada Hambreada drena vida si no matas; etc. Se eliminan con el futuro 'Rompecontratos', que desbloquea los objetos sin trampa (Llave Maestra, Poción de Plenitud).",
  theft: "Robar con clic derecho durante una ventana válida obtiene el objeto sin contrato. Si te descubre, el objeto queda encadenado al inventario con debuffs hasta pagar tres veces su precio, y entra en Focus Total contigo.",
  kill: "Tiene ~1000 HP; cada golpe activa Empuje de Tinieblas (onda que empuja 4–5 bloques). Si muere, revive a los 5 s y la tienda permanece —con un descuento 'por curiosidad científica'."
};
