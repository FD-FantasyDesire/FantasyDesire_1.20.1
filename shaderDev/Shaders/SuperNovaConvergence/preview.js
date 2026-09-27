/* 独立审查宿主：只加载 main.frag，GLSL Canvas 与浏览器共用唯一效果源。 */
"use strict";
(() => {
  const $ = id => document.getElementById(id);
  const canvas = $("canvas");
  const gl = canvas.getContext("webgl", {alpha:false, antialias:false, preserveDrawingBuffer:true});
  const aftermathDuration = 3.6;
  let explosionTime = 2.8, cycle = explosionTime + aftermathDuration;
  const quality = {draft:[32,620], review:[56,900], high:[80,1300]};
  let program = null, vertexBuffer = null, locations = {}, source = "", time = 0, playing = true;
  let last = performance.now(), statsTime = last, frames = 0, fps = 0;
  let orbit = [0.30,0.17], pointer = null, loadVersion = 0, contextLost = false;
  let seed = 17, errorMessage = "";
  const controls = ["seed","speed","grid","background","exposure","quality"];
  function readControls(preserveTime = true) {
    const parsed = Number($("seed").value);
    seed = Number.isFinite(parsed) ? Math.max(0,Math.min(9999,Math.round(parsed))) : 17;
    $("seed").value = seed;
    const duration = $("explosion").value.trim() === "" ? NaN : Number($("explosion").value);
    const next = Number.isFinite(duration) ? Math.max(.5,Math.min(12,Math.round(duration*10)/10)) : explosionTime;
    // 调整倒计时后保持当前所处阶段，便于比较快慢；爆发后的持续时间保持固定。
    if(preserveTime && next !== explosionTime)
      time = time <= explosionTime ? time / explosionTime * next : next + time - explosionTime;
    explosionTime = next; cycle = explosionTime + aftermathDuration;
    time = Math.max(0,Math.min(cycle-.001,time));
    $("explosion").value = explosionTime;
    $("timeline").max = (cycle-.001).toFixed(3);
    $("arrival").textContent = `第 ${explosionTime.toFixed(1)} 秒爆炸 · 共 7 对粒子`;
  }
  function showError(error) {
    errorMessage = String(error?.message || error);
    $("error").hidden = false;
    $("error").textContent = errorMessage;
    $("status").textContent = "载入失败；详细信息见画面。";
  }
  function compile(type, text) {
    const shader = gl.createShader(type);
    gl.shaderSource(shader, text); gl.compileShader(shader);
    if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
      const log = gl.getShaderInfoLog(shader);
      gl.deleteShader(shader); throw Error(log);
    }
    return shader;
  }
  function build() {
    if (!gl || contextLost || !source) return;
    let vs = null, fs = null, next = null;
    try {
      vs = compile(gl.VERTEX_SHADER,"attribute vec2 position; void main(){gl_Position=vec4(position,0.,1.);}");
      fs = compile(gl.FRAGMENT_SHADER,`#define REVIEW_HOST\n#define VOLUME_STEPS ${quality[$("quality").value][0]}\n`+source);
      next = gl.createProgram(); gl.attachShader(next,vs); gl.attachShader(next,fs); gl.linkProgram(next);
      if (!gl.getProgramParameter(next,gl.LINK_STATUS)) throw Error(gl.getProgramInfoLog(next));
      // 新程序完整编译成功后才替换旧程序，编辑错误不会破坏上一版画面。
      if (program) gl.deleteProgram(program);
      program = next; next = null; gl.useProgram(program);
      locations = {};
      for (const name of ["u_time","u_resolution","u_mouse","u_seed","u_orbit","u_grid","u_background","u_exposure","u_explosion_time"])
        locations[name] = gl.getUniformLocation(program,name);
      if(vertexBuffer)gl.deleteBuffer(vertexBuffer);
      vertexBuffer = gl.createBuffer();
      gl.bindBuffer(gl.ARRAY_BUFFER,vertexBuffer);
      gl.bufferData(gl.ARRAY_BUFFER,new Float32Array([-1,-1,3,-1,-1,3]),gl.STATIC_DRAW);
      const position = gl.getAttribLocation(program,"position");
      gl.enableVertexAttribArray(position); gl.vertexAttribPointer(position,2,gl.FLOAT,false,0,0);
      errorMessage = ""; $("error").hidden = true;
      $("status").textContent = `WebGL 1 编译通过\n${quality[$("quality").value][0]} 步体积采样 · 无贴图`;
      render();
    } catch (error) { showError(error); }
    finally { if(vs)gl.deleteShader(vs); if(fs)gl.deleteShader(fs); if(next)gl.deleteProgram(next); }
  }
  async function load() {
    const version = ++loadVersion;
    try {
      const response = await fetch("main.frag",{cache:"no-store"});
      if (!response.ok) throw Error(`main.frag: HTTP ${response.status}`);
      const text = await response.text();
      if (version !== loadVersion) return;
      source = text; build();
    } catch (error) {
      if(version !== loadVersion)return;
      showError(location.protocol === "file:" ? "直接打开 HTML 时，请用右侧文件选择器载入同目录的 main.frag。\n也可以使用 README 中的本地启动方式。" : error);
    }
  }
  function render() {
    if (!gl || !program || contextLost) return;
    const rect = canvas.getBoundingClientRect();
    const scale = Math.min(window.devicePixelRatio || 1, quality[$("quality").value][1] / Math.max(rect.width,rect.height,1));
    const width = Math.max(1,Math.round(rect.width*scale)), height = Math.max(1,Math.round(rect.height*scale));
    if(canvas.width!==width||canvas.height!==height) { canvas.width=width;canvas.height=height; }
    gl.viewport(0,0,width,height); gl.useProgram(program);
    gl.uniform1f(locations.u_time,time); gl.uniform2f(locations.u_resolution,width,height);
    gl.uniform2f(locations.u_mouse,0,0); gl.uniform1f(locations.u_seed,seed);
    gl.uniform2f(locations.u_orbit,...orbit); gl.uniform1f(locations.u_grid,$("grid").checked?1:0);
    gl.uniform1f(locations.u_background,Number($("background").value));
    gl.uniform1f(locations.u_exposure,Number($("exposure").value));
    gl.uniform1f(locations.u_explosion_time,explosionTime);
    gl.drawArrays(gl.TRIANGLES,0,3);
    $("timeline").value=time; $("time").textContent=`${time.toFixed(3)} / ${cycle.toFixed(3)} s`;
    const age=time-explosionTime;
    $("phase").textContent=time<explosionTime*.06?"星核初现":age<0?`星光汇聚 · 距爆炸 ${(-age).toFixed(2)} 秒`:age<.15?"倒计时结束 · 激发":age<.65?"星云爆发":age<1.8?"球壳翻卷":age<3.1?"云团消散":"余光消隐";
    $("telemetry").textContent=`${width} × ${height}  /  ${fps} FPS  /  SEED ${seed}`;
  }
  function setPlaying(value) { playing=value;last=performance.now();$("play").textContent=playing?"暂停":"播放"; }
  function seek(value) {time=Math.max(0,Math.min(cycle-.001,value));setPlaying(false);render();}
  function syncOrbit() {$("yaw").value=orbit[0]*180/Math.PI;$("pitch").value=orbit[1]*180/Math.PI;}
  function animate(now) {
    const dt=Math.min((now-last)/1000,.1);last=now;
    const speed = $("speed").value === "gif" ? cycle / 1.4 : Number($("speed").value);
    if(playing&&!document.hidden)time=(time+dt*speed)%cycle;
    frames++;
    if(now-statsTime>1000){fps=Math.round(frames*1000/(now-statsTime));frames=0;statsTime=now;}
    if(playing)render();
    requestAnimationFrame(animate);
  }
  $("play").onclick=()=>setPlaying(!playing);
  $("restart").onclick=()=>{time=0;setPlaying(true);render();};
  $("back").onclick=()=>seek(time-1/60);$("next").onclick=()=>seek(time+1/60);
  $("timeline").oninput=()=>seek(Number($("timeline").value));
  document.querySelectorAll("[data-event]").forEach(button=>button.onclick=()=>{
    const moments={gather:explosionTime*.5,before:explosionTime-1/60,impact:explosionTime+.025,cloud:explosionTime+.5,fade:explosionTime+2.1};
    seek(moments[button.dataset.event]);
  });
  controls.forEach(id=>$(id).addEventListener("input",()=>{readControls();if(id==="quality")build();else render();}));
  $("explosion").addEventListener("change",()=>{readControls();render();});
  $("random").onclick=()=>{$("seed").value=Math.floor(Math.random()*10000);readControls();render();};
  for(const id of ["yaw","pitch"])$(id).oninput=()=>{orbit=[Number($("yaw").value),Number($("pitch").value)].map(x=>x*Math.PI/180);render();};
  canvas.onpointerdown=e=>{pointer=[e.clientX,e.clientY];canvas.setPointerCapture(e.pointerId);};
  canvas.onpointermove=e=>{if(!pointer)return;orbit[0]+=(e.clientX-pointer[0])*.007;orbit[1]=Math.max(-1.3,Math.min(1.3,orbit[1]+(e.clientY-pointer[1])*.007));orbit[0]=((orbit[0]+3*Math.PI)%(2*Math.PI))-Math.PI;pointer=[e.clientX,e.clientY];syncOrbit();render();};
  canvas.onpointerup=canvas.onpointercancel=()=>pointer=null;
  canvas.ondblclick=()=>{orbit=[.30,.17];syncOrbit();render();};
  $("reload").onclick=load;
  $("file").onchange=async()=>{const file=$("file").files[0];if(!file)return;const version=++loadVersion;try{const text=await file.text();if(version===loadVersion){source=text;build();}}catch(error){showError(error);}};
  $("capture").onclick=()=>{render();canvas.toBlob(blob=>{if(!blob)return;const link=document.createElement("a");const url=URL.createObjectURL(blob);link.href=url;link.download=`supernova-s${seed}-e${explosionTime.toFixed(1)}-t${time.toFixed(3)}.png`;link.click();setTimeout(()=>URL.revokeObjectURL(url),1000);});};
  $("copy").onclick=async()=>{const url=new URL(location.href);url.hash=new URLSearchParams({t:time.toFixed(5),explode:explosionTime,seed,yaw:orbit[0].toFixed(4),pitch:orbit[1].toFixed(4),quality:$("quality").value,exposure:$("exposure").value,grid:$("grid").checked?1:0,bg:$("background").value}).toString();history.replaceState(null,"",url);try{await navigator.clipboard.writeText(url.href);$("status").textContent="已复制当前时间、爆炸倒计时、种子、视角与显示设置。";}catch{$("status").textContent="当前设置已写入地址栏，可手动复制链接。";}};
  document.addEventListener("keydown",e=>{if(/INPUT|SELECT|BUTTON|TEXTAREA/.test(e.target.tagName))return;if(e.code==="Space"){e.preventDefault();setPlaying(!playing);}if(e.code==="ArrowLeft"){e.preventDefault();seek(time-1/60);}if(e.code==="ArrowRight"){e.preventDefault();seek(time+1/60);}});
  canvas.addEventListener("webglcontextlost",e=>{e.preventDefault();contextLost=true;setPlaying(false);showError("图形上下文暂时丢失，等待恢复。");});
  canvas.addEventListener("webglcontextrestored",()=>{contextLost=false;program=null;vertexBuffer=null;build();});
  new ResizeObserver(()=>render()).observe(canvas);
  // 链接中的数值统一校验；URL 只承载审查状态，不能提供着色器路径。
  const params=new URLSearchParams(location.hash.slice(1));
  const numeric=(key,fallback,min,max)=>{const raw=params.get(key);const value=raw===null?NaN:Number(raw);return Number.isFinite(value)?Math.max(min,Math.min(max,value)):fallback;};
  $("explosion").value=numeric("explode",2.8,.5,12);
  readControls(false);
  time=numeric("t",0,0,cycle-.001);$("seed").value=numeric("seed",17,0,9999);
  orbit=[numeric("yaw",.3,-Math.PI,Math.PI),numeric("pitch",.17,-1.3,1.3)];
  $("exposure").value=numeric("exposure",1.35,.5,2.5);$("grid").checked=numeric("grid",0,0,1)>.5;$("background").value=String(Math.round(numeric("bg",0,0,1)));
  if(Object.hasOwn(quality,params.get("quality")))$("quality").value=params.get("quality");
  readControls();syncOrbit();if(params.has("t"))setPlaying(false);
  if(!gl){showError("此浏览器未提供 WebGL 1 上下文，请在支持 WebGL 的浏览器或 GLSL Canvas 中打开。");return;}
  load();requestAnimationFrame(animate);
})();
