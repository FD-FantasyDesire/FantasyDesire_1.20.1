/* 双深度地面投影宿主：方块地形 → 完整场景 → 预乘 Alpha 星海覆盖。 */
"use strict";
(async () => {
    const $ = id => document.getElementById(id);
    const canvas = $("view"), status = $("status");
    const gl = canvas.getContext("webgl2", { alpha: false, antialias: false, preserveDrawingBuffer: true });
    if (!gl) { status.textContent = "需要支持 WebGL 2 的浏览器。"; return; }
    const params = new URLSearchParams(location.search);
    const controls = ["radius", "edge", "rate", "nebula", "nebulaSeed", "opacity", "yaw", "pitch"];
    for (const name of controls) {
        const value = Number(params.get(name));
        if (params.has(name) && Number.isFinite(value)) $(name).value = String(value);
    }
    if (["0", "1", "2"].includes(params.get("debug"))) $("debug").value = params.get("debug");
    let seconds = params.has("time") && Number.isFinite(Number(params.get("time"))) ? Math.max(0, Number(params.get("time"))) : 6;
    let paused = params.get("paused") === "1", dirty = true, distance = 14.5;
    let last = performance.now(), fpsStart = last, frameCount = 0, fps = 0;
    let effect = null, scene = null, blockTarget = null, sceneTarget = null;
    let failure = "", loadId = 0, drag = null;
    function multiply(a, b) {
        const out = new Float32Array(16);
        for (let c = 0; c < 4; c++) for (let r = 0; r < 4; r++) for (let k = 0; k < 4; k++) out[c * 4 + r] += a[k * 4 + r] * b[c * 4 + k];
        return out;
    }
    function inverse(matrix) {
        const rows = Array.from({ length: 4 }, (_, r) => Array.from({ length: 8 }, (_, c) => c < 4 ? matrix[c * 4 + r] : Number(c - 4 === r)));
        for (let k = 0; k < 4; k++) {
            let pivot = k;
            for (let r = k + 1; r < 4; r++) if (Math.abs(rows[r][k]) > Math.abs(rows[pivot][k])) pivot = r;
            if (Math.abs(rows[pivot][k]) < 1e-10) throw new Error("相机矩阵不可逆。");
            [rows[k], rows[pivot]] = [rows[pivot], rows[k]];
            const scale = rows[k][k];
            for (let c = 0; c < 8; c++) rows[k][c] /= scale;
            for (let r = 0; r < 4; r++) if (r !== k) {
                const factor = rows[r][k];
                for (let c = 0; c < 8; c++) rows[r][c] -= rows[k][c] * factor;
            }
        }
        return new Float32Array(Array.from({ length: 16 }, (_, i) => rows[i % 4][4 + Math.floor(i / 4)]));
    }
    const normalize = v => { const length = Math.max(Math.hypot(...v), 1e-8); return v.map(x => x / length); };
    const cross = (a, b) => [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]];
    function camera() {
        const yaw = Number($("yaw").value) * Math.PI / 180, pitch = Number($("pitch").value) * Math.PI / 180;
        const eye = [distance * Math.cos(pitch) * Math.sin(yaw), distance * Math.sin(pitch), distance * Math.cos(pitch) * Math.cos(yaw)];
        const back = normalize(eye), right = normalize(cross([0, 1, 0], back)), up = cross(back, right);
        const rotation = new Float32Array([right[0], up[0], back[0], 0, right[1], up[1], back[1], 0, right[2], up[2], back[2], 0, 0, 0, 0, 1]);
        const f = 1 / Math.tan(48 * Math.PI / 360), near = 0.1, far = 90;
        const projection = new Float32Array([f / (canvas.width / canvas.height), 0, 0, 0, 0, f, 0, 0, 0, 0, (far + near) / (near - far), -1, 0, 0, 2 * far * near / (near - far), 0]);
        const vp = multiply(projection, rotation);
        return { eye, vp, inv: inverse(vp) };
    }

    function compile(type, source) {
        const shader = gl.createShader(type);
        gl.shaderSource(shader, source); gl.compileShader(shader);
        if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
            const message = gl.getShaderInfoLog(shader); gl.deleteShader(shader); throw new Error(message);
        }
        return shader;
    }
    function makeProgram(vsh, fsh) {
        const shaders = [], handle = gl.createProgram();
        try {
            shaders.push(compile(gl.VERTEX_SHADER, vsh)); shaders.push(compile(gl.FRAGMENT_SHADER, fsh));
            for (const shader of shaders) gl.attachShader(handle, shader);
            gl.bindAttribLocation(handle, 0, "Position"); gl.bindAttribLocation(handle, 1, "UV0");
            gl.linkProgram(handle);
            if (!gl.getProgramParameter(handle, gl.LINK_STATUS)) throw new Error(gl.getProgramInfoLog(handle));
            const locations = new Map();
            return { handle, uniform(name) { if (!locations.has(name)) locations.set(name, gl.getUniformLocation(handle, name)); return locations.get(name); } };
        } catch (error) { gl.deleteProgram(handle); throw error; }
        finally { for (const shader of shaders) gl.deleteShader(shader); }
    }
    async function read(name) {
        const response = await fetch(name, { cache: "no-store" });
        if (!response.ok) throw new Error(`${name}: HTTP ${response.status}`);
        return response.text();
    }
    function webgl(source) {
        return source.replace(/^#version 150\s*\n/, "#version 300 es\nprecision highp float;\nprecision highp int;\n");
    }
    async function reload() {
        const id = ++loadId;
        let nextScene = null, nextEffect = null;
        try {
            const [vsh, fsh, sceneVsh, sceneFsh, starfield, sky] = await Promise.all([read("main.vsh"), read("main.fsh"), read("scene.vsh"), read("scene.fsh"), read("starfield.glsl"), read("sky.glsl")]);
            if (id !== loadId) return;
            const begin = starfield.indexOf("\n", starfield.indexOf("// STARFIELD_SHARED_BEGIN"));
            const end = starfield.indexOf("// STARFIELD_SHARED_END");
            if (!starfield.includes("// STARFIELD_SHARED_BEGIN") || begin < 0 || end <= begin) throw new Error("Starfield 共享源码标记缺失。");
            const expanded = fsh.replace("// @include STARFIELD_SHARED", starfield.slice(begin + 1, end))
                .replace("// @include STARSEA_SKY", sky);
            nextScene = makeProgram(sceneVsh, sceneFsh);
            nextEffect = makeProgram(webgl(vsh), webgl(expanded));
            if (scene) gl.deleteProgram(scene.handle);
            if (effect) gl.deleteProgram(effect.handle);
            scene = nextScene; effect = nextEffect;
            failure = ""; dirty = true;
        } catch (error) {
            if (nextScene) gl.deleteProgram(nextScene.handle);
            if (nextEffect) gl.deleteProgram(nextEffect.handle);
            failure = String(error.message || error); status.textContent = failure; console.error(error);
        }
    }

    // 顶点包含世界坐标、法线和演示材质色，场景中没有领域外壳几何。
    function box(data, min, max, color) {
        const [x, y, z] = min, [X, Y, Z] = max;
        const points = [[x,y,z],[X,y,z],[X,Y,z],[x,Y,z],[x,y,Z],[X,y,Z],[X,Y,Z],[x,Y,Z]];
        const faces = [[[4,5,6,7],[0,0,1]],[[1,0,3,2],[0,0,-1]],[[0,4,7,3],[-1,0,0]],[[5,1,2,6],[1,0,0]],[[0,1,5,4],[0,-1,0]],[[7,6,2,3],[0,1,0]]];
        for (const [face, normal] of faces) for (const i of [0,1,2,0,2,3]) data.push(...points[face[i]], ...normal, ...color);
    }
    function mesh(data, fullscreen = false) {
        const vao = gl.createVertexArray(); gl.bindVertexArray(vao);
        const buffer = gl.createBuffer(); gl.bindBuffer(gl.ARRAY_BUFFER, buffer); gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(data), gl.STATIC_DRAW);
        const stride = fullscreen ? 5 : 9;
        gl.enableVertexAttribArray(0); gl.vertexAttribPointer(0, 3, gl.FLOAT, false, stride * 4, 0);
        gl.enableVertexAttribArray(1); gl.vertexAttribPointer(1, fullscreen ? 2 : 3, gl.FLOAT, false, stride * 4, 12);
        if (!fullscreen) { gl.enableVertexAttribArray(2); gl.vertexAttribPointer(2, 3, gl.FLOAT, false, 36, 24); }
        return { vao, count: data.length / stride };
    }
    const groundData = [], stepData = [], wallData = [], entityData = [];
    box(groundData, [-14,-0.5,-14], [14,0,14], [0.32,0.35,0.41]);
    box(stepData, [-4.2,0,-4.0], [-1.2,0.5,-1.8], [0.38,0.40,0.47]);
    box(stepData, [-4.2,0.5,-4.0], [-1.2,1.0,-2.8], [0.38,0.40,0.47]);
    box(stepData, [3.4,0,-1.8], [4.7,0.7,-0.5], [0.38,0.40,0.47]);
    box(stepData, [-6.5,0,1.0], [-5.5,1.6,2.0], [0.28,0.32,0.40]);
    // L 形墙面用于核对地面—墙面—墙角之间是否出现星图接缝或拉伸。
    box(wallData, [-4.6,0,-3.0], [3.6,4.2,-2.7], [0.34,0.37,0.44]);
    box(wallData, [-4.6,0,-2.7], [-4.3,3.4,2.2], [0.31,0.34,0.41]);
    // 中央人物与右前方柱体仅进入完整场景深度，不进入地形深度。
    const skin = [0.75,0.61,0.53], cloth = [0.32,0.66,0.69], trousers = [0.19,0.26,0.38];
    box(entityData, [0.12,0.02,0.05], [0.40,0.78,0.39], trousers);
    box(entityData, [0.48,0.02,0.05], [0.76,0.78,0.39], trousers);
    box(entityData, [0.12,0.78,0.04], [0.76,1.5,0.40], cloth);
    box(entityData, [-0.15,0.78,0.04], [0.10,1.48,0.40], cloth);
    box(entityData, [0.78,0.78,0.04], [1.03,1.48,0.40], cloth);
    box(entityData, [0.13,1.5,-0.08], [0.75,2.12,0.54], skin);
    box(entityData, [0.12,1.99,-0.09], [0.76,2.15,0.55], [0.16,0.13,0.17]);
    box(entityData, [2.25,0,2.0], [2.95,1.65,2.7], [0.57,0.43,0.27]);
    const ground = mesh(groundData), steps = mesh(stepData), walls = mesh(wallData), entities = mesh(entityData);
    const quad = mesh([-1,-1,0,0,0, 3,-1,0,2,0, -1,3,0,0,2], true);

    function destroy(target) {
        if (!target) return;
        gl.deleteFramebuffer(target.fbo); gl.deleteTexture(target.color); gl.deleteTexture(target.depth);
    }
    function makeTarget(width, height) {
        const target = { fbo: gl.createFramebuffer(), color: gl.createTexture(), depth: gl.createTexture() };
        gl.bindFramebuffer(gl.FRAMEBUFFER, target.fbo);
        for (const [name, format, internal, type, attachment] of [
            ["color", gl.RGBA, gl.RGBA8, gl.UNSIGNED_BYTE, gl.COLOR_ATTACHMENT0],
            ["depth", gl.DEPTH_COMPONENT, gl.DEPTH_COMPONENT24, gl.UNSIGNED_INT, gl.DEPTH_ATTACHMENT]
        ]) {
            gl.bindTexture(gl.TEXTURE_2D, target[name]);
            gl.texImage2D(gl.TEXTURE_2D, 0, internal, width, height, 0, format, type, null);
            gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.NEAREST);
            gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.NEAREST);
            gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
            gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
            gl.framebufferTexture2D(gl.FRAMEBUFFER, attachment, gl.TEXTURE_2D, target[name], 0);
        }
        if (gl.checkFramebufferStatus(gl.FRAMEBUFFER) !== gl.FRAMEBUFFER_COMPLETE) { destroy(target); throw new Error("无法创建场景深度缓冲。"); }
        return target;
    }
    function resize() {
        const rect = canvas.getBoundingClientRect();
        const scale = Math.min(window.devicePixelRatio || 1, Number($("quality").value) / Math.max(rect.width, rect.height, 1));
        const width = Math.max(1, Math.round(rect.width * scale)), height = Math.max(1, Math.round(rect.height * scale));
        if (canvas.width === width && canvas.height === height && blockTarget && sceneTarget) return;
        canvas.width = width; canvas.height = height;
        destroy(blockTarget); destroy(sceneTarget); blockTarget = sceneTarget = null;
        blockTarget = makeTarget(width, height); sceneTarget = makeTarget(width, height);
        dirty = true;
    }
    function drawMesh(mesh) { gl.bindVertexArray(mesh.vao); gl.drawArrays(gl.TRIANGLES, 0, mesh.count); }
    function blit(from, to, mask) {
        gl.bindFramebuffer(gl.READ_FRAMEBUFFER, from.fbo);
        gl.bindFramebuffer(gl.DRAW_FRAMEBUFFER, to ? to.fbo : null);
        gl.blitFramebuffer(0, 0, canvas.width, canvas.height, 0, 0, canvas.width, canvas.height, mask, gl.NEAREST);
    }
    function render() {
        if (!effect || !scene || gl.isContextLost()) return;
        resize();
        const { eye, vp, inv } = camera();
        gl.viewport(0, 0, canvas.width, canvas.height);
        gl.enable(gl.DEPTH_TEST); gl.depthFunc(gl.LEQUAL); gl.depthMask(true); gl.disable(gl.CULL_FACE); gl.disable(gl.BLEND);
        // 先解绑上一帧的采样资源，确保不会采样正在写入的深度附件。
        for (let unit = 0; unit < 2; unit++) { gl.activeTexture(gl.TEXTURE0 + unit); gl.bindTexture(gl.TEXTURE_2D, null); }
        gl.bindFramebuffer(gl.FRAMEBUFFER, blockTarget.fbo);
        gl.clearColor(0.035, 0.046, 0.072, 1); gl.clearDepth(1); gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);
        gl.useProgram(scene.handle);
        gl.uniformMatrix4fv(scene.uniform("ViewProj"), false, vp);
        gl.uniform3fv(scene.uniform("CameraPosition"), eye);
        gl.uniform1f(scene.uniform("Terrain"), 1);
        drawMesh(ground); if ($("steps").checked) drawMesh(steps); if ($("walls").checked) drawMesh(walls);
        blit(blockTarget, sceneTarget, gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);
        gl.bindFramebuffer(gl.FRAMEBUFFER, sceneTarget.fbo);
        if ($("entities").checked) { gl.uniform1f(scene.uniform("Terrain"), 0); drawMesh(entities); }
        // 完整场景颜色复制到最终目标；星海只读取离屏深度，向默认目标合成。
        blit(sceneTarget, null, gl.COLOR_BUFFER_BIT);
        gl.bindFramebuffer(gl.FRAMEBUFFER, null);
        gl.disable(gl.DEPTH_TEST); gl.depthMask(false); gl.enable(gl.BLEND);
        gl.blendFuncSeparate(gl.ONE, gl.ONE_MINUS_SRC_ALPHA, gl.ZERO, gl.ONE);
        gl.useProgram(effect.handle);
        gl.activeTexture(gl.TEXTURE0); gl.bindTexture(gl.TEXTURE_2D, sceneTarget.depth); gl.uniform1i(effect.uniform("DepthSampler"), 0);
        gl.activeTexture(gl.TEXTURE1); gl.bindTexture(gl.TEXTURE_2D, blockTarget.depth); gl.uniform1i(effect.uniform("BlockDepthSampler"), 1);
        gl.uniformMatrix4fv(effect.uniform("InvViewProj"), false, inv);
        gl.uniform3fv(effect.uniform("FieldCenter"), eye.map(v => -v));
        gl.uniform2f(effect.uniform("DepthUvScale"), 1, 1);
        for (const [name, value] of Object.entries({ FieldRadius: Number($("radius").value), FieldTime: seconds, FieldAge: seconds, FieldOpacity: Number($("opacity").value), EdgeMotion: Number($("edge").value), MeteorRate: Number($("rate").value), NebulaClusterGain: Number($("nebula").value), NebulaSeed: Number($("nebulaSeed").value) })) gl.uniform1f(effect.uniform(name), value);
        gl.uniform1i(effect.uniform("DebugView"), Number($("debug").value));
        drawMesh(quad);
        gl.depthMask(true); gl.disable(gl.BLEND);
        const error = gl.getError();
        if (error !== gl.NO_ERROR) throw new Error(`WebGL 渲染错误：${error}`);
        dirty = false;
    }

    function refresh() {
        $("radiusValue").value = `${Number($("radius").value).toFixed(1)} 格`;
        $("edgeValue").value = `${Math.round(Number($("edge").value) * 100)}%`;
        $("rateValue").value = `${Number($("rate").value).toFixed(2)} /星区·秒`;
        $("nebulaValue").value = `${Math.round(Number($("nebula").value) * 100)}%`;
        $("seedValue").value = Number($("nebulaSeed").value).toFixed(0);
        $("opacityValue").value = `${Math.round(Number($("opacity").value) * 100)}%`;
        dirty = true;
    }
    function setPaused(value) { paused = value; $("pause").textContent = paused ? "继续" : "暂停"; last = performance.now(); dirty = true; }
    function defaultView() { $("yaw").value = 28; $("pitch").value = 54; distance = 14.5; dirty = true; }
    for (const name of controls) $(name).oninput = refresh;
    for (const name of ["quality", "entities", "steps", "walls", "debug"]) $(name).onchange = refresh;
    $("randomSeed").onclick = () => { $("nebulaSeed").value = String(Math.floor(Math.random() * 10000)); refresh(); };
    $("pause").onclick = () => setPaused(!paused);
    $("step").onclick = () => { seconds += 1 / 60; setPaused(true); };
    $("time").oninput = () => { seconds = Number($("time").value); setPaused(true); };
    $("restart").onclick = () => { seconds = 0; setPaused(false); };
    $("overview").onclick = () => { $("yaw").value = 0; $("pitch").value = 84; dirty = true; };
    $("lowview").onclick = () => { $("pitch").value = 18; dirty = true; };
    $("reset").onclick = defaultView; canvas.ondblclick = defaultView;
    $("reload").onclick = reload;
    $("save").onclick = () => {
        if (!effect || gl.isContextLost()) return;
        render();
        canvas.toBlob(blob => {
            if (!blob) return;
            const url = URL.createObjectURL(blob), link = document.createElement("a");
            link.href = url; link.download = `starsea-${seconds.toFixed(2)}s.png`; link.click();
            setTimeout(() => URL.revokeObjectURL(url), 1000);
        });
    };
    canvas.onpointerdown = event => {
        if (event.button !== 0) return;
        canvas.setPointerCapture(event.pointerId);
        drag = { x: event.clientX, y: event.clientY, yaw: Number($("yaw").value), pitch: Number($("pitch").value) };
    };
    canvas.onpointermove = event => {
        if (!drag) return;
        $("yaw").value = ((drag.yaw - (event.clientX - drag.x) * 0.3 + 540) % 360) - 180;
        $("pitch").value = Math.max(12, Math.min(89, drag.pitch + (event.clientY - drag.y) * 0.2)); dirty = true;
    };
    canvas.onpointerup = canvas.onpointercancel = canvas.onlostpointercapture = () => { drag = null; };
    canvas.addEventListener("wheel", event => { event.preventDefault(); distance = Math.max(8, Math.min(25, distance + event.deltaY * 0.012)); dirty = true; }, { passive: false });
    canvas.addEventListener("webglcontextlost", event => { event.preventDefault(); failure = "WebGL 上下文已丢失，请刷新页面。"; status.textContent = failure; setPaused(true); });
    document.addEventListener("visibilitychange", () => { last = performance.now(); });
    new ResizeObserver(() => { dirty = true; }).observe(canvas);
    function frame(now) {
        const delta = Math.max(0, (now - last) / 1000); last = now;
        if (!paused && effect && !document.hidden) { seconds += delta; dirty = true; }
        try {
            if (dirty && !document.hidden && !gl.isContextLost()) { render(); frameCount++; }
        } catch (error) { failure = String(error.message || error); status.textContent = failure; console.error(error); return; }
        if (now - fpsStart >= 1000) { fps = Math.round(frameCount * 1000 / (now - fpsStart)); frameCount = 0; fpsStart = now; }
        $("time").max = String(Math.max(60, Math.ceil(seconds))); $("time").value = String(seconds); $("timeValue").value = `${seconds.toFixed(2)} 秒`;
        if (!failure && effect) status.textContent = `WebGL 2 · 双深度覆盖\n${canvas.width} × ${canvas.height} · ${paused ? "已暂停" : `${fps} FPS`}`;
        requestAnimationFrame(frame);
    }
    refresh(); setPaused(paused); await reload(); requestAnimationFrame(frame);
})();
