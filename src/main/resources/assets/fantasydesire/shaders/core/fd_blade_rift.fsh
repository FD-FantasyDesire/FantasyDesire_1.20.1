#version 150
in vec4 vertexColor;
in vec2 localCoord;
uniform float Progress;
uniform float FlowTime;
uniform float RiftLength;
uniform float RiftWidth;
uniform vec3 CoreColor;
uniform vec3 EnergyColor;
out vec4 fragColor;

// BladeRift 的固定视觉调参；按设计不暴露到 ParticleOption。
const float DISTORTION=0.095;
const float EXPOSURE=1.35;
const float EMISSIVE_STRENGTH=0.82;
const float SPLIT_STRENGTH=1.0;

float hash21(vec2 p){vec3 q=fract(vec3(p.xyx)*0.1031);q+=dot(q,q.yzx+33.33);return fract((q.x+q.y)*q.z);}
float noise2(vec2 p){vec2 i=floor(p),f=fract(p);f=f*f*(3.0-2.0*f);return mix(mix(hash21(i),hash21(i+vec2(1,0)),f.x),mix(hash21(i+vec2(0,1)),hash21(i+vec2(1)),f.x),f.y);}
float fbm(vec2 p){float n=0.0,a=0.5;for(int i=0;i<5;i++){n+=noise2(p)*a;p=p*2.03+vec2(17.1,-9.4);a*=0.5;}return n;}
float finiteDistance(vec2 p,float len,float wid,float power){float taper=pow(max(0.0,1.0-abs(p.x)/max(len,0.001)),power);float hw=max(wid*taper,0.0001);vec2 outside=max(vec2(abs(p.x)-len,abs(p.y)-hw),vec2(0.0));float inside=-min(len-abs(p.x),hw-abs(p.y));return(outside.x>0.0||outside.y>0.0)?length(outside):inside;}

void main(){
 vec2 p=localCoord;float spawn=smoothstep(0.0,0.135,Progress),fade=1.0-smoothstep(0.68,1.0,Progress),active=spawn*fade;
 float d=finiteDistance(p,RiftLength,RiftWidth*(0.72+0.34*spawn),0.58),coreD=finiteDistance(p,RiftLength*0.94,RiftWidth*0.19,0.72);
 float front=mix(-RiftLength,RiftLength,spawn),reveal=mix(1.0,1.0-smoothstep(front-0.015,front+0.045,p.x),1.0-step(0.135,Progress));
 float aa=max(fwidth(d)*1.35,0.0015),body=(1.0-smoothstep(-aa,aa,d))*reveal*active,outside=max(d,0.0);
 float flow=fbm(vec2(p.x*7.0-FlowTime*7.8,p.y*17.0+FlowTime*5.0));
 float split=DISTORTION*SPLIT_STRENGTH*(0.62+0.38*flow);
 float revealGlow=mix(1.0,1.0-smoothstep(front+0.055,front+0.18,p.x),1.0-step(0.135,Progress));
 float haloDomain=smoothstep(-2.0*aa,0.0,d);
 float nearGlow=exp(-outside*(22.0+split*6.0))*haloDomain*active*revealGlow*EMISSIVE_STRENGTH;
 float farGlow=exp(-outside/max(RiftWidth*4.8,0.020))*haloDomain*active*revealGlow*EMISSIVE_STRENGTH;
 float inside01=clamp((-d)/max(RiftWidth,0.001),0.0,1.0);
 // Bias the configured-color ramp toward the outer energy color while preserving
 // a continuous transition into the center's configured core color.
 float coreRamp=smoothstep(0.02,0.92,inside01);
 float energyWeight=pow(1.0-coreRamp,3.0);
 vec3 bodyGradient=EnergyColor*energyWeight+CoreColor*coreRamp;
 // Minecraft's particle target has no post-process bloom.  Emit an HDR near halo
 // plus a broad low-frequency halo; the Java render type accumulates these with
 // ONE/ONE blending to reproduce the preview's apparent self-illumination.
 vec3 col=EnergyColor*(farGlow*1.35+nearGlow*2.15);
 float vein=exp(-abs(sin(p.x*24.0+FlowTime*34.0+flow*8.0))*8.0)*body;
 col+=bodyGradient*body*(1.10+0.45*flow)+bodyGradient*vein*1.35;
 float coreAA=max(fwidth(coreD)*1.4,0.0012);
 float core=(1.0-smoothstep(-coreAA,coreAA,coreD))*reveal*active;
 float spawnBoost=0.80*exp(-Progress*30.0);
 col+=CoreColor*core*(1.65+spawnBoost);
 // EXPOSURE remains the original fixed artistic control, but is applied linearly
 // so values above 1 survive into the additive target instead of being tone-mapped
 // back below 1 and losing the bloom-like intensity.
 col=max(col,vec3(0.0))*EXPOSURE;
 vec3 coreSurface=CoreColor*EXPOSURE*(1.35+spawnBoost);
 col=mix(col,coreSurface,clamp(core,0.0,1.0));
 float coverage=max(body,max(nearGlow*0.72,farGlow*0.42));
 float alpha=clamp(coverage+core*0.18,0.0,1.0)*vertexColor.a;
 if(alpha<0.002||dot(col,col)<0.000004)discard;
 fragColor=vec4(col*vertexColor.rgb*vertexColor.a,alpha);
}
