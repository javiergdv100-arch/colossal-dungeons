/* ============================================================
   Colossal Dungeons Enhanced — app.js
   Renderizado dinámico e interacción (sin dependencias)
   ============================================================ */
(function(){
  "use strict";
  var D = window.CDE_DATA;
  var esc = function(s){ return String(s==null?"":s)
    .replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;"); };
  var q = function(sel,ctx){ return (ctx||document).querySelector(sel); };
  var qa = function(sel,ctx){ return Array.prototype.slice.call((ctx||document).querySelectorAll(sel)); };

  var dungeonById = {};
  D.dungeons.forEach(function(d){ dungeonById[d.id] = d; });
  var creatureById = {};
  D.creatures.forEach(function(c){ creatureById[c.id] = c; });

  var RANK_LABEL = { normal:"Común", elite:"Élite", miniboss:"Semijefe", boss:"Jefe", rare:"Raro / especial" };

  function dungeonName(id){
    if(id === "shared") return "Compartida";
    return dungeonById[id] ? dungeonById[id].name : id;
  }
  function dungeonColor(id){
    return dungeonById[id] ? dungeonById[id].color : "#d8b26a";
  }

  /* ---------- NAV DEFINITION ---------- */
  var NAV = [
    { id:"inicio",       label:"Inicio",           ix:"01" },
    { id:"dungeons",     label:"Las siete dungeons", ix:"02" },
    { id:"bestiario",    label:"Bestiario",        ix:"03" },
    { id:"trampas",      label:"Trampas",          ix:"04" },
    { id:"puzles",       label:"Puzles",           ix:"05" },
    { id:"mecanismos",   label:"Mecanismos",       ix:"06" },
    { id:"npcs",         label:"NPCs y campamento", ix:"07" },
    { id:"dependencias", label:"Dependencias",     ix:"08" },
    { id:"rendimiento",  label:"Rendimiento",      ix:"09" },
    { id:"roadmap",      label:"Producción",       ix:"10" }
  ];


  /* ---------- SIDEBAR NAV ---------- */
  function renderNav(){
    q("#sideNav").innerHTML = NAV.map(function(n){
      return '<a href="#'+n.id+'" data-target="'+n.id+'">'
        + '<span class="ix">'+n.ix+'</span>'+esc(n.label)+'</a>';
    }).join("");
  }

  /* ---------- HERO + INICIO ---------- */
  function renderInicio(){
    var m = D.meta;
    var stats = D.stats.map(function(s){
      return '<div class="stat reveal"><div class="v">'+esc(s.value)+'</div>'
        + '<div class="l">'+esc(s.label)+'</div><div class="s">'+esc(s.sub)+'</div></div>';
    }).join("");
    var principles = m.principles.map(function(p,i){
      var n = (i+1<10?"0":"")+(i+1);
      return '<div class="principle reveal"><span class="n">'+n+'</span><span>'+esc(p)+'</span></div>';
    }).join("");
    return ''
    + '<section class="hero" id="inicio">'
    +   '<div class="eyebrow">Documento maestro de diseño</div>'
    +   '<h1>Colossal Dungeons<span class="sub">Enhanced</span></h1>'
    +   '<p class="hero-tag">'+esc(m.tagline)+'</p>'
    +   '<div class="hero-meta">'
    +     '<span class="pill hot">'+esc(m.platform)+'</span>'
    +     '<span class="pill">Estado: '+esc(m.status)+'</span>'
    +     '<span class="pill">7 dungeons</span><span class="pill">70 criaturas</span>'
    +   '</div>'
    +   '<p class="hero-intro">'+esc(m.intro)+'</p>'
    + '</section>'
    + '<section class="block">'
    +   '<div class="stat-grid">'+stats+'</div>'
    + '</section>'
    + '<section class="block" style="padding-top:26px">'
    +   '<div class="eyebrow">Principios confirmados</div>'
    +   '<h2 class="sec-title">Reglas del proyecto</h2>'
    +   '<p class="sec-lead">Decisiones ya cerradas que gobiernan todo el diseño, desde la dificultad hasta el rendimiento y el soporte para addons.</p>'
    +   '<div class="principles">'+principles+'</div>'
    + '</section>'
    + '<div class="divider"></div>';
  }


  /* ---------- DUNGEONS ---------- */
  function renderDungeons(){
    var cards = D.dungeons.map(function(d,i){
      var boss = creatureById[d.bossId];
      var idx = (i+1<10?"0":"")+(i+1);
      return '<article class="dcard reveal" style="--c:'+d.color+'" data-dungeon="'+d.id+'">'
        + '<span class="idx">DUNGEON '+idx+'</span>'
        + '<h3>'+esc(d.name)+'</h3>'
        + '<div class="sub">'+esc(d.subtitle)+'</div>'
        + (boss ? '<div class="boss-tag">Jefe · <b>'+esc(boss.name)+'</b></div>' : '')
        + '<div class="theme">'+esc(d.themeWord)+'</div>'
        + '<span class="arrow">Ver ficha →</span>'
        + '</article>';
    }).join("");
    return '<section class="block" id="dungeons">'
      + '<div class="eyebrow">Fichas individuales</div>'
      + '<h2 class="sec-title">Las siete dungeons</h2>'
      + '<p class="sec-lead">Siete mazmorras colosales de dificultad alta y equivalente, jugables en cualquier orden. Cada una impone una regla exclusiva, un ritmo propio y un jefe final. Pulsa una ficha para ver su diseño completo.</p>'
      + '<div class="dungeon-grid">'+cards+'</div></section>'
      + '<div class="divider"></div>';
  }

  function dungeonModal(d){
    var boss = creatureById[d.bossId];
    var mobs = D.creatures.filter(function(c){ return c.dungeon === d.id; });
    var sectors = d.sectors.map(function(s,i){
      return '<div class="sector"><span class="sn">'+(i+1)+'</span>'
        + '<div class="st"><b>'+esc(s.name)+'</b><span>'+esc(s.desc)+'</span></div></div>';
    }).join("");
    function chips(arr){ return '<div class="chips-inline">'+arr.map(function(x){return '<span>'+esc(x)+'</span>';}).join("")+'</div>'; }
    function list(arr){ return '<ul class="m-list">'+arr.map(function(x){return '<li>'+esc(x)+'</li>';}).join("")+'</ul>'; }
    var rows = [
      ["Entrada", d.entrance],["Salida", d.exit],["Reinicio", d.reset],
      ["Multijugador", d.multiplayer],["Rendimiento", d.performance]
    ].map(function(r){ return '<div class="meta-row"><div class="rk">'+esc(r[0])+'</div><div class="rv">'+esc(r[1])+'</div></div>'; }).join("");
    return ''
    + '<div class="m-head" style="border-color:color-mix(in srgb,'+d.color+' 30%, var(--line-soft))">'
    +   '<div class="m-rank" style="color:'+d.color+'">Dungeon · '+esc(d.themeWord)+'</div>'
    +   '<h2>'+esc(d.name)+'</h2><div class="m-en">'+esc(d.subtitle)+'</div>'
    + '</div>'
    + '<p class="desc">'+esc(d.identity)+'</p>'
    + '<div class="callout" style="border-left-color:'+d.color+'"><b>Regla exclusiva.</b> '+esc(d.rule)+'</div>'
    + '<div class="callout"><b>Objetivo.</b> '+esc(d.objective)+'</div>'
    + (boss ? '<div class="m-sub">Jefe principal</div><p class="desc" style="margin-top:0"><b style="color:'+d.color+'">'+esc(boss.name)+'</b> — '+esc(boss.desc)+'</p>' : '')
    + '<div class="m-sub">Recorrido y sectores</div>'+sectors
    + '<div class="dgrid2" style="margin-top:22px">'
    +   '<div><div class="m-sub">Puzles</div>'+chips(d.puzzles)+'</div>'
    +   '<div><div class="m-sub">Trampas</div>'+chips(d.traps)+'</div>'
    + '</div>'
    + '<div class="dgrid2" style="margin-top:20px">'
    +   '<div><div class="m-sub">Loot</div>'+list(d.loot)+'</div>'
    +   '<div><div class="m-sub">Armas y accesorios</div>'+list(d.weapons)+'</div>'
    + '</div>'
    + '<div class="dgrid2" style="margin-top:20px">'
    +   '<div><div class="m-sub">Eventos</div>'+list(d.events)+'</div>'
    +   '<div><div class="m-sub">Secretos y rutas</div>'+list(d.secrets)+'</div>'
    + '</div>'
    + '<div class="m-sub">Enemigos asignados ('+mobs.length+')</div>'+chips(mobs.map(function(c){return c.name+" · "+RANK_LABEL[c.rank];}))
    + '<div class="m-sub">Entrada · salida · reinicio</div><div class="meta-rows">'+rows+'</div>';
  }


  /* ---------- BESTIARIO ---------- */
  var bestState = { dungeon:"all", rank:"all", search:"" };

  function renderBestiario(){
    var dChips = '<div class="chip active" data-f="dungeon" data-v="all">Todas</div>'
      + '<div class="chip" data-f="dungeon" data-v="shared">Compartidas</div>'
      + D.dungeons.map(function(d){ return '<div class="chip" data-f="dungeon" data-v="'+d.id+'">'+esc(d.name)+'</div>'; }).join("");
    var rChips = '<div class="chip active" data-f="rank" data-v="all">Todos los rangos</div>'
      + ["normal","elite","miniboss","boss","rare"].map(function(r){
          return '<div class="chip" data-f="rank" data-v="'+r+'">'+RANK_LABEL[r]+'</div>'; }).join("");
    return '<section class="block" id="bestiario">'
      + '<div class="eyebrow">Criaturas, IA y combate</div>'
      + '<h2 class="sec-title">Bestiario · 70 criaturas</h2>'
      + '<p class="sec-lead">Objetivo base de 70 criaturas únicas (hasta 90 con variantes), al menos un jefe y dos semijefes por dungeon. Filtra por mazmorra o rango, o busca por nombre. Pulsa una criatura para ver su ficha completa.</p>'
      + '<div class="filters" id="fDungeon">'+dChips+'</div>'
      + '<div class="filters ranks" id="fRank">'+rChips+'</div>'
      + '<div class="count-line" id="mobCount"></div>'
      + '<div class="mob-grid" id="mobGrid"></div></section>'
      + '<div class="divider"></div>';
  }

  function mobMatches(c){
    if(bestState.dungeon!=="all" && c.dungeon!==bestState.dungeon) return false;
    if(bestState.rank!=="all" && c.rank!==bestState.rank) return false;
    if(bestState.search){
      var hay=(c.name+" "+c.en+" "+c.desc+" "+dungeonName(c.dungeon)).toLowerCase();
      if(hay.indexOf(bestState.search.toLowerCase())===-1) return false;
    }
    return true;
  }

  function renderMobGrid(){
    var list = D.creatures.filter(mobMatches);
    var grid = q("#mobGrid"); if(!grid) return;
    q("#mobCount").textContent = list.length + (list.length===1?" criatura":" criaturas");
    if(!list.length){ grid.innerHTML='<div class="empty">Sin resultados con estos filtros.</div>'; return; }
    grid.innerHTML = list.map(function(c){
      var col = dungeonColor(c.dungeon);
      return '<article class="mcard" data-mob="'+c.id+'" style="--dc:'+col+'">'
        + '<span class="rank r-'+c.rank+'">'+RANK_LABEL[c.rank]+'</span>'
        + '<h4>'+esc(c.name)+'</h4><div class="en">'+esc(c.en)+'</div>'
        + '<div class="home">'+esc(dungeonName(c.dungeon))+'</div>'
        + '<div class="stats-mini"><span><b>♥</b> '+esc(c.hp)+'</span>'
        + '<span><b>⛨</b> '+esc(c.armor)+'</span><span><b>⚔</b> '+esc(c.dmg)+'</span></div>'
        + '</article>';
    }).join("");
  }

  function mobModal(c){
    function list(arr,cls){ if(!arr||!arr.length) return ""; return '<ul class="m-list '+(cls||"")+'">'+arr.map(function(x){return '<li>'+esc(x)+'</li>';}).join("")+'</ul>'; }
    var col = dungeonColor(c.dungeon);
    return '<div class="m-head"><div class="m-rank r-'+c.rank+'" style="display:inline-block;padding:4px 12px;border-radius:999px">'+RANK_LABEL[c.rank]+'</div>'
      + '<h2>'+esc(c.name)+'</h2><div class="m-en">'+esc(c.en)+' · <span style="color:'+col+'">'+esc(dungeonName(c.dungeon))+'</span></div></div>'
      + '<div class="m-stats"><div class="m-stat"><div class="k">Vida</div><div class="val">'+esc(c.hp)+'</div></div>'
      + '<div class="m-stat"><div class="k">Armadura</div><div class="val">'+esc(c.armor)+'</div></div>'
      + '<div class="m-stat"><div class="k">Daño</div><div class="val">'+esc(c.dmg)+'</div></div></div>'
      + '<p class="desc">'+esc(c.desc)+'</p>'
      + (c.behavior&&c.behavior.length ? '<div class="m-sub">Comportamiento y ataques</div>'+list(c.behavior) : '')
      + (c.drops&&c.drops.length ? '<div class="m-sub">Botín</div>'+list(c.drops,"drops") : '');
  }


  /* ---------- TRAMPAS / PUZLES / MECANISMOS ---------- */
  function catCard(o){
    var isNew = o.nuevo;
    var tag = o.category ? '<span class="tag">'+esc(o.category)+'</span>' : '';
    var homes = o.dungeon ? o.dungeon.map(dungeonName).join(" · ") : "";
    var cfg = o.config ? '<div class="cfg">'+o.config.map(function(x){return '<span>'+esc(x)+'</span>';}).join("")+'</div>' : "";
    return '<article class="tcard reveal"><h4>'+esc(o.name)+(isNew?'<span class="badge-new">NUEVO</span>':'')+'</h4>'
      + tag + '<p>'+esc(o.desc)+'</p>'
      + (homes?'<div class="cfg" style="margin-top:10px"><span style="color:var(--gold-soft);border-color:rgba(216,178,106,.3)">'+esc(homes)+'</span></div>':'')
      + cfg + '</article>';
  }

  function renderTrampas(){
    return '<section class="block" id="trampas">'
      + '<div class="eyebrow">Peligros del entorno</div>'
      + '<h2 class="sec-title">Trampas y peligros</h2>'
      + '<p class="sec-lead">Familias de trampas configurables y reutilizables, cada una con variante temática por dungeon. Las marcadas como NUEVO amplían el catálogo original con peligros propios de cada mazmorra.</p>'
      + '<div class="card-grid">'+D.traps.map(catCard).join("")+'</div></section>'
      + '<div class="divider"></div>';
  }
  function renderPuzles(){
    return '<section class="block" id="puzles">'
      + '<div class="eyebrow">Ingenio y cooperación</div>'
      + '<h2 class="sec-title">Puzles</h2>'
      + '<p class="sec-lead">Puzles altamente configurables y compatibles con addons: solución, dificultad, castigos, recompensas y modo individual o cooperativo. Cada dungeon reutiliza y reinterpreta varios.</p>'
      + '<div class="card-grid">'+D.puzzles.map(catCard).join("")+'</div></section>'
      + '<div class="divider"></div>';
  }
  function renderMecanismos(){
    return '<section class="block" id="mecanismos">'
      + '<div class="eyebrow">Activadores y señales</div>'
      + '<h2 class="sec-title">Mecanismos</h2>'
      + '<p class="sec-lead">La base física reutilizable del mod: activadores, mecanismos manuales, pasadizos y el sistema de resonancia que conecta sonido, sigilo, trampas y secretos.</p>'
      + '<div class="card-grid">'+D.mechanisms.map(catCard).join("")+'</div></section>'
      + '<div class="divider"></div>';
  }


  /* ---------- NPCs ---------- */
  function renderNpcs(){
    var camp = D.camp, v = D.valdris;
    var fns = camp.functions.map(function(f){return '<li>'+esc(f)+'</li>';}).join("");
    var states = v.states.map(function(s){
      return '<div class="state-row"><b>'+esc(s.name)+'</b><span>'+esc(s.detail)+'</span></div>';
    }).join("");
    return '<section class="block" id="npcs">'
      + '<div class="eyebrow">Mundo exterior</div>'
      + '<h2 class="sec-title">NPCs y campamento</h2>'
      + '<p class="sec-lead">Fuera de las mazmorras, el campamento de aventureros es el punto de entrada al mod. Los NPCs usan un sistema propio de diálogos con memoria por jugador, decisiones y consecuencias.</p>'
      + '<div class="npc-wrap">'
      +   '<div class="npc-card reveal"><h3>'+esc(camp.title)+'</h3>'
      +     '<div class="role">Centro de operaciones</div>'
      +     '<p style="color:var(--text-dim);font-size:14px">'+esc(camp.desc)+'</p>'
      +     '<div class="m-sub">Funciones</div><ul class="m-list">'+fns+'</ul>'
      +     '<div class="m-sub">Compañeros</div><p style="color:var(--text-dim);font-size:13.5px">'+esc(camp.companions)+'</p>'
      +     '<div class="m-sub">Diálogos</div><p style="color:var(--text-dim);font-size:13.5px">'+esc(camp.dialogue)+'</p>'
      +   '</div>'
      +   '<div class="npc-card reveal"><h3>'+esc(v.name)+'</h3>'
      +     '<div class="role">'+esc(v.role)+'</div>'
      +     '<p style="color:var(--text-dim);font-size:14px">'+esc(v.desc)+'</p>'
      +     '<div class="m-sub">Estados de IA</div>'+states
      +     '<div class="m-sub">Contratos invisibles</div><p style="color:var(--text-dim);font-size:13.5px">'+esc(v.contracts)+'</p>'
      +     '<div class="m-sub">Robo</div><p style="color:var(--text-dim);font-size:13.5px">'+esc(v.theft)+'</p>'
      +     '<div class="m-sub">¿Matarlo?</div><p style="color:var(--text-dim);font-size:13.5px">'+esc(v.kill)+'</p>'
      +   '</div>'
      + '</div></section><div class="divider"></div>';
  }

  /* ---------- DEPENDENCIAS ---------- */
  function renderDeps(){
    var cards = D.dependencies.map(function(dep){
      var uses = dep.uses.map(function(u){return '<li>'+esc(u)+'</li>';}).join("");
      var flag = dep.highlight ? '<span class="badge-new" style="background:rgba(216,178,106,.16);color:var(--gold-soft);border-color:rgba(216,178,106,.35)">SUSTITUYE A PHOTON</span>'
        : (dep.provisional ? '<span class="badge-new">PROVISIONAL</span>' : '');
      return '<article class="tcard reveal"'+(dep.highlight?' style="border-color:rgba(216,178,106,.4)"':'')+'>'
        + '<h4>'+esc(dep.name)+flag+'</h4>'
        + '<span class="tag">'+esc(dep.role)+'</span>'
        + '<p>'+esc(dep.desc)+'</p>'
        + '<div class="m-sub" style="margin:14px 0 8px">Usos</div><ul class="m-list">'+uses+'</ul>'
        + (dep.note?'<p style="color:var(--text-mute);font-size:12.5px;margin-top:12px">'+esc(dep.note)+'</p>':'')
        + '</article>';
    }).join("");
    return '<section class="block" id="dependencias">'
      + '<div class="eyebrow">Plataforma y arquitectura</div>'
      + '<h2 class="sec-title">Dependencias</h2>'
      + '<p class="sec-lead">El mod se desarrolla para NeoForge 1.21.1. El motor de efectos visuales pasa de Photon a <b style="color:var(--gold-soft)">AAA Particles</b> (efectos Effekseer). Varias dependencias son provisionales y se migrarán al framework interno.</p>'
      + '<div class="card-grid">'+cards+'</div></section><div class="divider"></div>';
  }


  /* ---------- RENDIMIENTO ---------- */
  function renderPerf(){
    var items = [
      ["Principio central","La dungeon puede ser enorme, pero solo una pequeña parte está activa a la vez."],
      ["Activación por sala","Estados: no cargada, preparada, activa, completada, dormida, suspendida. Solo las salas cercanas ejecutan IA, trampas, block entities y VFX."],
      ["Presupuesto de entidades","8–15 mobs activos en exploración normal; 15–25 en encuentros grandes; un jefe con acompañantes limitados. Pathfinding limitado a la sala."],
      ["Trampas y block entities","Las trampas inactivas no hacen tick; un controlador de sala gestiona señales y temporizadores; la decoración es bloque estático siempre que se pueda."],
      ["VFX y animaciones","Perfiles gráficos bajo/medio/alto: se reducen partículas, estelas, luces, objetos dentro del Gelatinous Cube y animaciones lejanas."],
      ["Multijugador","Presupuesto por grupo, límite de salas activas, reinicios bloqueados con jugadores dentro y sincronización de puertas, trampas, jefes y salas cambiantes."],
      ["Medición","El rendimiento se mide desde los primeros prototipos con perfiles de servidor y cliente. No se deja la optimización para el final."]
    ];
    var rows = items.map(function(r){
      return '<div class="meta-row reveal"><div class="rk">'+esc(r[0])+'</div><div class="rv">'+esc(r[1])+'</div></div>';
    }).join("");
    return '<section class="block" id="rendimiento">'
      + '<div class="eyebrow">Rendimiento y multijugador</div>'
      + '<h2 class="sec-title">Diseñado para rendir</h2>'
      + '<p class="sec-lead">El rendimiento se diseña desde el framework. Una sola copia de cada dungeon por mundo, generación por lotes y activación estricta por sala.</p>'
      + '<div class="meta-rows">'+rows+'</div></section><div class="divider"></div>';
  }

  /* ---------- ROADMAP ---------- */
  function renderRoadmap(){
    var phases = [
      ["01","Núcleo reutilizable","Framework interno de trampas, activadores y señales, temporizadores, hitboxes, movimiento físico, efectos, guardado, networking y herramientas de depuración."],
      ["02","Mobs y NPCs","Plantillas modulares de mobs, IA táctica, percepción y navegación, ataques, diálogos, comerciantes, memoria por jugador. Valdris, Gelatinous Cube, Neotélido y Mimic."],
      ["03","Mazmorras oficiales","Las siete dungeons, cada una con sus trampas, mobs, puzles, eventos, loot, NPCs y reglas exclusivas."],
      ["04","API de addons","Registros públicos, eventos, plantillas, documentación, ejemplos, compatibilidad estable y validadores para contenido externo."],
      ["05","Sistemas avanzados","Generación procedural selectiva, arquitectura cambiante, intercambio de salas y validación de rutas (en espera)."]
    ];
    var html = phases.map(function(p){
      return '<div class="phase reveal"><div class="pn">'+p[0]+'</div><div><h4>'+esc(p[1])+'</h4><p>'+esc(p[2])+'</p></div></div>';
    }).join("");
    return '<section class="block" id="roadmap">'
      + '<div class="eyebrow">Producción y roadmap</div>'
      + '<h2 class="sec-title">Fases de desarrollo</h2>'
      + '<p class="sec-lead">Del framework reutilizable al lanzamiento y los addons de la comunidad, que son un objetivo central del proyecto.</p>'
      + html + '</section>';
  }


  /* ---------- MODAL ---------- */
  var modal = null, modalBody = null;
  function openModal(html){
    modalBody.innerHTML = html;
    modal.classList.add("open");
    modal.setAttribute("aria-hidden","false");
    document.body.style.overflow = "hidden";
    modalBody.parentNode.scrollTop = 0;
  }
  function closeModal(){
    modal.classList.remove("open");
    modal.setAttribute("aria-hidden","true");
    document.body.style.overflow = "";
  }

  /* ---------- SCROLLSPY ---------- */
  function initScrollSpy(){
    var links = qa("#sideNav a");
    var map = {}; links.forEach(function(a){ map[a.getAttribute("data-target")] = a; });
    var ids = NAV.map(function(n){return n.id;});
    var crumb = q("#crumb");
    function onScroll(){
      var pos = window.scrollY + 140, current = ids[0];
      ids.forEach(function(id){
        var el = document.getElementById(id);
        if(el && el.offsetTop <= pos) current = id;
      });
      links.forEach(function(a){ a.classList.remove("active"); });
      if(map[current]){ map[current].classList.add("active");
        var lbl = NAV.filter(function(n){return n.id===current;})[0];
        if(lbl) crumb.textContent = lbl.label;
      }
    }
    window.addEventListener("scroll", onScroll, { passive:true });
    onScroll();
  }

  /* ---------- REVEAL ON SCROLL ---------- */
  function initReveal(){
    if(!("IntersectionObserver" in window)){ qa(".reveal").forEach(function(e){e.classList.add("in");}); return; }
    var io = new IntersectionObserver(function(entries){
      entries.forEach(function(en){ if(en.isIntersecting){ en.target.classList.add("in"); io.unobserve(en.target); } });
    }, { threshold:.12 });
    qa(".reveal").forEach(function(e){ io.observe(e); });
  }


  /* ---------- BOOT ---------- */
  function boot(){
    renderNav();
    q("#sections").innerHTML =
        renderInicio() + renderDungeons() + renderBestiario()
      + renderTrampas() + renderPuzles() + renderMecanismos()
      + renderNpcs() + renderDeps() + renderPerf() + renderRoadmap();

    renderMobGrid();

    modal = q("#modal"); modalBody = q("#modalBody");
    q("#modalClose").addEventListener("click", closeModal);
    modal.addEventListener("click", function(e){ if(e.target === modal) closeModal(); });
    document.addEventListener("keydown", function(e){ if(e.key === "Escape") closeModal(); });

    /* Dungeon + mob cards (event delegation) */
    q("#sections").addEventListener("click", function(e){
      var dc = e.target.closest("[data-dungeon]");
      if(dc){ var d = dungeonById[dc.getAttribute("data-dungeon")]; if(d) openModal(dungeonModal(d)); return; }
      var mc = e.target.closest("[data-mob]");
      if(mc){ var c = creatureById[mc.getAttribute("data-mob")]; if(c) openModal(mobModal(c)); return; }
      var chip = e.target.closest(".chip");
      if(chip){
        var f = chip.getAttribute("data-f"), v = chip.getAttribute("data-v");
        var parent = chip.parentNode;
        qa(".chip", parent).forEach(function(c){ c.classList.remove("active"); });
        chip.classList.add("active");
        bestState[f] = v; renderMobGrid();
      }
    });

    /* Global search -> feeds bestiary + jumps to it */
    var search = q("#globalSearch");
    search.addEventListener("input", function(){
      bestState.search = search.value.trim();
      renderMobGrid();
    });
    search.addEventListener("keydown", function(e){
      if(e.key === "Enter"){ document.getElementById("bestiario").scrollIntoView({behavior:"smooth"}); }
    });

    /* Mobile nav */
    var sidebar = q("#sidebar"), toggle = q("#navToggle");
    toggle.addEventListener("click", function(){ sidebar.classList.toggle("open"); });
    q("#sideNav").addEventListener("click", function(e){
      if(e.target.closest("a")) sidebar.classList.remove("open");
    });

    initScrollSpy();
    initReveal();
  }

  if(document.readyState === "loading") document.addEventListener("DOMContentLoaded", boot);
  else boot();
})();
