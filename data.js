/* ============================================================
   Colossal Dungeons Enhanced - Documento de diseno (datos)
   Minecraft 1.21.1 - NeoForge
   Todo el contenido vive en window.CDE_DATA
   ============================================================ */
window.CDE_DATA = {};


/* ---------- META / PROYECTO ---------- */
CDE_DATA.meta = {
  title: "Colossal Dungeons Enhanced",
  tagline: "Siete mazmorras colosales. Un solo intento por mundo. Ningun atajo.",
  platform: "Minecraft 1.21.1 - NeoForge",
  status: "Preproduccion",
  intro: "Un mod centrado casi por completo en siete mazmorras colosales hechas con calidad de juego completo. Cada dungeon tiene identidad visual, reglas propias, estructuras, mobs, jefes, trampas, puzles, eventos, loot y mecanicas unicas. El mundo exterior no se llena de mobs: su punto de entrada es un campamento de aventureros desde el que se descubren las dungeons mediante mapas, rumores, misiones y NPCs reclutables. Todas las mecanicas integran items vanilla de Minecraft de forma natural: cubos de agua, polvo de hueso, bolas de nieve, huevos, perlas de ender, cizallas, escudos, ballestas, canas de pescar, catalejos y mas son herramientas utiles dentro de las mazmorras.",
  principles: [
    "Una sola copia de cada dungeon por mundo.",
    "Las siete tienen dificultad alta y aproximadamente equivalente.",
    "Se pueden intentar en cualquier orden.",
    "No hay escalado artificial segun el equipo del jugador.",
    "El equipo ayuda, pero no sustituye la habilidad.",
    "La mision principal de cada dungeon es derrotar a su jefe.",
    "Las misiones del campamento son secundarias y opcionales.",
    "Las dungeons pueden restaurarse para ser rejugables.",
    "El soporte para addons es una parte central, no un extra.",
    "El rendimiento se disena desde el framework, no al final.",
    "Cada mecanica debe integrar al menos 2 items vanilla de Minecraft como contramedida o herramienta.",
    "El mod debe sentirse como una extension natural de Minecraft, no como un juego separado."
  ]
};

/* ---------- ESTADISTICAS DE PANEL ---------- */
CDE_DATA.stats = [
  { label: "Dungeons disenadas", value: "7", sub: "de 7 objetivo" },
  { label: "Criaturas unicas", value: "75+", sub: "hasta 95 con variantes" },
  { label: "Jefes principales", value: "7", sub: "uno por dungeon" },
  { label: "Semijefes", value: "9", sub: "uno o dos por dungeon" },
  { label: "Familias de trampas", value: "28", sub: "configurables, con contramedidas vanilla" },
  { label: "Puzles base", value: "14", sub: "ampliables por addons" },
  { label: "Items vanilla integrados", value: "25+", sub: "cada uno con multiples usos" },
  { label: "Ejemplos de codigo", value: "13", sub: "Java/NeoForge 1.21.1 compilables" },
  { label: "Paquetes Java", value: "18+", sub: "arquitectura modular completa" },
  { label: "Sistemas del framework", value: "12", sub: "data-driven y extensibles" }
];


/* ---------- DEPENDENCIAS ---------- */
CDE_DATA.dependencies = [
  {
    name: "AAA Particles",
    role: "Motor de efectos visuales (VFX)",
    provisional: false,
    highlight: true,
    desc: "Libreria de efectos que integra el sistema Effekseer (archivos .efkefc) dentro de Minecraft. Reemplaza a Photon como motor de VFX del mod. Expone API para desarrolladores: emisores de particulas ligados a entidades, orientacion del emisor a partir de un vector de direccion, metadatos de finalizacion cuando un efecto ligado a una entidad desaparece, y registro/precarga de efectos.",
    version: "Latest para NeoForge 1.21.1",
    apiSurface: "EffekseerEffect.create(), EffectAPI.spawn(), BoneAttachment para GeckoLib sync",
    uses: [
      "Efectos visuales avanzados de armas, mobs y jefes.",
      "Auras, rastros de ataque, explosiones y ondas de choque.",
      "Portales, polvo, derrumbamientos, gas y niebla.",
      "Fragmentos de memoria, efectos psiquicos y transformaciones.",
      "Efectos ambientales por dungeon (reflejos, brasas solares, esporas organicas).",
      "Feedback visual de interacciones vanilla (vapor al usar cubo de agua en fuego, particulas de hueso al limpiar aceite)."
    ],
    note: "Los efectos .efkefc se empaquetan como recursos del mod y se instancian mediante la API de AAA Particles, ligandolos a huesos de GeckoLib cuando se necesita sincronia con animaciones."
  },
  {
    name: "GeckoLib",
    role: "Modelos y animaciones",
    provisional: false,
    version: "4.x para NeoForge 1.21.1",
    apiSurface: "GeoEntity, GeoBlockEntity, AnimationController, AnimatableInstanceCache, KeyframeAnimationEvents",
    desc: "Motor de animacion y renderizado 3D para entidades, bloques, objetos y armaduras, con animaciones por keyframes, keyframes de sonido y particulas, y eventos sincronizados.",
    uses: ["Animaciones de mobs, jefes, trampas y mecanismos.", "Bloques y objetos animados.", "Eventos sincronizados con animaciones.", "Reacciones visuales a items vanilla (animacion de limpieza con polvo de hueso, salpicadura de agua)."]
  },
  {
    name: "SmartBrainLib",
    role: "Inteligencia artificial avanzada",
    provisional: true,
    version: "Para NeoForge 1.21.1",
    apiSurface: "ExtendedSensor, BrainActivityGroup, SmartBrainOwner, CustomDelayedBehaviour",
    desc: "Se usa al principio para acelerar la creacion de IA compleja: sensores, memorias, percepcion, navegacion, seleccion de ataques, estados y coordinacion. Con el tiempo, sus comportamientos podran migrarse al framework modular interno.",
    uses: ["Comportamientos de mobs, busqueda de objetivos y deteccion por sonido.", "Cambio de fases y seleccion de ataques.", "Elites, semijefes y jefes.", "Reacciones de IA a items vanilla (huir del fuego, investigar sonidos de bolas de nieve, responder a huevos como cebo)."]
  },
  {
    name: "Curios API",
    role: "Accesorios equipables",
    provisional: false,
    version: "Para NeoForge 1.21.1",
    apiSurface: "ICurioItem, SlotTypePreset, CuriosApi.getSlot(), CurioEquipEvent",
    desc: "Sistema oficial de accesorios del mod en NeoForge 1.21.1.",
    uses: ["Ranuras de accesorios: amuletos, anillos, collares, cinturones y reliquias.", "Objetos equipables con efectos especiales.", "Reliquias de dungeon con efectos pasivos."]
  },
  {
    name: "FDLib",
    role: "Cinematicas y presentacion",
    provisional: true,
    version: "Para NeoForge 1.21.1",
    apiSurface: "CameraController, BossBarRenderer, ImpactFrame, CinematicSequence",
    desc: "Se mantiene mientras acelere el desarrollo. En el futuro podra sustituirse por sistemas propios de camaras, cinematicas, bossbars e impact frames.",
    uses: ["Camaras controladas y presentaciones de jefes.", "Bossbars personalizadas e impact frames.", "Escenas especiales de NPCs y transformaciones."]
  },
  {
    name: "More Hitboxes",
    role: "Entidades multiparte",
    provisional: true,
    version: "Para NeoForge 1.21.1",
    apiSurface: "MultipartEntity, HitboxPart, DamageSource routing, BoneSync",
    desc: "Primera dependencia prevista para sustituir por un sistema multiparte propio (hitboxes vinculadas a huesos, dano localizado, partes rompibles/desprendibles, API publica para addons).",
    uses: ["Criaturas grandes y jefes con partes independientes.", "Trampas animadas, estatuas y pilares.", "Sincronizacion de hitboxes con animaciones."]
  }
];


/* ---------- DUNGEONS ---------- */
CDE_DATA.dungeons = [];

CDE_DATA.dungeons.push({
  id: "mirror-castle",
  name: "The Mirror Castle",
  subtitle: "El castillo de los reflejos",
  color: "#8fb7c9",
  themeWord: "Reflejo - Ilusion - Percepcion",
  identity: "Un castillo senorial de plata, azogue y cristal donde cada superficie miente. Salones de espejos, ventanales pulidos y suelos de mercurio convierten la orientacion en un acto de fe. La dungeon se juega leyendo el entorno: distinguir lo real de lo reflejado, y a uno mismo de sus copias.",
  rule: "Regla exclusiva: Ley del Reflejo. En las salas marcadas, atacar de frente a una imagen especular devuelve el dano al jugador. Muchos enemigos solo son vulnerables si se les golpea a traves de su reflejo real, no de su cuerpo aparente. Romper la superficie especular correcta debilita o anula a los enemigos vinculados a ella.",
  objective: "Alcanzar el Salon del Trono Invertido y destruir al Soberano Anicos rompiendo, una a una, las superficies que sostienen su reflejo.",
  bossId: "shattered-sovereign",
  vanillaItemsKey: ["Escudo (refleja ataques de luz del Soberano)", "Catalejo (distingue reflejos reales de falsos a distancia)", "Bolas de nieve (rompen espejos fragiles desde lejos)", "Antorchas (revelan paneles de espejo falsos por la sombra que proyectan)", "Cubo de agua (empana espejos temporalmente impidiendo que generen reflejos hostiles)", "Perla de ender (teletransporte al otro lado de paredes de cristal selladas)", "Ballesta (dispara a marcos de espejo distantes para romperlos)", "Cana de pescar (engancha y gira espejos orientables a distancia)"],
  sectors: [
    { name: "Galeria de Bienvenida", desc: "Pasillos duplicados por espejos; ensena la Ley del Reflejo con enemigos inofensivos y puertas falsas. Las bolas de nieve rompen los cristales de prueba." },
    { name: "Salon de Azogue", desc: "Suelos de mercurio que reflejan el techo; el jugador debe caminar por las vigas reales, no por su reflejo. El catalejo revela cuales son solidas." },
    { name: "Laberinto de Marcos", desc: "Marcos de espejo que se reorganizan cuando nadie mira. La brujula senala el centro real del laberinto." },
    { name: "Camara de los Gemelos", desc: "Arena de los semijefes Los Gemelos Rotos. Los escudos reflejan sus ataques de vuelta." },
    { name: "Salon del Trono Invertido", desc: "Arena final, un salon simetrico donde arriba y abajo se confunden. El cubo de agua empana las superficies revelando la orientacion real." }
  ],
  puzzles: ["Espejos de trayectoria (usar cana de pescar para girar espejos lejanos)", "Pasillo de memoria (el catalejo revela pistas ocultas)", "Puzle de luz y espejos (colocar antorchas en posiciones clave para crear sombras-guia)"],
  traps: ["Sala de espejos giratorios (bolas de nieve rompen paneles)", "Reflejo hostil (cubo de agua empana el espejo origen)", "Pasillo infinito de la lampara magica (apagar con flecha de agua o bola de nieve)", "Bola rodante colosal esfera reflectante (esquivar; escudo bloquea parcialmente)"],
  loot: ["Fragmentos de azogue (material de crafteo).", "Espejo de bolsillo: revela una ruta real durante unos segundos.", "Armadura de Plata Pulida: refleja una parte del dano a distancia."],
  weapons: ["Estoque de la Locura (posible, si el Duelista Hueco se ubica aqui).", "Filo Espejado: al parar un golpe, proyecta un reflejo cortante."],
  events: ["Apagon especular: todas las luces se invierten y las salas reales e ilusorias intercambian su iluminacion. Las antorchas colocadas por el jugador se mantienen y marcan las salas reales.", "El Cortejo: una fila de reflejos del jugador desfila y uno de ellos es real y hostil. El catalejo identifica al real por un detalle."],
  secrets: ["Tras un espejo agrietado concreto hay una trastienda con loot de plata. Se revela lanzando una bola de nieve al cristal correcto.", "Un reflejo que no imita al jugador senala un pasadizo. Usar el catalejo desde lejos lo marca."],
  entrance: "Un porton de plata que solo refleja a quien no lleva casco; entrar empana todos los espejos durante unos segundos.",
  exit: "Salida normal disponible tras el jefe; existe un espejo-portal de retirada de un solo uso.",
  reset: "Reinicio por sectores: las superficies rotas se regeneran y los marcos se reorganizan.",
  multiplayer: "Los reflejos pueden separar al grupo en salas espejo distintas hasta que resuelven un puzle comun.",
  performance: "Los reflejos son planos renderizados con recorte de portal, no camaras duplicadas; solo la sala activa refleja en tiempo real."
});


CDE_DATA.dungeons.push({
  id: "worldbearer",
  name: "The Worldbearer",
  subtitle: "El portador del mundo",
  color: "#c8a96a",
  themeWord: "Escala colosal - Peso - Gravedad",
  identity: "La dungeon esta construida sobre, dentro y alrededor de un titan petreo del tamano de una montana que sostiene una porcion del mundo sobre sus hombros. Se explora ascendiendo por su columna, sus costillas huecas y las plataformas que penden de sus cadenas. Todo aqui pesa: la piedra, los contrapesos, las decisiones.",
  rule: "Regla exclusiva: Ley del Peso. Puertas, ascensores y puentes funcionan por contrapeso real. Para avanzar hay que mover masa (bloques empujables, contrapesos, el propio Martillo de Contrapeso). Retirar peso de un soporte puede abrir una ruta y derrumbar otra a la vez.",
  objective: "Escalar hasta el corazon del titan y liberar/derrotar al Atlas Moribundo, la voluntad que lo mantiene erguido, sin quedar aplastado por lo que se derrumba.",
  bossId: "dying-atlas",
  vanillaItemsKey: ["TNT (destruye muros debiles marcados revelando secretos y atajos)", "Bloques de piedra/tierra (colocar en placas de peso como sustitutos de contrapeso)", "Leads/riendas (conectar a bloques empujables para moverlos a distancia o entre jugadores)", "Pistones de redstone (interactuan con bloques del mod para crear contrapesos temporales)", "Cubo de agua (lubrica rieles de bloques empujables para moverlos mas rapido)", "Cana de pescar (engancha contrapesos colgantes y los acerca)", "Escudo (protege de escombros durante derrumbes)", "Perla de ender (salvar caidas al vacio con teletransporte de emergencia)"],
  sectors: [
    { name: "Los Talones", desc: "Base del titan; cabrestantes manuales y ascensores por cadenas dan acceso al interior. Las riendas pueden conectarse a las barras del cabrestante para que un jugador tire desde lejos." },
    { name: "La Columna", desc: "Ascenso vertical por vertebras huecas; plataformas basculantes segun el peso del grupo. Colocar bloques en un lado equilibra temporalmente." },
    { name: "La Caja Toracica", desc: "Salas amplias entre costillas; los Atlantes Menores sostienen techos que caen al morir. TNT puede derribar costillas debiles para crear atajos." },
    { name: "Los Hombros", desc: "Exterior azotado por el viento, con la carga del mundo suspendida arriba. El escudo reduce el empuje del viento." },
    { name: "El Corazon de Piedra", desc: "Arena del Atlas Moribundo. Las perlas de ender permiten escapar de zonas que se derrumban." }
  ],
  puzzles: ["Puzle de peso y sustitucion (colocar bloques vanilla en placas)", "Puzle de bloques empujables (usar riendas y cana de pescar)", "Cooperacion asimetrica (cabrestantes con riendas a distancia)"],
  traps: ["Paredes aplastantes (cubo de agua lubrica para deslizarse y escapar)", "Bola rodante colosal (cana de pescar engancha el mecanismo de liberacion)", "Trampa de derrumbamiento (escudo protege de escombros)", "Placa basculante colosal (equilibrar con bloques vanilla)", "Cadena tensada (escudo bloquea el golpe de barrido)"],
  loot: ["Nucleo de contrapeso: material pesado para forja.", "Botas de Lastre: inmunidad al empuje a cambio de velocidad.", "Cincel del Atlas: rompe muros debiles marcados."],
  weapons: ["Martillo de Contrapeso: cuanto mas se carga, mas dano, empuje e interaccion con mecanismos."],
  events: ["El Titan se mueve: toda una seccion se inclina y hay que reubicar el peso antes del derrumbe. Colocar bloques rapidamente en las placas correctas estabiliza.", "Lluvia de escombros: fragmentos del mundo suspendido caen por rutas concretas. El escudo protege de impactos directos."],
  secrets: ["Una vertebra floja esconde un atajo vertical si se derriba con TNT.", "Dentro de una cadena hueca hay una camara de expedicionarios perdidos. Se accede enganchando la cana de pescar al cerrojo interior."],
  entrance: "Una gran compuerta que solo se abre bajando un ascensor manual con suficiente peso encima. Bloques de piedra o tierra del inventario cuentan como peso.",
  exit: "Ascensor rapido de descenso tras el jefe; antes, la unica salida es volver a bajar por la columna.",
  reset: "Los contrapesos vuelven a su posicion y los derrumbes se reconstruyen por sectores.",
  multiplayer: "Muchos mecanismos requieren varios jugadores empujando cabrestantes o repartiendo peso simultaneamente.",
  performance: "El titan es estructura estatica por plantillas; solo las secciones moviles cercanas simulan fisica."
});


CDE_DATA.dungeons.push({
  id: "hollow-leviathan",
  name: "The Hollow Leviathan",
  subtitle: "El leviatan hueco",
  color: "#9d6b8f",
  themeWord: "Biologia - Organos - Defensas vivas",
  identity: "Una mazmorra organica dentro de un leviatan colosal muerto, o casi muerto. Conductos, organos, tejidos, fluidos y defensas vivas. Las paredes respiran, los suelos digieren y las puertas muerden. El cuerpo reacciona a la intrusion como un organismo a una infeccion.",
  rule: "Regla exclusiva: Ley de la Infeccion. Cada organo danado altera toda la dungeon (destruir un pulmon despeja el gas de una zona; reventar una glandula inunda otra de acido). El jugador es tratado como patogeno: hacer ruido o dano acelera la respuesta inmune (oleadas de anticuerpos).",
  objective: "Alcanzar la cavidad central y destruir al Parasito Regente, el organismo que reanima al leviatan, decidiendo que organos sacrificar por el camino.",
  bossId: "sovereign-parasite",
  vanillaItemsKey: ["Cizallas/shears (cortan membranas, telaranas organicas y capullos antes de que eclosionen)", "Polvo de hueso (hace crecer barreras organicas para bloquear conductos de acido o gas; tambien limpia superficies aceitosas internas)", "Cubo de agua (diluye acido, limpia suelos digestivos temporalmente, ahoga larvas)", "Antorchas (cauterizan heridas del leviatan sellando conductos, ahuyentan parasitos)", "Botes de miel/honey bottles (inmunidad al deslizamiento en superficies mucosas, pacifica insectos)", "Huevos (atraen a larvas devoradoras como cebo alejandolas del jugador, alimentan al Neothelid bebe)", "Leche (limpia todos los debuffs de veneno, acido y digestion)", "Pedernal y acero/flint and steel (cauteriza tejido vivo cerrando bocas de pared permanentemente)"],
  sectors: [
    { name: "Las Fauces", desc: "Entrada por la boca; primeras trampas vivas y suelos digestivos. El cubo de agua neutraliza el acido digestivo temporalmente." },
    { name: "El Tracto", desc: "Conductos estrechos con fluidos, esfinteres-puerta y capullos que liberan mobs. Las cizallas cortan los capullos antes de la eclosion." },
    { name: "La Camara Pulmonar", desc: "Grandes sacos que se inflan y desinflan; el nivel de gas depende de si respiran. El polvo de hueso hace crecer una barrera de musgo que filtra el gas." },
    { name: "El Nido de Nervios", desc: "Arena del semijefe El Sistema Nervioso; paredes de ojos organicos. Las antorchas ciegan los ojos organicos cercanos." },
    { name: "La Cavidad del Corazon", desc: "Arena del Parasito Regente. Las cizallas cortan sus raices de forma mas eficiente que las armas." }
  ],
  puzzles: ["Puzle biologico de organos (usar polvo de hueso para estimular crecimiento, agua para diluir, fuego para cauterizar)", "Secuencia de sonidos/latidos (note blocks del jugador reproducen la secuencia)", "Puzle de peso y sustitucion en esfinteres (huevos como cebo de peso ligero)"],
  traps: ["Trampas vivas (cizallas cortan tentaculos, fuego cauteriza)", "Puerta devoradora (pedernal y acero cauteriza la boca cerrando permanentemente)", "Suelos digestivos (cubo de agua neutraliza, honey bottles dan inmunidad)", "Paredes que respiran (polvo de hueso bloquea conductos con musgo)", "Capullos que liberan mobs (cizallas previenen eclosion)"],
  loot: ["Muestras organicas: ingredientes alquimicos.", "Membrana Resiliente: armadura que absorbe veneno.", "Glandula de bilis: arma arrojadiza corrosiva."],
  weapons: ["Bisturi del Cirujano: hace dano extra a partes y organos, poco a armadura."],
  events: ["Respuesta inmune: oleada masiva de anticuerpos tras hacer demasiado ruido. Las bolas de nieve distraen a los anticuerpos creando sonido lejos del jugador.", "Espasmo: todo el sector se contrae, cerrando y abriendo conductos. Colocar bloques solidos evita que ciertos conductos se cierren del todo."],
  secrets: ["Una vena secundaria lleva a una perla del leviatan (loot raro). Se accede cortando una membrana con cizallas.", "Un organo vestigial esconde a un Vigia Cosido dormido y su recompensa. Alimentar al organo con huevos lo activa revelando el pasadizo."],
  entrance: "La boca del leviatan, que traga al jugador; al entrar, un esfinter-puerta se cierra detras.",
  exit: "Reventar el corazon abre una brecha en el costado como salida; antes, hay que retroceder por el tracto.",
  reset: "Los organos regeneran tejido y los fluidos se rellenan; los capullos vuelven a poblarse.",
  multiplayer: "El sistema de fluidos y gas afecta a todo el grupo; conviene coordinar que organos tocar.",
  performance: "Los fluidos son volumenes logicos por sala, no fisica de liquidos; la respiracion es animacion de bloques."
});


CDE_DATA.dungeons.push({
  id: "veiled-peak",
  name: "The Veiled Peak",
  subtitle: "El pico velado",
  color: "#a9c4d6",
  themeWord: "Verticalidad - Clima - Espiritus",
  identity: "Un pico montanoso oculto tras un velo magico de niebla, ventisca y espiritus. Se asciende por cornisas, puentes de cuerda y templos colgantes mientras el clima cambia y la visibilidad desaparece. La montana juzga a quien sube: solo los que respetan sus reglas llegan a la cima.",
  rule: "Regla exclusiva: Ley del Velo. La niebla oculta el camino real y muestra rutas falsas. Encender los faros espirituales en orden disipa el velo por tramos. El viento empuja hacia los precipicios: moverse contra rafagas mal calculadas es mortal.",
  objective: "Ascender a la cima disipando el velo y enfrentar a la Tormenta Coronada, el espiritu que protege el pico.",
  bossId: "crowned-tempest",
  vanillaItemsKey: ["Botes de miel/honey bottles (inmunidad al deslizamiento en placas de hielo)", "Cubo de polvo de nieve/powder snow bucket (crea plataformas temporales sobre abismos, ralentiza enemigos de fuego)", "Bolas de nieve (activan placas de presion a distancia y distraen criaturas de sonido)", "Escudo (reduce empuje del viento en un 70%)", "Pedernal y acero (enciende los faros espirituales, derrite barreras de hielo)", "Catalejo/spyglass (revela el camino real a traves de la niebla a distancia)", "Antorchas (crean zonas seguras donde la niebla no penetra, revelan Almas Veladas)", "Cubo de lava (derrite barricadas de hielo grueso que bloquean atajos)"],
  sectors: [
    { name: "El Sendero Bajo", desc: "Bosque de piedra con almas veladas; primeras rafagas de viento suaves. Las antorchas colocadas revelan las almas haciendolas vulnerables." },
    { name: "Los Puentes de Cuerda", desc: "Travesias sobre el vacio que oscilan con el viento; el escudo reduce el empuje durante el cruce." },
    { name: "El Templo de los Faros", desc: "Zona de puzles: encender faros espirituales con pedernal y acero en secuencia para abrir la subida." },
    { name: "La Cornisa del Oraculo", desc: "Arena del semijefe El Guardian de la Cima. El cubo de polvo de nieve crea plataformas de emergencia durante la pelea." },
    { name: "La Cima Coronada", desc: "Arena abierta de la Tormenta Coronada, azotada por el rayo. Los botes de miel evitan ser lanzado por el suelo helado." }
  ],
  puzzles: ["Secuencia de sonidos/campanas de niebla (note blocks reproducen la melodia)", "Puzle de luz/faros espirituales (encender con pedernal y acero en orden)", "Cooperacion asimetrica (un jugador sostiene puentes con escudo mientras otros cruzan)"],
  traps: ["Ventisca cegadora (antorchas colocadas crean burbujas de visibilidad)", "Placa de hielo deslizante (honey bottles dan inmunidad, powder snow frena)", "Rafaga de viento de precipicio (escudo reduce empuje)", "Trampa de derrumbamiento/aludes (cubo de agua congela el alud si se aplica a tiempo en el frio)", "Cuchillas y puas retractiles/estalactitas (catalejo revela el patron desde lejos)"],
  loot: ["Cristal de faro: fuente de luz portatil que disipa niebla.", "Capa Cortavientos: reduce el empuje del viento.", "Reliquia del Oraculo: revela la ruta segura brevemente."],
  weapons: ["Arco de las Ventiscas: sus flechas ignoran el desvio del viento y empujan."],
  events: ["Tormenta subita: el viento cambia de direccion y hay que reubicarse antes de ser lanzado. El escudo es crucial.", "Procesion de almas: espiritus guian, o enganan, hacia una ruta. El catalejo distingue las almas guia verdaderas (brillan mas)."],
  secrets: ["Una cornisa oculta tras la niebla lleva a un santuario con loot espiritual. El catalejo la revela desde el puente anterior.", "Un faro apagado adrede con cubo de agua abre una gruta bajo el hielo."],
  entrance: "Un totem en la base que solo deja pasar cuando se ofrenda una fuente de luz (antorcha o linterna).",
  exit: "Desde la cima, un descenso rapido por corriente de aire ascendente; antes, hay que rehacer la subida.",
  reset: "El velo vuelve a cubrir la montana y los faros se apagan; los puentes se rearman.",
  multiplayer: "Algunos puentes solo se mantienen si un jugador sostiene un mecanismo mientras otros cruzan.",
  performance: "La niebla es niebla volumetrica por sala con distancia de render adaptada; el viento es un campo de fuerzas por zona."
});


CDE_DATA.dungeons.push({
  id: "descent-madness",
  name: "The Descent into Madness",
  subtitle: "El descenso a la locura",
  color: "#b0605a",
  themeWord: "Terror psicologico - Percepcion - Sin retorno",
  identity: "Un descenso interminable a la oscuridad donde la realidad se deforma. Voces humanas piden ayuda, los pasillos se repiten, los recuerdos se corrompen y no siempre se puede confiar en lo que se ve o se oye. La dungeon no ataca solo al cuerpo: ataca a la certeza.",
  rule: "Regla exclusiva confirmada: Sin salida normal. Una vez dentro, solo se sale (1) completando el descenso, (2) muriendo o (3) usando un Cristal de Escape. Las puertas se cierran al entrar y en zonas protegidas no se pueden romper ni colocar bloques para huir.",
  objective: "Descender hasta el fondo y enfrentar a la Cordura Rota, aceptando que gran parte de lo que se percibe por el camino es mentira.",
  bossId: "broken-sanity",
  vanillaItemsKey: ["Leche/milk bucket (limpia TODOS los debuffs de locura, nausea y ceguera de una vez)", "Antorchas/linternas (previenen spawns en salas limpiadas, revelan Susurrantes invisibles)", "Brujula de recuperacion/recovery compass (senala la salida real en los pasillos que se repiten)", "Catalejo (identifica los recuerdos falsos por detalles incorrectos a distancia)", "Bolas de nieve (activan trampas desde la seguridad, prueban si un suelo es real)", "Note blocks (reproducen las secuencias de sonido correctas para distinguir voces reales)", "Comida/pan (pacifica temporalmente a Peregrinos sin Rostro si se les ofrece antes de que ataquen)", "Componentes de redstone (sculk sensors detectan movimiento de enemigos invisibles a traves de paredes)"],
  sectors: [
    { name: "El Umbral", desc: "La advertencia y el punto sin retorno; las puertas sellan detras. Una ultima oportunidad de usar la leche para limpiar debuffs previos." },
    { name: "Los Pasillos que Repiten", desc: "Corredores en bucle con la lampara magica y voces de auxilio. La brujula de recuperacion senala la direccion de la salida real." },
    { name: "La Galeria de Recuerdos", desc: "Salas que recrean escenas falsas de aventureros muertos. El catalejo revela inconsistencias en los recuerdos." },
    { name: "El Coro", desc: "Arena del semijefe Semblante, entre voces superpuestas. Los note blocks permiten crear contra-frecuencias que lo aturden." },
    { name: "El Fondo", desc: "Arena de la Cordura Rota. La leche es vital para limpiar las distorsiones de percepcion entre fases." }
  ],
  puzzles: ["Pasillo de memoria (catalejo revela detalles clave, brujula orienta)", "Secuencia de sonidos/voces reales vs falsas (note blocks reproducen la secuencia correcta)", "Puzle de rutas falsas y repeticion (brujula de recuperacion marca el avance real, bolas de nieve prueban suelos)"],
  traps: ["Pasillo infinito de la lampara magica (bolas de nieve rompen la lampara a distancia)", "Trampas vivas/variantes perturbadoras (antorchas las hacen visibles)", "Suelo que desaparece (bolas de nieve prueban solidez desde distancia segura)", "Voces falsas y engano sensorial (sculk sensors propios detectan la fuente real)", "Bola rodante colosal/esfera de carne y ojos (perla de ender para esquivar de emergencia)"],
  loot: ["Cristal de Escape: unica salida de emergencia; drop raro y limitado.", "Vela de Cordura: revela que es real dentro de su radio.", "Diario reconstruido: pistas y lore."],
  weapons: ["Estoque de la Locura: irrompible, ligerisimo, con Punzada rapida y Rafaga de estocadas."],
  events: ["Falso rescate: una voz conocida guia a una emboscada del Duelista Hueco. El sculk sensor propio delata movimiento real en otra direccion.", "Colapso de realidad: la sala se reordena y las salidas cambian de sitio. La brujula de recuperacion siempre apunta a la verdadera."],
  secrets: ["Ignorar todas las voces en un tramo abre la ruta real. Colocar antorchas marca el progreso verificado.", "Un recuerdo completado sin errores revela un alijo lucido. El catalejo muestra el detalle correcto."],
  entrance: "Un umbral con la advertencia grabada; no hay confirmacion: se puede entrar por accidente y quedar sellado.",
  exit: "Solo completar, morir o Cristal de Escape. No hay retirada convencional.",
  reset: "Restauracion total por sectores al vaciarse de jugadores; el sellado se rearma.",
  multiplayer: "Los bucles pueden separar al grupo en copias distintas; las voces pueden imitar a companeros reales.",
  performance: "Los bucles reutilizan pocos segmentos con teletransporte imperceptible; las voces son audios ligeros pre-generados."
});


CDE_DATA.dungeons.push({
  id: "primordial-tower",
  name: "The Primordial Tower",
  subtitle: "La torre primordial",
  color: "#6ea27a",
  themeWord: "Verticalidad - Elementos - Conocimiento antiguo",
  identity: "Una torre ancestral cuyos pisos representan las fuerzas primordiales: fuego, agua, aire y tierra, coronadas por el rayo del genesis. Cada nivel impone su elemento y reescribe como se mueve, se lucha y se sobrevive. Subir es aprender el lenguaje del mundo antes de que existieran las reglas.",
  rule: "Regla exclusiva: Ley de los Ciclos. Cada piso esta regido por un elemento que altera la fisica local (el fuego prende, el agua arrastra, el aire empuja, la tierra atrapa). Llevar la afinidad equivocada castiga; usar el elemento del piso contra sus guardianes es la clave.",
  objective: "Ascender por los cuatro pisos elementales y derrotar al Concilio Primordial en la cuspide, dominando un elemento por fase.",
  bossId: "primordial-concord",
  vanillaItemsKey: ["Cubo de agua (esencial en piso de fuego: apaga aceite, apaga jugador, dano a elementales de fuego)", "Pedernal y acero (esencial en piso de tierra: enciende braseros de puzle, prende aceite ofensivamente contra enemigos)", "Bolas de nieve (piso de fuego: enfria superficies calientes; piso de aire: contrarresta corrientes leves)", "Cubo de polvo de nieve (crea plataformas en piso de aire, congela agua en piso de agua)", "Polvo de hueso (limpia aceite en piso de fuego, hace crecer barreras organicas en piso de tierra)", "TNT (resuelve puzles de peso explosivamente en piso de tierra, destruye barreras elementales debilitadas)", "Pociones splash de resistencia al fuego (inmunidad temporal en piso de fuego)", "Cubo de lava (arma ofensiva contra elementales de agua y hielo en piso de agua)"],
  sectors: [
    { name: "Piso de la Tierra", desc: "Suelos que atrapan, columnas que crecen; puzles de peso y bloques. TNT rompe formaciones debiles, polvo de hueso estimula crecimiento de plataformas organicas." },
    { name: "Piso del Agua", desc: "Corrientes que arrastran y niveles que suben y bajan. El cubo de polvo de nieve congela secciones creando puentes temporales." },
    { name: "Piso del Aire", desc: "Plataformas flotantes y rafagas; verticalidad extrema. El cubo de polvo de nieve crea plataformas de emergencia; las perlas de ender salvan caidas." },
    { name: "Piso del Fuego", desc: "Lava alterna, braseros y aceite; el calor es constante. El cubo de agua y polvo de hueso son las herramientas clave; pociones de resistencia al fuego dan ventanas seguras." },
    { name: "La Cuspide del Genesis", desc: "Arena del Concilio Primordial, banada por el rayo. Cada fase requiere el item vanilla del elemento correspondiente." }
  ],
  puzzles: ["Puzle de elementos/afinidad por piso (items vanilla son la clave de cada piso)", "Puzle de peso y sustitucion (TNT como solucion explosiva alternativa)", "Secuencia de sonidos/resonancia elemental (note blocks reproducen la frecuencia)", "Espejos de trayectoria/rayo (escudo refleja el rayo del genesis hacia el sello)"],
  traps: ["Columnas elementales (item contrario las desactiva: agua vs fuego, nieve vs aire)", "Suelo de lava/hielo alterno (cubo de agua o polvo de nieve segun fase)", "Estatua o pilar lanzallamas (cubo de agua apaga, escudo bloquea)", "Bola rodante colosal/nucleo elemental (item elemental opuesto la destruye)", "Rafaga de viento de precipicio (escudo + bolas de nieve contrarrestan)"],
  loot: ["Esencias elementales: reactivos de crafteo.", "Anillo de Afinidad: inmunidad parcial a un elemento a elegir.", "Tablilla del Genesis: lore y receta."],
  weapons: ["Cetro de los Ciclos: cambia de elemento segun el piso y potencia el dano afin."],
  events: ["Cambio de ciclo: el elemento de un piso muta temporalmente al siguiente del ciclo. Hay que cambiar el item vanilla equipado rapidamente.", "Marea primordial: el agua sube varios pisos y hay que ascender rapido. El cubo de polvo de nieve congela secciones creando escalones."],
  secrets: ["Combinar dos elementos en un altar abre una camara oculta. Se combina usando dos items vanilla opuestos simultaneamente.", "Un piso apagado contiene la reliquia si se reactiva con el item vanilla correcto (pedernal en tierra, cubo en fuego)."],
  entrance: "Una puerta sellada por los cuatro sellos elementales; basta con activar el primero con el item vanilla correcto para entrar.",
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
  themeWord: "Sol - Fuego - Luz - Calor",
  identity: "Un palacio majestuoso de oro, bronce y marmol consagrado al sol. Patios abrasadores, salones de espejos solares y braseros ceremoniales. La luz aqui no es decoracion: es arma, llave y castigo. Un lugar deslumbrante donde el calor y el aceite convierten cualquier chispa en catastrofe.",
  rule: "Regla exclusiva: Ley de la Luz. La mayoria de puertas, ascensores y defensas se alimentan de luz solar redirigida con espejos. La oscuridad las apaga; la luz mal dirigida incinera. Muchos enemigos se fortalecen bajo la luz directa y son vulnerables en la sombra.",
  objective: "Atravesar el palacio redirigiendo la luz y liberar/derrotar a Helios Encadenado, la entidad solar cautiva en el corazon del trono.",
  bossId: "chained-helios",
  vanillaItemsKey: ["Polvo de hueso/bone meal (limpia aceite del jugador y superficies ANTES de que arda, 30 min de proteccion preventiva)", "Cubo de agua (apaga fuego, NO apaga aceite ardiendo (lo empeora), llena activadores hidraulicos)", "Pedernal y acero (enciende braseros de puzle en secuencia, prende aceite ofensivamente contra guardias)", "Items de oro (activan mecanismos dorados, sobornan Guardias Dorados, pagan a Valdris)", "Escudo (refleja ataques de luz del Coloso de Espejos y del propio Helios)", "Cana de pescar (gira espejos orientables a distancia, tira de palancas lejanas)", "Ballesta (dispara a botones distantes para desactivar trampas de luz a distancia)", "Cubo de polvo de nieve (enfria superficies calientes temporalmente, ralentiza Igneos de Brasero)"],
  sectors: [
    { name: "El Atrio Dorado", desc: "Entrada ceremonial con braseros activables (pedernal y acero) y guardias dorados (items de oro los sobornan). El polvo de hueso previene el aceite del suelo." },
    { name: "La Sala de Espejos Solares", desc: "Puzles de luz: redirigir haces con cana de pescar para girar espejos sin quemarse." },
    { name: "Los Jardines Ardientes", desc: "Patios con aceite, esferas solares rodantes y trampas igneas. El polvo de hueso es vital aqui para limpiar aceite. El cubo de agua apaga a los Igneos." },
    { name: "El Observatorio del Alba", desc: "Arena del semijefe El Guardian del Mediodia. El escudo refleja sus ataques de luz." },
    { name: "El Trono del Sol", desc: "Arena de Helios Encadenado. El cubo de agua + polvo de nieve son esenciales para sobrevivir el Cenit." }
  ],
  puzzles: ["Puzle de luz y espejos (cana de pescar gira espejos, ballesta activa botones)", "Secuencia de antorchas y braseros (pedernal y acero en orden correcto)", "Puzle de peso y sustitucion/compuertas de luz (items de oro como contrapeso ceremonial)"],
  traps: ["Rayo solar concentrado (escudo lo bloquea, espejos lo redirigen con cana de pescar)", "Esfera solar ardiente/bola rodante (cubo de agua la apaga si impacta)", "Braseros en cadena (cubo de agua los apaga todos en cascada)", "Aceite y fuego (polvo de hueso limpia ANTES de prender; cubo de agua empeora aceite ardiendo)", "Estatua o pilar lanzallamas (cubo de agua apaga, escudo protege)", "Trampa oscilante ardiente (escudo bloquea, cubo de polvo de nieve enfria)"],
  loot: ["Fragmento solar: material luminoso de forja.", "Manto del Alba: reduce dano de fuego y luz.", "Lente ustoria: arma/herramienta que concentra luz."],
  weapons: ["Lanza del Mediodia: acumula calor con la luz y libera un tajo igneo."],
  events: ["Cenit: toda la luz del palacio se intensifica; las sombras seguras se reducen. El polvo de nieve crea zonas frias temporales de refugio.", "Eclipse: la luz se apaga, las defensas mueren y despiertan los enemigos de sombra. Las antorchas del jugador son la unica defensa."],
  secrets: ["Alinear tres espejos hacia un sello oculto con la cana de pescar abre la camara del tesoro solar.", "Un brasero apagado adrede con cubo de agua durante el Cenit revela un pasadizo en sombra."],
  entrance: "Un gran porton que solo se abre cuando un haz de luz solar redirigido incide en su cerradura. El jugador debe girar el primer espejo con cana de pescar o ballesta.",
  exit: "Un pozo de luz de teletransporte tras el jefe; antes, hay que rehacer la ruta de espejos.",
  reset: "Los espejos vuelven a su angulo inicial, los braseros se apagan y el aceite se repone.",
  multiplayer: "Los grandes puzles de luz requieren varios jugadores girando espejos coordinadamente.",
  performance: "Los haces de luz son rayos logicos con VFX de AAA Particles; solo la sala activa calcula reflexiones."
});


/* ---------- CRIATURAS (75+) ---------- */
CDE_DATA.creatures = [];
function mob(o){ CDE_DATA.creatures.push(o); }

/* ===== COMPARTIDAS ===== */
mob({ id:"gelatinous-cube", name:"Gelatinous Cube", en:"Gelatinous Cube", dungeon:"shared", rank:"rare",
  hp:"30 (15h)", armor:"50% reduccion", dmg:"< 1h",
  desc:"Gran cubo gelatinoso translucido que actua como aspiradora viviente de la mazmorra: recoge drops, objetos perdidos y restos para evitar acumulacion de entidades. Ciego, se guia por sonido, vibraciones y objetos tirados.",
  behavior:["Lento y ciego; se mueve en las cuatro direcciones sin girar.","Bloquea pasillos: su peligro principal es cortar el paso.","Digiere cada objeto durante hasta 1 hora; si muere antes, suelta el 100% de lo no digerido.","Deja un rastro gelatinoso que ralentiza durante 20 s."],
  vanilla:["Bolas de nieve: lo distraen creando sonido en otra direccion, desviando su ruta.","Huevos: atrae al cubo como cebo de sonido y organico.","Cubo de agua: disuelve temporalmente su superficie haciendolo mas fragil (armadura -25% durante 10s).","Antorchas: no reacciona a la luz (es ciego) pero las digiere creando humo de localizacion."],
  drops:["25%: Trozo de cubo gelatinoso: al usarlo sobre un bloque valido, lo digiere y desaparece (no afecta a bedrock, bloques protegidos ni de progresion)."] });

mob({ id:"mimic", name:"Mimic", en:"Mimic", dungeon:"shared", rank:"normal",
  hp:"20 (10h)", armor:"0", dmg:"5 (2.5h)",
  desc:"Criatura codiciosa que adopta la forma de cofres, contenedores y objetos valiosos. Aunque este disfrazada, se distingue por unos ojos visibles mediante una capa de textura superpuesta. Ataca por sorpresa y huye para volver a esconderse.",
  behavior:["Espera proximidad o interaccion y ataca por sorpresa.","Siempre aplica Nausea I (5 s) y Ceguera (10 s) al 100%.","Tras atacar huye, busca otro cofre y puede sustituir uno real.","Si no encuentra donde esconderse, sigue atacando hasta morir."],
  vanilla:["Catalejo: revela sus ojos a distancia (se marca con particulas si se mira con catalejo).","Huevos: lanzar un huevo al cofre sospechoso lo activa prematuramente sin estar cerca.","Bolas de nieve: mismo efecto que huevos para activar desde lejos.","Leche: limpia la Nausea y Ceguera inmediatamente."],
  drops:["Siempre suelta algunas esmeraldas.","Posible loot valioso adicional por definir."] });

mob({ id:"stitched-watcher", name:"Vigia Cosido", en:"Stitched Watcher", dungeon:"shared", rank:"elite",
  hp:"variable", armor:"variable", dmg:"Indirecto",
  desc:"Criatura integrada en paredes, techos o estructuras que vigila la mazmorra. No combate directamente: percibe y coordina.",
  behavior:["Sigue al jugador con varios ojos y detecta movimiento, luz o presencia.","Alerta mobs, activa trampas, cierra puertas y coordina emboscadas.","Se neutraliza con dano, ceguera, oscuridad, distracciones, puzles o destruyendo su organo central."],
  vanilla:["Cubo de polvo de nieve: colocado sobre sus ojos lo ciega durante 30s.","Antorchas: colocadas cerca lo deslumbran reduciendo su rango de deteccion.","Cizallas: cortan sus conexiones nerviosas deshabilitando su coordinacion con trampas.","Bolas de nieve: impactar en su ojo principal lo aturde 5s."],
  drops:["Organo optico: componente de accesorios de deteccion."] });

mob({ id:"adventurer-wraith", name:"Espectro del Aventurero", en:"Adventurer Wraith", dungeon:"shared", rank:"rare",
  hp:"90 (45h)", armor:"2", dmg:"7 (3.5h)",
  desc:"Aparicion rara que surge en los puntos donde han muerto aventureros. Repite en bucle sus ultimos instantes y ataca a quien perturba su recuerdo. No es hostil de inmediato: da tiempo a retirarse.",
  behavior:["Aparece cerca de restos de muertes previas.","Al derrotarlo o apaciguarlo revela pistas del loot perdido en la zona.","Puede senalar pasadizos o cofres olvidados."],
  vanilla:["Comida/pan: ofrecerle comida lo pacifica sin combate (recuerdo de su ultima comida).","Antorchas: la luz lo debilita (-30% dano).","Leche: limpiar sus debuffs de contacto (escalofrio).","Note blocks: reproducir una melodia especifica lo calma instantaneamente."],
  drops:["Fragmento de recuerdo: pista de mapa o de secreto cercano."] });

mob({ id:"neothelid-adult", name:"Neotelido adulto", en:"Neothelid (Adult)", dungeon:"shared", rank:"rare",
  hp:"320", armor:"6", dmg:"Cola 16 (8h)",
  desc:"Mob raro colosal, ciego, serpentiforme y aberrante, nacido cuando una colonia de 60+ bebes pasa 10 minutos sin comer y se devora hasta dejar un unico superviviente. Detecta sonidos, vibraciones y golpes hasta ~120 bloques.",
  behavior:["Golpe de cola (16, area amplia, gran empuje) y embestida corporal (12-18).","Devorar: atrapa, arrastra y puede curarse.","Furia ciega: al recibir un golpe que no localiza, ataca indiscriminadamente alrededor.","Vomito de crias: escupe 8-10 bebes que atacan a su objetivo.","Golpe corporal al suelo (14-18) con onda de choque y barrido descontrolado."],
  vanilla:["Bolas de nieve: crean sonido en otra direccion desviando su atencion (criatura ciega, guiada por sonido).","Huevos: cebo organico que lo distrae durante 8s mientras intenta devorarlos.","TNT: atrae con la explosion pero el dano lo enfurece (uso estrategico para emboscar).","Sculk sensors: detectan su movimiento a traves de paredes permitiendo preparar emboscadas.","Cubo de agua: inunda su ruta haciendo que se resbale y cancele la embestida."],
  drops:["1 objeto garantizado (25% c/u): Cerebro neotelido, Cristal de recuerdo, Mapa incompleto o Diario reconstruido.","Huevos de bebe (30%:1; 20%:2; 50%:0).","~1/3 de la experiencia del Dragon del End."] });

mob({ id:"neothelid-baby", name:"Neotelido bebe", en:"Neothelid (Baby)", dungeon:"shared", rank:"normal",
  hp:"4", armor:"0", dmg:"Mordisco 2 (1h)",
  desc:"Cria que se mueve en grupo buscando comida. Variante base del Neotelido; sin comer demasiado tiempo, las salvajes se vuelven canibales y desencadenan la transformacion adulta.",
  behavior:["Se desplazan en enjambre y atacan mordiendo.","Domesticadas siguen al jugador y aceptan ordenes basicas.","Colonia domesticada de 30 forma un adulto domesticado (160 HP, 4 armadura, cola 10)."],
  vanilla:["Huevos: alimentarlas con huevos previene la transformacion canibal y las mantiene dociles.","Comida/pan: cualquier alimento las pacifica y las acerca al jugador.","Cubo de agua: las dispersa temporalmente rompiendo el enjambre."],
  drops:["n/a"] });

/* ===== THE MIRROR CASTLE ===== */
mob({ id:"reflection-twin", name:"Gemelo de Reflejo", en:"Reflection Twin", dungeon:"mirror-castle", rank:"elite",
  hp:"Igual al jugador", armor:"Variable", dmg:"Copia del jugador",
  desc:"Copia parcialmente la apariencia, el equipo y los movimientos y ataques recientes del jugador. Se debilita destruyendo o alterando la superficie reflectante que lo mantiene.",
  behavior:["Imita los ultimos movimientos memorizados del jugador.","Vinculado a una superficie especular concreta.","Romper o cubrir esa superficie lo debilita o lo destruye."],
  vanilla:["Cubo de agua: empanar el espejo vinculado lo debilita (-50% HP).","Bolas de nieve: rompen el espejo vinculado si es fragil (1-2 impactos).","Escudo: bloquea sus ataques copiados (el reflejo no puede copiar el bloqueo).","Catalejo: revela que espejo lo vincula (brilla con un aura)."],
  drops:["Astilla de azogue.","Posible pieza de equipo copiada de baja durabilidad."] });

mob({ id:"glass-servant", name:"Sirviente de Cristal", en:"Glass Servant", dungeon:"mirror-castle", rank:"normal",
  hp:"24 (12h)", armor:"3", dmg:"6 (3h)",
  desc:"Sirviente humanoide hecho de vidrio pulido y astillas. Camina con paso quebradizo por los salones y estalla en esquirlas al caer.",
  behavior:["Al morir explota en esquirlas que causan dano en area.","Se vuelve casi invisible cuando queda quieto frente a un espejo.","Vulnerable a ataques contundentes (martillo)."],
  vanilla:["Escudo: bloquea la explosion de esquirlas al morir (crucial).","Bolas de nieve: lo aturde por el impacto en su estructura fragil.","Antorchas: colocadas detras de el proyectan su sombra revelando su posicion cuando esta quieto."],
  drops:["Esquirlas de cristal.","Raro: vidrio pulido perfecto."] });

mob({ id:"quicksilver-maiden", name:"Doncella de Azogue", en:"Quicksilver Maiden", dungeon:"mirror-castle", rank:"normal",
  hp:"30 (15h)", armor:"0", dmg:"5 (2.5h)",
  desc:"Figura liquida de mercurio que se desliza por suelos y espejos. Al recibir dano se divide en gotas menores que vuelven a fusionarse.",
  behavior:["Se divide al ser golpeada y se recompone si se la deja reposar.","Puede viajar por superficies especulares para reaparecer detras.","El fuego la endurece temporalmente (mas lenta, mas fragil)."],
  vanilla:["Pedernal y acero: el fuego la endurece (+vulnerability, -speed) durante 15s.","Cubo de agua: la dispersa impidiendo la re-fusion durante 10s.","Cubo de polvo de nieve: la congela completamente durante 5s (critica)."],
  drops:["Gota de azogue.","Ingrediente alquimico."] });

mob({ id:"frame-sentinel", name:"Centinela de Marco", en:"Frame Sentinel", dungeon:"mirror-castle", rank:"normal",
  hp:"40 (20h)", armor:"6", dmg:"7 (3.5h)",
  desc:"Marco de espejo animado que patrulla los pasillos y proyecta puertas y ventanas falsas para desorientar. Su cristal es su punto debil.",
  behavior:["Proyecta aberturas ilusorias que llevan a callejones.","Bloquea el paso girando su marco como una hitbox solida.","Romper su cristal lo inutiliza; el borde metalico resiste."],
  vanilla:["Bolas de nieve: rompen su cristal en 3 impactos (el punto debil).","Ballesta: un disparo certero al cristal lo rompe de un tiro a distancia.","Cana de pescar: engancha su marco y lo gira, desorientando su proyeccion."],
  drops:["Marco de plata (decorativo crafteable).","Cristal intacto."] });

mob({ id:"specular-echo", name:"Eco Especular", en:"Specular Echo", dungeon:"mirror-castle", rank:"normal",
  hp:"18 (9h)", armor:"0", dmg:"Repite el ultimo ataque del jugador",
  desc:"Copia diferida que reproduce, con un segundo de retraso, la ultima accion del jugador. Ensena a controlar los propios ataques dentro de la dungeon.",
  behavior:["Reproduce con retraso el ultimo golpe o habilidad del jugador.","No inicia acciones propias: solo repite.","Se anula quedandose quieto el tiempo suficiente."],
  vanilla:["Escudo: levantar escudo y no atacar lo neutraliza (no tiene nada que copiar).","Bolas de nieve: lanzar bolas de nieve hace que copie algo inofensivo.","Huevos: mismo efecto que bolas de nieve (copia algo sin dano)."],
  drops:["Fragmento de eco: componente de crafteo sonoro."] });

mob({ id:"mirror-lantern-bearer", name:"Portador de Faroles", en:"Lantern-bearer", dungeon:"mirror-castle", rank:"normal",
  hp:"22 (11h)", armor:"2", dmg:"5 (2.5h) + ceguera",
  desc:"Sirviente encorvado que porta un farol cuya luz, rebotada en los espejos, ciega al jugador. Apagar su farol lo vuelve inofensivo y oscurece la sala.",
  behavior:["Su luz reflejada aplica Ceguera breve en linea de vision.","Busca espejos para amplificar su resplandor.","Apagar el farol (agua/flecha) lo debilita y cambia la iluminacion de la sala."],
  vanilla:["Cubo de agua: apaga su farol instantaneamente a distancia.","Bolas de nieve: impactar en el farol lo apaga temporalmente (5s).","Escudo: bloquea el efecto de Ceguera si se mira tras el escudo."],
  drops:["Farol de plata: fuente de luz portatil."] });

mob({ id:"glass-harlequin", name:"Arlequin de Vidrio", en:"Glass Harlequin", dungeon:"mirror-castle", rank:"elite",
  hp:"70 (35h)", armor:"4", dmg:"9 (4.5h)",
  desc:"Bufon de cristal agil y burlon que intercambia su posicion con la del jugador o con sus propios reflejos, atacando desde angulos imposibles.",
  behavior:["Teletransporte corto entre espejos y reflejos.","Lanza esquirlas giratorias en abanico.","Se rie con voces reflejadas para confundir la direccion del ataque."],
  vanilla:["Escudo: bloquea las esquirlas giratorias.","Cubo de agua: empanar los espejos cercanos limita sus puntos de teletransporte.","Catalejo: predecir desde que espejo aparecera (brilla antes).","Bolas de nieve: romper espejos cercanos reduce sus opciones de movimiento."],
  drops:["Cascabel de vidrio (accesorio).","Raro: mascara del arlequin."] });

mob({ id:"broken-twins", name:"Los Gemelos Rotos", en:"The Broken Twins", dungeon:"mirror-castle", rank:"miniboss",
  hp:"2x120 (comparten dano)", armor:"6", dmg:"12 (6h)",
  desc:"Dos caballeros especulares que comparten una unica reserva de vida repartida: danar a uno hiere al otro, pero solo mueren si caen casi a la vez.",
  behavior:["Comparten pool de vida; hay que igualar el dano de ambos.","Uno es el real y el otro su reflejo, e intercambian roles al girar la sala.","Golpear al reflejo equivocado devuelve el dano (Ley del Reflejo)."],
  vanilla:["Catalejo: revela cual es el real (detalles sutiles en su armadura).","Escudo: protege del dano devuelto por la Ley del Reflejo.","Cubo de agua: empanar el suelo los ralentiza y revela al real por sus pisadas.","Bolas de nieve: lanzar a ambos simultaneamente; el real reacciona primero."],
  drops:["Par de hojas espejadas.","Fragmento de trono invertido."] });

mob({ id:"lady-quicksilver", name:"La Dama del Azogue", en:"The Lady of Quicksilver", dungeon:"mirror-castle", rank:"miniboss",
  hp:"180 (90h)", armor:"3", dmg:"11 (5.5h)",
  desc:"Bruja del castillo cuyo cuerpo es mercurio vivo. Convierte los suelos en espejos liquidos y hace emerger manos de azogue desde cada superficie reflectante.",
  behavior:["Convierte el suelo en espejo: caer en el teletransporta a otra sala.","Invoca manos de azogue que agarran e inmovilizan.","Es vulnerable solo cuando su reflejo real queda expuesto por una rotura."],
  vanilla:["Cubo de polvo de nieve: congela el suelo de mercurio impidiendo la conversion en espejo.","Pedernal y acero: endurece las manos de azogue haciendolas fragiles.","Cubo de agua: diluye el mercurio del suelo haciendolo inerte temporalmente.","Bolas de nieve: rompen la superficie donde su reflejo real aparece."],
  drops:["Corazon de azogue (reliquia).","Receta de Armadura de Plata Pulida."] });

mob({ id:"shattered-sovereign", name:"El Soberano Anicos", en:"The Shattered Sovereign", dungeon:"mirror-castle", rank:"boss",
  hp:"1200 (fases)", armor:"8", dmg:"14-20",
  desc:"JEFE. Un rey compuesto de mil anicos de espejo suspendidos que se recomponen en distintas formas. Solo puede morir si se destruyen, una a una, las grandes superficies especulares que sostienen su reflejo en el Salon del Trono Invertido.",
  behavior:["Fase 1: combate como caballero espejado, devolviendo dano a los golpes frontales mal dirigidos.","Fase 2: se fragmenta y ataca desde multiples reflejos simultaneos; hay que romper el espejo ancla.","Fase 3: invierte la sala (techo/suelo) y proyecta reflejos hostiles del propio jugador.","Cada gran espejo destruido reduce su armadura y expone su forma real."],
  vanilla:["Escudo: imprescindible para sobrevivir la Fase 1 (refleja su dano devuelto de vuelta a el).","Bolas de nieve: rompen los espejos ancla en Fase 2 a distancia segura.","Cubo de agua: empanar suelo y paredes en Fase 3 revela la orientacion real de la sala.","Catalejo: identifica cual de los reflejos en Fase 2 es el ancla real.","Ballesta: rompe espejos a distancia en las fases criticas.","Cana de pescar: arranca anicos individuales debilitandolo progresivamente."],
  drops:["Corona Anicos (casco unico).","Nucleo del Trono Invertido (material legendario).","Fragmento de azogue puro."] });


/* ===== THE WORLDBEARER ===== */
mob({ id:"walking-fragment", name:"Fragmento Andante", en:"Walking Fragment", dungeon:"worldbearer", rank:"normal",
  hp:"28 (14h)", armor:"6", dmg:"6 (3h)",
  desc:"Trozo de la piel petrea del titan que ha cobrado vida y camina con pesadez. Lento pero muy resistente; ideal para bloquear pasos estrechos.",
  behavior:["Muy resistente al dano cortante; debil al contundente.","Al morir se desmorona en bloques que pueden usarse como peso.","Se agrupa para taponar rutas."],
  vanilla:["TNT: lo destruye instantaneamente y despeja la ruta.","Piston de redstone: lo empuja sin necesidad de matarlo.","Cubo de agua: lo ralentiza aun mas y lo hace resbalar en pendientes."],
  drops:["Piedra viva (bloque de construccion)."] });

mob({ id:"ballast-pilgrim", name:"Peregrino de Lastre", en:"Ballast Pilgrim", dungeon:"worldbearer", rank:"normal",
  hp:"24 (12h)", armor:"2", dmg:"7 (3.5h)",
  desc:"Fanatico que carga bloques de contrapeso a la espalda para aliviar al titan. Cuando muere, suelta su lastre, que puede activar placas o aplastar.",
  behavior:["Al morir suelta un bloque pesado que cae y puede activar mecanismos.","Se sacrifican arrojandose sobre placas para bloquear rutas.","Lentos por el peso que cargan."],
  vanilla:["Cana de pescar: engancha su bloque de lastre y se lo arranca (queda liviano y debil).","Bolas de nieve: lo distrae haciendolo cambiar de ruta (no se sacrifica en la placa).","Leads/riendas: se pueden atar a su bloque y arrastrarlo a otra placa util."],
  drops:["Bloque de contrapeso.","Reliquia del peregrino (rara)."] });

mob({ id:"counterweight-guardian", name:"Guardian de Contrapeso", en:"Counterweight Guardian", dungeon:"worldbearer", rank:"normal",
  hp:"46 (23h)", armor:"6", dmg:"10 (5h) + empuje",
  desc:"Automata de piedra armado con una maza de contrapeso. Carga sus golpes para lanzar por los aires.",
  behavior:["Carga el golpe: mas espera, mas dano y empuje enorme.","Puede desplazar bloques empujables al golpear.","Vulnerable durante la larga recuperacion de su golpe cargado."],
  vanilla:["Escudo: bloquea el empuje del golpe cargado (el dano se reduce un 50%).","Bolas de nieve: lo interrumpen durante la carga si impactan en su cabeza.","Cubo de agua: crea una superficie resbaladiza que anula su empuje."],
  drops:["Nucleo de contrapeso.","Raro: plano del Martillo de Contrapeso."] });

mob({ id:"lithic-swarm", name:"Colonia Litica", en:"Lithic Swarm", dungeon:"worldbearer", rank:"normal",
  hp:"3 c/u (enjambre)", armor:"0", dmg:"2 (1h)",
  desc:"Nube de guijarros vivos que fluye como arena por las grietas del titan. En masa cubren el suelo y erosionan la armadura.",
  behavior:["Se mueven en enjambre y rodean al jugador.","El contacto continuo desgasta durabilidad de armadura.","Vulnerables a area, agua y explosiones."],
  vanilla:["Cubo de agua: dispersa todo el enjambre de una vez (la mejor contramedida).","TNT: elimina la colonia entera en un radio.","Bolas de nieve: dispersa una porcion pequena del enjambre."],
  drops:["Grava viva."] });

mob({ id:"lesser-atlas", name:"Atlante Menor", en:"Lesser Atlas", dungeon:"worldbearer", rank:"elite",
  hp:"140 (70h)", armor:"8", dmg:"12 (6h)",
  desc:"Coloso humanoide que sostiene con los brazos una seccion del techo. Mientras vive, ese techo se mantiene; al morir, la estructura cae.",
  behavior:["Sostiene un techo o plataforma: matarlo provoca un derrumbe controlado.","Golpea con punos que agrietan el suelo.","Puede usarse su muerte para abrir o cerrar rutas."],
  vanilla:["TNT: lo mata rapido pero el derrumbe es inmediato (usar con precaucion).","Escudo: protege del impacto de sus punos.","Bloques de piedra: colocar pilares antes de matarlo previene el derrumbe total.","Perla de ender: escapar del derrumbe tras matarlo."],
  drops:["Nucleo de atlante (material pesado).","Fragmento de mundo suspendido."] });

mob({ id:"foundation-wyrm", name:"Sierpe de Cimientos", en:"Foundation Wyrm", dungeon:"worldbearer", rank:"elite",
  hp:"120 (60h)", armor:"5", dmg:"11 (5.5h)",
  desc:"Gusano de roca que horada los huesos y cimientos del titan, emergiendo por donde menos se espera.",
  behavior:["Se entierra y emerge por sorpresa (IA de emboscada).","Al emerger crea un agujero que puede usarse como ruta.","Debil justo despues de emerger, mientras se reorienta."],
  vanilla:["Sculk sensors: detectan su movimiento bajo tierra y alertan de su posicion.","Cubo de agua: vertido en su agujero lo fuerza a salir prematuramente (vulnerable).","TNT: detonado sobre su posicion subterranea lo dana y fuerza a emerger aturdido."],
  drops:["Coraza de sierpe (armadura ligera pesada).","Diente perforante."] });

mob({ id:"living-vertebra", name:"El Vertebra Viva", en:"The Living Vertebra", dungeon:"worldbearer", rank:"miniboss",
  hp:"260 (130h) por segmentos", armor:"7", dmg:"13 (6.5h)",
  desc:"Un segmento de la columna del titan que se ha independizado y se enrosca por la sala como una serpiente de piedra multiparte.",
  behavior:["Entidad multiparte: destruir vertebras concretas la acorta y debilita.","Barre la sala con su cuerpo y aplasta con las secciones.","Protege su vertebra-nucleo, unico punto letal."],
  vanilla:["TNT: destruye multiples segmentos a la vez (la forma mas rapida).","Catalejo: identifica la vertebra-nucleo (tiene una grieta luminosa visible con zoom).","Ballesta: permite atacar segmentos especificos a distancia.","Escudo: bloquea el barrido corporal."],
  drops:["Vertebra-nucleo (reliquia).","Modulos de piedra viva."] });

mob({ id:"core-custodian", name:"Custodio del Nucleo", en:"Core Custodian", dungeon:"worldbearer", rank:"miniboss",
  hp:"300 (150h)", armor:"9", dmg:"14 (7h) + empuje",
  desc:"Guardian colosal que vigila el ascenso al corazon. Usa contrapesos gigantes como armas y controla las placas basculantes de su arena.",
  behavior:["Manipula placas basculantes para desequilibrar al jugador.","Lanza y recoge una bola de contrapeso encadenada.","Se aturde si su propia bola impacta contra un muro."],
  vanilla:["Cana de pescar: engancha su cadena y desvia la trayectoria de la bola contra el muro (aturdimiento).","Escudo: resiste el empuje de las placas basculantes.","Bloques de piedra: colocados en las placas las estabiliza.","Perla de ender: esquiva de emergencia cuando lanza la bola."],
  drops:["Cadena del custodio (accesorio de anti-empuje).","Sello del corazon."] });

mob({ id:"dying-atlas", name:"Atlas Moribundo", en:"The Dying Atlas", dungeon:"worldbearer", rank:"boss",
  hp:"1500 (fases)", armor:"10", dmg:"16-22",
  desc:"JEFE. La voluntad viva del titan, un coloso agonizante que sostiene el mundo aun mientras lucha. Cada fase debilita un soporte de la arena, que empieza a derrumbarse.",
  behavior:["Fase 1: punetazos sismicos y ondas de choque que agrietan el suelo.","Fase 2: arranca contrapesos de la arena y los lanza; el techo empieza a ceder.","Fase 3: la arena se derrumba por partes y hay que combatir reubicando el peso para no caer.","Sus manos y hombros son partes independientes que pueden inutilizarse."],
  vanilla:["Escudo: esencial para sobrevivir las ondas de choque de Fase 1.","Bloques de piedra/tierra: colocar en las grietas previene la expansion del derrumbe en Fase 3.","Perla de ender: escapar de secciones que caen.","TNT: danar sus manos cuando las clava en el suelo (ventana critica).","Cana de pescar: desviar los contrapesos lanzados en Fase 2.","Leads/riendas: atar contrapesos a posiciones fijas para estabilizar la arena."],
  drops:["Corazon del Titan (reliquia legendaria).","Martillo de Contrapeso mejorado (posible).","Nucleo de mundo."] });


/* ===== THE HOLLOW LEVIATHAN ===== */
mob({ id:"clinging-parasite", name:"Parasito Adherido", en:"Clinging Parasite", dungeon:"hollow-leviathan", rank:"normal",
  hp:"16 (8h)", armor:"0", dmg:"4 (2h)",
  desc:"Sanguijuela colosal adherida a las paredes de carne del leviatan. Se deja caer sobre los intrusos y se aferra, drenando salud lentamente.",
  behavior:["Espera adherido al techo o pared y cae sobre el objetivo.","Se aferra y drena vida hasta que se lo golpea.","Debil una vez despegado del muro."],
  vanilla:["Antorchas: colocadas en el techo/pared los ahuyenta antes de que caigan.","Cubo de agua: los despega instantaneamente si ya estan aferrados.","Pedernal y acero: cauteriza su punto de agarre en la pared previniendo que vuelvan."],
  drops:["Glandula de sanguijuela (alquimia)."] });

mob({ id:"antibody", name:"Anticuerpo", en:"Antibody", dungeon:"hollow-leviathan", rank:"normal",
  hp:"12 (6h)", armor:"0", dmg:"5 (2.5h)",
  desc:"Organismo defensivo blanco que el leviatan genera en masa para expulsar patogenos. Aparecen en oleadas proporcionales al ruido y al dano que causa el jugador.",
  behavior:["Aparecen en oleadas al activarse la respuesta inmune.","Se lanzan en enjambre contra el intruso.","Menos ruido y dano = menos anticuerpos."],
  vanilla:["Bolas de nieve: crean sonido LEJOS del jugador, desviando la oleada.","Honey bottles: el jugador cubierto de miel es ignorado temporalmente (confunde sus sensores).","Cubo de agua: los dispersa como enjambre.","Polvo de hueso: esparcido en el suelo crea una barrera organica que los confunde (los trata como tejido propio)."],
  drops:["Plasma coagulado."] });

mob({ id:"devouring-larva", name:"Larva Devoradora", en:"Devouring Larva", dungeon:"hollow-leviathan", rank:"normal",
  hp:"20 (10h)", armor:"1", dmg:"6 (3h)",
  desc:"Larva voraz que repta por los conductos digestivos consumiendo todo a su paso. Crece si devora suficiente materia.",
  behavior:["Devora drops y restos; puede engordar y volverse mas peligrosa.","Repta rapido por superficies humedas.","Vulnerable al fuego."],
  vanilla:["Huevos: lanzados como cebo la atraen y la distraen de devorar loot valioso.","Pedernal y acero: prende fuego que la mata rapidamente.","Comida/pan: arrojada como cebo la aleja del grupo.","Cubo de agua: la ralentiza (contradictoriamente, las superficies humedas la aceleran pero el impacto directo la aturde)."],
  drops:["Saco larval.","Raro: larva madura (invocable)."] });

mob({ id:"wall-maw", name:"Boca de Pared", en:"Wall Maw", dungeon:"hollow-leviathan", rank:"normal",
  hp:"36 (18h)", armor:"4", dmg:"8 (4h) + agarre",
  desc:"Boca oculta en el tejido de las paredes, entre trampa y criatura. Muerde a quien pasa cerca y puede tragar y escupir a otra zona.",
  behavior:["Camuflada hasta que el jugador se acerca.","Muerde, agarra y puede transportar a otra sala.","Su interior blando es su punto debil cuando esta abierta."],
  vanilla:["Pedernal y acero: cauteriza la boca permanentemente cerrandola para siempre.","Cizallas: cortan sus tendones internos impidiendo que muerda.","Escudo: bloquea la mordida si se activa a tiempo.","Huevos: lanzados dentro la sacian temporalmente (no muerde durante 30s)."],
  drops:["Diente de pared.","Membrana."] });

mob({ id:"spitting-polyp", name:"Polipo Escupidor", en:"Spitting Polyp", dungeon:"hollow-leviathan", rank:"normal",
  hp:"22 (11h)", armor:"0", dmg:"Acido 7 (3.5h)",
  desc:"Polipo fijo en suelos y techos que escupe bilis acida a distancia. Su acido dana la armadura y crea charcos peligrosos.",
  behavior:["Ataque a distancia con proyectiles acidos.","Deja charcos de acido que degradan armadura.","Inmovil: se elimina facil si se llega a corta distancia."],
  vanilla:["Escudo: bloquea los proyectiles acidos completamente.","Cubo de agua: diluye los charcos de acido del suelo.","Ballesta: lo elimina a distancia antes de que escupa.","Polvo de hueso: neutraliza el acido en los charcos (reaccion quimica)."],
  drops:["Vejiga acida (arma arrojadiza)."] });

mob({ id:"membrane-weaver", name:"Tejedor de Membranas", en:"Membrane Weaver", dungeon:"hollow-leviathan", rank:"elite",
  hp:"110 (55h)", armor:"3", dmg:"9 (4.5h)",
  desc:"Criatura que sella pasajes con tejido vivo y teje capullos que liberan mobs. Controla el flujo de la dungeon cerrando y abriendo conductos.",
  behavior:["Sella salidas con membranas que hay que cortar o quemar.","Crea capullos que eclosionan en Larvas o Anticuerpos.","Huye y re-sella si se ve superado."],
  vanilla:["Cizallas: cortan membranas y capullos instantaneamente (herramienta definitiva aqui).","Pedernal y acero: quema membranas permanentemente.","Ballesta: destruye capullos a distancia antes de la eclosion."],
  drops:["Seda de membrana (armadura anti-veneno).","Capullo intacto."] });

mob({ id:"false-heart", name:"Corazon Falso", en:"False Heart", dungeon:"hollow-leviathan", rank:"elite",
  hp:"150 (75h)", armor:"5", dmg:"Onda 10 (5h)",
  desc:"Organo senuelo que late con fuerza para atraer al jugador y confundirlo respecto al corazon real.",
  behavior:["Late emitiendo ondas de empuje ritmicas (usa resonancia).","Bombea Anticuerpos con cada latido.","Destruirlo altera el sistema de fluidos... pero no mata al leviatan."],
  vanilla:["Cubo de agua: vertido sobre el corazon reduce la frecuencia de latidos (enfria).","Note blocks: reproducir el contra-ritmo lo desincroniza (no bombea anticuerpos).","Sculk sensors: ayudan a distinguir el falso del real por el patron de vibracion."],
  drops:["Valvula falsa.","Pista sobre la ubicacion del corazon real."] });

mob({ id:"nerve-cluster", name:"El Sistema Nervioso", en:"The Nerve Cluster", dungeon:"hollow-leviathan", rank:"miniboss",
  hp:"320 (160h) multiparte", armor:"4", dmg:"12 (6h)",
  desc:"Marana de ganglios y nervios luminosos que recorre toda la sala como un candelabro vivo. Coordina las defensas del leviatan.",
  behavior:["Entidad multiparte: cada ganglio controla trampas u ojos de la sala.","Descargas electricas por los nervios que conectan los ganglios.","Cortar ramas concretas apaga defensas antes del nucleo."],
  vanilla:["Cizallas: cortan las conexiones nerviosas (la mejor herramienta).","Cubo de agua: cortocircuita las descargas electricas temporalmente.","Antorchas: cauterizadas contra los nervios cortados impiden que se regeneren.","Ballesta: corta nervios a distancia evitando las descargas."],
  drops:["Ganglio luminoso (accesorio).","Fibra nerviosa conductora."] });

mob({ id:"sovereign-parasite", name:"El Parasito Regente", en:"The Sovereign Parasite", dungeon:"hollow-leviathan", rank:"boss",
  hp:"1300 (fases)", armor:"6", dmg:"15-21",
  desc:"JEFE. El organismo que reanima al leviatan desde su cavidad central, un parasito colosal enraizado en el corazon real.",
  behavior:["Fase 1: golpea con tentaculos-raiz y escupe crias.","Fase 2: retrae el corazon tras costillas vivas que hay que abrir.","Fase 3: al verse morir, provoca un espasmo global del leviatan (la sala se contrae).","Sus raices son partes independientes; cortarlas reduce su regeneracion."],
  vanilla:["Cizallas: cortan sus raices con eficiencia x3 vs armas normales (esencial).","Pedernal y acero: cauteriza las raices cortadas impidiendo regeneracion.","Cubo de agua: diluye su acido de Fase 1 que cubre el suelo.","Polvo de hueso: aplicado en las costillas en Fase 2 las debilita (estimula crecimiento descontrolado que las agrieta).","Leche: limpia los debuffs de veneno parasitario entre fases.","Honey bottles: inmunidad a los suelos mucosos resbaladizos durante la Fase 3."],
  drops:["Corazon del Leviatan (reliquia legendaria).","Bisturi del Cirujano mejorado (posible).","Ambar organico."] });


/* ===== THE VEILED PEAK ===== */
mob({ id:"veiled-soul", name:"Alma Velada", en:"Veiled Soul", dungeon:"veiled-peak", rank:"normal",
  hp:"18 (9h)", armor:"0", dmg:"5 (2.5h)",
  desc:"Espiritu de un escalador perdido, envuelto en jirones de tela y niebla. Casi invisible en la ventisca; se materializa al atacar.",
  behavior:["Se difumina en la niebla y reaparece cerca.","La luz de los faros la hace visible y vulnerable.","Atraviesa parcialmente obstaculos."],
  vanilla:["Antorchas: colocadas o sostenidas la hacen visible permanentemente en su radio.","Pedernal y acero: prender un bloque cercano la mantiene visible.","Cubo de polvo de nieve: sorprendentemente, la nieve concentrada la solidifica brevemente."],
  drops:["Jiron espectral (textil magico)."] });

mob({ id:"petrified-climber", name:"Escalador Petrificado", en:"Petrified Climber", dungeon:"veiled-peak", rank:"normal",
  hp:"30 (15h)", armor:"5", dmg:"6 (3h)",
  desc:"Aventurero congelado y reanimado por el velo, aun aferrado a su piolet. Se mueve rigido pero golpea con fuerza sorprendente.",
  behavior:["Resistente al frio; fragil al calor/fuego.","Escala paredes verticales para flanquear.","Puede empujar hacia precipicios."],
  vanilla:["Pedernal y acero: el fuego lo debilita enormemente (-50% HP max por calor).","Cubo de lava: lo derrite rapidamente (muy efectivo pero recurso valioso).","Escudo: bloquea su empuje hacia el precipicio."],
  drops:["Piolet desgastado.","Provisiones congeladas."] });

mob({ id:"zephyr-wisp", name:"Cefiro", en:"Zephyr Wisp", dungeon:"veiled-peak", rank:"normal",
  hp:"14 (7h)", armor:"0", dmg:"3 (1.5h) + empuje fuerte",
  desc:"Elemental de viento que apenas hace dano pero empuja con violencia hacia el vacio. En las cornisas, un solo empujon puede ser mortal.",
  behavior:["Su ataque principal es empujar hacia precipicios.","Se mueve erratico y rapido.","Se dispersa con proyectiles pesados o sin viento a favor."],
  vanilla:["Escudo: reduce su empuje a casi cero (contramedida perfecta).","Bolas de nieve: 3 impactos lo dispersan temporalmente.","Honey bottles: inmunidad al deslizamiento tras ser empujado (no resbalas al borde)."],
  drops:["Esencia de viento."] });

mob({ id:"fog-bellringer", name:"Campanero de Niebla", en:"Fog Bellringer", dungeon:"veiled-peak", rank:"normal",
  hp:"26 (13h)", armor:"2", dmg:"5 (2.5h)",
  desc:"Monje espectral que toca una campana para invocar niebla espesa y llamar a otras almas. Silenciar su campana despeja la zona.",
  behavior:["Toca la campana: genera niebla cegadora y atrae aliados.","Coordina emboscadas con Almas Veladas.","Romper la campana lo desarma y disipa la niebla local."],
  vanilla:["Bolas de nieve: impactar en la campana la silencia temporalmente.","Ballesta: un disparo certero rompe la campana permanentemente.","Note blocks: reproducir la contra-frecuencia anula la campana en su radio."],
  drops:["Campana de niebla (mecanismo sonoro)."] });

mob({ id:"spectral-yak", name:"Yak Espectral", en:"Spectral Yak", dungeon:"veiled-peak", rank:"normal",
  hp:"44 (22h)", armor:"3", dmg:"9 (4.5h) embestida",
  desc:"Bestia fantasmal de montana que carga en linea recta desde la niebla. Su embestida puede arrojar al jugador por una cornisa.",
  behavior:["Carga telegrafiada de largo alcance con gran empuje.","Tras fallar la carga queda aturdido contra el muro.","Se mueve en pequenas manadas."],
  vanilla:["Escudo: bloquea la embestida y lo aturde (como si chocara contra un muro).","Bolas de nieve: lanzadas a su trayectoria de carga lo desvian ligeramente.","Comida/pan: lo pacifica antes de que cargue si se ofrece a tiempo."],
  drops:["Pelaje espectral (aislante del frio)."] });

mob({ id:"veil-warden", name:"Guardian del Velo", en:"Veil Warden", dungeon:"veiled-peak", rank:"elite",
  hp:"120 (60h)", armor:"5", dmg:"11 (5.5h)",
  desc:"Centinela encapuchado que se vuelve invisible dentro de la niebla y solo es visible bajo la luz de los faros.",
  behavior:["Invisible en niebla; visible y vulnerable bajo luz de faro.","Ataca desde angulos muertos (IA de emboscada).","Apaga faros cercanos para recuperar su ventaja."],
  vanilla:["Antorchas: colocadas en multiples puntos crean una red de luz que lo mantiene visible.","Pedernal y acero: reenciende faros apagados rapidamente.","Catalejo: lo revela brevemente incluso en niebla (destello visible con zoom).","Cubo de polvo de nieve: esparcido en el suelo muestra sus pisadas invisibles."],
  drops:["Manto del velo (sigilo).","Llave de cornisa."] });

mob({ id:"frozen-oracle", name:"Oraculo Congelado", en:"Frozen Oracle", dungeon:"veiled-peak", rank:"elite",
  hp:"130 (65h)", armor:"4", dmg:"Escarcha 10 (5h)",
  desc:"Vidente petrificada en hielo que lanza esquirlas de escarcha y profecias que aplican debuffs de lentitud y miedo.",
  behavior:["Congela el suelo creando placas deslizantes.","Aplica Lentitud y vision reducida con sus profecias.","Vulnerable mientras profetiza (animacion larga)."],
  vanilla:["Honey bottles: inmunidad al deslizamiento en el hielo que crea.","Leche: limpia Lentitud y debuffs de profecia instantaneamente.","Pedernal y acero: derrite las placas de hielo que crea en el suelo.","Cubo de lava: contraataque termico que la dana enormemente."],
  drops:["Reliquia del Oraculo (revela ruta segura).","Nucleo de escarcha."] });

mob({ id:"summit-keeper", name:"El Guardian de la Cima", en:"The Summit Keeper", dungeon:"veiled-peak", rank:"miniboss",
  hp:"340 (170h)", armor:"7", dmg:"13 (6.5h)",
  desc:"Coloso de hielo y viento que defiende la cornisa final antes de la cima. Controla el viento de la arena y desata pequenos aludes.",
  behavior:["Cambia la direccion del viento de la arena a voluntad.","Provoca aludes que hay que esquivar o cubrirse.","Se enfria y endurece; el fuego rompe su coraza temporalmente."],
  vanilla:["Pedernal y acero: debilita su coraza de hielo (quita armadura temporal).","Escudo: bloquea los aludes y reduce el empuje del viento.","Cubo de lava: dano masivo termico a su cuerpo de hielo.","Cubo de polvo de nieve: paradojicamente, exceso de nieve en sus grietas lo debilita por expansion."],
  drops:["Corona de escarcha (accesorio).","Sello de la cima."] });

mob({ id:"crowned-tempest", name:"La Tormenta Coronada", en:"The Crowned Tempest", dungeon:"veiled-peak", rank:"boss",
  hp:"1250 (fases)", armor:"5", dmg:"14-20 + rayo",
  desc:"JEFE. El espiritu que corona el pico: una tormenta viva de viento, hielo y relampago.",
  behavior:["Fase 1: rafagas que empujan hacia los bordes y esquirlas de hielo.","Fase 2: convoca la niebla y ataca desde la invisibilidad, guiada por el trueno.","Fase 3: descarga rayos en zonas marcadas y desata la ventisca total.","El velo la protege; encender los faros de la arena la expone por turnos."],
  vanilla:["Escudo: imprescindible en Fase 1 para no ser empujado al vacio.","Pedernal y acero: enciende los faros de la arena que la exponen (esencial).","Honey bottles: inmunidad al deslizamiento del hielo en Fase 1 y 3.","Cubo de polvo de nieve: crea plataformas de emergencia cuando el suelo se rompe por los rayos.","Antorchas: marcan las zonas seguras durante la ventisca de Fase 3.","Catalejo: revela su posicion real en Fase 2 a traves de la niebla.","Leche: limpia el debuff de congelacion que aplica periodicamente."],
  drops:["Corona de la Tempestad (yelmo unico).","Corazon del Velo (reliquia legendaria).","Cristal de rayo."] });


/* ===== THE DESCENT INTO MADNESS ===== */
mob({ id:"hollow-fencer", name:"El Duelista Hueco", en:"The Hollow Fencer", dungeon:"descent-madness", rank:"rare",
  hp:"Por definir (agil)", armor:"Ligera", dmg:"Moderado / muy rapido",
  desc:"Cadaver sin cabeza de un aventurero agil, controlado por un parasito alojado en el pecho que usa la cavidad toracica como organo de resonancia para imitar voces humanas.",
  behavior:["Emite voces de auxilio pre-grabadas desde el pecho antes de atacar.","Se desplaza lateralmente, flanquea y usa esquinas (IA de emboscada).","Punzada rapida y Rafaga de estocadas (puede deshabilitar escudos).","Postura de parry con 100% de exito durante su animacion reconocible.","Tras ~10 s ofensivos se fatiga 3 s; con poca vida intenta curarse con una pocion interrumpible."],
  vanilla:["Escudo: bloquea la Punzada rapida pero la Rafaga lo deshabilita; usar con timing.","Bolas de nieve: interrumpen su curacion si impactan durante la animacion de pocion.","Sculk sensors: detectan su movimiento real vs las voces falsas que emite.","Leche: limpia el debuff de Hemorragia que aplican sus estocadas."],
  drops:["Buena cantidad de esmeraldas, armas y armadura encantadas.","10%: Estoque de la Locura (irrompible, ligerisimo, con Punzada rapida y Rafaga de estocadas)."] });

mob({ id:"the-whisperer", name:"Susurrante", en:"The Whisperer", dungeon:"descent-madness", rank:"normal",
  hp:"20 (10h)", armor:"0", dmg:"6 (3h)",
  desc:"Presencia invisible que solo se delata al susurrar. Usa fragmentos de conversacion para atraer al jugador a trampas.",
  behavior:["Invisible hasta que habla; su voz proviene de una direccion falsa.","Guia hacia trampas con susurros.","La Vela de Cordura lo revela y lo hace vulnerable."],
  vanilla:["Antorchas: sostenidas revelan su silueta como un destello en el aire.","Sculk sensors: detectan su posicion real (no la falsa de su voz).","Leche: limpia la desorientacion que causan sus susurros."],
  drops:["Eco susurrado (componente sonoro)."] });

mob({ id:"faceless-pilgrim", name:"Peregrino sin Rostro", en:"Faceless Pilgrim", dungeon:"descent-madness", rank:"normal",
  hp:"24 (12h)", armor:"1", dmg:"6 (3h)",
  desc:"Figura encapuchada que copia la apariencia de otros jugadores o NPCs conocidos, sembrando la duda sobre quien es real en multijugador.",
  behavior:["Adopta el aspecto de un companero o NPC.","Se mezcla entre el grupo hasta atacar.","Al morir revela su rostro vacio."],
  vanilla:["Comida/pan: ofrecerle comida antes de que ataque lo pacifica (acepta el regalo y se revela).","Catalejo: revela detalles incorrectos en su disfraz a distancia.","Bolas de nieve: la nieve queda pegada en el real pero no en la copia (marca al impostor)."],
  drops:["Mascara vacia."] });

mob({ id:"the-crowd", name:"La Multitud", en:"The Crowd", dungeon:"descent-madness", rank:"normal",
  hp:"6 c/u (enjambre)", armor:"0", dmg:"3 (1.5h)",
  desc:"Marea de manos y sombras que emergen del suelo y las paredes para agarrar y frenar.",
  behavior:["Agarran y ralentizan en masa.","Surgen de superficies oscuras; la luz las reduce.","Vulnerables a area y fuego."],
  vanilla:["Antorchas: colocadas eliminan las superficies oscuras de las que emergen.","Pedernal y acero: prende fuego que las elimina en masa.","Cubo de agua: las dispersa momentaneamente.","Cubo de lava: crea una barrera que no pueden cruzar."],
  drops:["Sombra coagulada."] });

mob({ id:"rotten-memory", name:"Recuerdo Podrido", en:"Rotten Memory", dungeon:"descent-madness", rank:"normal",
  hp:"28 (14h)", armor:"0", dmg:"7 (3.5h) + nausea",
  desc:"Recreacion distorsionada de un ser querido o aliado del jugador, deformada por la locura.",
  behavior:["Aparenta ser un NPC o rescate y muta al acercarse.","Aplica Nausea y vision deformada.","Se disipa si se le ignora el tiempo suficiente."],
  vanilla:["Leche: limpia la Nausea y vision deformada inmediatamente.","Catalejo: revela la distorsion antes de acercarse (se ve mutado con zoom).","Comida/pan: ofrecida hace que la ilusion se complete revelando que es falsa (el real no come)."],
  drops:["Fragmento de recuerdo corrompido."] });

mob({ id:"choir-of-cries", name:"Coro de Auxilio", en:"The Choir of Cries", dungeon:"descent-madness", rank:"elite",
  hp:"100 (50h)", armor:"2", dmg:"Sonico 9 (4.5h)",
  desc:"Amasijo de bocas que emite decenas de voces de auxilio superpuestas, saturando el sistema de resonancia.",
  behavior:["Sobrecarga el sistema de ruido: atrae mobs de otras salas.","Ataques sonicos en area que desorientan.","Silenciarlo (o el sigilo) reduce su poder."],
  vanilla:["Note blocks: reproducir una contra-frecuencia lo aturde y reduce su volumen.","Bolas de nieve: impactos en las bocas las cierran temporalmente (3s por boca).","Cubo de agua: lo ahoga brevemente silenciandolo por completo 5s.","Leche: limpia los debuffs sonicos."],
  drops:["Nucleo del coro (resonancia).","Fragmento resonante."] });

mob({ id:"the-one-who-repeats", name:"El Que Repite", en:"The One Who Repeats", dungeon:"descent-madness", rank:"elite",
  hp:"140 (70h)", armor:"3", dmg:"10 (5h)",
  desc:"Entidad ligada al bucle de los pasillos: cuando esta presente, la sala se reinicia y todo lo hecho parece deshacerse.",
  behavior:["Fuerza reinicios de la sala (usa el pasillo infinito).","Cada repeticion cambia un detalle: la pista esta en lo que difiere.","Al derrotarlo, el bucle se rompe y se abre la ruta real."],
  vanilla:["Brujula de recuperacion: senala su posicion real dentro del bucle.","Antorchas: colocadas como marcadores persisten entre repeticiones (la clave).","Catalejo: revela el detalle que cambia entre iteraciones.","Bolas de nieve: marcadores que persisten entre bucles si impactan en ciertos puntos."],
  drops:["Ancla de bucle (evita un reinicio).","Cristal de Escape (posible)."] });

mob({ id:"semblance", name:"Semblante", en:"The Semblance", dungeon:"descent-madness", rank:"miniboss",
  hp:"280 (140h)", armor:"4", dmg:"12 (6h)",
  desc:"Un espejo viviente de las peores decisiones del jugador: combina sus errores, su equipo y sus tacticas mas predecibles para castigarlo con ellas.",
  behavior:["Aprende y repite los patrones de ataque del jugador.","Cambia de forma segun como lucha el jugador.","Solo es vulnerable cuando el jugador rompe su propia rutina."],
  vanilla:["Bolas de nieve/huevos: usar ataques no-convencionales que no puede predecir lo confunde.","Escudo: alternar entre atacar y defender rompe su prediccion.","Cana de pescar: lo arrastra fuera de posicion (no puede copiar algo fisico).","Items variados: cambiar de tactica constantemente usando items distintos es la clave."],
  drops:["Reflejo lucido (accesorio).","Pieza de equipo espejada."] });

mob({ id:"broken-sanity", name:"La Cordura Rota", en:"The Broken Sanity", dungeon:"descent-madness", rank:"boss",
  hp:"1400 (fases)", armor:"5", dmg:"15-22",
  desc:"JEFE. La personificacion de la locura del descenso: una entidad cambiante que ataca la percepcion tanto como el cuerpo.",
  behavior:["Fase 1: proyecta clones falsos del jefe; solo uno hace dano real.","Fase 2: distorsiona la sala y las voces guian hacia trampas del propio combate.","Fase 3: invierte controles percibidos y satura con recuerdos hostiles; la Vela de Cordura marca lo real.","Ignorar los enganos y confiar en patrones aprendidos es la clave."],
  vanilla:["Leche: ESENCIAL entre fases para limpiar las distorsiones de percepcion acumuladas.","Catalejo: identifica al clon real en Fase 1 (tiene una sombra distinta).","Brujula de recuperacion: senala la salida real cuando la sala se distorsiona en Fase 2.","Note blocks: la contra-frecuencia aturde sus voces falsas en Fase 2.","Antorchas: colocadas en posiciones fijas ayudan a orientarse cuando invierte controles en Fase 3.","Bolas de nieve: impactar en los clones; solo el real reacciona al impacto (pista de Fase 1).","Sculk sensors: detectan cual clon se mueve realmente (el real pisa, los falsos flotan)."],
  drops:["Corazon de la Locura (reliquia legendaria).","Estoque de la Locura mejorado (posible).","Cristal de lucidez."] });


/* ===== THE PRIMORDIAL TOWER ===== */
mob({ id:"primal-ember", name:"Ascua Primigenia", en:"Primal Ember", dungeon:"primordial-tower", rank:"normal",
  hp:"16 (8h)", armor:"0", dmg:"Fuego 6 (3h)",
  desc:"Chispa viva del piso del fuego que prende todo lo que toca, incluido el aceite y al jugador aceitado. Fragil pero peligrosa en grupo.",
  behavior:["Incendia al contacto y prende charcos de aceite.","Se apaga con agua (muere).","Mas rapida en el piso del fuego."],
  vanilla:["Cubo de agua: la mata instantaneamente (un cubo, un kill).","Bolas de nieve: dano significativo (2 bolas la matan).","Cubo de polvo de nieve: la apaga y crea zona segura temporal.","Polvo de hueso: limpia el aceite del jugador antes de que ella lo prenda."],
  drops:["Brasa primordial (reactivo)."] });

mob({ id:"ancestral-droplet", name:"Gota Ancestral", en:"Ancestral Droplet", dungeon:"primordial-tower", rank:"normal",
  hp:"18 (9h)", armor:"0", dmg:"5 (2.5h) + arrastre",
  desc:"Masa de agua viva del piso acuatico que arrastra al jugador con corrientes y puede apagar sus fuentes de luz y fuego.",
  behavior:["Genera corrientes que empujan y arrastran.","Apaga antorchas y al jugador ardiendo.","Se congela y se vuelve fragil con frio."],
  vanilla:["Cubo de polvo de nieve: la congela instantaneamente (shatter con un golpe).","Cubo de lava: la evapora causando dano masivo.","Honey bottles: inmunidad al arrastre de sus corrientes."],
  drops:["Esencia de agua."] });

mob({ id:"old-gust", name:"Rafaga Antigua", en:"Old Gust", dungeon:"primordial-tower", rank:"normal",
  hp:"14 (7h)", armor:"0", dmg:"3 (1.5h) + empuje",
  desc:"Torbellino del piso del aire que empuja al jugador entre plataformas flotantes.",
  behavior:["Empuja hacia los huecos entre plataformas.","Puede elevar y soltar al jugador.","Se disipa con proyectiles pesados."],
  vanilla:["Escudo: anula su empuje completamente.","Bolas de nieve: 3 impactos la dispersan.","Honey bottles: inmunidad al deslizamiento post-empuje."],
  drops:["Esencia de aire."] });

mob({ id:"primordial-pebble", name:"Guijarro Primordial", en:"Primordial Pebble", dungeon:"primordial-tower", rank:"normal",
  hp:"26 (13h)", armor:"6", dmg:"6 (3h)",
  desc:"Roca viva del piso de la tierra que atrapa los pies del jugador con raices de piedra.",
  behavior:["Inmoviliza brevemente con roca emergente.","Muy resistente; debil al contundente.","Se endurece formando muros temporales."],
  vanilla:["TNT: lo destruye y sus muros de una explosion.","Cubo de agua: disuelve sus raices de piedra liberando al jugador atrapado.","Piston de redstone: lo empuja fuera de posicion rompiendo su trampa."],
  drops:["Esencia de tierra."] });

mob({ id:"genesis-spark", name:"Chispa de Genesis", en:"Genesis Spark", dungeon:"primordial-tower", rank:"normal",
  hp:"20 (10h)", armor:"0", dmg:"Rayo 8 (4h)",
  desc:"Descarga viva del genesis que salta entre superficies metalicas y agua. Encadena rayos entre enemigos y charcos.",
  behavior:["Sus rayos rebotan entre metal y agua.","Encadena dano si el jugador esta mojado.","Estatica lenta pero de reaccion rapida."],
  vanilla:["Cubo de polvo de nieve: no conduce electricidad (crea zona segura).","Escudo: si es de madera, no conduce; bloquea el rayo.","Leche: limpia el debuff de electrificacion."],
  drops:["Chispa de genesis (reactivo de rayo)."] });

mob({ id:"unstable-golem", name:"Golem Elemental Inestable", en:"Unstable Elemental Golem", dungeon:"primordial-tower", rank:"elite",
  hp:"150 (75h)", armor:"6", dmg:"12 (6h)",
  desc:"Coloso que cicla entre fuego, agua, aire y tierra, cambiando sus ataques y debilidades con cada mutacion.",
  behavior:["Cambia de elemento cada pocos segundos.","Usar el elemento opuesto al activo lo dana de mas.","En su cambio de fase queda vulnerable un instante."],
  vanilla:["Cubo de agua: dano x3 durante fase de fuego.","Pedernal y acero: dano x3 durante fase de tierra (prende raices).","Bolas de nieve: dano x3 durante fase de aire (peso lo desestabiliza).","Cubo de lava: dano x3 durante fase de agua.","Catalejo: predice el siguiente elemento por el color del nucleo."],
  drops:["Nucleo inestable (multi-elemento).","Fragmento de afinidad."] });

mob({ id:"cycle-custodian", name:"Custodio de los Ciclos", en:"Cycle Custodian", dungeon:"primordial-tower", rank:"elite",
  hp:"130 (65h)", armor:"5", dmg:"11 (5.5h)",
  desc:"Guardian que controla el orden de los elementos del piso y puede forzar un cambio de ciclo.",
  behavior:["Fuerza el evento de cambio de ciclo del piso.","Protege los altares elementales.","Vulnerable cuando su ciclo queda desincronizado."],
  vanilla:["Items elementales opuestos: usar el item incorrecto para el ciclo actual lo desincroniza.","Cubo de agua en piso de fuego: lo desincroniza y lo debilita.","Polvo de hueso: estimula cambio de ciclo prematuro (vulnerable durante la transicion)."],
  drops:["Engranaje del ciclo.","Sello elemental."] });

mob({ id:"first-warden", name:"El Primer Guardian", en:"The First Warden", dungeon:"primordial-tower", rank:"miniboss",
  hp:"330 (165h)", armor:"7", dmg:"13 (6.5h)",
  desc:"El mas antiguo de los custodios, hecho de los cuatro elementos en equilibrio. Domina dos elementos a la vez.",
  behavior:["Combina dos elementos (ej: vapor = agua+fuego) en cada ataque.","Cambia el elemento dominante de la arena.","Solo es vulnerable al elemento que no esta usando."],
  vanilla:["Catalejo: revela que dos elementos esta usando (aura de color).","Cubo de agua + Bolas de nieve: combinacion contra fuego+tierra.","Pedernal + TNT: combinacion contra agua+aire.","Escudo: bloquea los ataques combinados parcialmente."],
  drops:["Anillo de Afinidad (eleccion de elemento).","Fragmento del genesis."] });

mob({ id:"primordial-concord", name:"El Concilio Primordial", en:"The Primordial Concord", dungeon:"primordial-tower", rank:"boss",
  hp:"1500 (4 fases)", armor:"8", dmg:"16-22",
  desc:"JEFE. Un ser unico que encarna los cuatro elementos primordiales, combatido por fases en la cuspide banada por el rayo.",
  behavior:["Fase Tierra: atrapa y aplasta con columnas emergentes.","Fase Agua: inunda y arrastra la arena, subiendo el nivel.","Fase Aire: reduce la arena a plataformas flotantes y empuja.","Fase Fuego: prende el suelo y el aceite; culmina con el rayo del genesis."],
  vanilla:["Fase Tierra: Cubo de agua disuelve columnas, TNT las destruye explosivamente.","Fase Agua: Cubo de polvo de nieve congela secciones creando plataformas, Honey bottles inmunidad a corrientes.","Fase Aire: Escudo anula empuje, Cubo de polvo de nieve crea plataformas de emergencia.","Fase Fuego: Cubo de agua apaga suelo, Polvo de hueso limpia aceite, Pociones de resistencia al fuego.","General: Perla de ender para escapar entre fases, Leche para limpiar debuffs elementales acumulados."],
  drops:["Corazon del Genesis (reliquia legendaria).","Cetro de los Ciclos mejorado (posible).","Esencia primordial pura."] });


/* ===== THE SOLAR PALACE ===== */
mob({ id:"solar-dancer", name:"Danzante Solar", en:"Solar Dancer", dungeon:"solar-palace", rank:"normal",
  hp:"22 (11h)", armor:"1", dmg:"6 (3h)",
  desc:"Sacerdote agil consagrado al sol que combate girando cintas ardientes. Se fortalece bajo la luz directa y flaquea en la sombra.",
  behavior:["Mas rapido y fuerte bajo luz solar directa.","Deja estelas de fuego breves al girar.","Vulnerable si se lo arrastra a la sombra."],
  vanilla:["Cubo de agua: apaga sus estelas de fuego y lo debilita.","Cana de pescar: lo arrastra hacia zonas de sombra (donde es vulnerable).","Cubo de polvo de nieve: enfria su zona eliminando su buff de luz."],
  drops:["Cinta solar (textil igneo)."] });

mob({ id:"gilded-guard", name:"Guardia Dorado", en:"Gilded Guard", dungeon:"solar-palace", rank:"normal",
  hp:"40 (20h)", armor:"8", dmg:"8 (4h)",
  desc:"Guardian de armadura dorada brunida que refleja la luz y ciega al atacante que golpea de frente bajo el sol.",
  behavior:["Su armadura refleja luz: golpear de frente al sol aplica Ceguera al jugador.","Alta armadura frontal; debil por la espalda.","Forma muros de escudos con otros guardias."],
  vanilla:["Items de oro: mostrar un lingote de oro lo soborna (se aparta 30s sin atacar).","Cubo de agua: empana su armadura temporalmente (no refleja luz, no ciega).","Escudo: protege de la Ceguera por reflejo al acercarse.","Cana de pescar: lo gira exponiendo su espalda vulnerable."],
  drops:["Placa dorada (forja).","Escudo brunido."] });

mob({ id:"brazier-igneous", name:"Igneo de Brasero", en:"Brazier Igneous", dungeon:"solar-palace", rank:"normal",
  hp:"18 (9h)", armor:"0", dmg:"Fuego 7 (3.5h)",
  desc:"Criatura de fuego que nace de los braseros del palacio. Prende el aceite y al jugador aceitado.",
  behavior:["Emerge de braseros encendidos; prende aceite y jugador.","Se apaga con agua o cerrando su brasero.","Explota en chispas al morir."],
  vanilla:["Cubo de agua: lo mata instantaneamente y apaga su brasero.","Bolas de nieve: 2 impactos lo matan.","Cubo de polvo de nieve: lo congela y apaga sin explosion de chispas.","Polvo de hueso: limpia el aceite del jugador antes de que lo prenda."],
  drops:["Brasa dorada."] });

mob({ id:"bronze-falcon", name:"Halcon de Bronce", en:"Bronze Falcon", dungeon:"solar-palace", rank:"normal",
  hp:"16 (8h)", armor:"4", dmg:"7 (3.5h) picado",
  desc:"Automata alado de bronce que patrulla los patios y se lanza en picado concentrando luz solar en un destello cegador.",
  behavior:["Ataca en picado desde el aire.","Concentra luz para cegar antes de impactar.","Fragil una vez en tierra tras fallar el picado."],
  vanilla:["Ballesta: lo derriba en el aire antes del picado (un disparo critico).","Escudo: bloquea el destello cegador.","Bolas de nieve: lanzadas al aire lo desequilibran y fuerzan el aterrizaje."],
  drops:["Engranaje de bronce.","Pluma metalica."] });

mob({ id:"burning-standard", name:"Portaestandarte Ardiente", en:"Burning Standard-bearer", dungeon:"solar-palace", rank:"normal",
  hp:"30 (15h)", armor:"3", dmg:"6 (3h)",
  desc:"Portador de un estandarte solar que potencia y enardece a los aliados cercanos.",
  behavior:["Otorga dano y velocidad extra a los enemigos cercanos.","No es muy agresivo; se protege detras de aliados.","Su estandarte marca la prioridad de objetivo."],
  vanilla:["Cubo de agua: apaga el estandarte desactivando el buff (prioridad maxima).","Ballesta: elimina el estandarte a distancia sin acercarse a los guardias.","Cana de pescar: arranca el estandarte de sus manos."],
  drops:["Estandarte solar (decorativo/funcional)."] });

mob({ id:"solar-mirror-colossus", name:"Coloso de Espejos Solares", en:"Solar Mirror Colossus", dungeon:"solar-palace", rank:"elite",
  hp:"150 (75h)", armor:"7", dmg:"Haz 13 (6.5h)",
  desc:"Automata cubierto de espejos que redirige haces de luz solar mortales por la sala.",
  behavior:["Dispara y redirige haces de luz que incineran en linea.","Los mismos haces pueden usarse para resolver puzles o danarlo.","Sus espejos son partes rompibles que reducen su alcance."],
  vanilla:["Escudo: REFLEJA sus haces de luz de vuelta a el (dano critico).","Bolas de nieve: rompen sus espejos individuales (reduce opciones de redireccion).","Cubo de agua: empana todos sus espejos temporalmente (no puede disparar 10s).","Cana de pescar: gira sus espejos haciendo que se dane a si mismo."],
  drops:["Espejo ustorio (lente de arma).","Nucleo solar."] });

mob({ id:"dawn-priestess", name:"Sacerdotisa del Alba", en:"Dawn Priestess", dungeon:"solar-palace", rank:"elite",
  hp:"120 (60h)", armor:"3", dmg:"Luz 10 (5h)",
  desc:"Sanadora del culto solar que cura a sus aliados y purga la oscuridad.",
  behavior:["Cura a aliados y se cura bajo luz directa.","Lanza destellos que danan y ciegan.","En la sombra pierde su curacion y queda vulnerable."],
  vanilla:["Cubo de agua: empana espejos cercanos creando sombra (la debilita).","Escudo: bloquea los destellos cegadores.","Cana de pescar: la arrastra a zonas de sombra donde es vulnerable.","Polvo de hueso: curiosamente, esparcido sobre ella la satura de vida y la aturde por sobreestimulacion."],
  drops:["Caliz del alba (accesorio de curacion).","Incienso solar."] });

mob({ id:"noon-guardian", name:"El Guardian del Mediodia", en:"The Noon Guardian", dungeon:"solar-palace", rank:"miniboss",
  hp:"340 (170h)", armor:"8", dmg:"14 (7h)",
  desc:"Coloso ceremonial que alcanza su maximo poder al mediodia simulado del observatorio.",
  behavior:["Aumenta la luz de la arena (Cenit) para fortalecerse.","Barre la sala con un haz solar giratorio.","Provocar un eclipse (apagar focos) lo debilita drasticamente."],
  vanilla:["Cubo de agua: apaga los focos provocando el eclipse (lo debilita enormemente).","Escudo: refleja parcialmente su haz solar giratorio.","Cana de pescar: gira los espejos del observatorio desviando la luz.","Cubo de polvo de nieve: crea niebla temporal que bloquea la luz.","Items de oro: ofrendarlos en el altar solar distrae su atencion ceremonial."],
  drops:["Corona del mediodia (accesorio).","Sello del trono solar."] });

mob({ id:"chained-helios", name:"Helios Encadenado", en:"The Chained Helios", dungeon:"solar-palace", rank:"boss",
  hp:"1450 (fases)", armor:"7", dmg:"16-22 + fuego",
  desc:"JEFE. Una entidad solar cautiva, un pequeno sol vivo, encadenada al trono del palacio y explotada como fuente de poder.",
  behavior:["Fase 1: lanza llamaradas y esferas solares rodantes por la arena.","Fase 2: rompe cadenas y desata haces de luz que hay que bloquear con espejos.","Fase 3: entra en Cenit total incendiando el aceite; provocar un eclipse abre su ventana letal.","Sus cadenas y nucleo son partes independientes; romperlas altera sus ataques."],
  vanilla:["Polvo de hueso: CRITICO para limpiar el aceite del suelo antes de Fase 3 (30s de proteccion).","Cubo de agua: apaga llamaradas en Fase 1, provoca el eclipse en Fase 3 (vertido en el altar central).","Escudo: refleja haces de luz en Fase 2 de vuelta (dano al jefe).","Cana de pescar: redirige espejos para bloquear/redirigir haces en Fase 2.","Cubo de polvo de nieve: crea zonas frias de refugio durante el Cenit total.","Pociones de resistencia al fuego: ventana de invulnerabilidad durante las peores llamaradas.","Ballesta: rompe sus cadenas a distancia en momentos criticos.","Items de oro: ofrendados en los altares laterales debilitan su conexion con el trono."],
  drops:["Corazon Solar (reliquia legendaria).","Lanza del Mediodia mejorada (posible).","Fragmento de sol."] });


/* ---------- TRAMPAS ---------- */
CDE_DATA.traps = [];
function trap(o){ CDE_DATA.traps.push(o); }

trap({ name:"Mecanica del aceite", category:"Superficie", dungeon:["solar-palace","primordial-tower","descent-madness"],
  desc:"Charco extremadamente plano que no fluye ni ralentiza: es una superficie peligrosa. Al pisarlo, el jugador queda aceitado. Si toca fuego, arde el doble de tiempo; el agua no lo apaga (lo empeora y duplica el dano).",
  vanilla:["Polvo de hueso: limpia el aceite si aun no arde Y da 30 min de proteccion preventiva (la contramedida principal).","Leche: limpia el status aceitado como cualquier otro efecto.","Pedernal y acero: prende el aceite OFENSIVAMENTE contra enemigos que pisan los charcos.","Cubo de polvo de nieve: enfria el aceite ardiendo (no lo apaga pero reduce dano un 50%)."],
  config:["Duracion del fuego","Multiplicador con agua","Proteccion de polvo de hueso"] });

trap({ name:"Lampara de aceite", category:"Ambiental", dungeon:["solar-palace","descent-madness"],
  desc:"Lampara colgada del techo sobre un charco de aceite, con una zona de deteccion invisible de 3 bloques. Cada vez que un jugador entra hay un 10% de que caiga, impacte y prenda el aceite.",
  vanilla:["Ballesta: disparo a la cadena la tira prematuramente cuando no hay nadie debajo (desactiva).","Bolas de nieve: mismo efecto que ballesta a corta distancia.","Polvo de hueso: aplicado al charco debajo previene la ignicion aunque caiga la lampara.","Cubo de agua: vertido sobre el charco lo diluye pero CUIDADO si ya hay aceite (empeora si ya arde)."],
  config:["Probabilidad de caida","Ancho de zona","Reinicio"] });

trap({ name:"Paredes aplastantes", category:"Aplastamiento", dungeon:["worldbearer","hollow-leviathan"],
  desc:"Dos paredes con hitbox fisica real avanzan lentamente hacia el centro para aplastar. Empujan entidades y bloquean proyectiles.",
  vanilla:["Bloques de piedra/obsidiana: colocados en el centro las detienen (actuan como tope).","Perla de ender: teletransporte al otro lado antes de que cierren.","Cubo de agua: lubrica el suelo permitiendo deslizarse mas rapido hacia la salida.","Piston de redstone: empuja bloques que frenan las paredes."],
  config:["Velocidad","Dano","Tiempo de reaccion","Metodo de escape","Reinicio"] });

trap({ name:"Estatua o pilar lanzallamas", category:"Fuego a distancia", dungeon:["solar-palace","primordial-tower"],
  desc:"Estatua o pilar retractil que se despliega, se vuelve solido y dispara rafagas de fuego en hasta 4 direcciones.",
  vanilla:["Cubo de agua: apaga la llama y desactiva temporalmente el pilar (15s).","Escudo: bloquea la rafaga de fuego completamente.","Bolas de nieve: impacto en la boca del pilar lo atasca un ciclo.","Cubo de polvo de nieve: crea barrera de vapor que bloquea la linea de fuego."],
  config:["Direcciones","Dano","Duracion de rafaga","Interaccion con aceite"] });

trap({ name:"Trampa de derrumbamiento", category:"Estructural", dungeon:["worldbearer","veiled-peak","hollow-leviathan"],
  desc:"El suelo (o el techo) se derrumba progresivamente. Da 3-6 s para escapar; afecta a un maximo de ~20 bloques.",
  vanilla:["Escudo: protege de impactos de escombros (reduce dano un 80%).","Perla de ender: escape instantaneo a zona segura.","Bloques de piedra: colocados como pilares preventivos evitan el colapso total.","Cubo de agua: la cascada de agua frena la caida de escombros levemente."],
  config:["Suelo o techo","Tiempo de aviso","Tamano","Destino de caida","Permanencia de escombros"] });

trap({ name:"Skeever Trap (jaula que cae)", category:"Encierro", dungeon:["hollow-leviathan","descent-madness"],
  desc:"Jaula pesada que cae del techo y encierra al jugador. 500 HP y 50% de reduccion de dano. Tras 3 min se llena de gas venenoso.",
  vanilla:["Ballesta: disparo al accionador desde dentro libera la jaula.","Bolas de nieve: activar el accionador a distancia desde dentro.","Perla de ender: teletransporte fuera de la jaula (si hay linea de vision).","Leche: limpia el veneno del gas.","TNT: destruye la jaula rapidamente pero con autodano."],
  config:["Tiempo antes del gas","Nivel maximo","Proyectiles validos","Posicion del accionador"] });

trap({ name:"Tablero de pinchos movil", category:"Perforacion", dungeon:["worldbearer","primordial-tower"],
  desc:"Plataforma de pinchos con hitbox fisica que se desplaza para aplastar y perforar. Hace 16 de dano (8h).",
  vanilla:["Escudo: reduce el dano de perforacion un 60%.","Bloques de piedra: detienen la plataforma al colocarlos en su ruta.","Perla de ender: escape de emergencia.","Cubo de agua: ralentiza el mecanismo por friccion hidraulica."],
  config:["Tamano","Orientacion","Velocidad","Recorrido","Activador"] });

trap({ name:"Bola rodante colosal", category:"Persecucion", dungeon:["solar-palace","mirror-castle","hollow-leviathan","descent-madness","primordial-tower"],
  desc:"Gran masa solida que rueda por gravedad siguiendo la pendiente. Apariencia tematica por dungeon.",
  vanilla:["Cana de pescar: engancha el mecanismo de liberacion para activarla cuando conviene (usar contra enemigos).","Escudo: reduce empuje al ser golpeado (no el dano total).","Perla de ender: esquiva lateral instantanea.","Cubo de agua: ralentiza la bola en superficies mojadas.","TNT: destruye bolas de piedra/organicas (no las metalicas)."],
  config:["Modelo/tamano","Peso","Velocidad/aceleracion","Dano","Recorrido","Reinicio","Num de bolas"] });

trap({ name:"Pasillo infinito de la lampara magica", category:"Espacial", dungeon:["descent-madness","mirror-castle"],
  desc:"Trampa espacial: el jugador avanza pero regresa imperceptiblemente al mismo tramo. Mantenido por una lampara magica en el techo.",
  vanilla:["Bolas de nieve: rompen la lampara a distancia (escapar sin acercarse).","Ballesta: misma funcion con mayor alcance y precision.","Antorchas: colocadas como marcadores revelan la repeticion (el marcador desaparece al reiniciar).","Brujula de recuperacion: apunta a la salida real ayudando a orientarse."],
  config:["Longitud/segmentos","Solucion","Cambios por repeticion","Jugadores afectados"] });

trap({ name:"Trampa oscilante modular", category:"Impacto", dungeon:["primordial-tower","solar-palace","worldbearer"],
  desc:"Sistema comun para hachas gigantes, mazas, martillos, bolas de pinchos, cuchillas y troncos suspendidos.",
  vanilla:["Escudo: bloquea el impacto con reduccion de empuje.","Cana de pescar: engancha el pendulo y lo frena brevemente (ventana de paso).","Cubo de agua: mojando la zona reduce velocidad del pendulo.","Bolas de nieve: lanzadas al mecanismo superior lo desincronizan del ritmo."],
  config:["Modelo","Movimiento","Dano/empuje","Fuego","Activador","Reinicio"] });

trap({ name:"Cuchillas y puas retractiles de pared", category:"Corte", dungeon:["veiled-peak","primordial-tower","hollow-leviathan"],
  desc:"Familia de trampas que salen de las paredes: cuchillas deslizantes, puas retractiles en ciclos o puas fijas.",
  vanilla:["Catalejo: revela el patron de extension/retraccion desde la distancia.","Escudo: bloquea dano de cuchillas frontales.","Bloques de piedra: colocados en los agujeros bloquean la extension.","Cubo de polvo de nieve: congela el mecanismo brevemente."],
  config:["Tipo de filo","Recorrido","Tiempo de extension/retraccion","Dano/empuje","Activador"] });

trap({ name:"Cadena tensada", category:"Barrido", dungeon:["worldbearer","veiled-peak"],
  desc:"Una cadena atraviesa una zona a gran velocidad para cortar, derribar, empujar, arrastrar o enganchar.",
  vanilla:["Escudo: bloquea el golpe de la cadena sin empuje.","Cana de pescar: engancha la cadena y la redirige.","Honey bottles: inmunidad al arrastre si la cadena engancha."],
  config:["Trayectoria","Velocidad","Dano/empuje","Tiempo de agarre","Activador"] });

trap({ name:"Cofre de peso muerto", category:"Engano", dungeon:["worldbearer","solar-palace"],
  desc:"Parece un cofre valioso, pero al abrirlo inclina el suelo y hace deslizar entidades hacia un precipicio.",
  vanilla:["Honey bottles: inmunidad al deslizamiento (contramedida perfecta).","Escudo: reduce velocidad de deslizamiento.","Perla de ender: teletransporte de vuelta al borde seguro.","Cana de pescar: abre el cofre a distancia sin estar encima."],
  config:["Angulo","Velocidad","Peso necesario","Loot","Reinicio"] });

trap({ name:"Puerta devoradora", category:"Organo vivo", dungeon:["hollow-leviathan","descent-madness"],
  desc:"Parece una puerta normal, pero es un organismo vivo: se abre como una boca, absorbe, muerde, sujeta, traga y escupe.",
  vanilla:["Pedernal y acero: cauteriza la boca PERMANENTEMENTE (cerrada para siempre, no se puede usar como paso).","Cizallas: cortan los tendones de la mandibula (se abre sin morder, paso seguro).","Huevos: lanzados dentro la sacian (no muerde durante 30s, paso libre).","Escudo: bloquea la mordida si se activa justo al entrar."],
  config:["Fuerza de absorcion","Dano","Tiempo de agarre","Metodo de liberacion","Destino"] });

trap({ name:"Trampas vivas", category:"Organo vivo", dungeon:["hollow-leviathan","descent-madness"],
  desc:"Familia organica: paredes que respiran, raices/tentaculos que agarran, bocas ocultas, suelos digestivos, capullos y ojos organicos.",
  vanilla:["Cizallas: cortan tentaculos y membranas instantaneamente (la herramienta clave).","Pedernal y acero: cauteriza tejido vivo impidiendo regeneracion.","Polvo de hueso: estimula crecimiento descontrolado que debilita la estructura.","Cubo de agua: neutraliza suelos digestivos.","Antorchas: ciegan ojos organicos y ahuyentan parasitos."],
  config:["Area de deteccion","Dano/empuje","Duracion del agarre","Regeneracion","Reinicio"] });

trap({ name:"Sala de espejos giratorios", category:"Ilusion", dungeon:["mirror-castle"],
  desc:"Sala cuyos paneles de espejo giran periodicamente reorganizando el reflejo del recorrido.",
  vanilla:["Bolas de nieve: rompen paneles concretos reduciendo la complejidad del puzle.","Catalejo: revela cual es la salida real (no imita al jugador si se mira con zoom).","Cubo de agua: empana paneles temporalmente revelando cuales son reales."],
  config:["Velocidad de giro","Num de paneles","Salida real fija o cambiante"] });

trap({ name:"Reflejo hostil", category:"Ilusion", dungeon:["mirror-castle","descent-madness"],
  desc:"Al pasar frente a ciertos espejos, el reflejo del jugador sale del cristal y ataca con su mismo equipo.",
  vanilla:["Cubo de agua: empanar el espejo origen PREVIENE que salga el reflejo.","Bolas de nieve: romper el espejo origen mata al reflejo ya activo.","Escudo: bloquea los ataques del reflejo (el reflejo no puede bloquear simultaneamente)."],
  config:["Duracion del reflejo","Dano","Espejo vinculado"] });

trap({ name:"Ventisca cegadora", category:"Clima", dungeon:["veiled-peak"],
  desc:"Rafaga subita de nieve que reduce la vision casi a cero y empuja en una direccion.",
  vanilla:["Escudo: reduce el empuje un 70%.","Antorchas: crean burbujas de visibilidad de 3 bloques alrededor.","Honey bottles: inmunidad al deslizamiento en suelo helado durante la ventisca.","Cubo de polvo de nieve: paradojicamente, crear mas nieve satura la ventisca y la acorta."],
  config:["Intensidad","Duracion","Direccion del empuje","Radio de faro"] });

trap({ name:"Placa de hielo deslizante", category:"Movimiento", dungeon:["veiled-peak","primordial-tower"],
  desc:"Tramos de hielo que anulan la friccion: el jugador se desliza sin control hacia precipicios.",
  vanilla:["Honey bottles: INMUNIDAD completa al deslizamiento (la contramedida definitiva).","Pedernal y acero: derrite una seccion de hielo creando paso seguro.","Cubo de lava: derrite una seccion grande.","Cubo de polvo de nieve: crea capa de nieve sobre el hielo que da traccion."],
  config:["Longitud","Friccion","Destino del deslizamiento"] });

trap({ name:"Rafaga de viento de precipicio", category:"Empuje", dungeon:["veiled-peak","primordial-tower"],
  desc:"Corrientes ritmicas junto a los bordes que empujan hacia el vacio.",
  vanilla:["Escudo: reduce empuje un 70% (esencial en las cornisas).","Honey bottles: evita el deslizamiento post-empuje.","Bolas de nieve: lanzadas contra la corriente revelan su patron visual.","Bloques de piedra: colocados como barandilla temporal."],
  config:["Fuerza","Cadencia","Direccion"] });

trap({ name:"Suelo que desaparece", category:"Percepcion", dungeon:["descent-madness"],
  desc:"Secciones de suelo se vuelven ilusorias sin previo aviso: parecen solidas pero no lo son.",
  vanilla:["Bolas de nieve: rebotan en suelo real, ATRAVIESAN el ilusorio (la forma de testear).","Huevos: mismo efecto diagnostico que bolas de nieve.","Perla de ender: escape si caes por suelo falso.","Cubo de polvo de nieve: la nieve se asienta en suelo real pero no en el falso (marca visual)."],
  config:["Patron","Aviso (si/no)","Radio de revelado"] });

trap({ name:"Voces falsas y engano sensorial", category:"Sonido", dungeon:["descent-madness"],
  desc:"Reproduce voces de auxilio o de companeros desde direcciones falsas para atraer a trampas.",
  vanilla:["Sculk sensors: detectan la FUENTE real del sonido (no la falsa percibida).","Note blocks: crear sonido propio satura el sistema y silencia las voces falsas temporalmente.","Leche: limpia el debuff de desorientacion sonora."],
  config:["Idioma (ES/EN)","Direccion falsa","Frecuencia"] });

trap({ name:"Columnas elementales", category:"Elemental", dungeon:["primordial-tower"],
  desc:"Columnas que emergen del suelo cargadas del elemento del piso siguiendo patrones.",
  vanilla:["Item elemental opuesto: agua vs fuego, nieve vs aire, TNT vs tierra.","Catalejo: revela el patron de emergencia antes de entrar a la zona.","Escudo: bloquea el impacto de la columna si no puedes esquivarla."],
  config:["Elemento","Patron","Altura","Dano por afinidad"] });

trap({ name:"Rayo solar concentrado", category:"Luz", dungeon:["solar-palace"],
  desc:"Haz de luz solar redirigido por espejos que incinera todo lo que cruza su linea.",
  vanilla:["Escudo: REFLEJA el haz si se posiciona correctamente (arma contra enemigos).","Cana de pescar: gira los espejos del recorrido redirigiendo el haz.","Cubo de agua: crea vapor que dispersa el haz temporalmente (paso seguro 5s).","Cubo de polvo de nieve: niebla que bloquea la linea del haz."],
  config:["Recorrido del haz","Dano","Espejos moviles","Interaccion con puzles"] });


/* ---------- PUZLES ---------- */
CDE_DATA.puzzles = [];
function puz(o){ CDE_DATA.puzzles.push(o); }

puz({ name:"Espejos de trayectoria", dungeon:["mirror-castle","primordial-tower","solar-palace"],
  desc:"Redirigir un haz (luz o proyectil) rebotandolo en espejos orientables hasta alcanzar un sello.",
  vanilla:["Cana de pescar: gira espejos a distancia sin exponerse al haz.","Ballesta: disparo que activa el sello final a distancia.","Escudo: refleja el haz si se interpone (solucion alternativa).","Cubo de agua: crea vapor que revela la trayectoria del haz invisible."] });

puz({ name:"Puzle de peso y sustitucion", dungeon:["worldbearer","hollow-leviathan","primordial-tower","solar-palace"],
  desc:"Igualar o sustituir el peso en balanzas y placas usando bloques empujables, objetos o contrapesos.",
  vanilla:["Bloques de piedra/tierra/arena: cada tipo tiene peso diferente para calibrar balanzas.","TNT: solucion explosiva que destruye el contrapeso en lugar de igualarlo (atajo con consecuencias).","Leads/riendas: conectan bloques a distancia para moverlos sin estar encima.","Items de oro: peso exacto necesario en ciertas balanzas ceremoniales del Solar Palace."] });

puz({ name:"Secuencia de sonidos", dungeon:["hollow-leviathan","veiled-peak","descent-madness","primordial-tower"],
  desc:"Reproducir o identificar una secuencia sonora (latidos, campanas, voces, resonancia elemental).",
  vanilla:["Note blocks: el jugador reproduce la secuencia con note blocks colocados (la solucion directa).","Campanas: alternativa a note blocks para ciertos tonos.","Sculk sensors: amplifican sonidos debiles ayudando a identificar la secuencia.","Catalejo: en variantes visuales, el zoom revela detalles de la secuencia."] });

puz({ name:"Puzle biologico de organos", dungeon:["hollow-leviathan"],
  desc:"Manipular organos del leviatan en el orden correcto (bombear, drenar, sellar) sin desencadenar la respuesta inmune.",
  vanilla:["Polvo de hueso: estimula crecimiento en organos especificos (activar/expandir).","Cubo de agua: drena/diluye fluidos organicos.","Pedernal y acero: cauteriza y sella conductos permanentemente.","Cizallas: corta conexiones entre organos.","Honey bottles: lubrica esfinteres trabados permitiendo el paso de fluidos."] });

puz({ name:"Pasillo de memoria", dungeon:["mirror-castle","descent-madness"],
  desc:"Recordar y reproducir un recorrido, secuencia o escena que se muestra brevemente y luego se oculta.",
  vanilla:["Antorchas: colocadas como marcadores durante la fase de memorizacion.","Catalejo: revela detalles adicionales durante la fase de muestra (pistas ocultas).","Brujula de recuperacion: senala la direccion correcta cuando todo se oculta.","Bolas de nieve: marcadores arrojados que persisten brevemente como referencia."] });

puz({ name:"Cooperacion asimetrica", dungeon:["worldbearer","veiled-peak","solar-palace"],
  desc:"Puzle multijugador donde cada jugador ve o controla algo distinto y deben coordinarse.",
  vanilla:["Leads/riendas: permiten conectar mecanismos entre jugadores distantes.","Cana de pescar: un jugador alcanza mecanismos lejanos mientras otro sujeta algo.","Escudo: un jugador protege mientras otro resuelve.","Ballesta: activar mecanismos del otro lado del puzle."] });

puz({ name:"Puzle de luz y espejos", dungeon:["solar-palace","mirror-castle"],
  desc:"Girar espejos y abrir/cerrar compuertas de luz para llevar un haz solar hasta una cerradura.",
  vanilla:["Cana de pescar: la herramienta principal para girar espejos a distancia.","Ballesta: activa botones/compuertas que no se alcanzan con la mano.","Escudo: se interpone para redirigir haces como espejo de emergencia.","Cubo de agua: empana espejos especificos para bloquear caminos incorrectos del haz.","Antorchas: revelan la trayectoria del haz en zonas oscuras."] });

puz({ name:"Puzle de elementos (afinidad por piso)", dungeon:["primordial-tower"],
  desc:"Activar altares con el elemento correcto del piso. Llevar la afinidad equivocada castiga.",
  vanilla:["Cubo de agua: activa altares de agua.","Cubo de lava: activa altares de fuego.","Bolas de nieve: activan altares de aire/hielo.","Bloques de tierra/piedra: activan altares de tierra.","Pedernal y acero: alternativa para altares de fuego.","Polvo de hueso: activa altares de tierra (variante organica)."] });

puz({ name:"Puzle de rutas falsas y repeticion", dungeon:["descent-madness"],
  desc:"En los pasillos que se repiten, la salida real se descubre observando que detalle cambia en cada vuelta.",
  vanilla:["Antorchas: marcadores que desaparecen con cada repeticion (la desaparicion confirma el reinicio).","Brujula de recuperacion: apunta a la salida real independientemente de la distorsion.","Catalejo: revela el detalle que cambia (objeto movido, textura diferente).","Bolas de nieve: marcadores arrojados que persisten o no entre ciclos."] });

puz({ name:"Secuencia de antorchas y braseros", dungeon:["solar-palace"],
  desc:"Encender antorchas y braseros activables en el orden correcto para abrir puertas.",
  vanilla:["Pedernal y acero: la herramienta PRINCIPAL para encender braseros (esencial en esta dungeon).","Cubo de agua: apaga braseros encendidos incorrectamente para reiniciar la secuencia.","Ballesta con flecha de fuego: enciende braseros a distancia.","Flechas espectrales: marcan el orden correcto al iluminar brevemente todos los braseros."] });

puz({ name:"Puzle de presion a distancia", dungeon:["worldbearer","veiled-peak","solar-palace"],
  desc:"Placas de presion que deben activarse simultaneamente o en secuencia sin poder estar encima de todas.",
  vanilla:["Bolas de nieve: activan placas de presion a distancia por impacto.","Huevos: mismo efecto que bolas de nieve.","Bloques de piedra/arena: colocados permanecen sobre la placa.","Items de oro: peso especifico para placas doradas en el Solar Palace.","TNT: activa placas por peso antes de explotar (timing critico)."] });

puz({ name:"Puzzle de corrientes y fluidos", dungeon:["hollow-leviathan","primordial-tower"],
  desc:"Redirigir fluidos (sangre, agua, acido) por conductos para activar mecanismos o inundar/drenar salas.",
  vanilla:["Cubo de agua: anade volumen al sistema de fluidos.","Cubo de polvo de nieve: congela secciones de fluido creando presas.","Polvo de hueso: hace crecer barreras organicas que redirigen fluidos (Leviatan).","Bloques de piedra: sellan conductos rotos."] });

puz({ name:"Puzle de navegacion en niebla", dungeon:["veiled-peak","descent-madness"],
  desc:"Encontrar el camino correcto cuando la visibilidad es casi nula y las rutas son multiples.",
  vanilla:["Catalejo: penetra la niebla parcialmente revelando contornos lejanos.","Brujula/recovery compass: orientacion constante hacia el objetivo.","Antorchas: crean puntos de referencia luminosos en la niebla.","Bolas de nieve: lanzadas rebotan con sonido distinto si golpean pared vs vacio."] });


/* ---------- MECANISMOS Y ACTIVADORES ---------- */
CDE_DATA.mechanisms = [];
function mech(o){ CDE_DATA.mechanisms.push(o); }

mech({ name:"Activador hidraulico", desc:"Mecanismo tipo palanca que se activa con agua (botellas o cubo). Fuente, cuenco o altar con estados visuales de llenado.",
  vanilla:["Cubo de agua: llena completamente de una vez.","Botellas de agua: llenan parcialmente (3 botellas = 1 cubo).","Cubo de polvo de nieve: llena con nieve derretida (alternativa en Veiled Peak).","Pocion splash de agua: llena a distancia sin acercarse."],
  config:["Capacidad","Cantidad necesaria","Color","Senal emitida"] });

mech({ name:"Pasadizo secreto", desc:"Pared normal que pierde opacidad y hitbox hasta permitir el paso. Se abre por golpes, palanca, placa o combinacion.",
  vanilla:["TNT: revela todos los pasadizos secretos en el radio de explosion.","Piston de redstone: empuja bloques revelando huecos detras.","Catalejo: algunos pasadizos tienen un detalle visual visible solo con zoom.","Ballesta: activar el boton oculto a distancia."],
  config:["Material","Tamano","Num de golpes","Metodo de cierre"] });

mech({ name:"Bloques empujables", desc:"Bloques solidos y desplazables para puzles fisicos. Se empujan mirandolos y manteniendo presion.",
  vanilla:["Leads/riendas: se atan al bloque para tirar a distancia (cooperativo).","Cana de pescar: engancha y tira del bloque a distancia.","Piston de redstone: empuja el bloque una posicion sin esfuerzo.","TNT: la explosion mueve bloques segun fuerza y direccion.","Cubo de agua: lubrica la superficie debajo, reduciendo friccion (mueve mas facil)."],
  config:["Peso/resistencia","Fuerza minima","Direcciones validas","Explosiones"] });

mech({ name:"Activador de balanza", desc:"Compara el peso de dos lados. Igualarlo abre puertas; equivocarse activa castigos.",
  vanilla:["Bloques de piedra/tierra: pesos estandar para calibrar.","Arena/grava: pesos intermedios con comportamiento de gravedad.","Items de oro: peso exacto para balanzas ceremoniales.","TNT: destruye un lado de la balanza (solucion bruta con consecuencias)."],
  config:["Peso exacto/margen","Tipo de objeto","Castigo por fallo"] });

mech({ name:"Pulsador oculto configurable", desc:"Convierte casi cualquier bloque en un boton camuflado, distinguible solo por detalles minimos.",
  vanilla:["Catalejo: revela el detalle distintivo del pulsador con zoom (runa, grieta).","Bolas de nieve: activan pulsadores a distancia si impactan sobre ellos.","Ballesta: activa pulsadores a larga distancia con precision.","Flechas espectrales: iluminan brevemente todos los pulsadores de la sala."],
  config:["Apariencia","Metodo","Modo","Num de pulsaciones"] });

mech({ name:"Cabrestante manual de barras", desc:"Eje con barras que el jugador empuja caminando en circulo para transmitir energia mecanica.",
  vanilla:["Leads/riendas: se atan a las barras para que un jugador tire desde lejos.","Bloques de piedra: colocados en las barras como peso (giran mas lento pero solos).","Cana de pescar: engancha las barras para girar a distancia parcialmente."],
  config:["Num de barras","Vueltas necesarias","Energia por vuelta","Num de jugadores"] });

mech({ name:"Ascensor manual por cadenas", desc:"Plataforma elevadora pesada accionada con cadenas, poleas y cabrestante.",
  vanilla:["Bloques de piedra: anadidos como contrapeso del otro lado facilitan la subida.","Leads/riendas: conectan jugadores al mecanismo de cadenas a distancia.","Cubo de agua: lubrica las poleas reduciendo el esfuerzo necesario."],
  config:["Tamano","Altura maxima","Num de paradas","Num de jugadores"] });

mech({ name:"Antorcha o brasero activable", desc:"Antorcha o brasero que, al encenderse, emite una senal: abre puertas, desactiva trampas, activa eventos o forma secuencias.",
  vanilla:["Pedernal y acero: la forma PRINCIPAL de encenderlos (esencial en Solar Palace).","Cubo de agua: los apaga (resetear secuencias incorrectas).","Bolas de nieve: apagan braseros a distancia.","Flechas de fuego (ballesta): encienden braseros a distancia."],
  config:["Tipo","Color de llama","Secuencia","Reinicio"] });

mech({ name:"Sistema de Resonancia de la Mazmorra", desc:"Cristales, vetas y placas que absorben sonido y vibraciones y se cargan en 4 estados (dormido a sobrecargado).",
  vanilla:["Note blocks: generan sonido controlado para cargar cristales en la cantidad exacta.","Sculk sensors: detectan el nivel de carga de cristales cercanos.","Bolas de nieve: generan vibracion menor al impactar (carga parcial segura).","TNT: carga MAXIMA instantanea (riesgo de sobrecarga/explosion).","Campanas: generan vibracion de frecuencia especifica para cristales concretos."],
  config:["Sensibilidad","Capacidad","Propagacion por vetas","Recompensas"] });

mech({ name:"Mecanismo dorado", desc:"Interruptores, cerraduras y altares del Solar Palace que solo responden a items o bloques de oro.",
  vanilla:["Lingotes de oro: activan mecanismos estandar.","Bloques de oro: activan mecanismos mayores.","Manzanas doradas: activan altares sagrados especiales.","Items de oro (armadura, herramientas): sirven como llave temporal al equiparlos cerca."],
  config:["Tipo de oro requerido","Cantidad","Consumible o reutilizable","Senal emitida"] });

mech({ name:"Plataforma de polvo de nieve", desc:"Mecanismo que permite crear plataformas temporales sobre abismos usando powder snow.",
  vanilla:["Cubo de polvo de nieve: crea la plataforma (desaparece tras 15-30s segun configuracion).","Botas de cuero: NECESARIAS para caminar sobre powder snow sin hundirse.","Bolas de nieve: extienden la duracion de una plataforma existente."],
  config:["Duracion","Tamano maximo","Puede extenderse","Requiere botas de cuero"] });


/* ---------- NPCs Y CAMPAMENTO ---------- */
CDE_DATA.camp = {
  title: "Campamento de aventureros",
  desc: "La principal estructura del mod fuera de las mazmorras y el punto de entrada natural al contenido. Menor que una dungeon colosal, pero detallada como centro de operaciones: tiendas, hogueras, mesas con mapas, almacenes, zona de entrenamiento y restos de expediciones.",
  functions: [
    "Descubrir la existencia de las siete mazmorras y obtener mapas, fragmentos y coordenadas.",
    "Hablar con aventureros, recibir encargos y misiones sencillas.",
    "Entregar y recibir objetos; comprar o intercambiar recursos.",
    "Conocer rumores y secretos; prepararse antes de entrar.",
    "Reclutar a ciertos aventureros como companeros.",
    "Preparar items vanilla: craftear note blocks, llenar cubos de agua, fabricar bolas de nieve, equipar escudos."
  ],
  companions: "Companeros reclutables con sistema propio (adaptado a NeoForge 1.21.1): seguimiento, ordenes basicas, combate, equipamiento, inventario, estados de espera/seguimiento, reaccion a trampas, dialogos y condiciones de reclutamiento.",
  dialogue: "Sistema propio de NPCs y dialogos: arboles con varias respuestas, ramas, condiciones y consecuencias; memoria por jugador; definido por datos (JSON) con claves de idioma para traduccion completa."
};

CDE_DATA.valdris = {
  name: "Valdris el Eterno",
  role: "Comerciante especial con IA, memoria y contratos invisibles",
  desc: "Un craneo flotante rodeado de manos esqueleticas y gemas que cambian de color segun su estado. No es un vendedor normal: su tienda es una mecanica completa de observacion, riesgo y engano.",
  states: [
    { name:"Mirando al jugador", detail:"8-12 s - gemas azul frio - no se puede robar." },
    { name:"Girando la cabeza", detail:"2-3 s - gemas blancas - ventana de robo de 1,5 s." },
    { name:"Cogiendo algo detras", detail:"4-5 s cada 45-60 s - da la espalda - ventana larga de robo." },
    { name:"Durmiendo", detail:"tras 90 s sin interaccion - 6-8 s - la ventana de robo mas larga." },
    { name:"Focus Total", detail:"permanente si te descubre robando - gemas rojas - rotacion 360 - sin mas ventanas para ese jugador." }
  ],
  vanilla:["Items de oro: pago principal por sus mercancias.","Comida/pan: ofrecida le agrada y reduce precios un 5%.","Leche: ofrecida rompe contratos simples (alternativa a Rompecontratos).","Perla de ender: robar y teletransportarse fuera de su rango de vision (escape tras robo)."],
  contracts: "Los objetos llevan un contrato invisible. Se eliminan con el futuro Rompecontratos, que desbloquea los objetos sin trampa (Llave Maestra, Pocion de Plenitud).",
  theft: "Robar con clic derecho durante una ventana valida obtiene el objeto sin contrato. Si te descubre, el objeto queda encadenado al inventario con debuffs hasta pagar tres veces su precio.",
  kill: "Tiene ~1000 HP; cada golpe activa Empuje de Tinieblas. Si muere, revive a los 5 s y la tienda permanece."
};


/* ---------- VANILLA INTERACTIONS (NUEVO) ---------- */
CDE_DATA.vanillaInteractions = [
  { item: "Polvo de hueso (Bone Meal)", icon: "bone_meal", category: "Organico/Limpieza", interactions: [
    { context: "Mecanica del aceite", effect: "Limpia aceite del jugador y superficies si no arde aun. 30 min de proteccion preventiva.", dungeon: ["solar-palace","primordial-tower"] },
    { context: "Barreras organicas", effect: "Hace crecer musgo/barreras que bloquean conductos de gas o acido.", dungeon: ["hollow-leviathan"] },
    { context: "Estimular organos", effect: "Activa organos del leviatan por crecimiento descontrolado (puzle biologico).", dungeon: ["hollow-leviathan"] },
    { context: "Altares de tierra", effect: "Activa altares elementales de tierra (variante organica).", dungeon: ["primordial-tower"] },
    { context: "Neutralizar acido", effect: "Reaccion quimica que neutraliza charcos de acido de Polipos.", dungeon: ["hollow-leviathan"] },
    { context: "Anticuerpos", effect: "Esparcido en el suelo confunde anticuerpos (los trata como tejido propio).", dungeon: ["hollow-leviathan"] }
  ]},
  { item: "Cubo de agua (Water Bucket)", icon: "water_bucket", category: "Elemental/Utilitario", interactions: [
    { context: "Apagar fuego", effect: "Apaga trampas de fuego, lanzallamas, braseros, Igneos de Brasero.", dungeon: ["solar-palace","primordial-tower"] },
    { context: "Activador hidraulico", effect: "Llena mecanismos hidraulicos completamente de una vez.", dungeon: ["solar-palace","worldbearer","primordial-tower"] },
    { context: "Empanar espejos", effect: "Empana superficies especulares impidiendo reflejos hostiles y revelando orientacion real.", dungeon: ["mirror-castle"] },
    { context: "Diluir acido", effect: "Neutraliza charcos acidos y suelos digestivos temporalmente.", dungeon: ["hollow-leviathan"] },
    { context: "Dispersar enjambres", effect: "Dispersa Colonias Liticas, Anticuerpos y enjambres organicos.", dungeon: ["worldbearer","hollow-leviathan"] },
    { context: "Lubricar mecanismos", effect: "Reduce friccion en bloques empujables, rieles y poleas.", dungeon: ["worldbearer"] },
    { context: "Matar elementales de fuego", effect: "Kill instantaneo contra Ascuas Primigenias e Igneos.", dungeon: ["primordial-tower","solar-palace"] },
    { context: "Provocar eclipse", effect: "Vertido en el altar central del Solar Palace apaga la luz y debilita al Guardian/Helios.", dungeon: ["solar-palace"] },
    { context: "ADVERTENCIA con aceite", effect: "NO apaga aceite ardiendo; lo EMPEORA duplicando el dano. Solo usar en aceite NO encendido.", dungeon: ["solar-palace","primordial-tower"] }
  ]},
  { item: "Pedernal y acero (Flint and Steel)", icon: "flint_and_steel", category: "Fuego/Activacion", interactions: [
    { context: "Encender braseros", effect: "La forma principal de resolver puzles de secuencia de braseros.", dungeon: ["solar-palace"] },
    { context: "Cauterizar tejido", effect: "Sella conductos organicos permanentemente, cierra Bocas de Pared para siempre.", dungeon: ["hollow-leviathan"] },
    { context: "Encender faros", effect: "Enciende faros espirituales para disipar el velo.", dungeon: ["veiled-peak"] },
    { context: "Prender aceite ofensivamente", effect: "Usar aceite del suelo como arma contra enemigos que lo pisan.", dungeon: ["solar-palace","primordial-tower"] },
    { context: "Derretir hielo", effect: "Derrite barreras y placas de hielo creando paso seguro.", dungeon: ["veiled-peak","primordial-tower"] },
    { context: "Endurecer azogue", effect: "El fuego endurece a la Doncella de Azogue haciendola fragil.", dungeon: ["mirror-castle"] },
    { context: "Danar criaturas de hielo", effect: "Debilita enormemente a criaturas congeladas/de hielo.", dungeon: ["veiled-peak"] }
  ]},
  { item: "Bolas de nieve (Snowballs)", icon: "snowball", category: "Distraccion/Activacion remota", interactions: [
    { context: "Distraccion sonora", effect: "Crea sonido en punto de impacto desviando criaturas ciegas/de sonido.", dungeon: ["hollow-leviathan","worldbearer","descent-madness"] },
    { context: "Activar placas a distancia", effect: "Activan placas de presion por impacto sin estar encima.", dungeon: ["worldbearer","solar-palace","veiled-peak"] },
    { context: "Romper espejos fragiles", effect: "2-3 impactos rompen paneles de espejo y cristal.", dungeon: ["mirror-castle"] },
    { context: "Testar suelo ilusorio", effect: "Rebotan en suelo real, ATRAVIESAN el falso (diagnostico).", dungeon: ["descent-madness"] },
    { context: "Interrumpir curaciones", effect: "Impactan y cancelan animaciones de curacion de enemigos.", dungeon: ["descent-madness"] },
    { context: "Danar elementales de fuego", effect: "Causan dano significativo a criaturas de fuego (2 bolas matan Ascuas).", dungeon: ["primordial-tower","solar-palace"] },
    { context: "Dispersar Cefiros", effect: "3 impactos dispersan elementales de viento.", dungeon: ["veiled-peak","primordial-tower"] },
    { context: "Silenciar campanas", effect: "Impacto en campanas las silencia temporalmente.", dungeon: ["veiled-peak"] }
  ]},
  { item: "Huevos (Eggs)", icon: "egg", category: "Cebo/Distraccion", interactions: [
    { context: "Cebo organico", effect: "Atraen criaturas hambrientas (Larvas, Neotelidos, Bocas de Pared) como cebo.", dungeon: ["hollow-leviathan","descent-madness"] },
    { context: "Alimentar Neotelidos bebe", effect: "Previene la transformacion canibal manteniendolos dociles.", dungeon: ["hollow-leviathan","worldbearer"] },
    { context: "Activar Mimics", effect: "Lanzados al cofre sospechoso activan el Mimic prematuramente a distancia.", dungeon: ["mirror-castle","solar-palace","worldbearer"] },
    { context: "Activar placas", effect: "Mismo efecto que bolas de nieve en placas de presion.", dungeon: ["worldbearer","solar-palace"] },
    { context: "Testar suelo", effect: "Diagnostico de suelo ilusorio igual que bolas de nieve.", dungeon: ["descent-madness"] }
  ]},
  { item: "Perla de ender (Ender Pearl)", icon: "ender_pearl", category: "Movilidad/Emergencia", interactions: [
    { context: "Escape de caidas", effect: "Teletransporte de emergencia al caer al vacio o en derrumbes.", dungeon: ["worldbearer","veiled-peak","primordial-tower"] },
    { context: "Atravesar cristal sellado", effect: "Teletransporte al otro lado de paredes de cristal (con restricciones de linea de vision).", dungeon: ["mirror-castle"] },
    { context: "Esquivar bolas rodantes", effect: "Esquiva lateral instantanea para evitar bolas colosales.", dungeon: ["solar-palace","worldbearer","primordial-tower"] },
    { context: "Salir de jaulas", effect: "Teletransporte fuera de la Skeever Trap si hay linea de vision.", dungeon: ["hollow-leviathan","descent-madness"] },
    { context: "Escape tras robo a Valdris", effect: "Robar y teletransportarse fuera de su rango de vision.", dungeon: ["solar-palace"] },
    { context: "LIMITACIONES", effect: "No funciona en zonas protegidas del Descent into Madness. Cooldown de 5s dentro de dungeons.", dungeon: ["descent-madness"] }
  ]},
  { item: "Riendas (Leads)", icon: "lead", category: "Conexion/Control mecanico", interactions: [
    { context: "Mover bloques a distancia", effect: "Se atan a bloques empujables para tirar cooperativamente.", dungeon: ["worldbearer","primordial-tower"] },
    { context: "Cabrestantes a distancia", effect: "Conectan jugadores al mecanismo de cabrestante desde lejos.", dungeon: ["worldbearer"] },
    { context: "Redirigir enemigos", effect: "Atan al bloque de lastre de Peregrinos para moverlo a placas utiles.", dungeon: ["worldbearer"] },
    { context: "Estabilizar contrapesos", effect: "Atan contrapesos del Atlas a posiciones fijas durante la pelea.", dungeon: ["worldbearer"] }
  ]},
  { item: "Escudo (Shield)", icon: "shield", category: "Defensa/Reflejo", interactions: [
    { context: "Bloquear empuje de viento", effect: "Reduce empuje de Cefiros y rafagas un 70%.", dungeon: ["veiled-peak","primordial-tower"] },
    { context: "Bloquear proyectiles de trampa", effect: "Bloquea esquirlas, acido, fuego y cadenas.", dungeon: ["hollow-leviathan","solar-palace","worldbearer"] },
    { context: "Reflejar haces de luz", effect: "Refleja rayos solares y haces de espejos de vuelta al origen.", dungeon: ["solar-palace","mirror-castle"] },
    { context: "Proteger de derrumbes", effect: "Reduce dano de escombros un 80%.", dungeon: ["worldbearer","veiled-peak"] },
    { context: "Bloquear Ley del Reflejo", effect: "Protege del dano devuelto al golpear reflejos equivocados.", dungeon: ["mirror-castle"] },
    { context: "Detener embestidas", effect: "Aturde a criaturas que embisten (Yak Espectral, etc.).", dungeon: ["veiled-peak"] },
    { context: "Neutralizar Eco Especular", effect: "Levantar escudo sin atacar neutraliza al Eco (no tiene nada que copiar).", dungeon: ["mirror-castle"] }
  ]},
  { item: "Ballesta (Crossbow)", icon: "crossbow", category: "Activacion a distancia/Precision", interactions: [
    { context: "Botones y pulsadores distantes", effect: "Activa mecanismos a larga distancia con precision.", dungeon: ["solar-palace","worldbearer","mirror-castle"] },
    { context: "Romper espejos", effect: "Un disparo certero rompe cristales de marcos y paneles.", dungeon: ["mirror-castle"] },
    { context: "Derribar voladores", effect: "Derriba Halcones de Bronce y criaturas aereas.", dungeon: ["solar-palace"] },
    { context: "Destruir capullos", effect: "Elimina capullos a distancia antes de eclosion.", dungeon: ["hollow-leviathan"] },
    { context: "Cortar nervios", effect: "Corta conexiones del Sistema Nervioso a distancia.", dungeon: ["hollow-leviathan"] },
    { context: "Romper cadenas de jefes", effect: "Rompe cadenas de Helios en momentos criticos.", dungeon: ["solar-palace"] }
  ]},
  { item: "Cizallas (Shears)", icon: "shears", category: "Corte organico", interactions: [
    { context: "Membranas y telaranas", effect: "Cortan instantaneamente membranas, capullos, tejido organico y telaranas.", dungeon: ["hollow-leviathan"] },
    { context: "Conexiones nerviosas", effect: "Cortan fibras del Sistema Nervioso con eficiencia maxima.", dungeon: ["hollow-leviathan"] },
    { context: "Tendones de Bocas", effect: "Cortan mandibulas de Puertas Devoradoras (paso seguro).", dungeon: ["hollow-leviathan"] },
    { context: "Raices del Parasito", effect: "Eficiencia x3 vs armas normales contra raices del jefe.", dungeon: ["hollow-leviathan"] },
    { context: "Vigia Cosido", effect: "Cortan conexiones del Vigia deshabilitando coordinacion.", dungeon: ["hollow-leviathan","descent-madness"] }
  ]},
  { item: "Honey Bottles (Botes de miel)", icon: "honey_bottle", category: "Inmunidad/Movimiento", interactions: [
    { context: "Inmunidad al hielo", effect: "Elimina deslizamiento en placas de hielo completamente.", dungeon: ["veiled-peak","primordial-tower"] },
    { context: "Inmunidad a corrientes", effect: "El jugador no es arrastrado por corrientes de agua.", dungeon: ["primordial-tower","hollow-leviathan"] },
    { context: "Inmunidad a arrastre", effect: "Las cadenas no enganchan y el viento no desliza.", dungeon: ["worldbearer","veiled-peak"] },
    { context: "Pacificar insectos", effect: "Las criaturas tipo insecto/parasito ignoran al jugador (olor dulce).", dungeon: ["hollow-leviathan"] },
    { context: "Superficies mucosas", effect: "Permite caminar sin resbalar en suelos mucosos del Leviatan.", dungeon: ["hollow-leviathan"] }
  ]},
  { item: "Cubo de polvo de nieve (Powder Snow Bucket)", icon: "powder_snow_bucket", category: "Plataformas/Control termico", interactions: [
    { context: "Plataformas temporales", effect: "Crea plataformas sobre abismos (15-30s, requiere botas de cuero).", dungeon: ["veiled-peak","primordial-tower"] },
    { context: "Congelar criaturas de agua", effect: "Congela elementales acuaticos instantaneamente.", dungeon: ["primordial-tower"] },
    { context: "Zonas frias de refugio", effect: "Crea areas donde el calor extremo se reduce.", dungeon: ["solar-palace"] },
    { context: "Cegar al Vigia", effect: "Colocado sobre ojos del Vigia lo ciega 30s.", dungeon: ["hollow-leviathan"] },
    { context: "Revelar pisadas", effect: "Esparcido en el suelo muestra pisadas de enemigos invisibles.", dungeon: ["veiled-peak","descent-madness"] },
    { context: "Congelar mercurio", effect: "Congela a la Doncella de Azogue completamente 5s.", dungeon: ["mirror-castle"] }
  ]},
  { item: "Leche (Milk Bucket)", icon: "milk_bucket", category: "Limpieza de debuffs", interactions: [
    { context: "Limpiar TODOS los debuffs", effect: "Elimina aceite, veneno, nausea, ceguera, locura, congelacion.", dungeon: ["solar-palace","hollow-leviathan","descent-madness","veiled-peak","primordial-tower"] },
    { context: "Contra locura", effect: "ESENCIAL entre fases del jefe de Descent para limpiar distorsiones.", dungeon: ["descent-madness"] },
    { context: "Contra veneno parasitario", effect: "Limpia veneno del Parasito Regente entre fases.", dungeon: ["hollow-leviathan"] },
    { context: "Romper contratos simples", effect: "Ofrecida a Valdris rompe contratos basicos de sus objetos.", dungeon: ["solar-palace"] }
  ]},
  { item: "Cana de pescar (Fishing Rod)", icon: "fishing_rod", category: "Manipulacion a distancia", interactions: [
    { context: "Girar espejos", effect: "Engancha y gira espejos orientables para puzles de luz.", dungeon: ["solar-palace","mirror-castle"] },
    { context: "Tirar palancas", effect: "Activa palancas lejanas sin acercarse a zonas peligrosas.", dungeon: ["worldbearer","solar-palace"] },
    { context: "Enganchar bloques", effect: "Tira de bloques empujables a distancia.", dungeon: ["worldbearer","primordial-tower"] },
    { context: "Desviar bolas de cadena", effect: "Engancha cadenas de enemigos y desvia la trayectoria.", dungeon: ["worldbearer"] },
    { context: "Arrancar equipo", effect: "Arranca el bloque de lastre de Peregrinos o estandartes de portadores.", dungeon: ["worldbearer","solar-palace"] },
    { context: "Arrastrar enemigos", effect: "Arrastra criaturas a zonas ventajosas (sombra, trampas, etc.).", dungeon: ["solar-palace","mirror-castle"] }
  ]},
  { item: "Catalejo (Spyglass)", icon: "spyglass", category: "Deteccion/Revelacion", interactions: [
    { context: "Revelar pulsadores ocultos", effect: "El zoom muestra detalles minimos (runas, grietas) de botones camuflados.", dungeon: ["worldbearer","solar-palace","mirror-castle","veiled-peak"] },
    { context: "Distinguir real vs falso", effect: "Identifica reflejos reales, clones verdaderos y detalles incorrectos.", dungeon: ["mirror-castle","descent-madness"] },
    { context: "Ver a traves de niebla", effect: "Penetra parcialmente la niebla revelando contornos.", dungeon: ["veiled-peak"] },
    { context: "Predecir patrones", effect: "Revela el patron de trampas retractiles y columnas desde la distancia.", dungeon: ["primordial-tower","veiled-peak"] },
    { context: "Identificar debilidades", effect: "Revela vertebra-nucleo, espejo-ancla y otros puntos debiles.", dungeon: ["worldbearer","mirror-castle"] }
  ]},
  { item: "TNT", icon: "tnt", category: "Destruccion/Acceso", interactions: [
    { context: "Muros debiles", effect: "Destruye paredes marcadas revelando atajos y secretos.", dungeon: ["worldbearer","primordial-tower"] },
    { context: "Puzles de peso", effect: "Solucion explosiva alternativa (destruir en vez de igualar).", dungeon: ["worldbearer","primordial-tower"] },
    { context: "Eliminar enjambres", effect: "Destruye colonias liticas y enjambres de un golpe.", dungeon: ["worldbearer"] },
    { context: "Forzar emergencia de sierpes", effect: "Detonado sobre su posicion subterranea las fuerza a emerger aturdidas.", dungeon: ["worldbearer"] },
    { context: "Sobrecargar resonancia", effect: "Carga MAXIMA instantanea de cristales de resonancia (riesgo).", dungeon: ["worldbearer","primordial-tower","solar-palace"] },
    { context: "ADVERTENCIA", effect: "No usar cerca de aceite no limpiado. La explosion prende todo el aceite cercano.", dungeon: ["solar-palace"] }
  ]},
  { item: "Sculk Sensors (colocables)", icon: "sculk_sensor", category: "Deteccion/Alerta", interactions: [
    { context: "Detectar enemigos invisibles", effect: "Detectan movimiento de Susurrantes y Guardian del Velo a traves de paredes.", dungeon: ["descent-madness","veiled-peak"] },
    { context: "Localizar sierpes", effect: "Detectan movimiento subterraneo de la Sierpe de Cimientos.", dungeon: ["worldbearer"] },
    { context: "Fuente real de voces", effect: "Distinguen la fuente REAL del sonido vs la percibida falsamente.", dungeon: ["descent-madness"] },
    { context: "Nivel de resonancia", effect: "Amplifican la informacion del Sistema de Resonancia.", dungeon: ["worldbearer","primordial-tower"] },
    { context: "Clon real del jefe", effect: "Detectan cual clon se mueve de verdad (pisa el suelo).", dungeon: ["descent-madness"] }
  ]},
  { item: "Antorchas y linternas (Torches/Lanterns)", icon: "torch", category: "Luz/Revelacion/Seguridad", interactions: [
    { context: "Revelar invisibles", effect: "La luz revela Almas Veladas, Susurrantes y La Multitud.", dungeon: ["veiled-peak","descent-madness"] },
    { context: "Prevenir spawns", effect: "Colocadas en salas limpiadas previenen respawns de mobs.", dungeon: ["descent-madness","mirror-castle"] },
    { context: "Marcadores persistentes", effect: "Sirven como marcadores en pasillos repetitivos y niebla.", dungeon: ["descent-madness","veiled-peak"] },
    { context: "Ciegar ojos organicos", effect: "Colocadas cerca de ojos del Vigia Cosido los ciegan.", dungeon: ["hollow-leviathan"] },
    { context: "Burbujas de visibilidad", effect: "Crean zonas de 3 bloques donde la niebla/ventisca no afecta.", dungeon: ["veiled-peak"] },
    { context: "Revelar sombras", effect: "Proyectan sombras de Sirvientes de Cristal invisibles.", dungeon: ["mirror-castle"] }
  ]},
  { item: "Note Blocks", icon: "note_block", category: "Sonido/Puzles", interactions: [
    { context: "Secuencias de sonido", effect: "Reproducen notas para resolver puzles de secuencia sonora.", dungeon: ["hollow-leviathan","veiled-peak","primordial-tower"] },
    { context: "Contra-frecuencia", effect: "Anulan campanas de niebla y voces del Coro de Auxilio.", dungeon: ["veiled-peak","descent-madness"] },
    { context: "Calmar espectros", effect: "Melodias especificas pacifican al Espectro del Aventurero.", dungeon: ["mirror-castle","descent-madness"] },
    { context: "Cargar resonancia", effect: "Generan sonido controlado para cargar cristales en cantidad exacta.", dungeon: ["worldbearer","primordial-tower"] },
    { context: "Desincronizar corazones", effect: "Contra-ritmo desincroniza al Corazon Falso.", dungeon: ["hollow-leviathan"] }
  ]},
  { item: "Comida / Pan (Food/Bread)", icon: "bread", category: "Pacificacion/Comercio", interactions: [
    { context: "Pacificar criaturas", effect: "Ofrecida pacifica Peregrinos sin Rostro, Espectros y Yaks antes de atacar.", dungeon: ["descent-madness","veiled-peak"] },
    { context: "Alimentar bebes", effect: "Mantiene dociles a Neotelidos bebe previniendo transformacion.", dungeon: ["hollow-leviathan"] },
    { context: "Cebo de larvas", effect: "Arrojada aleja Larvas Devoradoras del grupo.", dungeon: ["hollow-leviathan"] },
    { context: "Agradar a Valdris", effect: "Reduce precios un 5% como ofrenda.", dungeon: ["solar-palace"] }
  ]},
  { item: "Items de oro (Gold Items)", icon: "gold_ingot", category: "Ceremonial/Economico", interactions: [
    { context: "Pagar a Valdris", effect: "Moneda principal del comerciante.", dungeon: ["solar-palace"] },
    { context: "Mecanismos dorados", effect: "Activan cerraduras, altares y defensas doradas del Solar Palace.", dungeon: ["solar-palace"] },
    { context: "Sobornar guardias", effect: "Lingotes de oro pacifican Guardias Dorados temporalmente.", dungeon: ["solar-palace"] },
    { context: "Puzles ceremoniales", effect: "Peso exacto para balanzas y altares sagrados.", dungeon: ["solar-palace"] }
  ]},
  { item: "Componentes de redstone", icon: "redstone", category: "Mecanismos/Ingenieria", interactions: [
    { context: "Pistones para bloques", effect: "Empujan bloques del mod sin esfuerzo fisico del jugador.", dungeon: ["worldbearer","primordial-tower"] },
    { context: "Sculk sensors", effect: "Sistema de deteccion remota y alerta.", dungeon: ["descent-madness","worldbearer"] },
    { context: "Note blocks para puzles", effect: "Solucion directa a puzles de secuencia sonora.", dungeon: ["veiled-peak","primordial-tower"] },
    { context: "Campanas", effect: "Frecuencia especifica para cristales de resonancia.", dungeon: ["worldbearer"] }
  ]},
  { item: "Pociones splash/lingering", icon: "splash_potion", category: "Efectos especializados", interactions: [
    { context: "Resistencia al fuego", effect: "Inmunidad temporal en piso de fuego y durante Cenit.", dungeon: ["primordial-tower","solar-palace"] },
    { context: "Invisibilidad", effect: "Bypass temporal de Vigias Cosidos y criaturas visuales.", dungeon: ["hollow-leviathan","mirror-castle"] },
    { context: "Caida lenta", effect: "Seguridad en zonas de verticalidad y precipicios.", dungeon: ["veiled-peak","primordial-tower","worldbearer"] },
    { context: "Pocion de agua splash", effect: "Llena activadores hidraulicos a distancia.", dungeon: ["solar-palace","worldbearer"] }
  ]},
  { item: "Brujula de recuperacion (Recovery Compass)", icon: "recovery_compass", category: "Navegacion", interactions: [
    { context: "Pasillos repetitivos", effect: "Apunta a la salida real en el Descent into Madness.", dungeon: ["descent-madness"] },
    { context: "Niebla y laberintos", effect: "Orientacion constante hacia el objetivo en Veiled Peak.", dungeon: ["veiled-peak"] },
    { context: "Fases de distorsion", effect: "Indica la salida durante distorsiones del jefe de Descent.", dungeon: ["descent-madness"] }
  ]}
];


/* ---------- TECHNICAL (NUEVO) ---------- */
CDE_DATA.technical = {
  overview: "Arquitectura completa del mod Colossal Dungeons Enhanced para NeoForge 1.21.1. Disenado como un framework extensible, data-driven y orientado al rendimiento. Todas las mecanicas se definen via JSON y se registran mediante DeferredRegister. El sistema de dungeons gestiona el ciclo de vida completo: descubrimiento, entrada, activacion de salas, completado y reinicio.",
  javaVersion: "Java 21",
  minecraftVersion: "1.21.1",
  modLoader: "NeoForge",
  mappings: "Mojang Official",
  modId: "colossal_dungeons_enhanced",

  packageStructure: [
    { path: "com.colossaldungeons.enhanced", desc: "Root package - Clase principal @Mod" },
    { path: "com.colossaldungeons.enhanced.core", desc: "Core mod initialization, event bus registration, config" },
    { path: "com.colossaldungeons.enhanced.core.registry", desc: "Todas las instancias de DeferredRegister (bloques, items, entidades, efectos, sonidos, particulas)" },
    { path: "com.colossaldungeons.enhanced.core.config", desc: "Configuracion del mod via NeoForge ConfigSpec" },
    { path: "com.colossaldungeons.enhanced.core.event", desc: "Event handlers globales del mod" },
    { path: "com.colossaldungeons.enhanced.dungeon", desc: "Sistema de dungeons (salas, sectores, estado, ciclo de vida)" },
    { path: "com.colossaldungeons.enhanced.dungeon.room", desc: "Room state machine, activacion, bounds, transiciones" },
    { path: "com.colossaldungeons.enhanced.dungeon.sector", desc: "Sectores y conexiones entre salas" },
    { path: "com.colossaldungeons.enhanced.dungeon.trap", desc: "Trap framework: clases base, activadores, configuracion JSON" },
    { path: "com.colossaldungeons.enhanced.dungeon.puzzle", desc: "Puzzle engine: interfaces, solvers, estados" },
    { path: "com.colossaldungeons.enhanced.dungeon.mechanism", desc: "Mechanism handlers: hidraulicos, balanzas, resonancia" },
    { path: "com.colossaldungeons.enhanced.dungeon.loot", desc: "Loot tables custom y rewards por sala/dungeon" },
    { path: "com.colossaldungeons.enhanced.entity", desc: "Todas las entidades del mod" },
    { path: "com.colossaldungeons.enhanced.entity.ai", desc: "AI behaviors, goals, sensors, memory modules" },
    { path: "com.colossaldungeons.enhanced.entity.boss", desc: "Boss entities, phase logic, cinematics triggers" },
    { path: "com.colossaldungeons.enhanced.entity.npc", desc: "NPCs del campamento, dialogos, companeros" },
    { path: "com.colossaldungeons.enhanced.item", desc: "Custom items (armas, herramientas, reliquias, consumibles)" },
    { path: "com.colossaldungeons.enhanced.item.curio", desc: "Items de Curios API (anillos, amuletos, reliquias)" },
    { path: "com.colossaldungeons.enhanced.block", desc: "Custom blocks y block entities" },
    { path: "com.colossaldungeons.enhanced.block.trap", desc: "Bloques de trampas (lanzallamas, pinchos, etc)" },
    { path: "com.colossaldungeons.enhanced.block.mechanism", desc: "Bloques de mecanismos (hidraulicos, braseros, balanzas)" },
    { path: "com.colossaldungeons.enhanced.block.puzzle", desc: "Bloques de puzles (espejos, altares, note blocks custom)" },
    { path: "com.colossaldungeons.enhanced.effect", desc: "MobEffects custom y status system" },
    { path: "com.colossaldungeons.enhanced.network", desc: "Networking payloads (records con StreamCodec)" },
    { path: "com.colossaldungeons.enhanced.client", desc: "Client-side: rendering, particulas, GUI, HUD" },
    { path: "com.colossaldungeons.enhanced.client.renderer", desc: "Entity y block renderers (GeckoLib integration)" },
    { path: "com.colossaldungeons.enhanced.client.particle", desc: "Particle integration con AAA Particles" },
    { path: "com.colossaldungeons.enhanced.client.gui", desc: "Screens, overlays, bossbar custom, dialogos" },
    { path: "com.colossaldungeons.enhanced.data", desc: "Data generation: recipes, loot, tags, advancements, JSON schemas" },
    { path: "com.colossaldungeons.enhanced.data.dungeon", desc: "Dungeon definition loaders y schemas" },
    { path: "com.colossaldungeons.enhanced.vanilla", desc: "Sistema de interacciones con items vanilla" },
    { path: "com.colossaldungeons.enhanced.api", desc: "Public API para addons (interfaces, eventos, registros)" }
  ],

  registries: [
    { name: "CDEBlocks", type: "Block", count: "~55", keyEntries: ["oil_surface","pushable_block","hidden_button","hydraulic_activator","mirror_panel","brazier_activable","resonance_crystal","trap_spike_plate","wall_maw_block","membrane_block","ice_plate","golden_mechanism","powder_snow_platform","nerve_fiber"] },
    { name: "CDEItems", type: "Item", count: "~85", keyEntries: ["counterweight_hammer","surgeon_blade","madness_rapier","cycle_scepter","noon_lance","blizzard_bow","mirror_shard","mercury_drop","organic_sample","escape_crystal","sanity_candle","elemental_essence","solar_fragment","titan_heart","windcutter_cloak","oracle_relic","frost_crown","tempest_crown","shatter_crown"] },
    { name: "CDEEntities", type: "EntityType<?>", count: "~78", keyEntries: ["gelatinous_cube","mimic","neothelid_adult","neothelid_baby","shattered_sovereign","dying_atlas","sovereign_parasite","crowned_tempest","broken_sanity","primordial_concord","chained_helios","hollow_fencer","glass_harlequin","broken_twins","living_vertebra","nerve_cluster","summit_keeper","noon_guardian","semblance","valdris"] },
    { name: "CDEEffects", type: "MobEffect", count: "~18", keyEntries: ["oiled","madness","resonance_charge","hemorrhage","elemental_affinity","weight_encumbrance","solar_burn","frost_slow","wind_push","acid_corrosion","parasitic_poison","disorientation","immune_response","spectral_chill","sanity_drain"] },
    { name: "CDESounds", type: "SoundEvent", count: "~120", keyEntries: ["trap_activate","room_enter","boss_phase_change","resonance_charge_up","oil_ignite","bone_meal_cleanse","mirror_shatter","heartbeat_loop","wind_gust","elevator_chain","puzzle_complete","voice_false_help","campfire_ambient","valdris_laugh"] },
    { name: "CDEParticles", type: "ParticleType<?>", count: "~35", keyEntries: ["oil_drip","mercury_splash","solar_flare","frost_shard","bone_meal_cleanse_particle","resonance_wave","acid_bubble","nerve_spark","wind_trail","ember_float","spectral_wisp","blood_drop","echo_ripple"] },
    { name: "CDEAttachments", type: "AttachmentType<?>", count: "~8", keyEntries: ["dungeon_progress","room_state","player_reputation","companion_data","resonance_level","oil_status","madness_level","vanilla_interaction_cooldown"] },
    { name: "CDECreativeTabs", type: "CreativeModeTab", count: "3", keyEntries: ["cde_blocks_tab","cde_items_tab","cde_tools_tab"] }
  ],

  systems: [
    {
      name: "Dungeon State Machine",
      description: "Gestiona el ciclo de vida de dungeons: descubrimiento, entrada, activacion de salas, completado, reinicio. Cada dungeon es una instancia unica por mundo con estado persistente.",
      classes: ["DungeonManager","DungeonInstance","DungeonState","DungeonSaveData","DungeonDefinition"],
      events: ["DungeonEnteredEvent","RoomActivatedEvent","DungeonCompletedEvent","DungeonResetEvent","SectorTransitionEvent"],
      persistence: "Usa NeoForge AttachmentTypes en Level para estado de dungeon y en Player para progreso individual. Serializa con Codec."
    },
    {
      name: "Room Activation System",
      description: "Controla que salas estan activas. Estados: UNLOADED, PREPARED, ACTIVE, COMPLETED, DORMANT, SUSPENDED. Solo 1-3 salas activas simultaneamente para rendimiento.",
      classes: ["RoomController","RoomState","RoomBounds","RoomActivationHandler","RoomTransition"],
      performance: "Salas inactivas deshabilitan AI (no tick), block entities (no tick), VFX (no render) y collision avanzada. Activacion lazy al acercarse el jugador."
    },
    {
      name: "Trap Framework",
      description: "Clases base e interfaces para todas las trampas. Configuracion data-driven via JSON. Template Method para ciclo de vida, Strategy para activadores, Observer para reacciones en cadena.",
      classes: ["AbstractTrap","TrapActivator","TrapConfig","TrapRegistry","ITrapBehavior","TrapChainReaction","VanillaTrapInteraction"],
      patterns: "Template Method pattern para ciclo de vida (prepare, arm, trigger, damage, reset). Strategy pattern para activadores (pressure, proximity, timer, manual). Observer para chain reactions entre trampas."
    },
    {
      name: "Puzzle Engine",
      description: "Motor de puzles con estados, validacion de solucion y feedback visual/sonoro. Cada tipo de puzle implementa IPuzzle con su logica de solucion.",
      classes: ["PuzzleEngine","IPuzzle","PuzzleState","PuzzleSolver","PuzzleValidator","PuzzleFeedback"],
      patterns: "State pattern para el progreso del puzle. Strategy para validacion. Observer para feedback al jugador."
    },
    {
      name: "Mechanism System",
      description: "Handlers para todos los mecanismos interactivos: hidraulicos, balanzas, cabrestantes, braseros, bloques empujables. Cada mecanismo es un BlockEntity con estado sincronizado.",
      classes: ["IMechanism","MechanismBlockEntity","HydraulicActivator","BalanceScale","Winch","PushableBlock","BrazierBlock","GoldenMechanism"],
      patterns: "Command pattern para activaciones. State para niveles de llenado/carga. Observer para propagacion de senales."
    },
    {
      name: "Vanilla Item Interaction System",
      description: "Sistema central que intercepta el uso de items vanilla en contexto de dungeon y aplica efectos especificos. Escucha PlayerInteractEvent y aplica logica contextual basada en el bloque/entidad objetivo.",
      classes: ["VanillaInteractionHandler","IVanillaInteraction","InteractionRegistry","InteractionContext","InteractionResult"],
      events: ["VanillaItemUsedInDungeonEvent"],
      patterns: "Registry pattern para mapear item+contexto a efecto. Chain of Responsibility para prioridad de interacciones. Observer para notificar al sistema de VFX."
    },
    {
      name: "Resonance System",
      description: "Cristales y vetas que absorben vibraciones y se cargan progresivamente. Integra con Sculk sensors vanilla y note blocks. 4 estados: dormido, cargando, cargado, sobrecargado.",
      classes: ["ResonanceCrystal","ResonanceVein","ResonanceState","ResonanceNetwork","ResonanceOverloadEvent"],
      patterns: "State pattern con 4 estados. Observer para propagacion por vetas. Mediator para red de cristales conectados."
    },
    {
      name: "Combat System",
      description: "Extiende el combate vanilla con partes rompibles, fases de jefe, ataques telegrafados y dano localizado. Integra con More Hitboxes para entidades multiparte.",
      classes: ["CDECombatHandler","BossPhaseManager","AttackPattern","TelegraphedAttack","PartDamageHandler","MultipartEntityBase"],
      patterns: "State pattern para fases de jefe. Strategy para seleccion de ataques. Observer para cinematicas de transicion."
    },
    {
      name: "VFX System (AAA Particles Integration)",
      description: "Gestiona todos los efectos visuales del mod via AAA Particles (Effekseer). Cada efecto se liga a entidades o posiciones con sincronizacion de huesos GeckoLib.",
      classes: ["CDEEffectManager","CDEEffectInstance","CDEBoneAttachment","EffectPool","EffectDefinition"],
      performance: "Pool de efectos reutilizables. Solo efectos en la sala activa se renderizan. LOD para distancia."
    },
    {
      name: "Dialogue and NPC System",
      description: "Sistema de dialogos data-driven con arboles, condiciones, consecuencias y memoria por jugador. Los NPCs del campamento usan este sistema para misiones y lore.",
      classes: ["DialogueTree","DialogueNode","DialogueChoice","DialogueCondition","NPCMemory","DialogueRenderer"],
      persistence: "Memoria de NPC almacenada en AttachmentType del jugador. Decisiones afectan dialogos futuros."
    },
    {
      name: "Companion System",
      description: "Companeros reclutables con AI de seguimiento, combate, equipamiento e inventario propio. Reaccionan a trampas, contribuyen a puzles cooperativos y tienen dialogos contextuales.",
      classes: ["CompanionEntity","CompanionAI","CompanionInventory","CompanionOrder","CompanionState"],
      patterns: "State pattern para modos (seguir, esperar, combatir, explorar). Strategy para comportamiento en combate."
    },
    {
      name: "Addon API",
      description: "API publica que permite a otros mods anadir dungeons, criaturas, trampas, puzles y mecanismos. Basado en registros y eventos.",
      classes: ["CDEApi","IAddonProvider","AddonRegistry","AddonDungeonDefinition","AddonContentPack"],
      patterns: "Service Locator para acceso al API. Registry pattern para contenido externo. Event-driven para integracion."
    }
  ],

  apis: {
    geckolib: {
      version: "4.x para NeoForge 1.21.1",
      usage: "Todas las entidades animadas extienden GeoEntity, bloques animados extienden GeoBlockEntity. AnimationControllers gestionan transiciones de estado. KeyframeEvents disparan sonidos y particulas sincronizados.",
      classes: ["CDEGeoEntity","CDEGeoModel","CDEAnimationController","CDEGeoBlockEntity"],
      keyPatterns: "Entity registration con GeckoLib: extender GeoEntity, implementar getAnimatableInstanceCache(), registerControllers(). Modelo .geo.json en assets. Animaciones .animation.json con keyframe events."
    },
    aaaParticles: {
      version: "Latest para 1.21.1",
      usage: "Efectos Effekseer (.efkefc) cargados como recursos. Spawneados via EffectAPI ligados a entidades o posiciones. Bone attachment para sincronizacion con GeckoLib.",
      classes: ["CDEEffectManager","CDEEffectInstance","CDEBoneAttachment"],
      keyPatterns: "Spawning: EffekseerEffect.create(level, pos, rotation, scale). Bone attachment: bindToEntity(entity, boneName). Lifecycle: onComplete callback para cleanup."
    },
    smartBrainLib: {
      version: "Para NeoForge 1.21.1",
      usage: "Boss y elite mobs usan Brain API via SmartBrainLib. Sensors, MemoryModuleTypes y Behaviors componen arboles de AI. Permite creacion rapida de IA compleja sin boilerplate.",
      classes: ["CDEBrainProvider","CDESensor","CDEBehavior","CDEBossAI"],
      keyPatterns: "Brain setup: registerBrainGoals() con ExtendedSensor, TargetOrRetaliate, SetWalkTargetToAttackTarget. Custom sensors para deteccion por sonido y vibracion."
    },
    curios: {
      version: "Para NeoForge 1.21.1",
      usage: "Slots custom: amulet, ring, belt, relic. Items implementan ICurioItem para logica de equip/tick/unequip. Renderers custom para visualizacion.",
      classes: ["CDECurioItem","CDECurioRenderer","CDESlotProvider"],
      keyPatterns: "Registro de slots en CuriosApi. Items implementan ICurioItem con onEquip(), curioTick(), onUnequip(). Renderer opcional via createRenderer()."
    },
    neoforge: {
      attachments: "Progreso del jugador, estado de dungeon y datos de sala almacenados via AttachmentTypes (reemplaza old Capabilities). Registro con DeferredRegister<AttachmentType<?>>. Serialization con Codec.",
      events: "IEventBus para lifecycle del mod, NeoForge.EVENT_BUS para game events. Key events: LivingHurtEvent, PlayerInteractEvent, BlockEvent, ServerTickEvent, LevelTickEvent",
      networking: "Custom payloads via PayloadRegistrar. Records que implementan CustomPacketPayload con StreamCodec. Registrado en RegisterPayloadHandlersEvent.",
      datagen: "DataGenerator para recipes, loot tables, tags, advancements y JSON configs custom. Extends DataProvider para schemas propios."
    }
  },

  networking: [
    { name: "RoomSyncPayload", direction: "S2C", desc: "Sincroniza cambios de estado de sala a todos los jugadores en la dungeon" },
    { name: "TrapActivatePayload", direction: "S2C", desc: "Dispara visual/audio de trampa en clientes (VFX + sonido)" },
    { name: "PuzzleStatePayload", direction: "S2C", desc: "Actualiza progreso de puzle para todos los jugadores" },
    { name: "BossPhasePayload", direction: "S2C", desc: "Anuncia transiciones de fase de jefe con datos de cinematica" },
    { name: "ResonanceUpdatePayload", direction: "S2C", desc: "Sincroniza nivel de carga de cristales de resonancia" },
    { name: "DungeonEventPayload", direction: "S2C", desc: "Eventos globales de dungeon (Eclipse, Cenit, Espasmo, etc)" },
    { name: "VFXSpawnPayload", direction: "S2C", desc: "Spawn de efectos AAA Particles en posicion/entidad" },
    { name: "PlayerInteractPayload", direction: "C2S", desc: "Interaccion de item vanilla con bloque/entidad del mod" },
    { name: "DialogueChoicePayload", direction: "C2S", desc: "Seleccion de dialogo del jugador" },
    { name: "MechanismActivatePayload", direction: "C2S", desc: "Jugador activa mecanismo (cabrestante, hidraulico, etc)" },
    { name: "CompanionOrderPayload", direction: "C2S", desc: "Orden al companero (seguir, esperar, atacar)" }
  ],

  datadriven: {
    description: "Todo el contenido es data-driven via JSON files en data/cde/. Addons pueden anadir/override mediante datapacks estandar de Minecraft.",
    schemas: [
      { name: "Dungeon Definition", path: "data/cde/dungeons/*.json", fields: "id, rooms[], connections[], rules{}, loot_tables{}, vanilla_items[], boss_id, reset_config" },
      { name: "Trap Config", path: "data/cde/traps/*.json", fields: "type, activator{}, damage{}, timing{}, vanilla_interactions[], chain_reactions[], vfx_id" },
      { name: "Puzzle Config", path: "data/cde/puzzles/*.json", fields: "type, solution{}, difficulty, rewards[], coop_mode, vanilla_tools[], feedback{}" },
      { name: "Creature Config", path: "data/cde/creatures/*.json", fields: "type, stats{}, ai_profile, drops[], vanilla_weaknesses[], phases[], vfx{}" },
      { name: "Dialogue Tree", path: "data/cde/dialogues/*.json", fields: "nodes[], choices[], conditions[], consequences[], i18n_keys{}" },
      { name: "Room Definition", path: "data/cde/rooms/*.json", fields: "bounds{}, traps[], puzzles[], mobs[], events[], transitions[], activation_conditions" },
      { name: "Vanilla Interaction", path: "data/cde/vanilla_interactions/*.json", fields: "item_id, context, target_type, effect{}, conditions[], cooldown, vfx_id" },
      { name: "Mechanism Config", path: "data/cde/mechanisms/*.json", fields: "type, activation{}, states[], signals[], vanilla_triggers[]" }
    ]
  },

  effectSystem: {
    description: "Sistema de efectos en tres capas: MobEffects (status de jugador), VisualEffects (AAA Particles), EnvironmentalEffects (por sala). Las capas interactuan entre si via eventos.",
    layers: [
      { name: "Status Effects (MobEffect)", examples: ["Oiled (vulnerabilidad al fuego, limpiable con bone meal/leche)","Madness (distorsion perceptiva, limpiable con leche)","Resonance Charge (sensibilidad al sonido)","Elemental Affinity (bonus/penalty segun elemento)","Weight Encumbrance (lentitud por peso)","Hemorrhage (sangrado por estoque)","Parasitic Poison (veneno del Leviatan)","Spectral Chill (congelacion del Veiled Peak)"] },
      { name: "Visual Effects (AAA Particles)", examples: ["Auras de jefe por fase","Trails de ataque telegrafado","Explosiones y ondas de choque","Particulas ambientales por dungeon","Feedback de interaccion vanilla (vapor, limpieza, congelacion)","Warnings de trampa","Efectos de puzle resuelto"] },
      { name: "Environmental Effects (por sala)", examples: ["Niebla volumetrica (Veiled Peak, Descent)","Campos de viento (Veiled Peak, Primordial Air)","Suelos de acido/digestion (Leviathan)","Zonas de luz/sombra (Solar Palace)","Temperatura (fuego/hielo)","Campos de resonancia","Gravedad alterada (Worldbearer shoulder)"] }
    ],
    chaining: "Effects pueden triggear otros effects via eventos. Ejemplos: Oiled + Fire source = Burning (2x duracion). Water + Oiled = Steam explosion (dano area). Bone meal + Oiled = Cleanse + Protection. Honey + Ice = No slip. Milk = Clear ALL. Cold + Fire creature = Critical damage."
  },

  framework: {
    description: "Framework interno reutilizable que alimenta todo el contenido. Disenado para extensibilidad via addons y datapacks.",
    principles: ["Data-driven sobre hard-coded","Composicion sobre herencia","Comunicacion event-driven","Activacion lazy para rendimiento","API surface publica para addons","Vanilla-first: items vanilla siempre tienen uso","Testeable: cada sistema tiene unit tests"],
    coreInterfaces: ["IDungeonContent","ITrap","IPuzzle","IMechanism","ICreatureAI","IVanillaInteraction","IRoomEffect","IAddonProvider","IBossPhase","IDialogueNode","ICompanionBehavior","IResonanceReceiver"]
  }
};
