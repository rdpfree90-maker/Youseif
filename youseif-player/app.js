const $=s=>document.querySelector(s), $$=s=>[...document.querySelectorAll(s)];
const APP_VERSION='4.4.1';
const STORAGE_VERSION=1;
const LOGO_API='https://iptv-org.github.io/api/logos.json';
let logoIndex=null, logoLoading=null;
const normalizeName=(value)=>String(value||'').normalize('NFKC').toLocaleLowerCase().replace(/[\u064B-\u065F\u0670]/g,'').replace(/[^\p{L}\p{N}]+/gu,' ').trim().replace(/\s+/g,' ');
const safeUrl=(value,allowBlob=true)=>{try{const u=new URL(String(value||''),location.href); const ok=['http:','https:','file:']; if(allowBlob) ok.push('blob:'); return ok.includes(u.protocol)?u.href:'';}catch{return ''}};
const STORE={read(key,fallback){try{const raw=localStorage.getItem(key); if(raw==null)return fallback; return JSON.parse(raw)}catch{return fallback}}, write(key,value){try{localStorage.setItem(key,JSON.stringify(value));return true}catch(e){console.warn('storage write failed',e);return false}}, remove(key){try{localStorage.removeItem(key)}catch{}}};
const favoritesKey='youseif_favorites_v1';
const favorites=()=>new Set(STORE.read(favoritesKey,[]).filter(Boolean).map(String));
const saveFavorites=(set)=>STORE.write(favoritesKey,[...set]);
const toggleFavorite=(id)=>{const set=favorites(), key=String(id); set.has(key)?set.delete(key):set.add(key); saveFavorites(set); return set.has(key)};
const state={page:'home',speed:1,volume:1,muted:false,locked:false,cc:false,sheet:false,autoplay:true,rememberPosition:true};
window.__ypState=state;
let hls=null;
const THRIDY='https://api.thridy.com/i/';
const thridySlugs={home:'home-3d-icon',robot:'robot-3d-icon',wallpaper:'desktop-wallpaper-3d-icon',pdf:'pdf-3d-icon',heart:'heart-3d-icon',image:'image-resizer-3d-icon',coins:'coins-3d-icon',wallet:'wallet-3d-icon',money:'money-bag-3d-icon',target:'target-3d-icon',clover:'clover-3d-icon',train:'train-3d-icon',desk:'desk-3d-icon',paint:'paint-roller-3d-icon',story:'story-dice-3d-icon',bao:'bao-3d-icon'};
const SVG={
play:'<path d="M8 5v14l11-7z"/>',
pause:'<path d="M6 5h4v14H6zm8 0h4v14h-4z"/>',
prev:'<path d="M6 6h2v12H6zm3.5 6l8.5 6V6z"/>',
next:'<path d="M16 6h2v12h-2zM6 18l8.5-6L6 6v12z"/>',
seekback:'<path d="M12 5V1L7 6l5 5V7c3.3 0 6 2.7 6 6s-2.7 6-6 6-6-2.7-6-6H4c0 4.4 3.6 8 8 8s8-3.6 8-8-3.6-8-8-8z"/><text x="12" y="15" text-anchor="middle" font-size="7" fill="currentColor" font-weight="700">10</text>',
seekfwd:'<path d="M12 5V1l5 5-5 5V7c-3.3 0-6 2.7-6 6s2.7 6 6 6 6-2.7 6-6h2c0 4.4-3.6 8-8 8s-8-3.6-8-8 3.6-8 8-8z"/>',
shuffle:'<path d="M10.6 5.4L9.2 6.8 11.4 9H7v2h4.4l-2.2 2.2 1.4 1.4L14 11.2V13h2V9h-2v1.8L10.6 5.4zM17 15h-2v2h2v-2zm-6.6 0L7 18.4 8.4 19.8 11.2 17H14v-2h-3.6z"/>',
repeat:'<path d="M7 7h10v3l4-4-4-4v3H5v6h2V7zm10 10H7v-3l-4 4 4 4v-3h12v-6h-2v4z"/>',
quality:'<path d="M17 5H7c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h10c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm-5 12c-2.2 0-4-1.8-4-4s1.8-4 4-4 4 1.8 4 4-1.8 4-4 4z"/>',
audio:'<path d="M12 3v10.55A4 4 0 1 0 14 17V7h4V3h-6z"/>',
volume:'<path d="M3 10v4h4l5 5V5L7 10H3zm13.5 2c0-1.8-1-3.3-2.5-4v8c1.5-.7 2.5-2.2 2.5-4z"/>',
mute:'<path d="M16.5 12c0-1.8-1-3.3-2.5-4v2.2l2.5 2.5V12zm2.5 0c0 .9-.2 1.8-.5 2.6l1.5 1.5c.6-1.3 1-2.7 1-4.1 0-4.3-2.9-7.9-6.8-9.1v2.1c2.9 1.1 4.8 4 4.8 7zM4.3 3L3 4.3 7.7 9H3v4h4l5 5v-6.7l4.5 4.5c-.7.5-1.5.9-2.3 1.2v2.1c1.2-.3 2.3-.9 3.2-1.6L19.7 21 21 19.7 4.3 3zM12 4L9.9 6.1 12 8.2V4z"/>',
lock:'<path d="M17 8h-1V6c0-2.8-2.2-5-5-5S6 3.2 6 6v2H5c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zM12 17c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.7 1.4-3.1 3.1-3.1S15.1 4.3 15.1 6v2z"/>',
unlock:'<path d="M12 17c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm6-9h-1V6c0-2.8-2.2-5-5-5-2.3 0-4.3 1.6-4.9 3.7l1.9.6C9.4 4.3 10.6 3.4 12 3.4c1.7 0 3.1 1.4 3.1 3.1v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2z"/>',
fullscreen:'<path d="M7 14H5v5h5v-2H7v-3zm-2-4h2V7h3V5H5v5zm12 7h-3v2h5v-5h-2v3zM14 5v2h3v3h2V5h-5z"/>',
pip:'<path d="M3 5h18v14H3V5zm2 2v10h14V7H5zm8 5h5v3h-5v-3z"/>',
cast:'<path d="M21 3H3c-1.1 0-2 .9-2 2v3h2V5h18v14h-7v2h7c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM1 18v3h3c0-1.7-1.3-3-3-3zm0-4v2c2.8 0 5 2.2 5 5h2c0-3.9-3.1-7-7-7zm0-4v2c5 0 9 4 9 9h2c0-6.1-4.9-11-11-11z"/>',
refresh:'<path d="M17.6 6.4A8 8 0 0 0 4.4 12H2l3.5 3.5L9 12H6.4A5.5 5.5 0 1 1 12 17.5 5.5 5.5 0 0 1 6.9 14H4.7a7.5 7.5 0 1 0 2.2-7.1L5.4 5.4z"/>',
info:'<path d="M11 7h2v2h-2V7zm0 4h2v6h-2v-6zm1-9C6.5 2 2 6.5 2 12s4.5 10 10 10 10-4.5 10-10S17.5 2 12 2zm0 18c-4.4 0-8-3.6-8-8s3.6-8 8-8 8 3.6 8 8-3.6 8-8 8z"/>',
settings:'<path d="M19.1 12.9c0-.3.1-.6.1-.9s0-.6-.1-.9l2-1.6c.2-.1.2-.4.1-.6l-1.9-3.3c-.1-.2-.4-.3-.6-.2l-2.4 1c-.5-.4-1-.7-1.6-.9l-.4-2.5c0-.2-.2-.4-.5-.4h-3.8c-.2 0-.5.2-.5.4l-.4 2.5c-.6.2-1.1.5-1.6.9l-2.4-1c-.2-.1-.5 0-.6.2L2.7 8.9c-.1.2-.1.5.1.6l2 1.6c0 .3-.1.6-.1.9s0 .6.1.9l-2 1.6c-.2.1-.2.4-.1.6l1.9 3.3c.1.2.4.3.6.2l2.4-1c.5.4 1 .7 1.6.9l.4 2.5c0 .2.2.4.5.4h3.8c.2 0 .5-.2.5-.4l.4-2.5c.6-.2 1.1-.5 1.6-.9l2.4 1c.2.1.5 0 .6-.2l1.9-3.3c.1-.2.1-.5-.1-.6l-2-1.6zM12 15.5A3.5 3.5 0 1 1 12 8.5a3.5 3.5 0 0 1 0 7z"/>',
search:'<path d="M15.5 14h-.8l-.3-.3A6.5 6.5 0 1 0 14 15.5l.3.3v.8l5 5 1.5-1.5-5-5zm-6 0C7 14 5 12 5 9.5S7 5 9.5 5 14 7 14 9.5 12 14 9.5 14z"/>',
download:'<path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"/>',
home:'<path d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"/>',
cc:'<path d="M19 4H5c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm-8 7H9.5v-.5h-2v3h2V13H11c.6 0 1-.4 1-1v-1c0-.6-.4-1-1-1zm7 0h-1.5v-.5h-2v3h2V13H18c.6 0 1-.4 1-1v-1c0-.6-.4-1-1-1z"/>',
paste:'<path d="M19 2h-4.2c-.4-1.2-1.5-2-2.8-2s-2.4.8-2.8 2H5c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-7 0c.6 0 1 .4 1 1s-.4 1-1 1-1-.4-1-1 .4-1 1-1zm7 18H5V4h2v3h10V4h2v16z"/>',
films:'<path d="M18 4l2 4h-3l-2-4h-2l2 4h-3l-2-4H8l2 4H7L5 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V4h-4z"/>',
channels:'<path d="M21 3H3c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h5v2h8v-2h5c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 14H3V5h18v12z"/>',
sliders:'<path d="M3 17v2h6v-2H3zM3 5v2h10V5H3zm10 16v-2h8v-2h-8v-2h-2v6h2zM7 9v2H3v2h4v2h2V9H7zm14 4v-2H11v2h10zm-6-4h2V7h4V5h-4V3h-2v6z"/>',
close:'<path d="M19 6.4L17.6 5 12 10.6 6.4 5 5 6.4 10.6 12 5 17.6 6.4 19 12 13.4 17.6 19 19 17.6 13.4 12z"/>',
brightness:'<path d="M12 7a5 5 0 1 0 0 10 5 5 0 0 0 0-10zm0-5v2m0 16v2M4.2 4.2l1.4 1.4m12.8 12.8l1.4 1.4M1 12h2m18 0h2M4.2 19.8l1.4-1.4m12.8-12.8l1.4-1.4"/>'
};
const svgIcon=(name,cls='')=>{
  const p=SVG[name]||SVG.play;
  return `<span class="svg-ico ${cls}" data-ico="${name}" aria-hidden="true"><svg viewBox="0 0 24 24" fill="currentColor" width="1em" height="1em">${p}</svg></span>`;
};
/* Prefer local 3D PNG for nav; SVG for player controls (follow accent color) */
const icon=n=>{
  const local=['home','channels','films','downloads','settings','play','pause','lock','volume','refresh','cast','fullscreen','info','search','sliders','cc'];
  if(local.includes(n)) return `<img class="icon3d icon-tint icon-${n}" data-icon="${n}" src="assets/icons3d/${n}.png" alt="" loading="lazy" onerror="this.onerror=null;this.outerHTML=window.__svgFallback&&window.__svgFallback('${n}')||''">`;
  return svgIcon(n);
};
const emoji3d=(name,alt='')=>svgIcon(name);
window.__svgFallback=n=>svgIcon(n);
const remoteIcon=(slug,size=128)=>`${THRIDY}${slug}/${size}.webp`;
const themeKey='youseif_theme_v4';
const themes={
 aurora:{name:'Aurora Flow',accent:'#df4dff',accent2:'#42e7ff',glow:'rgba(161,65,255,.28)',bg:'#05031a',panel:'#110b25'},
 red:{name:'Red Neon',accent:'#ff3042',accent2:'#ff6570',glow:'rgba(255,48,66,.22)',bg:'#030304',panel:'#08090c'},
 green:{name:'Green Neon',accent:'#24f39a',accent2:'#8affc9',glow:'rgba(36,243,154,.22)',bg:'#020b08',panel:'#071713'},
 cyan:{name:'Cyber Cyan',accent:'#20d9ff',accent2:'#76ebff',glow:'rgba(32,217,255,.20)',bg:'#020507',panel:'#071015'},
 purple:{name:'Neon Purple',accent:'#b34cff',accent2:'#db9aff',glow:'rgba(179,76,255,.20)',bg:'#040207',panel:'#0c0811'},
 emerald:{name:'Emerald Glass',accent:'#28e69a',accent2:'#79f4c2',glow:'rgba(40,230,154,.18)',bg:'#020705',panel:'#07100d'},
 ice:{name:'Ice Glass',accent:'#9ddcff',accent2:'#d7f1ff',glow:'rgba(157,220,255,.20)',bg:'#030609',panel:'#081018'},
 gold:{name:'Gold Glass',accent:'#ffc84a',accent2:'#ffe29a',glow:'rgba(255,200,74,.18)',bg:'#070503',panel:'#100b06'}
};
function applyTheme(name,notify=false){
  const t=themes[name]||themes.red;
  document.documentElement.dataset.theme=name;
  Object.entries(t).forEach(([k,v])=>{
    if(k==='name') return;
    document.documentElement.style.setProperty('--'+(k==='accent'?'accent':k==='accent2'?'accent2':k==='glow'?'glow':k==='bg'?'bg':k==='panel'?'panel':k), v);
  });
  // ensure derived glow if missing
  if(t.accent){
    try{
      var h=t.accent; var r=parseInt(h.slice(1,3),16),g=parseInt(h.slice(3,5),16),b=parseInt(h.slice(5,7),16);
      if(!t.glow) document.documentElement.style.setProperty('--glow','rgba('+r+','+g+','+b+',0.28)');
    }catch(e){}
  }
  localStorage.setItem(themeKey,name);
  // clear custom accent override when picking a preset theme
  try{ localStorage.removeItem('youseif_custom_accent'); }catch(e){}
  const meta=document.getElementById('themeColor');
  if(meta) meta.content=t.accent||t.bg;
  applyIconTint(t.accent);
  if(notify) toast(t.name+' applied');
}

function currentTheme(){return localStorage.getItem(themeKey)||'aurora';}
function iconSource(name){const slug=thridySlugs[name]; return slug?remoteIcon(slug,128):`assets/icons3d/${name}.png`;}
function upgrade3DIcons(root=document){root.querySelectorAll('img.icon3d[data-icon]').forEach(img=>{const n=img.dataset.icon; const slug=thridySlugs[n]; if(slug && !img.dataset.remote){img.dataset.remote='1';const local=img.src; img.src=remoteIcon(slug,128); img.onerror=function(){this.onerror=null;this.src=local;this.dataset.remote='0';};}})}
const pages={
 home:()=>`<section class="page home">
  <div class="url-row"><label class="url-box"><span class="chain">⌁</span><input id="streamUrl" placeholder="Paste video URL or stream link" autocomplete="off"><button class="copy glass-chip" id="copyUrl" type="button" aria-label="Paste from clipboard">${svgIcon('paste')}</button></label><button class="play-btn glass-play" id="playUrl" type="button"><span class="play-ico" aria-hidden="true">${svgIcon('play')}</span><span class="play-label">PLAY</span></button></div>
  <div class="player" id="player">
    <div class="player-head">
      <div class="stream-left"><span class="live-tag"><i class="live-dot"></i>LIVE</span><span class="stream-title">Live Stream</span></div>
      <div class="player-actions">
        <button class="head-btn" id="settingsTopBtn" type="button" aria-label="Settings">${svgIcon('settings')}</button>
        <button class="head-btn head-cc" id="ccTopBtn" type="button" aria-label="Subtitles"><span>CC</span></button>
        <button class="head-btn" id="castBtn" type="button" aria-label="Cast">${svgIcon('cast')}</button>
      </div>
    </div>
    <div class="video-wrap">
      <video id="video" playsinline preload="metadata"></video>
      <div class="visual">
        <!-- left mid: audio-only -->
        <button class="side-fab side-left" id="audioOnlyBtn" type="button" aria-label="Audio only" title="Audio only">
          <svg class="svg-ico" viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M12 3v10.55A4 4 0 1 0 14 17V7h4V3h-6z"/></svg>
        </button>
        <!-- right mid: lock + rotate under it -->
        <div class="side-stack side-right">
          <button class="side-fab" id="lockBtn" type="button" aria-label="Lock controls">${svgIcon('lock')}</button>
          <button class="side-fab" id="orientBtn" type="button" aria-label="Rotate screen" title="Portrait / Landscape">
            <svg class="svg-ico" viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M16.48 2.52c3.27 1.55 5.61 4.72 5.97 8.48h1.5C23.44 4.84 18.29 0 12 0l-.66.03 3.81 3.81 1.33-1.32zm-6.25-.77c-.59-.59-1.54-.59-2.13 0L1.39 8.46c-.59.59-.59 1.54 0 2.13l10.23 10.23c.59.59 1.54.59 2.13 0l6.71-6.71c.59-.59.59-1.54 0-2.13L10.23 1.75zm7.06 14.48L4.41 9.34l6.71-6.72 12.88 12.88-6.71 6.72z"/></svg>
          </button>
        </div>
        <!-- center: only play/pause, large, appears on tap -->
        <button class="center-play-only" id="centerPlay" type="button" aria-label="Play or pause">${svgIcon('play')}</button>
      </div>
    </div>
    <div class="timeline"><span id="currentTime">00:00</span><input id="timeline" type="range" min="0" max="100" value="0"><span id="duration">00:00</span><span class="red">LIVE</span></div>
    <div class="controls" aria-label="Player controls">
      <div class="dock-bar" aria-label="Quick player controls">
        <button type="button" class="dock-btn" id="muteBtn" aria-label="Volume">${svgIcon('volume')}</button>
        <button type="button" class="dock-btn dock-label" id="speedBtn" aria-label="Playback speed"><span>1.0x</span></button>
        <button type="button" class="dock-btn dock-label" id="fitBtn" aria-label="Fit mode"><span>FIT</span></button>
        <button type="button" class="dock-btn" id="pipBtn" aria-label="Picture in picture">${svgIcon('pip')}</button>
        <button type="button" class="dock-btn" id="fullBtn" aria-label="Fullscreen">${svgIcon('fullscreen')}</button>
      </div>
    </div>
  </div>
  <div class="info-grid"><div class="info-card"><div class="info-head"><b>STREAM INFO</b><span>●</span></div><div id="streamInfo"><div class="info-row"><span>Resolution</span><span>Auto</span></div><div class="info-row"><span>Codec</span><span>—</span></div><div class="info-row"><span>Bitrate</span><span>Auto</span></div><div class="info-row"><span>Status</span><span>Ready</span></div></div></div><div class="info-card connection-card"><div class="info-head"><b>CONNECTION</b><span id="qualityLive" class="live-quality warn">CHECKING</span></div><div class="connection-grid"><div><small>NETWORK</small><b id="connectionState">Checking…</b></div><div><small>PING</small><b id="pingValue">—</b></div><div><small>SPEED</small><b id="speedValue">Detecting…</b></div><div><small>BUFFER</small><b id="bufferValue">—</b></div></div><div class="chart"><svg viewBox="0 0 220 60" preserveAspectRatio="none"><polyline id="connectionChart" points="0,48 20,45 38,49 56,29 73,38 90,18 108,31 125,12 145,28 163,22 181,35 200,17 220,23" fill="none" stroke="var(--accent)" stroke-width="2"/><polyline points="0,59 220,59" stroke="#272a2f" stroke-width="1"/></svg></div><div class="chart-meta"><span>real-time</span><span id="homePing">—</span></div></div></div>
</section>`,
 channels:()=>`<section class="page"><div class="title">CHANNELS <span style="color:var(--accent)" id="channelCount">0</span></div><p class="sub">Live channels and groups · multiple playlists supported</p><div class="search"><label class="searchbox">${icon('search')}<input id="channelSearch" placeholder="Search channels"></label><button class="filter-btn" id="addFile">+ PLAYLIST</button></div><div class="chips"><button class="chip active" data-filter="all">ALL</button><button class="chip" data-filter="fav">FAVORITES</button><button class="chip" data-filter="kids">KIDS</button><button class="chip" data-filter="sports">SPORTS</button><button class="chip" data-filter="news">NEWS</button></div><div class="group-row"><button class="category" data-groupjump="Movies">${icon('films')}<b>MOVIES</b></button><button class="category" data-groupjump="Kids">${icon('channels')}<b>CARTOON</b></button><button class="category" id="playlistUrlBtn">${icon('downloads')}<b>PLAYLIST URL</b></button><button class="category" id="addFile2">${icon('settings')}<b>ADD FILES</b></button></div><div id="channelList"><div class="empty">Loading channels…</div></div></section>`,
 films:()=>`<section class="page"><div class="title">FILMS</div><p class="sub">Movies · Cinema · Cartoon · Anime · Local files & links</p>
<div class="url-row" style="margin-bottom:12px"><label class="url-box"><span class="chain">⌁</span><input id="filmUrl" placeholder="Paste movie / series URL or file link" autocomplete="off"></label><button class="play-btn glass-play" id="playFilmUrl" type="button"><span class="play-ico">${svgIcon('play')}</span><span>PLAY</span></button></div>
<div class="group-row films-row" style="margin-bottom:12px">
  <button class="category" id="filmFileBtn">${icon('films')}<b>OPEN FILE</b></button>
  <button class="category" id="filmFolderHint">${icon('downloads')}<b>ZIP / ARCHIVE</b></button>
  <button class="category film-cat" data-cat="movies">${icon('films')}<b>MOVIES</b></button>
  <button class="category film-cat" data-cat="series">${icon('channels')}<b>SERIES</b></button>
  <button class="category film-cat" data-cat="anime">${icon('play')}<b>ANIME</b></button>
  <button class="category film-cat" data-cat="cartoon">${icon('channels')}<b>CARTOON</b></button>
</div>
<div class="section-head"><h2>RECENTLY ADDED</h2><span class="link" id="filmClear">CLEAR</span></div>
<div class="posters" id="filmRecent"></div>
<div class="section-head"><h2>CATEGORIES</h2></div>
<div class="chips"><button class="chip active" data-fcat="all">ALL</button><button class="chip" data-fcat="action">ACTION</button><button class="chip" data-fcat="drama">DRAMA</button><button class="chip" data-fcat="comedy">COMEDY</button><button class="chip" data-fcat="anime">ANIME</button></div>
<p class="sub" style="margin-top:14px">Supported files: MP4, MKV, WebM, AVI, MOV, 3GP, TS, M2TS, MPEG, FLV, WMV, OGV… · Streams: HTTP/HTTPS, HLS, DASH</p>
</section>`,
 downloads:()=>`<section class="page"><div class="title">DOWNLOADS</div><p class="sub">Download Manager · progressive media · queue · pause/resume</p>
<div class="url-row" style="margin-bottom:12px"><label class="url-box"><span class="chain">⌁</span><input id="dlUrl" placeholder="Paste direct file URL to download" autocomplete="off"></label><button class="play-btn glass-play" id="startDl" type="button"><span>DOWNLOAD</span></button></div>
<div class="section-head"><h2>ACTIVE</h2><span class="link" id="dlClearActive">CLEAR FAILED</span></div>
<div class="downloads-list" id="dlActive"><div class="empty">No active downloads</div></div>
<div class="section-head"><h2>COMPLETED</h2><span class="link" id="dlClearDone">CLEAR ALL</span></div>
<div class="downloads-list" id="dlDone"><div class="empty">No completed downloads yet</div></div>
<p class="sub" style="margin-top:12px">Supports progressive HTTP/HTTPS files (MP4, WebM, MP3…). HLS/DASH live streams cannot be saved as a single file in the browser.</p>
</section>`,
 settings:()=>`<section class="page settings-page">
  <div class="title">SETTINGS</div>
  <p class="sub">مظهر التطبيق والتشغيل</p>
  <div class="settings-list">
    <button type="button" class="setting" data-setting="colors" onclick="window.openColorsSheet&&window.openColorsSheet()">
      ${svgIcon('quality')}<div><b>الألوان والمظهر</b><small>ثيمات جاهزة · لون مخصص · أيقونات · خط</small></div>
    </button>
    <button type="button" class="setting" data-setting="file" onclick="window.handleSetting&&window.handleSetting('file',this)">
      ${svgIcon('download')}<div><b>Import Playlist</b><small>Load M3U / M3U8 file</small></div>
    </button>
    <button type="button" class="setting" data-setting="url" onclick="window.handleSetting&&window.handleSetting('url',this)">
      ${svgIcon('cast')}<div><b>Playlist URL</b><small>Import from a web address</small></div>
    </button>
    <button type="button" class="setting" data-setting="playback" onclick="window.openPlaybackSheet&&window.openPlaybackSheet()">
      ${svgIcon('sliders')}<div><b>Playback Settings</b><small>Speed, autoplay and controls</small></div>
    </button>
    <button type="button" class="setting" data-setting="subtitle" onclick="window.openSubtitleSheet&&window.openSubtitleSheet()">
      ${svgIcon('cc')}<div><b>Subtitle Settings</b><small>Captions and appearance</small></div>
    </button>
    <button type="button" class="setting" data-setting="reset" onclick="window.handleSetting&&window.handleSetting('reset',this)">
      ${svgIcon('refresh')}<div><b>استعادة الافتراضي</b><small>إعادة الألوان والأيقونات والخط كما كانت</small></div>
    </button>
    <button type="button" class="setting" data-setting="cache" onclick="window.handleSetting&&window.handleSetting('cache',this)">
      ${svgIcon('refresh')}<div><b>Clear Local Data</b><small>Remove saved playlists and preferences</small></div>
    </button>
    <button type="button" class="setting" data-setting="about" onclick="window.handleSetting&&window.handleSetting('about',this)">
      ${svgIcon('info')}<div><b>About</b><small>Youseif Player Pro</small></div>
    </button>
  </div>
</section>`,
};
function toast(msg){let t=$('.toast');if(!t){t=document.createElement('div');t.className='toast';document.body.append(t)}t.textContent=msg;t.classList.add('show');clearTimeout(t._t);t._t=setTimeout(()=>t.classList.remove('show'),1700)}
function render(page){if(!pages[page])page='home';state.page=page;$('#content').innerHTML=pages[page]();$$('.nav').forEach(n=>n.classList.toggle('active',n.dataset.page===page));window.scrollTo({top:0,behavior:'instant'});upgrade3DIcons();if(page==='home'){wireHome();setTimeout(forcePlayerLayout,0);setTimeout(forcePlayerLayout,50);}if(page==='channels')wireChannels();if(page==='films')wireFilms();if(page==='downloads')wireDownloads();if(page==='settings')wireSettings()}
function wireHome(){
  const v=$('#video'),input=$('#streamUrl'),play=$('#playUrl'),center=$('#centerPlay'),timeline=$('#timeline');
  let currentUrl='';
  let loadGeneration=0;
  const positionKey=()=>currentUrl?'youseif_position_'+normalizeName(currentUrl):'';
  const savedPlayback=STORE.read('youseif_playback',{});
  state.speed=Number(savedPlayback.speed)||1; state.autoplay=savedPlayback.autoplay!==false; state.rememberPosition=savedPlayback.pos!==false;
  v.playbackRate=state.speed; let metricTimer=null; let mediaMode='unknown'; let hlsLive=false; const pingHistory=[];
  let audioOnly=false; let orientLandscape=false;
  // Web Audio gain for volume up to 300%
  // WebAudio gain singleton (survives page re-renders)
  const showLevelHud=(label, pct)=>{
    let hud=document.getElementById('levelHud');
    if(!hud){
      hud=document.createElement('div'); hud.id='levelHud'; hud.className='level-hud';
      (document.querySelector('.video-wrap')||document.body).appendChild(hud);
    }
    const bar = Math.max(0, Math.min(100, pct));
    hud.innerHTML = '<div class="level-label">'+label+'</div><div class="level-track"><i style="width:'+bar+'%"></i></div><div class="level-pct">'+Math.round(pct)+'%</div>';
    hud.classList.add('show');
    clearTimeout(hud._t); hud._t=setTimeout(()=>hud.classList.remove('show'), 900);
  };
  const ensureGain=()=>{
    try{
      if(!window.__ypGain){
        const ctx = new (window.AudioContext||window.webkitAudioContext)();
        const g = ctx.createGain();
        g.gain.value = state.volume || 1;
        window.__ypGain = { ctx, gain:g, connected:false };
      }
      const G = window.__ypGain;
      if(!G.connected){
        try{
          const src = G.ctx.createMediaElementSource(v);
          src.connect(G.gain);
          G.gain.connect(G.ctx.destination);
          G.connected = true;
          G.src = src;
        }catch(err){
          // element already connected
          G.connected = true;
        }
      }
      if(G.ctx.state==='suspended') G.ctx.resume();
      return G;
    }catch(e){ console.warn('gain', e); return null; }
  };
  const setGain=(val, show)=>{
    state.volume = Math.max(0, Math.min(3, +val || 0));
    try{
      const G = ensureGain();
      if(G && G.gain) G.gain.value = state.volume;
    }catch(e){}
    try{
      // element volume max 1; boost beyond 1 is via GainNode only
      v.volume = Math.min(1, Math.max(0, state.volume > 0 ? 1 : 0));
      v.muted = state.volume===0;
    }catch(e){}
    if(show!==false) showLevelHud('VOLUME', state.volume*100);
  };
  try{ setGain(state.volume || 1, false); }catch(e){}

  const setPlay=()=>{
    const playing=!v.paused;
    if(center){
      center.innerHTML=svgIcon(playing?'pause':'play');
      center.classList.toggle('playing', playing);
      center.setAttribute('aria-label', playing?'Pause':'Play');
    }
    if(play){
      const ico=play.querySelector('.play-ico');
      if(ico) ico.innerHTML=svgIcon(playing?'pause':'play');
      const label=play.querySelector('.play-label');
      if(label) label.textContent=playing?'PAUSE':'PLAY';
    }
  };
  const setType=(type)=>{
    // Once we know live/video, never fall back to detecting/unknown on every tick
    if(type==='unknown' && mediaMode!=='unknown') return;
    if((type==='live'||type==='video') && mediaMode===type) return; // no-op same state
    if(type==='unknown' && mediaMode==='unknown') {
      // only update UI once while still detecting
    }
    mediaMode=type;
    const tag=$('.live-tag'),title=$('.stream-title');
    if(!tag||!title)return;
    if(type==='live'){tag.innerHTML='<i class="live-dot"></i>LIVE';title.textContent='Live Stream';tag.classList.add('is-live')}
    else if(type==='video'){tag.innerHTML='<i class="vod-dot"></i>VIDEO';title.textContent='Video';tag.classList.remove('is-live')}
    else{tag.innerHTML='<i class="detect-dot"></i>DETECT';title.textContent='Detecting media';tag.classList.remove('is-live')}
    $$('.timeline .red').forEach(x=>x.textContent=type==='live'?'LIVE':type==='video'?'VOD':'—');
  };
  const updateMediaInfo=()=>{const duration=v.duration,live=hlsLive||duration===Infinity;if(v.readyState>0&&(live||Number.isFinite(duration)))setType(live?'live':'video');else if(v.readyState===0&&mediaMode==='unknown')setType('unknown');if(Number.isFinite(duration)){timeline.disabled=false;timeline.value=duration?Math.min(100,v.currentTime/duration*100):0;timeline.style.setProperty('--progress',timeline.value+'%');$('#currentTime').textContent=fmt(v.currentTime);$('#duration').textContent=fmt(duration)}else{timeline.disabled=true;timeline.value=0;$('#duration').textContent=live?'LIVE':'—';$('#currentTime').textContent=live?'LIVE':'00:00'}const res=v.videoWidth&&v.videoHeight?v.videoWidth+'×'+v.videoHeight:'Detecting…';const status=v.error?'Error':v.readyState>=3?(v.paused?'Ready':'Playing'):v.readyState>=1?'Metadata loaded':'Buffering';const typeText=mediaMode==='live'?'Live stream':mediaMode==='video'?'Video':'Detecting…';const info=$('#streamInfo'); if(info) info.innerHTML=`<div class="info-row"><span>Type</span><span>${typeText}</span></div><div class="info-row"><span>Duration</span><span>${Number.isFinite(duration)?fmt(duration):(mediaMode==='live'?'LIVE':'Detecting…')}</span></div><div class="info-row"><span>Resolution</span><span>${res}</span></div><div class="info-row"><span>Status</span><span>${status}</span></div>`};
  const updateChart=(rtt)=>{
    const chart=$('#connectionChart'); if(!chart||!Number.isFinite(rtt)) return;
    pingHistory.push(Math.min(180, Math.max(1,rtt)));
    if(pingHistory.length>28) pingHistory.shift();
    const n=pingHistory.length;
    const pts=pingHistory.map((p,i)=>{
      const x = n<=1 ? 0 : i*(220/(n-1));
      const y = 52 - (p/180)*44;
      return x.toFixed(1)+','+y.toFixed(1);
    }).join(' ');
    chart.setAttribute('points', pts);
    // area fill under line
    let area=$('#connectionArea');
    if(!area){
      const svg=chart.ownerSVGElement; if(svg){
        area=document.createElementNS('http://www.w3.org/2000/svg','polygon');
        area.id='connectionArea';
        area.setAttribute('fill','url(#netGrad)');
        area.setAttribute('stroke','none');
        svg.insertBefore(area, chart);
        const defs=document.createElementNS('http://www.w3.org/2000/svg','defs');
        defs.innerHTML='<linearGradient id="netGrad" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="var(--accent)" stop-opacity=".45"/><stop offset="100%" stop-color="var(--accent)" stop-opacity="0"/></linearGradient>';
        svg.insertBefore(defs, svg.firstChild);
      }
    }
    if(area){
      const first=pts.split(' ')[0]||'0,52';
      const last=pts.split(' ').slice(-1)[0]||'220,52';
      const fx=first.split(',')[0], lx=last.split(',')[0];
      area.setAttribute('points', pts+' '+lx+',58 '+fx+',58');
    }
    chart.setAttribute('stroke','var(--accent)');
    chart.setAttribute('stroke-width','2.4');
    chart.setAttribute('stroke-linecap','round');
    chart.setAttribute('stroke-linejoin','round');
    chart.setAttribute('fill','none');
  };
  const updateConnection=()=>{const online=navigator.onLine;const st=$('#connectionState'),cs=$('#connStatus'),ql=$('#qualityLive');if(st)st.textContent=online?'Online':'Offline';if(cs)cs.textContent=online?'Online':'Offline';if(ql){ql.textContent=online?'ONLINE':'OFF';ql.className='live-quality '+(online?'ok':'warn')}const buf=v.buffered&&v.buffered.length? (v.buffered.end(v.buffered.length-1)-v.currentTime):0; const bv=$('#bufferValue'); if(bv) bv.textContent=buf>0?buf.toFixed(1)+'s':'—'};
  const pingServer=async()=>{try{const t0=performance.now();await fetch(location.href,{method:'HEAD',cache:'no-store'});const rtt=performance.now()-t0;const pv=$('#pingValue'),hp=$('#homePing');if(pv)pv.textContent=Math.round(rtt)+' ms';if(hp)hp.textContent=Math.round(rtt)+' ms';updateChart(rtt)}catch(e){}};
  const startMetrics=()=>{if(metricTimer)clearInterval(metricTimer);metricTimer=setInterval(()=>{updateMediaInfo();updateConnection();pingServer()},4000);pingServer()};
  const fmt=(s)=>{if(!Number.isFinite(s))return'—';s=Math.max(0,s|0);const h=(s/3600)|0,m=((s%3600)/60)|0,sec=s%60;return (h?h+':':'')+String(m).padStart(h?2:1,'0')+':'+String(sec).padStart(2,'0')};

  const load=async()=>{
    const url=safeUrl(input.value.trim());
    const generation=++loadGeneration;
    if(!url){toast('Paste a video, stream or playlist URL first');return}
    if(!url){toast('الرابط غير صالح أو غير مدعوم');return}
    currentUrl=url; mediaMode='unknown'; hlsLive=false;
    if(hls){try{hls.destroy()}catch(e){} hls=null}
    if(window.__dashPlayer){try{window.__dashPlayer.reset()}catch(e){} window.__dashPlayer=null}
    try{ while(v.querySelectorAll('track[data-youseif]').length){ v.querySelector('track[data-youseif]').remove(); } }catch(e){}
    const wrap=$('.video-wrap'); wrap.classList.remove('has-video'); setType('unknown');
    if(/\.m3u($|\?)/i.test(url) || /get\.php|player_api\.php|xtream/i.test(url)){
      toast('Playlist detected — importing…');
      try{ const n=await importPlaylistUrl(url); toast(n+' channels imported'); goPage('channels'); }catch(e){ toast('Playlist import failed'); }
      return;
    }
    const isHls=/\.m3u8($|\?)/i.test(url) || /\/hls\//i.test(url) || /format=m3u8/i.test(url);
    const isDash=/\.mpd($|\?)/i.test(url) || /\/dash\//i.test(url);
    const isAudio=/\.(mp3|aac|m4a|wav|flac|ogg|opus|ac3|eac3|amr)($|\?)/i.test(url);
    const isLimited=/\.(mkv|avi|ts|mts|m2ts|flv|f4v|wmv|asf|vob|rm|rmvb|divx|xvid|ogv)($|\?)/i.test(url);
    const isProgressive=/\.(mp4|m4v|webm|mov|3gp|3g2|mpeg|mpg|ogg)($|\?)/i.test(url);
    if(/^(rtsp|rtmp|rtp|udp):/i.test(url)){
      toast('RTSP/RTMP/RTP/UDP غير مدعوم في المتصفح — استخدم HLS/HTTP');
      return;
    }
    try{
      if(isHls && window.Hls && Hls.isSupported()){
        let fatalRetries=0;
        hls=new Hls({
          enableWorker:true,
          lowLatencyMode:false,
          backBufferLength:30,
          maxBufferLength:45,
          maxMaxBufferLength:90,
          startLevel:-1,
          abrEwmaDefaultEstimate:500000,
          manifestLoadingTimeOut:15000,
          manifestLoadingMaxRetry:3,
          levelLoadingTimeOut:15000,
          levelLoadingMaxRetry:3,
          fragLoadingTimeOut:20000,
          fragLoadingMaxRetry:4
        });
        hls.loadSource(url); hls.attachMedia(v);
        hls.on(Hls.Events.MANIFEST_PARSED,()=>{
          if(generation!==loadGeneration)return;
          wrap.classList.add('has-video');
          updateMediaInfo();
          startMetrics();
          ensureGain();
          if(state.autoplay) v.play().catch(()=>toast('اضغط PLAY لبدء التشغيل'));
          else toast('HLS stream ready');
        });
        hls.on(Hls.Events.LEVEL_LOADED,(_,data)=>{
          if(generation!==loadGeneration)return;
          hlsLive=!!(data&&data.details&&data.details.live);
          setType(hlsLive?'live':'video');
          updateMediaInfo();
          updateConnection();
        });
        hls.on(Hls.Events.ERROR,(_,data)=>{
          if(!data||!data.fatal) return;
          if(generation!==loadGeneration) return;
          if(data.type===Hls.ErrorTypes.NETWORK_ERROR){
            if(fatalRetries<3){ fatalRetries++; try{hls.startLoad()}catch(e){}; toast('Network error — retry '+fatalRetries); }
            else { toast('Stream failed after retries'); updateConnection(); }
          } else if(data.type===Hls.ErrorTypes.MEDIA_ERROR){
            if(fatalRetries<3){ fatalRetries++; try{hls.recoverMediaError()}catch(e){}; toast('Media error — recovering…'); }
            else { toast('Stream failed'); updateConnection(); }
          } else {
            toast('Stream error'); updateConnection();
          }
        });
      } else if(isDash && window.dashjs){
        window.__dashPlayer=dashjs.MediaPlayer().create();
        window.__dashPlayer.updateSettings({streaming:{retryAttempts:{MDP:3,XLink:3,MediaSegment:3}}});
        window.__dashPlayer.initialize(v, url, true);
        wrap.classList.add('has-video'); setType('video'); updateMediaInfo(); startMetrics(); toast('DASH stream ready');
      } else if(v.canPlayType('application/vnd.apple.mpegurl') && isHls){
        v.src=url; await v.play().catch(()=>{}); wrap.classList.add('has-video'); setType('live'); toast('HLS native');
      } else {
        v.src=url; v.load();
        try{
          if(state.autoplay) await v.play();
          wrap.classList.add('has-video');
          if(isAudio) toast('Audio / Radio stream');
          else if(isLimited) toast('تم التشغيل (دعم محدود حسب المتصفح)');
          else toast(isProgressive?'Video loaded':'Media detected');
        }catch(e){
          toast(isLimited?'الصيغة قد تحتاج دعم أجهزة أقوى أو تطبيق أصلي':'URL loaded — press PLAY');
        }
        updateMediaInfo(); startMetrics();
      }
      // re-apply audio only state
      if(audioOnly){ v.style.opacity='0'; v.classList.add('audio-only-mode'); }
    }catch(err){
      console.error(err);
      toast('فشل التحميل — تحقق من الرابط أو الصيغة');
      updateConnection();
    }
  };

  const hasMedia = ()=> !!(v.getAttribute('src') || v.src || v.currentSrc || (hls && hls.media) || window.__dashPlayer);
  let isLoading=false;
  const waitCanPlay = ()=> new Promise((resolve)=>{
    if(v.readyState >= 2) return resolve(true);
    const done = ()=>{ cleanup(); resolve(true); };
    const fail = ()=>{ cleanup(); resolve(false); };
    const cleanup = ()=>{
      v.removeEventListener('canplay', done);
      v.removeEventListener('loadeddata', done);
      v.removeEventListener('error', fail);
      clearTimeout(tm);
    };
    v.addEventListener('canplay', done, {once:true});
    v.addEventListener('loadeddata', done, {once:true});
    v.addEventListener('error', fail, {once:true});
    const tm=setTimeout(()=>resolve(v.readyState>=1), 8000);
  });
  const togglePlay = async ()=>{
    const url = (input.value||'').trim();
    const urlChanged = !!(url && currentUrl && url !== currentUrl);
    const needLoad = (!hasMedia() && url) || urlChanged || (!currentUrl && url);
    if(needLoad){
      if(isLoading) return;
      isLoading=true;
      try{ await load(); } finally { isLoading=false; }
      return;
    }
    if(!hasMedia()){
      toast('Paste a URL first');
      return;
    }
    try{
      if(v.paused){
        ensureGain();
        if(v.readyState < 2) await waitCanPlay();
        await v.play();
        setPlay();
      } else {
        v.pause();
        setPlay();
      }
    }catch(e){
      console.warn('play', e);
      // only reload if source is broken
      if(url && v.error) { await load(); }
      else toast('اضغط مرة أخرى للتشغيل');
    }
  };
  play.onclick = ()=> togglePlay();
  if(center) center.onclick = (e)=>{ e.stopPropagation(); togglePlay(); };
  // tap video to toggle play (never reload)
  const wrapEl=$('.video-wrap');
  if(wrapEl){
    wrapEl.addEventListener('click',(e)=>{
      if(e.target.closest('button')) return;
      if(state.locked) return;
      togglePlay();
    });
  }

  if($('#copyUrl')) $('#copyUrl').onclick=async()=>{try{const t=await navigator.clipboard.readText();if(t){input.value=t;toast('Pasted')}else toast('Clipboard empty')}catch(e){input.focus();toast('Clipboard access blocked — paste manually')}};

  timeline.oninput=()=>{if(Number.isFinite(v.duration))v.currentTime=v.duration*timeline.value/100};

  // Volume button — cycles 100% → 200% → 300% → mute (real WebAudio gain)
  const muteBtn=$('#muteBtn');
  if(muteBtn){
    muteBtn.onclick=()=>{
      ensureGain();
      if(v.muted || state.volume===0){ v.muted=false; setGain(1); muteBtn.innerHTML=svgIcon('volume'); }
      else if(state.volume < 1.2){ setGain(2); muteBtn.innerHTML=svgIcon('volume'); }
      else if(state.volume < 2.2){ setGain(3); muteBtn.innerHTML=svgIcon('volume'); }
      else { setGain(0); v.muted=true; muteBtn.innerHTML=svgIcon('mute'); }
    };
  }

  $('#speedBtn')&&($('#speedBtn').onclick=()=>{state.speed=state.speed===1?1.5:state.speed===1.5?2:state.speed===2?0.75:1;v.playbackRate=state.speed;STORE.write('youseif_playback',{...STORE.read('youseif_playback',{}),speed:state.speed});var sb=$('#speedBtn'); if(sb){ var sp=sb.querySelector('span'); if(sp) sp.textContent=state.speed.toFixed(1)+'x'; else sb.textContent=state.speed.toFixed(1)+'x'; }});

  // Lock
  $('#lockBtn')&&($('#lockBtn').onclick=()=>{
    state.locked=!state.locked;
    document.getElementById('player')?.classList.toggle('controls-locked', state.locked);
    toast(state.locked?'Controls locked':'Controls unlocked');
  });

  // Orientation toggle (CSS class on player / video-wrap)
  $('#orientBtn')&&($('#orientBtn').onclick=()=>{
    orientLandscape=!orientLandscape;
    const player=$('#player');
    if(player) player.classList.toggle('force-landscape', orientLandscape);
    if(orientLandscape){
      // try screen orientation API
      try{ screen.orientation && screen.orientation.lock && screen.orientation.lock('landscape').catch(()=>{}); }catch(e){}
      toast('Landscape');
    } else {
      try{ screen.orientation && screen.orientation.unlock && screen.orientation.unlock(); }catch(e){}
      toast('Portrait');
    }
  });

  // Audio only — hide video frames, keep audio (saves bandwidth when source allows; for progressive still downloads video but UI hides it; for adaptive we drop video levels if possible)
  $('#audioOnlyBtn')&&($('#audioOnlyBtn').onclick=()=>{
    audioOnly=!audioOnly;
    const btn=$('#audioOnlyBtn');
    if(audioOnly){
      v.style.opacity='0';
      v.classList.add('audio-only-mode');
      if(btn) btn.classList.add('active');
      // try drop video quality on HLS
      try{
        if(hls){
          const levels = hls.levels || [];
          // keep lowest or disable video by selecting audio-only if present
          hls.autoLevelEnabled = false;
          // prefer lowest bandwidth
          let min=0, minBr=Infinity;
          levels.forEach((lv,i)=>{ if(lv.bitrate<minBr){minBr=lv.bitrate; min=i;} });
          hls.currentLevel = min;
        }
      }catch(e){}
      toast('وضع صوت فقط — توفير نت');
    } else {
      v.style.opacity='1';
      v.classList.remove('audio-only-mode');
      if(btn) btn.classList.remove('active');
      try{ if(hls) hls.autoLevelEnabled = true; }catch(e){}
      toast('فيديو + صوت');
    }
  });

  // Top: settings, CC, cast
  $('#settingsTopBtn')&&($('#settingsTopBtn').onclick=()=>openPlaybackSheet());
  $('#ccTopBtn')&&($('#ccTopBtn').onclick=()=>{ state.cc=!state.cc; openSubtitleSheet(); });
  $('#castBtn')&&($('#castBtn').onclick=async()=>{const remote=v.remote; if(remote?.prompt){try{await remote.prompt();toast('Remote playback connected')}catch{toast('Remote playback cancelled')}}else{toast('Cast unavailable in this browser')}});

  // Fullscreen bottom
  // Fullscreen → true immersive video (fills screen, chrome auto-hides)
  let fsChromeTimer=null;
  const isDocFs=()=> !!(document.fullscreenElement||document.webkitFullscreenElement||document.webkitCurrentFullScreenElement);
  const setImp=(node, props)=>{
    if(!node) return;
    Object.keys(props).forEach(k=>{
      try{ node.style.setProperty(k, props[k], 'important'); }catch(e){}
    });
  };
  const clearImp=(node, keys)=>{
    if(!node) return;
    keys.forEach(k=>{ try{ node.style.removeProperty(k); }catch(e){} });
  };
  const applyFsClass=(on)=>{
    const el=$('#player');
    if(!el) return;
    const wrap=el.querySelector('.video-wrap');
    const vid=el.querySelector('video');
    el.classList.toggle('is-fullscreen', on);
    document.body.classList.toggle('player-fs', on);
    if(on){
      el.classList.add('fs-chrome-hidden');
      el.classList.remove('fs-chrome-show');
      setImp(el, {
        position:'fixed', top:'0', left:'0', right:'0', bottom:'0',
        width:'100vw', height:'100dvh', 'max-width':'none', 'max-height':'none',
        margin:'0', padding:'0', border:'0', 'border-radius':'0',
        background:'#000', 'z-index':'2147483646', overflow:'hidden', display:'block',
        inset:'0'
      });
      setImp(wrap, {
        position:'absolute', top:'0', left:'0', right:'0', bottom:'0',
        width:'100%', height:'100%', 'min-height':'100%', 'max-height':'none',
        margin:'0', padding:'0', inset:'0'
      });
      setImp(vid, {
        position:'absolute', top:'0', left:'0', right:'0', bottom:'0',
        width:'100%', height:'100%', 'max-width':'none', 'max-height':'none',
        'object-fit': (el.classList.contains('fit-cover')?'cover':'contain'),
        background:'#000', inset:'0'
      });
      // hide app chrome while immersive
      const app=$('#app'), nav=$('.bottom-nav'), top=$('.topbar');
      if(app) setImp(app, {overflow:'hidden'});
      if(nav) setImp(nav, {display:'none'});
      if(top) setImp(top, {display:'none'});
    } else {
      const app=$('#app'), nav=$('.bottom-nav'), top=$('.topbar');
      if(app) clearImp(app, ['overflow']);
      if(nav) clearImp(nav, ['display']);
      if(top) clearImp(top, ['display']);
      el.classList.remove('fs-chrome-hidden','fs-chrome-show','fs-css-only');
      clearImp(el, ['position','top','left','right','bottom','width','height','max-width','max-height','margin','padding','border','border-radius','background','z-index','overflow','display','inset']);
      clearImp(wrap, ['position','top','left','right','bottom','width','height','min-height','max-height','margin','padding','inset']);
      clearImp(vid, ['position','top','left','right','bottom','width','height','max-width','max-height','background','inset']);
      // leave object-fit as set by FIT mode
    }
  };
  const showFsChrome=()=>{
    const el=$('#player'); if(!el||!el.classList.contains('is-fullscreen')) return;
    el.classList.remove('fs-chrome-hidden');
    el.classList.add('fs-chrome-show');
    clearTimeout(fsChromeTimer);
    fsChromeTimer=setTimeout(()=>{
      if(el.classList.contains('is-fullscreen')){
        el.classList.add('fs-chrome-hidden');
        el.classList.remove('fs-chrome-show');
      }
    }, 2800);
  };
  const lockLandscape=async()=>{
    try{
      if(screen.orientation && screen.orientation.lock){
        await screen.orientation.lock('landscape');
        return true;
      }
    }catch(e){}
    try{
      if(screen.lockOrientation) screen.lockOrientation('landscape');
      else if(screen.mozLockOrientation) screen.mozLockOrientation('landscape');
      else if(screen.msLockOrientation) screen.msLockOrientation('landscape');
    }catch(e){}
    return false;
  };
  const enterFs=async()=>{
    const player=$('#player')||$('.video-wrap');
    applyFsClass(true);
    // Force landscape layout class even if Orientation API is blocked
    if(player) player.classList.add('force-landscape');
    orientLandscape=true;
    // 1) Fullscreen API on player first (required before orientation.lock on many mobile browsers)
    let gotNative=false;
    try{
      if(player.requestFullscreen){
        await player.requestFullscreen({navigationUI:'hide'});
        gotNative=true;
      } else if(player.webkitRequestFullscreen){
        player.webkitRequestFullscreen();
        gotNative=true;
      } else if(document.documentElement.requestFullscreen){
        await document.documentElement.requestFullscreen({navigationUI:'hide'});
        gotNative=true;
      }
    }catch(e){ console.warn('fs player', e); }
    // lock landscape after (or with) fullscreen
    await lockLandscape();
    // retry lock shortly after FS settles (some browsers only allow it then)
    setTimeout(()=>{ lockLandscape(); }, 350);
    if(gotNative) return;
    // 2) iOS native video fullscreen
    try{
      if(v.webkitEnterFullscreen){ v.webkitEnterFullscreen(); await lockLandscape(); return; }
      if(v.requestFullscreen){ await v.requestFullscreen(); await lockLandscape(); return; }
    }catch(e){ console.warn('fs video', e); }
    // 3) CSS-only immersive — true edge-to-edge
    player.classList.add('fs-css-only');
    document.body.classList.add('player-fs');
    await lockLandscape();
    toast('Fullscreen mode');
  };
  const exitFs=async()=>{
    applyFsClass(false);
    const player=$('#player');
    if(player){
      player.classList.remove('fs-css-only');
      player.classList.remove('force-landscape');
    }
    orientLandscape=false;
    document.body.classList.remove('player-fs');
    try{ if(screen.orientation&&screen.orientation.unlock) screen.orientation.unlock(); }catch(e){}
    try{
      if(isDocFs()){
        const exit=document.exitFullscreen||document.webkitExitFullscreen||document.webkitCancelFullScreen;
        if(exit) await exit.call(document);
      }
    }catch(e){}
  };
  $('#fullBtn')&&($('#fullBtn').onclick=async()=>{
    try{
      if(isDocFs() || ($('#player')&&$('#player').classList.contains('is-fullscreen'))){
        await exitFs();
        toast('Exit fullscreen');
      } else {
        await enterFs();
      }
    }catch(e){ toast('Fullscreen blocked'); }
  });
  document.addEventListener('fullscreenchange', ()=>{ applyFsClass(isDocFs()); });
  document.addEventListener('webkitfullscreenchange', ()=>{ applyFsClass(isDocFs()); });
  // iOS video native FS end
  v.addEventListener('webkitendfullscreen', ()=>{ applyFsClass(false); });
  v.addEventListener('webkitbeginfullscreen', ()=>{ applyFsClass(true); });

  const wrapFs=$('.video-wrap');
  if(wrapFs){
    wrapFs.addEventListener('click', (e)=>{
      const el=$('#player');
      if(!el||!el.classList.contains('is-fullscreen')) return;
      showFsChrome();
    });
  }

  // FIT modes: contain → cover → fill → 75%
  let fitMode = 0;
  const fitModes = [
    {name:'FIT', fit:'contain', scale:1},
    {name:'FILL', fit:'cover', scale:1},
    {name:'STRETCH', fit:'fill', scale:1},
    {name:'75%', fit:'contain', scale:0.75}
  ];
  const applyFit = ()=>{
    const m = fitModes[fitMode];
    v.style.objectFit = m.fit;
    v.style.transform = m.scale!==1 ? ('scale('+m.scale+')') : '';
    const pl=$('#player');
    if(pl){ pl.classList.toggle('fit-cover', m.fit==='cover'); pl.classList.toggle('fit-fill', m.fit==='fill'); }
    const b=$('#fitBtn'); if(b){ var sp=b.querySelector('span'); if(sp) sp.textContent=m.name; else b.textContent=m.name; }
    toast('Screen: '+m.name);
  };
  $('#fitBtn')&&($('#fitBtn').onclick=()=>{ fitMode = (fitMode+1)%fitModes.length; applyFit(); });

  // Picture-in-Picture floating frame
  $('#pipBtn')&&($('#pipBtn').onclick=async()=>{
    try{
      if(document.pictureInPictureElement){
        await document.exitPictureInPicture();
        toast('PiP closed');
      } else if(document.pictureInPictureEnabled){
        if(v.readyState < 1 && !v.src){ toast('Load media first'); return; }
        await v.requestPictureInPicture();
        toast('Floating window');
      } else toast('PiP not supported on this device');
    }catch(e){ toast('PiP unavailable'); }
  });

  ['loadedmetadata','durationchange','loadeddata','canplay','playing','progress','waiting','stalled','error','ended'].forEach(ev=>v.addEventListener(ev,()=>{updateMediaInfo();updateConnection()}));
  v.addEventListener('play',setPlay); v.addEventListener('pause',setPlay);
  let lastPosSave=0;
  v.addEventListener('timeupdate',()=>{if(Number.isFinite(v.duration)){timeline.value=v.currentTime/v.duration*100;$('#currentTime').textContent=fmt(v.currentTime);$('#duration').textContent=fmt(v.duration);if(state.rememberPosition && Date.now()-lastPosSave>3000 && positionKey()){STORE.write(positionKey(),{time:v.currentTime,saved:Date.now()});lastPosSave=Date.now()}}updateConnection()});
  v.addEventListener('loadedmetadata',()=>{const pos=state.rememberPosition&&positionKey()?STORE.read(positionKey(),null):null;if(pos&&pos.time>5&&pos.time<v.duration-2){if(confirm('Resume from '+fmt(pos.time)+'?'))v.currentTime=pos.time;else STORE.remove(positionKey())}});
  window.addEventListener('online',()=>{updateConnection();pingServer()});
  window.addEventListener('offline',updateConnection);
  updateMediaInfo(); startMetrics();

  // ===== Interactive gestures =====
  // - Double-tap left: -10s | right: +10s
  // - Long-press: scrub faster
  // - Vertical left: brightness (up to 2.5x)
  // - Vertical right: volume (up to 300%)
  (function(){
    const wrap=$('.video-wrap'); if(!wrap) return;
    let startX=0,startY=0,mode=null,startVol=1,startBright=1,lastTap=0,lastTapX=0;
    let longTimer=null, longActive=false, longDir=0;
    let overlay=document.getElementById('gestureHud');
    if(!overlay){overlay=document.createElement('div');overlay.id='gestureHud';overlay.className='gesture-hud';wrap.appendChild(overlay)}
    const showHud=(txt)=>{overlay.textContent=txt;overlay.classList.add('show');clearTimeout(overlay._t);overlay._t=setTimeout(()=>overlay.classList.remove('show'),700)};
    const brightness=(val)=>{
      val=Math.max(0.2, Math.min(2.5, val));
      wrap.dataset.bright=String(val);
      v.style.filter = 'brightness('+val+')';
      return val;
    };
    if(!wrap.dataset.bright) wrap.dataset.bright='1';

    wrap.addEventListener('touchstart',e=>{
      if(state.locked) return;
      if(e.touches.length!==1) return;
      if(e.target.closest('button')) return;
      const t=e.touches[0]; startX=t.clientX; startY=t.clientY; mode=null; longActive=false; longDir=0;
      startVol=state.volume||1; startBright=parseFloat(wrap.dataset.bright||'1');
      const now=Date.now();
      // double tap ±10s
      if(now-lastTap<280 && Math.abs(t.clientX-lastTapX)<80){
        const mid=wrap.getBoundingClientRect().width/2;
        if(t.clientX<mid){
          if(Number.isFinite(v.duration)){ v.currentTime=Math.max(0,v.currentTime-10); showHud('−10s'); }
          else showHud('Live');
        } else {
          if(Number.isFinite(v.duration)){ v.currentTime=Math.min(v.duration,v.currentTime+10); showHud('+10s'); }
          else showHud('Live');
        }
        lastTap=0;
        clearTimeout(longTimer);
        return;
      }
      lastTap=now; lastTapX=t.clientX;
      // long press → fast seek
      longTimer=setTimeout(()=>{
        longActive=true;
        mode='longseek';
        showHud('≫ Fast seek');
      }, 420);
    },{passive:true});

    wrap.addEventListener('touchmove',e=>{
      if(state.locked) return;
      if(e.touches.length!==1) return;
      const t=e.touches[0]; const dx=t.clientX-startX, dy=t.clientY-startY;
      const rect=wrap.getBoundingClientRect();
      if(Math.abs(dx)>14 || Math.abs(dy)>14){ clearTimeout(longTimer); }
      if(longActive && Number.isFinite(v.duration)){
        e.preventDefault();
        const seekDelta = dx / rect.width * Math.min(120, v.duration*0.25);
        const target = Math.max(0, Math.min(v.duration, v.currentTime + seekDelta));
        // preview only on HUD; apply on end for smoothness? apply live for interactivity
        v.currentTime = target;
        showHud((seekDelta>=0?'+':'')+Math.round(seekDelta)+'s');
        startX = t.clientX; // continuous
        return;
      }
      if(!mode){
        if(Math.abs(dx)>14 && Math.abs(dx)>Math.abs(dy)*1.25) mode='seek';
        else if(Math.abs(dy)>14){ mode = t.clientX < rect.left+rect.width*0.4 ? 'bright' : 'vol'; }
      }
      if(mode==='vol'){
        e.preventDefault();
        const nv = Math.max(0, Math.min(3, startVol - dy/140));
        setGain(nv, true);
        v.muted=false;
      } else if(mode==='bright'){
        e.preventDefault();
        const nb = brightness(startBright - dy/180);
        showLevelHud('BRIGHT', nb/2.5*100);
      } else if(mode==='seek' && Number.isFinite(v.duration)){
        e.preventDefault();
        const seekDelta = dx / rect.width * Math.min(90, v.duration*0.2);
        showHud((seekDelta>=0?'+':'')+Math.round(seekDelta)+'s');
      }
    },{passive:false});

    wrap.addEventListener('touchend',e=>{
      clearTimeout(longTimer);
      if(state.locked) return;
      if(mode==='seek' && Number.isFinite(v.duration) && !longActive){
        const rect=wrap.getBoundingClientRect(); const dx=(e.changedTouches?.[0]?.clientX||startX)-startX;
        v.currentTime=Math.max(0,Math.min(v.duration,v.currentTime + dx/rect.width*Math.min(90,v.duration*.2)));
      }
      longActive=false; mode=null;
    });
  })();
}

const channelStoreKey='youseif_playlists_v2';
function readChannels(){try{return JSON.parse(localStorage.getItem(channelStoreKey)||'[]')}catch{return[]}}
function writeChannels(list){localStorage.setItem(channelStoreKey,JSON.stringify(list))}
async function loadLogoIndex(){
  if(logoIndex) return logoIndex;
  if(logoLoading) return logoLoading;
  logoLoading=fetch(LOGO_API,{cache:'force-cache'}).then(r=>r.ok?r.json():[]).then(data=>{
    const byId=new Map(), byName=new Map();
    for(const c of Array.isArray(data)?data:[]){
      const logo=c.logo || c.tvg?.logo || c.url_logo;
      if(!logo) continue;
      if(c.id) byId.set(String(c.id).toLowerCase(),logo);
      if(c.tvg?.id) byId.set(String(c.tvg.id).toLowerCase(),logo);
      for(const n of [c.name,...(c.alt_names||[]),c.tvg?.name]){
        const k=normalizeName(n); if(k && !byName.has(k)) byName.set(k,logo);
      }
    }
    logoIndex={byId,byName}; return logoIndex;
  }).catch(()=>({byId:new Map(),byName:new Map()}));
  return logoLoading;
}
function findLogo(ch){
  if(ch.logo && /^https?:/i.test(ch.logo)) return ch.logo;
  if(!logoIndex) return '';
  const id=String(ch.tvgId||'').toLowerCase();
  if(id && logoIndex.byId.has(id)) return logoIndex.byId.get(id);
  const n=normalizeName(ch.name);
  if(n && logoIndex.byName.has(n)) return logoIndex.byName.get(n);
  // token overlap fallback, only for reasonably distinctive names
  const toks=n.split(' ').filter(x=>x.length>=3);
  let best='',score=0;
  for(const [key,url] of logoIndex.byName){
    const kt=key.split(' '); const hits=toks.filter(t=>kt.includes(t)).length;
    const sc=hits/(Math.max(toks.length,kt.length));
    if(hits>=2 && sc>score){score=sc;best=url}
  }
  return best;
}
function parseM3U(text,source='Playlist'){
  const lines=text.replace(/^\uFEFF/,'').split(/\r?\n/); const out=[]; let meta=null;
  for(const raw of lines){const line=raw.trim(); if(!line) continue;
    if(/^#EXTINF/i.test(line)){
      const comma=line.indexOf(','); const attrs=comma>=0?line.slice(0,comma):line; const name=comma>=0?line.slice(comma+1).trim():'Unnamed Channel';
      const get=a=>{const m=attrs.match(new RegExp(a+'=["\\\']([^"\\\']*)["\\\']','i')); return m?m[1]:''};
      meta={name,tvgId:get('tvg-id'),logo:get('tvg-logo'),group:get('group-title')||'Other',source};
    } else if(meta && !line.startsWith('#')){out.push({...meta,url:line,id:crypto.randomUUID?.()||String(Date.now()+Math.random())});meta=null;}
  }
  return out;
}
function initials(name){return String(name||'?').trim().split(/\s+/u).filter(Boolean).slice(0,2).map(x=>x[0]).join('').toUpperCase()||'?'}
function channelImg(ch){const logo=findLogo(ch); const fb=`<span class="logo-fallback">${initials(ch.name)}</span>`; return logo?`<img class="channel-logo" src="${logo.replace(/"/g,'&quot;')}" loading="lazy" referrerpolicy="no-referrer" onerror="this.onerror=null;this.style.display='none';this.nextElementSibling.style.display='grid'">${fb}`:fb}
function renderChannels(list){
  const box=$('#channelList'); if(!box)return;
  const favs=favorites();
  $('#channelCount').textContent=list.length;
  if(!list.length){box.innerHTML='<div class="empty">No channels yet. Add one or more M3U/M3U8 playlists.</div>';return}
  box.innerHTML=list.map((ch,i)=>`<article class="channel-card" data-group="${String(ch.group||'Other').toLowerCase()}" data-name="${normalizeName(ch.name)}"><div class="logo-slot">${channelImg(ch)}</div><div class="card-main"><b>${String(ch.name).replace(/[<>]/g,'')}</b><small>${String(ch.group||'Other').replace(/[<>]/g,'')} · <span class="live">LIVE</span></small></div><button class="fav ${favs.has(String(ch.id))?'selected':''}" data-id="${ch.id}" aria-label="Favorite ${String(ch.name).replace(/[<>]/g,'')}">${favs.has(String(ch.id))?'★':'☆'}</button></article>`).join('');
  $$('.fav').forEach(b=>b.onclick=()=>{const selected=toggleFavorite(b.dataset.id);b.classList.toggle('selected',selected);b.textContent=selected?'★':'☆';});
  if(!logoIndex) setTimeout(()=>loadLogoIndex().then(()=>renderChannels(list)),0);
}
async function importPlaylistFile(file){
  const text = await file.text();
  let parsed = parseM3U(text, file.name);
  if(!parsed.length && (file.name.endsWith('.json') || text.trim().startsWith('{') || text.trim().startsWith('['))){
    try{
      const j = JSON.parse(text);
      const arr = Array.isArray(j) ? j : (j.channels || j.playlist || j.data || []);
      if(Array.isArray(arr)){
        parsed = arr.map((c,i)=>({
          name: c.name || c.title || ('Channel '+(i+1)),
          url: c.url || c.stream_url || c.link || '',
          logo: c.logo || c.stream_icon || '',
          group: c.group || c.group_title || 'Other',
          tvgId: c.tvg_id || '',
          source: file.name,
          id: crypto.randomUUID?.()||String(Date.now()+i)
        })).filter(x=>x.url);
      }
    }catch(e){}
  }
  if(!parsed.length){ toast(file.name+': no channels found'); return 0; }
  const all = readChannels();
  const existing = new Set(all.map(x=>x.url));
  const fresh = parsed.filter(x=>!existing.has(x.url));
  writeChannels(all.concat(fresh));
  return fresh.length;
}
async function importPlaylistUrl(url){
  const r = await fetch(url, { redirect:'follow', headers:{ 'Accept':'application/vnd.apple.mpegurl, application/x-mpegURL, audio/mpegurl, application/json, text/plain, */*' } });
  if(!r.ok) throw new Error('HTTP '+r.status);
  const ct = (r.headers.get('content-type')||'').toLowerCase();
  const text = await r.text();
  let parsed = parseM3U(text, url);
  // JSON / Xtream-ish fallback
  if(!parsed.length && (ct.includes('json') || text.trim().startsWith('{') || text.trim().startsWith('['))){
    try{
      const j = JSON.parse(text);
      const arr = Array.isArray(j) ? j : (j.channels || j.available_channels || j.playlist || j.data || []);
      if(Array.isArray(arr)){
        parsed = arr.map((c,i)=>({
          name: c.name || c.title || c.stream_display_name || ('Channel '+(i+1)),
          url: c.url || c.stream_url || c.link || c.src || '',
          logo: c.logo || c.stream_icon || c.tvg_logo || '',
          group: c.group || c.group_title || c.category_name || 'Other',
          tvgId: c.tvg_id || c.epg_channel_id || '',
          source: url,
          id: crypto.randomUUID?.()||String(Date.now()+i)
        })).filter(x=>x.url);
      }
    }catch(e){}
  }
  if(!parsed.length) throw new Error('No channels');
  const all = readChannels();
  const existing = new Set(all.map(x=>x.url));
  const fresh = parsed.filter(x=>!existing.has(x.url));
  writeChannels(all.concat(fresh));
  return fresh.length;
}
function wireChannels(){
  let list=readChannels();
  renderChannels(list);
  $('#channelSearch').oninput=()=>{const q=normalizeName($('#channelSearch').value);$$('.channel-card').forEach(c=>c.hidden=!!q&&!c.dataset.name.includes(q))};
  const pick=()=>{const x=document.createElement('input');x.type='file';x.multiple=true;x.accept='.m3u,.m3u8,.txt,.pls,.xspf,.json,.csv,.xml,.asx,.wpl';x.onchange=async()=>{let n=0;for(const f of [...x.files]){try{n+=await importPlaylistFile(f)}catch(e){toast(f.name+': import failed')}}list=readChannels();renderChannels(list);toast(n+' new channels imported')};x.click()};
  $('#addFile').onclick=pick; $('#addFile2').onclick=pick;
  $('#playlistUrlBtn').onclick=async()=>{const url=prompt('Paste M3U / M3U8 playlist URL');if(!url)return;try{const n=await importPlaylistUrl(url.trim());list=readChannels();renderChannels(list);toast(n+' new channels imported')}catch(e){toast('Playlist URL blocked or invalid')}};
  $$('[data-groupjump]').forEach(b=>b.onclick=()=>{const q=normalizeName(b.dataset.groupjump);$$('.channel-card').forEach(c=>c.hidden=!c.dataset.group.includes(q))});
  $$('.chip').forEach(c=>c.onclick=()=>{ $$('.chip').forEach(x=>x.classList.remove('active'));c.classList.add('active'); const f=c.dataset.filter; $$('.channel-card').forEach(card=>{let hide=false;if(f==='kids')hide=!card.dataset.group.includes('kid')&&!card.dataset.group.includes('cartoon');if(f==='sports')hide=!card.dataset.group.includes('sport');if(f==='news')hide=!card.dataset.group.includes('news');if(f==='fav')hide=!card.querySelector('.fav')?.classList.contains('selected');card.hidden=hide})});
}

/* ===== Download Manager ===== */
const DL_KEY = 'youseif_downloads_v1';
const dlState = { queue: [], active: null, items: [], controllers:new Map() };
function readDLs(){ try{ return JSON.parse(localStorage.getItem(DL_KEY)||'[]'); }catch(e){ return []; } }
function writeDLs(list){ localStorage.setItem(DL_KEY, JSON.stringify(list)); }
function fmtBytes(n){ if(!n||n<=0) return '—'; const u=['B','KB','MB','GB','TB']; let i=0; while(n>=1024&&i<u.length-1){n/=1024;i++;} return n.toFixed(i?1:0)+' '+u[i]; }
function guessName(url){ try{ const u=new URL(url); let n=decodeURIComponent(u.pathname.split('/').pop()||'media'); if(!/\.\w{2,5}$/.test(n)) n+='.mp4'; return n.slice(0,80); }catch(e){ return 'download_'+Date.now()+'.bin'; } }
function isStreamUrl(url){ return /\.m3u8($|\?)|\/hls\/|\.mpd($|\?)|\/dash\/|format=m3u8/i.test(url) || /^(rtsp|rtmp|rtp|udp):/i.test(url); }

async function startDownload(url, name){
  url = (url||'').trim();
  if(!url){ toast('أدخل رابط تحميل مباشر'); return; }
  if(isStreamUrl(url)){ toast('بث HLS/DASH/Live لا يمكن تحميله كملف واحد في المتصفح'); return; }
  const list = readDLs();
  if(list.some(x=>x.url===url && x.status==='done')){ toast('موجود مسبقاً في المكتملة'); return; }
  const id = 'dl_'+Date.now()+'_'+Math.random().toString(36).slice(2,7);
  const item = { id, url, name: name||guessName(url), status:'downloading', progress:0, size:0, received:0, speed:0, error:'', blobUrl:'', created:Date.now() };
  list.unshift(item); writeDLs(list); renderDownloadUI();
  try{
    const ctrl = new AbortController();
    dlState.controllers.set(id,ctrl);
    const res = await fetch(url, { signal: ctrl.signal, mode:'cors', credentials:'omit' });
    if(!res.ok) throw new Error('HTTP '+res.status);
    const total = +res.headers.get('content-length') || 0;
    item.size = total;
    const reader = res.body && res.body.getReader ? res.body.getReader() : null;
    if(!reader){
      const blob = await res.blob();
      item.blobUrl = URL.createObjectURL(blob);
      item.received = blob.size; item.size = blob.size; item.progress = 100; item.status = 'done';
      writeDLs(list.map(x=>x.id===id?{...item,_ctrl:undefined}:x)); renderDownloadUI(); toast('تم التحميل: '+item.name); return;
    }
    const chunks = []; let received = 0; let t0 = performance.now(); let last = t0;
    while(true){
      const { done, value } = await reader.read();
      if(done) break;
      chunks.push(value); received += value.length; item.received = received;
      const now = performance.now();
      if(now - last > 250){
        item.speed = received / ((now - t0)/1000);
        item.progress = total ? Math.min(99, Math.round(received/total*100)) : 0;
        last = now;
        writeDLs(list.map(x=>x.id===id?{id:item.id,url:item.url,name:item.name,status:item.status,progress:item.progress,size:item.size,received:item.received,speed:item.speed,error:'',blobUrl:'',created:item.created}:x));
        renderDownloadUI();
      }
    }
    const blob = new Blob(chunks);
    item.blobUrl = URL.createObjectURL(blob);
    item.received = blob.size; if(!item.size) item.size = blob.size;
    item.progress = 100; item.status = 'done';
    writeDLs(list.map(x=>x.id===id?{id:item.id,url:item.url,name:item.name,status:'done',progress:100,size:item.size,received:item.received,speed:0,error:'',blobUrl:item.blobUrl,created:item.created}:x));
    renderDownloadUI(); toast('تم التحميل: '+item.name);
  }catch(e){
    if(e.name==='AbortError'){ item.status='cancelled'; item.error='Cancelled'; }
    else { item.status='failed'; item.error = String(e.message||e); toast('فشل التحميل: '+(e.message||'خطأ')); }
    dlState.controllers.delete(id);
    const cur = readDLs().map(x=>x.id===id?{id:item.id,url:item.url,name:item.name,status:item.status,progress:item.progress||0,size:item.size||0,received:item.received||0,speed:0,error:item.error||'',blobUrl:'',created:item.created}:x);
    writeDLs(cur); renderDownloadUI();
  }
}

function renderDownloadUI(){
  const activeBox = document.getElementById('dlActive');
  const doneBox = document.getElementById('dlDone');
  if(!activeBox || !doneBox) return;
  const list = readDLs();
  const active = list.filter(x=>x.status==='downloading'||x.status==='failed'||x.status==='paused'||x.status==='cancelled');
  const done = list.filter(x=>x.status==='done');
  activeBox.innerHTML = active.length ? active.map(it=>{
    const pct = it.progress||0;
    const meta = it.status==='failed' ? ('Failed · '+(it.error||'')) : (fmtBytes(it.received)+(it.size?(' / '+fmtBytes(it.size)):'')+' · '+(it.speed?fmtBytes(it.speed)+'/s':it.status));
    return `<article class="download-card" data-id="${it.id}"><span class="download-art art-two">${icon('downloads')}</span><div style="flex:1"><b>${it.name.replace(/</g,'&lt;')}</b><small>${meta}</small><div class="progress"><i style="width:${pct}%"></i></div></div><button class="cancel" data-act="cancel" type="button">×</button></article>`;
  }).join('') : '<div class="empty">No active downloads</div>';
  doneBox.innerHTML = done.length ? done.map(it=>{
    return `<article class="download-card" data-id="${it.id}"><span class="download-art art-one">${icon('play')}</span><div style="flex:1"><b>${it.name.replace(/</g,'&lt;')}</b><small>${fmtBytes(it.size)} · Completed</small></div><button class="done" data-act="play" type="button" title="Play">▶</button><button class="done" data-act="save" type="button" title="Save">↓</button><button class="done" data-act="del" type="button">×</button></article>`;
  }).join('') : '<div class="empty">No completed downloads yet</div>';
  activeBox.querySelectorAll('[data-act="cancel"]').forEach(b=>{
    b.onclick = ()=>{
      const id = b.closest('.download-card').dataset.id;
      dlState.controllers.get(id)?.abort(); dlState.controllers.delete(id);
      const next=readDLs().map(x=>x.id===id?{...x,status:'cancelled',error:'Cancelled'}:x); writeDLs(next);
      renderDownloadUI(); toast('Cancelled');
    };
  });
  doneBox.querySelectorAll('.download-card').forEach(card=>{
    const id = card.dataset.id;
    const it = readDLs().find(x=>x.id===id);
    card.querySelector('[data-act="play"]')?.addEventListener('click',()=>{
      if(it && it.blobUrl){ location.hash='home'; setTimeout(()=>{ const inp=document.getElementById('streamUrl'); if(inp){ inp.value=it.blobUrl; document.getElementById('playUrl')?.click(); } }, 200); }
      else if(it){ location.hash='home'; setTimeout(()=>{ const inp=document.getElementById('streamUrl'); if(inp){ inp.value=it.url; document.getElementById('playUrl')?.click(); } }, 200); }
    });
    card.querySelector('[data-act="save"]')?.addEventListener('click',()=>{
      if(it && it.blobUrl){ const a=document.createElement('a'); a.href=it.blobUrl; a.download=it.name; a.click(); }
      else toast('افتح الرابط الأصلي للحفظ');
    });
    card.querySelector('[data-act="del"]')?.addEventListener('click',()=>{
      if(it && it.blobUrl) try{ URL.revokeObjectURL(it.blobUrl); }catch(e){}
      writeDLs(readDLs().filter(x=>x.id!==id));
      renderDownloadUI(); toast('Deleted');
    });
  });
}

function wireDownloads(){
  renderDownloadUI();
  const start = document.getElementById('startDl');
  const input = document.getElementById('dlUrl');
  if(start) start.onclick = ()=> startDownload(input && input.value);
  document.getElementById('dlClearDone')?.addEventListener('click',()=>{
    const keep = readDLs().filter(x=>x.status!=='done');
    readDLs().filter(x=>x.status==='done').forEach(x=>{ if(x.blobUrl) try{URL.revokeObjectURL(x.blobUrl)}catch(e){} });
    writeDLs(keep); renderDownloadUI(); toast('Cleared completed');
  });
  document.getElementById('dlClearActive')?.addEventListener('click',()=>{
    writeDLs(readDLs().filter(x=>x.status==='downloading'||x.status==='done'));
    renderDownloadUI(); toast('Cleared failed/cancelled');
  });
}

function wireFilms(){
  const playBtn = document.getElementById('playFilmUrl');
  const input = document.getElementById('filmUrl');
  if(playBtn && input){
    playBtn.onclick = ()=>{
      const u = input.value.trim();
      if(!u){ toast('Paste a movie URL'); return; }
      // save recent
      try{
        const rec = JSON.parse(localStorage.getItem('youseif_films_recent')||'[]');
        rec.unshift({url:u, name:u.split('/').pop().slice(0,40), t:Date.now()});
        localStorage.setItem('youseif_films_recent', JSON.stringify(rec.slice(0,20)));
      }catch(e){}
      location.hash = 'home';
      setTimeout(()=>{ const s=document.getElementById('streamUrl'); if(s){ s.value=u; document.getElementById('playUrl')?.click(); } }, 180);
    };
  }
  document.getElementById('filmFileBtn')?.addEventListener('click',()=>{
    const x=document.createElement('input'); x.type='file'; x.accept='video/*,audio/*,.mkv,.avi,.ts,.m2ts,.flv,.wmv,.mov,.mp4,.webm,.m4v,.3gp,.mpeg,.mpg,.ogv,.ogg,.mp3,.aac,.flac,.wav';
    x.onchange=()=>{
      const f=x.files&&x.files[0]; if(!f) return;
      const url = URL.createObjectURL(f);
      try{
        const rec = JSON.parse(localStorage.getItem('youseif_films_recent')||'[]');
        // Blob URLs are session-only; keep metadata and warn after reload instead of pretending persistence.
        rec.unshift({url, name:f.name, t:Date.now(), local:1, sessionOnly:true});
        localStorage.setItem('youseif_films_recent', JSON.stringify(rec.slice(0,20)));
      }catch(e){}
      location.hash='home';
      setTimeout(()=>{ const s=document.getElementById('streamUrl'); if(s){ s.value=url; document.getElementById('playUrl')?.click(); toast(f.name); } }, 180);
    };
    x.click();
  });
  document.getElementById('filmFolderHint')?.addEventListener('click',()=>toast('فك ضغط الـ ZIP خارجياً ثم افتح الملفات من OPEN FILE'));
  // recent posters
  const box = document.getElementById('filmRecent');
  if(box){
    try{
      const rec = JSON.parse(localStorage.getItem('youseif_films_recent')||'[]');
      if(!rec.length){ box.innerHTML='<div class="empty">No recent films — paste a URL or open a file</div>'; }
      else box.innerHTML = rec.slice(0,12).map((r,i)=>`<article class="poster" data-i="${i}"><span class="poster-art art-${['one','two','three'][i%3]}"><i>${icon('play')}</i></span><span>${(r.name||'Media').replace(/</g,'&lt;')}</span></article>`).join('');
      box.querySelectorAll('.poster').forEach(el=>{
        el.onclick=()=>{
          const r = rec[+el.dataset.i]; if(!r) return;
          location.hash='home';
          setTimeout(()=>{ const s=document.getElementById('streamUrl'); if(s){ s.value=r.url; document.getElementById('playUrl')?.click(); } }, 180);
        };
      });
    }catch(e){ box.innerHTML=''; }
  }
  document.getElementById('filmClear')?.addEventListener('click',()=>{ localStorage.removeItem('youseif_films_recent'); if(box) box.innerHTML='<div class="empty">Cleared</div>'; toast('Recent cleared'); });
}

function wireSettings(){
  document.querySelectorAll('.setting[data-setting]').forEach(function(btn){
    var go = function(e){
      if(e){ e.preventDefault(); e.stopPropagation(); }
      var key = btn.getAttribute('data-setting');
      toast('Opening…');
      setTimeout(function(){ handleSetting(key, btn); }, 30);
    };
    btn.onclick = go;
    btn.ontouchend = function(e){ e.preventDefault(); go(e); };
  });
  upgrade3DIcons();
}

function applyCustomAccent(hex){
  try{
    var h = hex || '#ff3042';
    if(!/^#[0-9a-fA-F]{6}$/.test(h)) h = '#ff3042';
    var r = parseInt(h.slice(1,3),16), g = parseInt(h.slice(3,5),16), b = parseInt(h.slice(5,7),16);
    var lr = Math.min(255,r+45), lg = Math.min(255,g+45), lb = Math.min(255,b+45);
    var root = document.documentElement;
    root.style.setProperty('--accent', h);
    root.style.setProperty('--accent2', '#'+((1<<24)+(lr<<16)+(lg<<8)+lb).toString(16).slice(1));
    root.style.setProperty('--glow', 'rgba('+r+','+g+','+b+',0.28)');
    // also tint meta theme-color
    var meta = document.getElementById('themeColor');
    if(meta) meta.content = h;
    applyIconTint(h);
  }catch(e){ console.error(e); }
}

function openColorsSheet(){
  try{
    var old = document.querySelectorAll('.sheet');
    for(var i=0;i<old.length;i++) old[i].remove();
    var s = document.createElement('div');
    s.className = 'sheet';
    s.id = 'colorsSheet';
    var keys = Object.keys(themes);
    var presets = '';
    for(var i=0;i<keys.length;i++){
      var k = keys[i], t = themes[k];
      presets += '<button type="button" class="color-preset" data-theme="'+k+'" style="--p:'+t.accent+'"><i style="background:'+t.accent+';box-shadow:0 0 12px '+t.accent+'"></i><span>'+t.name+'</span></button>';
    }
    var cur = localStorage.getItem('youseif_custom_accent') || (themes[currentTheme()]||themes.red).accent;
    s.innerHTML =
      '<div class="sheet-panel colors-sheet">'+
        '<div class="sheet-head"><b>الألوان والمظهر</b><button type="button" class="sheet-x" id="csClose">×</button></div>'+
        '<p class="sheet-hint">اختار ثيم أو لون مخصص — الأيقونات والأزرار هتتغير معاه</p>'+
        '<div class="color-presets">'+presets+'</div>'+
        '<div class="custom-pick-row"><span>لون مخصص</span><input type="color" id="sheetAccent" value="'+cur+'"></div>'+
        '<div class="sheet-opts">'+
          '<label class="sheet-row"><span>شكل الأيقونات</span><select id="icoStyle"><option value="flat">Flat (يتبع اللون)</option><option value="glow">Glow</option><option value="3d">3D</option></select></label>'+
          '<label class="sheet-row"><span>حجم الخط</span><select id="fontScale"><option value="0.9">صغير</option><option value="1" selected>عادي</option><option value="1.1">كبير</option><option value="1.2">أكبر</option></select></label>'+
          '<label class="sheet-row"><span>كثافة الوهج</span><select id="glowLvl"><option value="0.12">خفيف</option><option value="0.22" selected>متوسط</option><option value="0.35">قوي</option></select></label>'+
        '</div>'+
        '<button type="button" class="sheet-save" id="colorsApply">تطبيق</button>'+
        '<button type="button" class="sheet-reset" id="colorsReset">استعادة الافتراضي</button>'+
      '</div>';
    document.body.appendChild(s);
    toast('لوحة الألوان');
    var close = function(){ if(s.parentNode) s.remove(); };
    var closeBtn = s.querySelector('#csClose');
    if(closeBtn) closeBtn.onclick = function(e){ e.stopPropagation(); close(); };
    s.addEventListener('click', function(e){ if(e.target === s) close(); });
    try{
      var ui = JSON.parse(localStorage.getItem('youseif_ui')||'{}');
      if(ui.ico) s.querySelector('#icoStyle').value = ui.ico;
      if(ui.font) s.querySelector('#fontScale').value = ui.font;
      if(ui.glow) s.querySelector('#glowLvl').value = ui.glow;
    }catch(e){}
    var presetsEls = s.querySelectorAll('.color-preset');
    for(var i=0;i<presetsEls.length;i++){
      (function(b){
        b.onclick = function(e){
          e.stopPropagation();
          applyTheme(b.getAttribute('data-theme'), true);
          var acc = (themes[b.getAttribute('data-theme')]||themes.red).accent;
          var inp = s.querySelector('#sheetAccent');
          if(inp) inp.value = acc;
          for(var j=0;j<presetsEls.length;j++) presetsEls[j].classList.toggle('on', presetsEls[j]===b);
        };
      })(presetsEls[i]);
    }
    var accentInp = s.querySelector('#sheetAccent');
    if(accentInp){
      accentInp.oninput = function(){
        applyCustomAccent(this.value);
        localStorage.setItem('youseif_custom_accent', this.value);
      };
    }
    var applyBtn = s.querySelector('#colorsApply');
    if(applyBtn) applyBtn.onclick = function(e){
      e.stopPropagation();
      var ui = {
        ico: s.querySelector('#icoStyle').value,
        font: s.querySelector('#fontScale').value,
        glow: s.querySelector('#glowLvl').value
      };
      localStorage.setItem('youseif_ui', JSON.stringify(ui));
      applyUIOptions(ui);
      toast('تم تطبيق المظهر');
      close();
    };
    var resetBtn = s.querySelector('#colorsReset');
    if(resetBtn) resetBtn.onclick = function(e){
      e.stopPropagation();
      localStorage.removeItem('youseif_custom_accent');
      localStorage.removeItem('youseif_ui');
      localStorage.setItem(themeKey,'aurora');
      applyTheme('aurora', true);
      applyUIOptions({ico:'flat',font:'1',glow:'0.22'});
      toast('تمت الاستعادة للافتراضي');
      close();
    };
  }catch(err){
    console.error(err);
    toast('Error: '+(err && err.message ? err.message : 'colors'));
  }
}


function accentToHueFilter(hex){
  try{
    var h=(hex||'#ff3042').replace('#','');
    if(h.length!==6) return '0deg';
    var r=parseInt(h.slice(0,2),16)/255, g=parseInt(h.slice(2,4),16)/255, b=parseInt(h.slice(4,6),16)/255;
    var max=Math.max(r,g,b), min=Math.min(r,g,b), d=max-min;
    var hue=0;
    if(d!==0){
      if(max===r) hue=((g-b)/d)%6;
      else if(max===g) hue=(b-r)/d+2;
      else hue=(r-g)/d+4;
      hue=Math.round(hue*60);
      if(hue<0) hue+=360;
    }
    // icons are authored in red (~0–10 deg); rotate to target hue
    return hue+'deg';
  }catch(e){ return '0deg'; }
}
function applyIconTint(hex){
  var hue = accentToHueFilter(hex || getComputedStyle(document.documentElement).getPropertyValue('--accent').trim());
  var root = document.documentElement;
  root.style.setProperty('--icon-hue', hue);
  // saturation boost so tint reads clearly
  root.style.setProperty('--icon-sat', '1.35');
  root.style.setProperty('--icon-bri', '1');
}

function applyUIOptions(ui){
  ui=ui||{};
  var root=document.documentElement;
  root.dataset.ico=ui.ico||'3d';
  root.style.setProperty('--font-scale',ui.font||'1');
  if(ui.glow){
    // scale current glow alpha roughly
    var acc=getComputedStyle(root).getPropertyValue('--accent').trim()||'#ff3042';
    var hex=acc.replace('#','');
    if(hex.length===6){
      var r=parseInt(hex.slice(0,2),16),g=parseInt(hex.slice(2,4),16),b=parseInt(hex.slice(4,6),16);
      root.style.setProperty('--glow','rgba('+r+','+g+','+b+','+(ui.glow||0.22)+')');
    }
  }
  root.style.fontSize=(16*parseFloat(ui.font||1))+'px';
}
function openPlaybackSheet(){
  var old = document.querySelector('.sheet'); if(old) old.remove();
  var s = document.createElement('div'); s.className = 'sheet';
  s.innerHTML = '<div class="sheet-panel"><div class="sheet-head"><b>Playback Settings</b><button type="button" class="sheet-x">×</button></div>'+
    '<label class="sheet-row"><span>Default speed</span><select id="spSpeed"><option value="0.75">0.75x</option><option value="1" selected>1.0x</option><option value="1.25">1.25x</option><option value="1.5">1.5x</option><option value="2">2.0x</option></select></label>'+
    '<label class="sheet-row"><span>Autoplay</span><input type="checkbox" id="spAuto" checked></label>'+
    '<label class="sheet-row"><span>Remember position</span><input type="checkbox" id="spPos" checked></label>'+
    '<button type="button" class="sheet-save" id="spSave">Save</button></div>';
  document.body.appendChild(s);
  var close = function(){ s.remove(); };
  s.querySelector('.sheet-x').onclick = close;
  s.onclick = function(e){ if(e.target===s) close(); };
  try{
    var saved = JSON.parse(localStorage.getItem('youseif_playback')||'{}');
    if(saved.speed) s.querySelector('#spSpeed').value = saved.speed;
    if(saved.autoplay!=null) s.querySelector('#spAuto').checked = !!saved.autoplay;
    if(saved.pos!=null) s.querySelector('#spPos').checked = !!saved.pos;
  }catch(e){}
  s.querySelector('#spSave').onclick = function(){
    var cfg = {speed:+s.querySelector('#spSpeed').value, autoplay:s.querySelector('#spAuto').checked, pos:s.querySelector('#spPos').checked};
    STORE.write('youseif_playback', cfg);
    state.speed = cfg.speed;
    toast('Playback settings saved');
    close();
  };
}
function openSubtitleSheet(){
  var old = document.querySelector('.sheet'); if(old) old.remove();
  var s = document.createElement('div'); s.className = 'sheet';
  s.innerHTML = '<div class="sheet-panel"><div class="sheet-head"><b>Subtitle Settings</b><button type="button" class="sheet-x">×</button></div>'+
    '<label class="sheet-row"><span>Enable CC</span><input type="checkbox" id="ccOn"></label>'+
    '<label class="sheet-row"><span>Font size</span><select id="ccSize"><option value="sm">Small</option><option value="md" selected>Medium</option><option value="lg">Large</option></select></label>'+
    '<label class="sheet-row"><span>Text color</span><input type="color" id="ccColor" value="#ffffff"></label>'+
    '<div class="sheet-row" style="flex-direction:column;align-items:stretch;gap:8px"><span>Load subtitle file / URL</span>'+
    '<input type="text" id="ccUrl" placeholder="SRT / VTT / ASS URL" style="width:100%;padding:8px;border-radius:8px;border:1px solid rgba(255,255,255,.15);background:rgba(0,0,0,.35);color:#fff">'+
    '<div style="display:flex;gap:8px"><button type="button" class="sheet-save" id="ccLoadUrl" style="flex:1">Load URL</button>'+
    '<button type="button" class="sheet-save" id="ccLoadFile" style="flex:1">Open File</button></div></div>'+
    '<button type="button" class="sheet-save" id="ccSave">Save</button></div>';
  document.body.appendChild(s);
  var close = function(){ s.remove(); };
  s.querySelector('.sheet-x').onclick = close;
  s.onclick = function(e){ if(e.target===s) close(); };
  try{
    var saved = JSON.parse(localStorage.getItem('youseif_cc')||'{}');
    if(saved.on!=null) s.querySelector('#ccOn').checked = !!saved.on; else s.querySelector('#ccOn').checked = !!state.cc;
    if(saved.size) s.querySelector('#ccSize').value = saved.size;
    if(saved.color) s.querySelector('#ccColor').value = saved.color;
  }catch(e){}
  function applyTrack(src, label){
    var v = document.getElementById('video'); if(!v){ toast('Open player first'); return; }
    // remove previous external tracks
    Array.from(v.querySelectorAll('track[data-youseif]')).forEach(t=>t.remove());
    var track = document.createElement('track');
    track.kind = 'subtitles'; track.label = label||'External'; track.srclang = 'ar';
    track.src = src; track.default = true; track.dataset.youseif = '1';
    v.appendChild(track);
    track.addEventListener('load', function(){
      try{
        var cues = track.track && track.track.cues;
        if(track.track) track.track.mode = 'showing';
        state.cc = true;
        toast('Subtitles loaded');
      }catch(e){ toast('Loaded (browser may limit ASS/SSA)'); }
    });
    // force mode
    setTimeout(function(){ try{ if(track.track) track.track.mode='showing'; }catch(e){} }, 400);
  }
  s.querySelector('#ccLoadUrl').onclick = function(){
    var u = (s.querySelector('#ccUrl').value||'').trim();
    if(!u){ toast('Enter subtitle URL'); return; }
    // convert srt to blob vtt if needed via fetch
    if(/\.srt($|\?)/i.test(u)){
      fetch(u).then(r=>r.text()).then(txt=>{
        var vtt = srtToVtt(txt);
        var blob = new Blob([vtt], {type:'text/vtt'});
        applyTrack(URL.createObjectURL(blob), 'SRT');
      }).catch(()=>toast('Failed to fetch SRT'));
    } else {
      applyTrack(u, 'External');
    }
  };
  s.querySelector('#ccLoadFile').onclick = function(){
    var x = document.createElement('input'); x.type='file'; x.accept='.srt,.vtt,.ass,.ssa,.ttml,.sub,.smi,text/vtt,text/plain';
    x.onchange = function(){
      var f = x.files && x.files[0]; if(!f) return;
      var reader = new FileReader();
      reader.onload = function(){
        var txt = reader.result;
        var name = f.name||'sub';
        if(/\.srt$/i.test(name) || (!/\.vtt$/i.test(name) && /^\d+\s*\n?\d{2}:/.test(txt))){
          txt = srtToVtt(txt);
        }
        var blob = new Blob([txt], {type:'text/vtt'});
        applyTrack(URL.createObjectURL(blob), name);
      };
      reader.readAsText(f);
    };
    x.click();
  };
  s.querySelector('#ccSave').onclick = function(){
    var cfg = {on:s.querySelector('#ccOn').checked, size:s.querySelector('#ccSize').value, color:s.querySelector('#ccColor').value};
    localStorage.setItem('youseif_cc', JSON.stringify(cfg));
    state.cc = cfg.on;
    var v = document.getElementById('video');
    if(v){
      Array.from(v.textTracks||[]).forEach(function(t){ t.mode = cfg.on ? 'showing' : 'hidden'; });
    }
    // style
    var st = document.getElementById('youseif-cc-style');
    if(!st){ st=document.createElement('style'); st.id='youseif-cc-style'; document.head.appendChild(st); }
    var fs = cfg.size==='sm'?'14px':cfg.size==='lg'?'22px':'18px';
    st.textContent = 'video::cue{color:'+cfg.color+';font-size:'+fs+';background:rgba(0,0,0,.55)}';
    toast(cfg.on ? 'Subtitles enabled' : 'Subtitles disabled');
    close();
  };
}
function srtToVtt(srt){
  var body = String(srt).replace(/\r+/g,'').trim();
  // remove index-only lines and convert commas to dots in timestamps
  body = body.replace(/^\d+\s*$/gm, '');
  body = body.replace(/(\d{2}:\d{2}:\d{2}),(\d{3})/g, '$1.$2');
  if(!/^WEBVTT/i.test(body)) body = 'WEBVTT\n\n' + body;
  return body;
}


function navigate(page){if(!pages[page]) page='home'; if(location.hash.slice(1)!==page) location.hash=page; else render(page);}
window.navigate=navigate;
window.addEventListener('hashchange',function(){
  render(location.hash.replace(/^#/,'')||'home');
});

function goPage(page){
  if(!page) return;
  page = String(page).toLowerCase();
  if(!pages[page]) page = 'home';
  var d = document.querySelector('.drawer');
  if(d) d.remove();
  if(location.hash.slice(1) !== page){
    location.hash = page;
  } else {
    render(page);
  }
}
window.goPage = goPage;

// Single delegated handler for ALL navigation + settings (click + touch)
function onAppActivate(e){
  var t = e.target;
  if(!t || !t.closest) return;

  // Bottom nav or any [data-page]
  var nav = t.closest('[data-page]');
  if(nav && nav.dataset.page){
    e.preventDefault();
    e.stopPropagation();
    goPage(nav.dataset.page);
    return;
  }

  // Settings rows
  var set = t.closest('.setting[data-setting]');
  if(set && set.dataset.setting){
    e.preventDefault();
    e.stopPropagation();
    handleSetting(set.dataset.setting, set);
    return;
  }

  // Theme choice
  var th = t.closest('.theme-choice[data-theme]');
  if(th && th.dataset.theme){
    e.preventDefault();
    applyTheme(th.dataset.theme, true);
    document.querySelectorAll('.theme-choice').forEach(function(x){
      x.classList.toggle('selected', x === th);
    });
    var lab = document.getElementById('themeCurrentLabel');
    if(lab) lab.textContent = (themes[th.dataset.theme]||{}).name || th.dataset.theme;
    var custom = document.getElementById('customAccent');
    if(custom) custom.value = (themes[th.dataset.theme]||themes.red).accent;
    return;
  }
}

document.addEventListener('click', onAppActivate, false);


function handleSetting(k, btn){
  try{
  if(k==='colors'){ openColorsSheet(); return; }
  else if(k==='reset'){
    localStorage.removeItem('youseif_custom_accent');
    localStorage.removeItem('youseif_ui');
    localStorage.setItem(themeKey,'aurora');
    applyTheme('aurora',true);
    applyUIOptions({ico:'flat',font:'1',glow:'0.22'});
    toast('تمت الاستعادة للافتراضي');
  }
  else if(k==='file'){
    var x = document.createElement('input');
    x.type = 'file'; x.accept = '.m3u,.m3u8,.txt,.pls,.xspf,.json,.csv,.xml,.asx,.wpl';
    x.onchange = async function(){
      try{ var n = await importPlaylistFile(x.files[0]); toast(n+' channels imported'); }
      catch(err){ toast('Playlist import failed'); }
    };
    x.click();
  } else if(k==='url'){
    goPage('channels');
    setTimeout(function(){ var b=document.querySelector('#playlistUrlBtn'); if(b) b.click(); }, 100);
  } else if(k==='theme'){
    var p = document.querySelector('.theme-panel');
    if(p) p.scrollIntoView({behavior:'smooth', block:'start'});
  } else if(k==='playback'){
    openPlaybackSheet();
  } else if(k==='subtitle'){
    openSubtitleSheet();
  } else if(k==='cache'){
    try{ localStorage.removeItem(channelStoreKey); }catch(e){}
    try{ localStorage.removeItem(themeKey); }catch(e){}
    try{ localStorage.removeItem('youseif_custom_accent'); }catch(e){}
    applyTheme('aurora');
    toast('Local data cleared');
  } else if(k==='about'){
    toast('Youseif Player Pro v'+APP_VERSION);
  } else {
    var title = btn && btn.querySelector('b') ? btn.querySelector('b').textContent : 'Option';
    toast(title+' opened');
  }
  }catch(err){ console.error(err); toast('Error: '+(err.message||'failed')); }
}

// Menu / refresh / downloads top
var menuBtn = document.getElementById('menuBtn');
if(menuBtn) menuBtn.onclick = function(){
  var d = document.querySelector('.drawer');
  if(d){ d.remove(); return; }
  d = document.createElement('div');
  d.className = 'drawer';
  var items = [
    {p:'home', label:'Home', ico:'home'},
    {p:'channels', label:'Channels', ico:'channels'},
    {p:'films', label:'Films', ico:'films'},
    {p:'downloads', label:'Downloads', ico:'downloads'},
    {p:'settings', label:'Settings', ico:'settings'}
  ];
  var navHtml = items.map(function(it){
    return '<button type="button" class="drawer-nav" data-page="'+it.p+'">'+
      '<img class="icon-tint drawer-ico" src="assets/icons3d/'+it.ico+'.png" alt="">'+
      '<span>'+it.label+'</span></button>';
  }).join('');
  d.innerHTML = '<div class="drawer-panel">'+
    '<button class="drawer-close" type="button">×</button>'+
    '<div class="drawer-brand"><span class="brand-name">youseif</span> <small class="brand-sub">player pro</small></div>'+
    '<div class="drawer-nav-list">'+navHtml+'</div></div>';
  document.body.appendChild(d);
  d.querySelector('.drawer-close').onclick = function(){ d.remove(); };
  d.onclick = function(e){ if(e.target===d) d.remove(); };
  d.querySelectorAll('[data-page]').forEach(function(btn){
    btn.onclick = function(e){
      e.preventDefault();
      d.remove();
      goPage(btn.getAttribute('data-page'));
    };
  });
};

var refreshBtn = document.getElementById('refreshBtn');
if(refreshBtn) refreshBtn.onclick = function(){ location.reload(); };

var downloadTop = document.getElementById('downloadTop');
if(downloadTop) downloadTop.onclick = function(){ goPage('downloads'); };

// Boot
try{
  var customAcc = localStorage.getItem('youseif_custom_accent');
  applyTheme(currentTheme());
  if(customAcc) applyCustomAccent(customAcc);
}catch(e){ applyTheme('red'); }
try{applyUIOptions(JSON.parse(localStorage.getItem('youseif_ui')||'{}'));}catch(e){}
try{var ca=localStorage.getItem('youseif_custom_accent'); applyIconTint(ca||(themes[currentTheme()]||themes.red).accent);}catch(e){}

function forcePlayerLayout(){
  try{
    var wrap=document.querySelector('.player .video-wrap');
    var vis=document.querySelector('.player .visual');
    var play=document.getElementById('centerPlay');
    var audio=document.getElementById('audioOnlyBtn');
    var stack=document.querySelector('.side-stack.side-right');
    if(wrap){
      var pl=document.getElementById('player');
      if(pl && pl.classList.contains('is-fullscreen')) return; // don't fight fullscreen
      wrap.style.position='relative'; wrap.style.overflow='hidden';
    }
    if(vis){ vis.style.cssText = 'position:absolute;left:0;top:0;right:0;bottom:0;width:100%;height:100%;display:block;pointer-events:none;z-index:3;padding:0;margin:0;'; }
    // Compact center play — no ring
    if(play){ play.style.cssText = 'position:absolute;left:50%;top:50%;transform:translate(-50%,-50%);width:34px;height:34px;border-radius:50%;display:grid;place-items:center;z-index:5;pointer-events:auto;margin:0;border:none;box-shadow:none;background:rgba(0,0,0,.55);'; }
    // Compact side icons bottom-aligned
    if(audio){ audio.style.cssText = 'position:absolute;left:8px;bottom:8px;top:auto;transform:none;width:26px;height:26px;z-index:6;pointer-events:auto;margin:0;display:grid;place-items:center;border-radius:8px;'; }
    if(stack){ stack.style.cssText = 'position:absolute;right:8px;bottom:8px;top:auto;transform:none;display:flex;flex-direction:row;gap:4px;z-index:6;pointer-events:auto;margin:0;'; }
    // Force every side-fab to 26px
    document.querySelectorAll('.player .side-fab').forEach(function(btn){
      btn.style.setProperty('width','26px','important');
      btn.style.setProperty('height','26px','important');
      btn.style.setProperty('min-width','26px','important');
      btn.style.setProperty('min-height','26px','important');
      btn.style.setProperty('border-radius','8px','important');
      var ico = btn.querySelector('.svg-ico, svg');
      if(ico){ ico.style.setProperty('width','14px','important'); ico.style.setProperty('height','14px','important'); }
    });
  }catch(e){}
}

render(location.hash.replace(/^#/,'') || 'home');
try{forcePlayerLayout()}catch(e){}


/* Global exports for inline onclick */
window.openColorsSheet = openColorsSheet;
window.openPlaybackSheet = openPlaybackSheet;
window.openSubtitleSheet = openSubtitleSheet;
window.handleSetting = handleSetting;
window.applyTheme = applyTheme;
window.goPage = goPage;
