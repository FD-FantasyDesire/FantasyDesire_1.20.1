#version 150

uniform sampler2D NoiseSampler;
uniform float Intensity;
uniform float Twist;
uniform float FlowSpeed;
uniform float DissolveWidth;
uniform mat4 InvProjMat;
uniform mat4 ProjMat;
uniform mat3 IViewRotMat;
uniform vec2 ScreenSize;
in vec2 localUV;
flat in float age;
flat in float seed;
flat in int layer;
flat in vec3 sphereCenterView;
flat in float sphereRadius;
out vec4 fragColor;

const vec3 JADE=vec3(0.06,0.73,0.53);
const vec3 CYAN=vec3(0.25,0.92,1.0);
const vec3 GOLD=vec3(1.0,0.59,0.13);
const vec3 WHITE=vec3(0.88,1.0,0.82);

// MIT 原型的低频噪声 + 0.3 倍五倍频细节，改为同一张共享纹理采样。
vec3 curlCloud(vec3 n,float time,float variant,float dissolve,out float density) {
    // 倾斜旋转轴，绕轴扭转采样空间；使用笛卡尔投影避免经纬接缝。
    n=vec3(n.x*0.8+n.y*0.6,n.y*0.8-n.x*0.6,n.z);
    float turn=n.y*Twist-time*FlowSpeed+variant*6.2831853;
    float c=cos(turn),s=sin(turn);
    vec2 orbit=mat2(c,-s,s,c)*n.xz;
    vec2 uv=orbit*vec2(0.50,0.22)+vec2(n.y*0.13,n.y*0.46)+variant*3.7;
    float broad=textureLod(NoiseSampler,uv,0.0).r;
    uv+=vec2(broad-0.5)*0.07;
    float fine=textureLod(NoiseSampler,uv*5.0+vec2(time*0.12,0.31),0.0).r;
    float field=(broad+fine*0.30)/1.30;
    float width=max(DissolveWidth,0.015);
    float cut=smoothstep(dissolve,dissolve+width,field);
    float crest=smoothstep(0.34,0.66,field);
    float folds=exp(-abs(broad-0.50)*12.0);
    float hotEdge=exp(-abs(field-dissolve-width*0.5)*42.0)*smoothstep(0.08,0.3,dissolve);
    density=cut*(0.16+crest*0.50);
    vec3 ramp=mix(JADE*0.25,CYAN*0.75,crest);
    return (ramp*(0.23+folds*0.60)+GOLD*(folds*0.22+hotEdge*0.55))*cut;
}

void main() {
    vec2 q=localUV;
    float r=length(q);
    float edge=1.0-smoothstep(0.80,0.995,r);
    vec3 rgb=vec3(0);
    gl_FragDepth=gl_FragCoord.z;
    if(layer==0) {
        float fade=pow(1.0-clamp(age/0.16,0.0,1.0),3.0);
        float core=exp(-r*r*38.0);
        float crossGlow=exp(-abs(q.x)*48.0-abs(q.y)*8.0)+exp(-abs(q.y)*48.0-abs(q.x)*8.0);
        rgb=(WHITE*core*3.5+GOLD*crossGlow*0.8+CYAN*exp(-r*r*7.0)*0.35)*fade*edge;
    } else if(layer==1) {
        vec2 screenUV=gl_FragCoord.xy/max(ScreenSize,vec2(1));
        vec4 farPoint=InvProjMat*vec4(screenUV*2.0-1.0,1.0,1.0);
        vec3 ray=normalize(farPoint.xyz);
        float b=dot(ray,sphereCenterView);
        float h=sphereRadius*sphereRadius-dot(sphereCenterView,sphereCenterView)+b*b;
        if(h<=0.0 || b<=0.0)discard;
        float root=sqrt(h);
        float hit=b-root;
        if(hit<=0.001)discard;
        vec3 surface=ray*hit;
        vec3 front=(surface-sphereCenterView)/max(sphereRadius,0.001);
        vec3 back=(ray*(b+root)-sphereCenterView)/max(sphereRadius,0.001);
        float p=clamp(age/0.58,0.0,1.0);
        float fade=1.0-smoothstep(0.55,1.0,p);
        float dissolve=mix(0.12,0.86,smoothstep(0.24,1.0,p));
        float frontDensity,backDensity;
        vec3 clouds=curlCloud(IViewRotMat*front,age,seed,dissolve,frontDensity);
        clouds+=curlCloud(IViewRotMat*back,age,seed+0.19,dissolve,backDensity)*0.38;
        float facing=clamp(dot(front,-ray),0.0,1.0);
        float rim=pow(1.0-facing,2.33);
        float limb=smoothstep(0.0,0.09,facing);
        // 厚度来自球面前后交点，内层颜色使用反菲涅尔权重。
        float depthWeight=pow(facing,2.33);
        float inner=exp(-dot(q,q)*12.0)*exp(-age*13.0);
        float shellFade=1.0-smoothstep(0.19,0.42,age);
        float brokenRim=0.22+frontDensity*1.4;
        rgb=(clouds*(0.60+depthWeight*0.85)*limb+CYAN*rim*brokenRim*shellFade*0.48+
            WHITE*inner*0.22)*fade;
        vec4 projected=ProjMat*vec4(surface,1);
        gl_FragDepth=clamp(projected.z/projected.w*0.5+0.5,0.0,1.0);
    } else {
        float p=age/0.64;
        float a=atan(q.y,q.x);
        float aa=max(fwidth(r),0.006);
        float radius=0.25+p*0.54;
        float bits=pow(max(cos(a*11.0+seed*21.0+p*0.9),0.0),48.0);
        float ring=1.0-smoothstep(0.012,0.032+aa,abs(r-radius));
        rgb=mix(GOLD,CYAN,seed)*bits*ring*pow(1.0-p,2.0)*0.80*edge;
    }
    rgb*=clamp(Intensity,0.0,3.0);
    if(max(max(rgb.r,rgb.g),rgb.b)<0.001)discard;
    // 全部为空中加法发光，避免密集透明球互相压暗与排序跳变。
    fragColor=vec4(rgb,0.0);
}
