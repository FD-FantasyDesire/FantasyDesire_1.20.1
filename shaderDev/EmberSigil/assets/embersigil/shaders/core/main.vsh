#version 150

in vec3 Position;
in vec2 UV0;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat3 IViewRotMat;
uniform float EffectTime;
uniform int EventCount;
uniform int RepeatPreview;
uniform float BurstWindow;
uniform float ScatterRadius;
uniform vec3 EffectCenter;

out vec2 localUV;
flat out float age;
flat out float seed;
flat out int layer;
flat out vec3 sphereCenterView;
flat out float sphereRadius;

float random(float n) { return fract(sin(n*127.1+311.7)*43758.5453); }

void main() {
    // 三层共用一次批量绘制，每层保留 100 个六顶点槽。
    int quad=gl_VertexID/6;
    int event=quad%100;
    layer=quad/100;
    seed=random(float(event)+11.0);
    int count=clamp(EventCount,0,100);
    float time=RepeatPreview!=0 ? mod(max(EffectTime,0.0),max(BurstWindow,0.0)+1.5) : EffectTime;
    float birth=count<=1 ? 0.0 : float(event)*max(BurstWindow,0.0)/float(count-1);
    age=time-birth;
    float life=layer==0?0.16:layer==1?0.58:0.64;
    sphereCenterView=vec3(0);
    sphereRadius=0.0;
    localUV=UV0;
    if(event>=count || age<0.0 || age>=life || layer>=3) {
        gl_Position=vec4(2.0,2.0,2.0,1.0);
        return;
    }
    float a=random(float(event)+71.0)*6.2831853;
    float r=sqrt(random(float(event)+29.0))*max(ScatterRadius,0.0);
    vec3 center=EffectCenter;
    if(count>1)center+=vec3(cos(a)*r,random(float(event)+43.0)*0.4,sin(a)*r);
    float variant=0.84+seed*0.32;
    float progress=clamp(age/0.58,0.0,1.0);
    float size=layer==0?0.36+age*2.0:layer==1?
        0.10+0.69*(1.0-pow(1.0-min(progress/0.42,1.0),3.0))+progress*0.12:
        0.55+age*1.6;
    center.y+=age*0.18;
    vec2 extent=vec2(size*variant);
    if(layer==1) {
        sphereRadius=size*variant;
        sphereCenterView=(ModelViewMat*vec4(center-vec3(0,1.5,0),1)).xyz;
        // 透视投影在斜视时会偏移，按视线夹角留足承载范围。
        float angleMargin=length(sphereCenterView)/max(-sphereCenterView.z,0.05);
        extent=vec2(sphereRadius*1.24*max(angleMargin,1.0));
    }
    vec3 p=center+IViewRotMat*vec3(Position.xy*extent,0);
    // 预览宿主的 quad 模型平移为 (0,1.5,0)。
    gl_Position=ProjMat*ModelViewMat*vec4(p-vec3(0,1.5,0),1.0);
}
