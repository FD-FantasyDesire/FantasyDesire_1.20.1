/* 预览宿主只管理输入与渲染；效果始终读取同目录 main.frag。 */
"use strict";
(() => {
  const get = (id) => document.getElementById(id);
  const canvas = get("view");
  const status = get("status");
  const gl = canvas.getContext("webgl", { alpha: false, antialias: false, depth: false, preserveDrawingBuffer: true });
  if (!gl) {
    status.textContent = "无法创建 WebGL 1 上下文，请启用浏览器硬件加速。";
    return;
  }
  const vertexSource = "attribute vec2 a_position; void main() { gl_Position = vec4(a_position, 0.0, 1.0); }";
  const vertices = gl.createBuffer();
  gl.bindBuffer(gl.ARRAY_BUFFER, vertices);
  gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 3, -1, -1, 3]), gl.STATIC_DRAW);
  let program = null;
  let uniforms = null;
  let sourceFile = null;
  let loadVersion = 0;
  let seconds = 0;
  let paused = false;
  let dirty = true;
  let last = performance.now();
  let drag = null;
  let failure = "";
  let frames = 0;
  let fps = 0;
  let measuredAt = last;

  function shader(type, source) {
    const result = gl.createShader(type);
    gl.shaderSource(result, source);
    gl.compileShader(result);
    if (!gl.getShaderParameter(result, gl.COMPILE_STATUS)) {
      const error = gl.getShaderInfoLog(result);
      gl.deleteShader(result);
      throw new Error(error || "着色器编译失败");
    }
    return result;
  }

  function compile(source) {
    const vs = shader(gl.VERTEX_SHADER, vertexSource);
    let fs = null;
    let next = null;
    try {
      fs = shader(gl.FRAGMENT_SHADER, source);
      next = gl.createProgram();
      gl.attachShader(next, vs);
      gl.attachShader(next, fs);
      gl.linkProgram(next);
      if (!gl.getProgramParameter(next, gl.LINK_STATUS)) throw new Error(gl.getProgramInfoLog(next));
    } catch (error) {
      if (next) gl.deleteProgram(next);
      throw error;
    } finally {
      gl.deleteShader(vs);
      if (fs) gl.deleteShader(fs);
    }
    if (program) gl.deleteProgram(program);
    program = next;
    gl.useProgram(program);
    const position = gl.getAttribLocation(program, "a_position");
    gl.enableVertexAttribArray(position);
    gl.vertexAttribPointer(position, 2, gl.FLOAT, false, 0, 0);
    uniforms = Object.fromEntries(["u_time", "u_resolution", "u_mouse"].map((name) => [name, gl.getUniformLocation(program, name)]));
    failure = "";
    dirty = true;
  }

  async function reload() {
    const version = ++loadVersion;
    try {
      let source;
      if (sourceFile) source = await sourceFile.text();
      else {
        if (location.protocol === "file:") throw new Error("请点击“选择 main.frag”载入同目录源文件，或按 README 启动本地服务。");
        const response = await fetch("main.frag", { cache: "no-store" });
        if (!response.ok) throw new Error(`读取 main.frag 失败：HTTP ${response.status}`);
        source = await response.text();
      }
      if (version === loadVersion) compile(source);
    } catch (error) {
      if (version === loadVersion) {
        failure = error.message;
        status.textContent = failure;
      }
    }
  }

  function resize() {
    const bounds = canvas.getBoundingClientRect();
    const scale = Math.min(window.devicePixelRatio || 1, Number(get("quality").value) / Math.max(bounds.width, bounds.height, 1));
    const width = Math.max(1, Math.round(bounds.width * scale));
    const height = Math.max(1, Math.round(bounds.height * scale));
    if (canvas.width !== width || canvas.height !== height) {
      canvas.width = width;
      canvas.height = height;
      dirty = true;
    }
  }

  function draw() {
    if (!program || gl.isContextLost()) return;
    gl.viewport(0, 0, canvas.width, canvas.height);
    gl.uniform1f(uniforms.u_time, seconds);
    gl.uniform2f(uniforms.u_resolution, canvas.width, canvas.height);
    // 两个滑条极小值对应 (0,0)，用小于 0.01 像素的偏移避开中性输入哨兵。
    gl.uniform2f(uniforms.u_mouse,
      Math.max(0.001, (Number(get("panX").value) + 1) * 0.5 * canvas.width),
      Math.max(0.001, (Number(get("panY").value) + 1) * 0.5 * canvas.height));
    gl.drawArrays(gl.TRIANGLES, 0, 3);
    dirty = false;
  }

  function setPaused(value) {
    paused = value;
    get("pause").textContent = paused ? "继续" : "暂停";
    last = performance.now();
    frames = 0;
    measuredAt = last;
    dirty = true;
  }

  function resetPan() {
    get("panX").value = get("panY").value = "0";
    dirty = true;
  }

  get("pause").onclick = () => setPaused(!paused);
  get("time").oninput = () => { seconds = Number(get("time").value); setPaused(true); };
  get("step").onclick = () => { seconds += 1 / 60; setPaused(true); };
  get("quality").onchange = resize;
  get("panX").oninput = get("panY").oninput = () => { dirty = true; };
  get("reset").onclick = resetPan;
  canvas.ondblclick = resetPan;
  get("reload").onclick = reload;
  get("source").onchange = () => { sourceFile = get("source").files[0] || null; reload(); };
  get("save").onclick = () => {
    if (!program || gl.isContextLost()) return;
    draw();
    canvas.toBlob((blob) => {
      if (!blob) return;
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `starfield-${seconds.toFixed(2)}s.png`;
      link.click();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
    });
  };
  canvas.onpointerdown = (event) => {
    if (event.button !== 0) return;
    canvas.setPointerCapture(event.pointerId);
    drag = { x: event.clientX, y: event.clientY, panX: Number(get("panX").value), panY: Number(get("panY").value) };
  };
  canvas.onpointermove = (event) => {
    if (!drag) return;
    const bounds = canvas.getBoundingClientRect();
    const clamp = (v) => Math.max(-1, Math.min(1, v));
    get("panX").value = clamp(drag.panX - (event.clientX - drag.x) / bounds.width * 3);
    get("panY").value = clamp(drag.panY + (event.clientY - drag.y) / bounds.height * 3);
    dirty = true;
  };
  canvas.onpointerup = canvas.onpointercancel = canvas.onlostpointercapture = () => { drag = null; };
  canvas.addEventListener("webglcontextlost", (event) => {
    event.preventDefault();
    setPaused(true);
    failure = "WebGL 上下文已丢失，请刷新页面重新加载。";
    status.textContent = failure;
  });
  document.addEventListener("visibilitychange", () => { last = performance.now(); });
  new ResizeObserver(resize).observe(canvas);

  function frame(now) {
    const delta = Math.max(0, (now - last) / 1000);
    last = now;
    if (program && !paused && !document.hidden) {
      seconds += delta;
      dirty = true;
    }
    if (dirty && !document.hidden) {
      draw();
      frames++;
    }
    if (now - measuredAt >= 1000) {
      fps = Math.round(frames * 1000 / (now - measuredAt));
      frames = 0;
      measuredAt = now;
    }
    get("time").max = String(Math.max(180, Math.ceil(seconds)));
    get("time").value = String(seconds);
    get("timeValue").value = `${seconds.toFixed(2)} 秒`;
    if (!failure && program) status.textContent = `WebGL 1 · ${canvas.width} × ${canvas.height} · ${paused ? "已暂停" : `${fps} FPS`}`;
    requestAnimationFrame(frame);
  }
  resize();
  reload();
  requestAnimationFrame(frame);
})();
