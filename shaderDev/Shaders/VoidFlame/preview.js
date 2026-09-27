/* 独立 WebGL 2 宿主，直接读取正式 shader，仅转换版本与精度声明。 */
"use strict";
(async () => {
    const canvas = document.querySelector("#view");
    const status = document.querySelector("#status");
    const gl = canvas.getContext("webgl2", { alpha: false, antialias: true, preserveDrawingBuffer: true });
    if (!gl) { status.textContent = "当前浏览器无法创建 WebGL 2 上下文。"; return; }
    const shaderRoot = "../../src/main/resources/assets/fantasydesire/shaders/core/fd_void_flame";
    const read = async suffix => {
        const response = await fetch(shaderRoot + suffix);
        if (!response.ok) throw new Error(`${suffix}: HTTP ${response.status}`);
        return (await response.text()).replace("#version 150", "#version 300 es\nprecision highp float;\nprecision highp int;");
    };
    function compile(type, source) {
        const shader = gl.createShader(type);
        gl.shaderSource(shader, source); gl.compileShader(shader);
        if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) throw new Error(gl.getShaderInfoLog(shader));
        return shader;
    }
    function program(vertex, fragment) {
        const p = gl.createProgram();
        gl.attachShader(p, compile(gl.VERTEX_SHADER, vertex)); gl.attachShader(p, compile(gl.FRAGMENT_SHADER, fragment));
        gl.bindAttribLocation(p, 0, "Position"); gl.bindAttribLocation(p, 1, "UV0");
        gl.linkProgram(p);
        if (!gl.getProgramParameter(p, gl.LINK_STATUS)) throw new Error(gl.getProgramInfoLog(p));
        return p;
    }
    try {
        const [vsh, fsh] = await Promise.all([read(".vsh"), read(".fsh")]);
        const effect = program(vsh, fsh);
        const base = program(vsh, `#version 300 es
precision highp float;
uniform sampler2D Sampler0;
in vec2 texCoord0;
in vec3 localPosition;
out vec4 fragColor;
void main(){
    vec4 skin=texture(Sampler0,texCoord0);
    if(skin.a<0.1)discard;
    vec3 normal=normalize(cross(dFdx(localPosition),dFdy(localPosition)));
    float light=0.60+0.40*abs(dot(normal,normalize(vec3(0.5,-0.8,0.9))));
    fragColor=vec4(skin.rgb*light,skin.a);
}`);
        const identity = () => new Float32Array([1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1]);
        function multiply(a,b) {
            const r = new Float32Array(16);
            for(let c=0;c<4;c++)for(let row=0;row<4;row++)for(let k=0;k<4;k++)r[c*4+row]+=a[k*4+row]*b[c*4+k];
            return r;
        }
        function rotationY(a) { const m=identity();m[0]=m[10]=Math.cos(a);m[8]=Math.sin(a);m[2]=-m[8];return m; }
        function rotationX(a) { const m=identity();m[5]=m[10]=Math.cos(a);m[6]=Math.sin(a);m[9]=-m[6];return m; }
        const data=[];
        function box(x0,y0,z0,x1,y1,z1) {
            const p=[[x0,y0,z0],[x1,y0,z0],[x1,y1,z0],[x0,y1,z0],[x0,y0,z1],[x1,y0,z1],[x1,y1,z1],[x0,y1,z1]];
            for(const face of [[4,5,6,7],[1,0,3,2],[0,4,7,3],[5,1,2,6],[0,1,5,4],[7,6,2,3]]){
                const uv=[[0,0],[1,0],[1,1],[0,1]];
                for(const i of [0,1,2,0,2,3])data.push(...p[face[i]],...uv[i]);
            }
        }
        // 与常见 EntityModel 一致：局部 Y 向下，头部在零点上方，脚位于 1.5 附近。
        box(-.25,-.5,-.25,.25,0,.25); box(-.25,0,-.125,.25,.75,.125);
        box(-.51,.03,-.125,-.27,.76,.125); box(.27,.03,-.125,.51,.76,.125);
        box(-.25,.75,-.125,-.025,1.5,.125); box(.025,.75,-.125,.25,1.5,.125);
        const vao=gl.createVertexArray();gl.bindVertexArray(vao);
        const buffer=gl.createBuffer();gl.bindBuffer(gl.ARRAY_BUFFER,buffer);gl.bufferData(gl.ARRAY_BUFFER,new Float32Array(data),gl.STATIC_DRAW);
        gl.enableVertexAttribArray(0);gl.vertexAttribPointer(0,3,gl.FLOAT,false,20,0);
        gl.enableVertexAttribArray(1);gl.vertexAttribPointer(1,2,gl.FLOAT,false,20,12);
        // 程序化演示材质，仅用于辨认侵蚀前后的覆盖关系。
        const pixels=new Uint8Array(16*16*4);
        for(let y=0;y<16;y++)for(let x=0;x<16;x++){
            const i=(y*16+x)*4, edge=x===0||y===0||x===15||y===15;
            const value=edge?95:(((x>>2)+(y>>2))%2?163:184);
            pixels.set([value*.77,value*.92,value,255],i);
        }
        gl.bindTexture(gl.TEXTURE_2D,gl.createTexture());
        gl.texImage2D(gl.TEXTURE_2D,0,gl.RGBA,16,16,0,gl.RGBA,gl.UNSIGNED_BYTE,pixels);
        gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_MIN_FILTER,gl.NEAREST);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_MAG_FILTER,gl.NEAREST);
        const stacks=document.querySelector("#stacks"),damage=document.querySelector("#damage"),angle=document.querySelector("#angle");
        let time=4,paused=false,last=performance.now(),seed=.274,priorDamage=Number(damage.value),nextPulse=0;
        const pulseTimes=[-100,-100,-100,-100],pulses=[0,0,0,0];
        function refresh(){
            document.querySelector("#stacksValue").value=stacks.value+" 层";
            document.querySelector("#damageValue").value=damage.value;
            document.querySelector("#angleValue").value=angle.value+"°";
            if(Number(damage.value)>priorDamage){
                const pulse=.35+.65*Math.sqrt((Number(damage.value)-priorDamage)/(Number(damage.value)+16));
                const previous=(nextPulse+3)%4;
                if(time-pulseTimes[previous]<.3)pulses[previous]=Math.min(1,pulses[previous]+pulse*.5);
                else{pulseTimes[nextPulse]=time;pulses[nextPulse]=pulse;nextPulse=(nextPulse+1)%4;}
            }
            if(Number(damage.value)===0){pulseTimes.fill(-100);pulses.fill(0);}
            priorDamage=Number(damage.value);
        }
        for(const input of [stacks,damage,angle])input.addEventListener("input",refresh);
        document.querySelector("#hit").onclick=()=>{damage.value=Math.min(2000,Number(damage.value)+40);refresh();};
        document.querySelector("#clear").onclick=()=>{damage.value=0;refresh();};
        document.querySelector("#pause").onclick=e=>{paused=!paused;e.target.textContent=paused?"继续动画":"暂停动画";};
        document.querySelector("#seed").onclick=()=>{seed=(seed+.173)%1;};
        for(const b of document.querySelectorAll("[data-preset]"))b.onclick=()=>{[stacks.value,damage.value]=b.dataset.preset.split(",");refresh();};
        let dragging=false,lastX=0;
        canvas.onpointerdown=e=>{dragging=true;lastX=e.clientX;canvas.setPointerCapture(e.pointerId);};
        canvas.onpointerup=()=>{dragging=false;};
        canvas.onpointermove=e=>{if(dragging){angle.value=((Number(angle.value)+(e.clientX-lastX)*.5+540)%360)-180;lastX=e.clientX;refresh();}};
        const locations=new Map();
        function uniform(p,name){const key=p===effect?"effect:"+name:"base:"+name;if(!locations.has(key))locations.set(key,gl.getUniformLocation(p,name));return locations.get(key);}
        function draw(p,mv,proj){
            gl.useProgram(p);
            gl.uniformMatrix4fv(uniform(p,"ModelViewMat"),false,mv);gl.uniformMatrix4fv(uniform(p,"ProjMat"),false,proj);
            gl.uniformMatrix4fv(uniform(p,"EffectLocalMat"),false,identity());
            gl.uniform1i(uniform(p,"FogShape"),0);gl.uniform1i(uniform(p,"Sampler0"),0);
            gl.uniform1f(uniform(p,"FogStart"),100);gl.uniform1f(uniform(p,"FogEnd"),200);
            gl.uniform1f(uniform(p,"EffectTime"),time);gl.uniform1f(uniform(p,"EffectSeed"),seed);
            gl.uniform1f(uniform(p,"VoidStrength"),Math.sqrt(Number(stacks.value)/50));
            gl.uniform1f(uniform(p,"EchoStrength"),Number(damage.value)/(Number(damage.value)+120));
            gl.uniform4fv(uniform(p,"EchoPulseAges"),pulseTimes.map(t=>Math.min(2,time-t)));
            gl.uniform4fv(uniform(p,"EchoPulses"),pulses);
            gl.drawArrays(gl.TRIANGLES,0,data.length/5);
        }
        function frame(now){
            const dt=Math.min(.1,(now-last)/1000);last=now;if(!paused)time+=dt;
            const ratio=Math.min(devicePixelRatio,2),w=Math.round(canvas.clientWidth*ratio),h=Math.round(canvas.clientHeight*ratio);
            if(canvas.width!==w||canvas.height!==h){canvas.width=w;canvas.height=h;}
            gl.viewport(0,0,w,h);gl.clearColor(.057,.06,.085,1);gl.clear(gl.COLOR_BUFFER_BIT|gl.DEPTH_BUFFER_BIT);
            const center=identity();center[13]=-.5;
            const flip=identity();flip[5]=-1;
            const camera=identity();camera[14]=-4.4;
            const mv=multiply(camera,multiply(rotationX(.12),multiply(rotationY(Number(angle.value)*Math.PI/180),multiply(flip,center))));
            const f=1/Math.tan(.56/2),n=.1,z=30,proj=new Float32Array([f/(w/h),0,0,0,0,f,0,0,0,0,(z+n)/(n-z),-1,0,0,2*z*n/(n-z),0]);
            gl.enable(gl.DEPTH_TEST);gl.depthFunc(gl.LEQUAL);gl.disable(gl.CULL_FACE);gl.disable(gl.BLEND);gl.depthMask(true);
            draw(base,mv,proj);
            gl.depthMask(false);gl.enable(gl.BLEND);gl.blendFuncSeparate(gl.SRC_ALPHA,gl.ONE_MINUS_SRC_ALPHA,gl.ONE,gl.ONE_MINUS_SRC_ALPHA);
            gl.enable(gl.POLYGON_OFFSET_FILL);gl.polygonOffset(-1,-1);draw(effect,mv,proj);gl.disable(gl.POLYGON_OFFSET_FILL);gl.depthMask(true);
            const error=gl.getError();if(error!==gl.NO_ERROR){status.textContent="WebGL 错误："+error;return;}
            requestAnimationFrame(frame);
        }
        status.textContent="着色器已编译 · WebGL 2";refresh();requestAnimationFrame(frame);
    } catch(error) { status.textContent=String(error); console.error(error); }
})();
