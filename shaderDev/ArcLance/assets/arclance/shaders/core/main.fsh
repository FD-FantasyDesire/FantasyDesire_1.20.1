#version 150

uniform sampler2D SceneSampler;
uniform sampler2D DepthSampler;
uniform mat4 InverseViewProjection;
uniform vec3 CameraPosition;
uniform vec2 ScreenSize;
uniform float SkillAge;
uniform int RepeatPreview;
uniform vec3 Origin;
uniform vec3 Target;
uniform float Intensity;
uniform int Steps;

in vec2 texCoord;
out vec4 fragColor;

const vec3 CYAN = vec3(0.10,0.80,1.0);
const vec3 GOLD = vec3(1.0,0.36,0.055);
const vec3 WHITE = vec3(0.78,0.96,1.0);
const float PI = 3.14159265359;

float hash(float n) { return fract(sin(n * 127.1 + 311.7) * 43758.5453); }

float noise(float x) {
    float i = floor(x);
    float f = fract(x);
    return mix(hash(i),hash(i + 1.0),f * f * (3.0 - 2.0 * f));
}

float pulse(float t, float begin, float peak, float end) {
    return smoothstep(begin,peak,t) * (1.0 - smoothstep(peak,end,t));
}

float gaussian(float d, float width) {
    float x = d / max(width,0.001);
    return x < 3.5 ? exp(-x*x) * (1.0-smoothstep(3.0,3.5,x)) : 0.0;
}

float segmentDistance(vec3 p, vec3 a, vec3 b) {
    vec3 ab = b-a;
    float h = clamp(dot(p-a,ab) / max(dot(ab,ab),1e-6),0.0,1.0);
    return length(p-a-ab*h);
}

vec3 unproject(vec2 uv, float depth) {
    vec4 p = InverseViewProjection * vec4(uv*2.0-1.0,depth*2.0-1.0,1.0);
    float safeW = abs(p.w) < 1e-6 ? (p.w < 0.0 ? -1e-6 : 1e-6) : p.w;
    return p.xyz / safeW;
}

// SkillAge 单位为秒。先蓄力，再沿有限线段推进，最后由目标位置扩散。
vec3 field(vec3 p, float t, vec3 axis, vec3 side, vec3 up, float beamLength, out float distanceHint) {
    float charge = pulse(t,0.0,0.78,1.18);
    float firing = smoothstep(0.80,0.88,t) * (1.0-smoothstep(1.70,2.05,t));
    float travel = clamp((t-0.83)/0.24,0.0,1.0);
    float impactAge = t-1.07;
    float hit = smoothstep(0.0,0.04,impactAge) * (1.0-smoothstep(0.9,1.9,impactAge));
    vec3 head = Origin + axis * beamLength * travel;
    vec3 local = p-Origin;
    float axial = dot(local,axis);
    vec2 radial = vec2(dot(local,side),dot(local,up));
    float radius = length(radial);
    vec3 energy = vec3(0);
    distanceHint = 1.0;

    float orbDistance = length(local);
    if ((charge > 0.0 || firing > 0.0) && orbDistance < 2.6) {
        float orbRadius = 0.12 + 0.24 * charge;
        float orbD = orbDistance;
        float shell = abs(orbD-orbRadius);
        energy += WHITE * gaussian(orbD,orbRadius*0.55) * 17.0 * (charge+firing*0.7);
        energy += CYAN * gaussian(shell,0.055) * charge*9.0;
        energy += CYAN * gaussian(orbD,0.72) * charge*0.65;
        for (int i=0;i<3;++i) {
            float fi=float(i);
            float ringRadius = 0.52 + fi*0.24 + (1.0-charge)*0.45;
            vec3 r = local + axis * (0.16+fi*0.28);
            float plane = dot(r,axis);
            vec2 xy = vec2(dot(r,side),dot(r,up));
            float torusD = length(vec2(length(xy)-ringRadius,plane));
            float angular = atan(xy.y,xy.x);
            float arc = smoothstep(-0.2,0.25,sin(angular*3.0+t*(4.0+fi)-fi*2.0));
            energy += mix(CYAN,WHITE,fi*0.2) * gaussian(torusD,0.033) * charge * arc*7.0;
            energy += CYAN * gaussian(torusD,0.12) * charge*0.5;
            distanceHint = min(distanceHint,torusD);
        }
        distanceHint = min(distanceHint,max(orbD-orbRadius,0.0));
    }

    float beamDistance = segmentDistance(p,Origin,head);
    if (firing > 0.0 && travel > 0.0 && beamDistance < 2.0) {
        float beamD = beamDistance;
        float width = (0.09+0.025*sin(t*41.0)) * (0.75+firing*0.25);
        float stream = 0.75 + 0.25*sin(axial*13.0-t*65.0);
        energy += WHITE * gaussian(beamD,width) * firing*22.0;
        energy += CYAN * gaussian(beamD,width*2.5) * firing*4.0*stream;
        energy += CYAN * gaussian(beamD,0.56) * firing*0.28;
        distanceHint = min(distanceHint,beamD);

        float envelope = smoothstep(0.0,0.3,axial) * (1.0-smoothstep(beamLength*travel-0.3,beamLength*travel,axial));
        if (axial > 0.0 && axial < beamLength*travel && envelope > 0.0 && radius < 1.25) {
            // 电弧偏移在两端归零，不产生默认原点或越过束头的分支。
            float waveMask = sin(clamp(axial/max(beamLength*travel,0.01),0.0,1.0)*PI);
            for (int i=0;i<3;++i) {
                float fi=float(i);
                float phase = t*9.0+fi*12.7;
                vec2 jitter = vec2(noise(axial*4.0-phase),noise(axial*4.0+phase+51.0))*2.0-1.0;
                vec2 offset = (vec2(sin(axial*2.4-phase),cos(axial*2.4-phase))*0.22+jitter*0.42)*waveMask;
                float arcD = length(radial-offset);
                energy += vec3(0.42,0.36,1.0) * gaussian(arcD,0.038) * firing*envelope*13.0;
                energy += CYAN * gaussian(arcD,0.12) * firing*envelope*0.7;
                distanceHint = min(distanceHint,arcD);
            }
            float spiralPhase = axial*6.0-t*22.0;
            vec2 helix = vec2(cos(spiralPhase),sin(spiralPhase))*0.38;
            float spiralD = length(radial-helix);
            energy += CYAN*gaussian(spiralD,0.024)*firing*envelope*5.0;
            distanceHint = min(distanceHint,spiralD);
        }
        energy += WHITE * gaussian(length(p-head),0.25)*firing*8.0;
    }

    if (charge > 0.0 || firing > 0.0) distanceHint=min(distanceHint,max(orbDistance-0.4,0.0));
    if (firing > 0.0 && travel > 0.0) distanceHint=min(distanceHint,beamDistance);

    if (impactAge > 0.0 && impactAge < 2.1) {
        vec3 impactP = p-Target;
        float r=length(impactP);
        if (r < 3.85 && hit > 0.0) {
            float flash = exp(-impactAge*7.0)*hit;
            energy += vec3(1.0,0.84,0.48)*gaussian(r,0.38+impactAge*0.5)*flash*42.0;
            energy += GOLD*gaussian(r,1.1)*hit*0.8;
        }
        float shellRadius=0.22+impactAge*2.7;
        float shellD=abs(r-shellRadius);
        float shellLife=1.0-smoothstep(0.25,1.15,impactAge);
        if (shellD < 0.21 && shellLife > 0.0) {
            float broken=0.4+0.6*noise(atan(impactP.z,impactP.x)*14.0+impactP.y*7.0-impactAge*8.0);
            energy += CYAN*gaussian(shellD,0.06)*broken*shellLife*3.0;
        }
        if (shellLife > 0.0) distanceHint=min(distanceHint,shellD);
        if (hit > 0.0) distanceHint=min(distanceHint,r);
        // 两道贴地震波均有独立年龄；尚未出生的第二环严格为零。
        vec3 ground = p-vec3(Target.x,0.08,Target.z);
        float groundR=length(ground.xz);
        for(int i=0;i<2;++i) {
            float age=impactAge-float(i)*0.20;
            if(age>0.0 && age<1.65) {
                float waveRadius=age*3.8;
                float waveD=length(vec2(groundR-waveRadius,ground.y*1.6));
                float fade=(1.0-smoothstep(0.75,1.65,age))*smoothstep(0.0,0.07,age);
                if(waveD<0.945) {
                    float ripples=0.65+0.35*sin(atan(ground.z,ground.x)*21.0-age*18.0);
                    energy += mix(GOLD,CYAN,float(i))*gaussian(waveD,0.07)*fade*ripples*8.0;
                    energy += mix(GOLD,CYAN,float(i))*gaussian(waveD,0.27)*fade*0.55;
                }
                distanceHint=min(distanceHint,waveD);
            }
        }
    }
    return energy;
}

vec3 sparks(vec3 ro,vec3 rd,float limit,float t,vec3 axis,vec3 side,vec3 up) {
    vec3 sum=vec3(0);
    // 每个碎光使用有限线段的解析投影，避免在体积循环里遍历粒子。
    for(int i=0;i<32;++i) {
        float fi=float(i);
        float birth=1.07+hash(fi+18.0)*0.18;
        float age=t-birth;
        if(age>0.0 && age<1.8) {
            float az=hash(fi+3.0)*PI*2.0;
            float vertical=hash(fi+11.0)*1.6-0.3;
            vec3 direction=normalize(side*cos(az)+axis*sin(az)+vec3(0,vertical,0));
            float speed=2.0+hash(fi+29.0)*4.5;
            vec3 pos=Target+direction*speed*age+vec3(0,-1.5*age*age,0);
            vec3 tail=pos-direction*(0.15+speed*0.055);
            vec3 segment=pos-tail;
            vec3 w=ro-tail;
            float b=dot(rd,segment);
            float c=dot(segment,segment);
            float d=dot(rd,w);
            float e=dot(segment,w);
            float along=clamp((e-b*d)/max(c-b*b,1e-5),0.0,1.0);
            vec3 nearest=tail+segment*along;
            float rayT=dot(nearest-ro,rd);
            float miss=length(ro+rd*rayT-nearest);
            if(rayT>0.0 && rayT<limit && pos.y>0.02) {
                float fade=(1.0-smoothstep(0.4,1.8,age))*smoothstep(0.0,0.04,age);
                sum += mix(GOLD,WHITE,hash(fi+73.0))*gaussian(miss,0.018)*fade*2.0;
                sum += GOLD*gaussian(miss,0.065)*fade*0.12;
            }
        }
    }
    return sum;
}

void main() {
    vec2 uv=gl_FragCoord.xy/max(ScreenSize,vec2(1));
    vec4 scene=texture(SceneSampler,uv);
    float t=RepeatPreview!=0 ? mod(max(SkillAge,0.0),4.6) : SkillAge;
    if(t<=0.0 || t>=3.2 || Intensity<=0.0 || length(Target-Origin)<0.01) {
        fragColor=scene;
        return;
    }
    vec3 ro=CameraPosition;
    vec3 rd=normalize(unproject(uv,0.9999)-ro);
    float depth=texture(DepthSampler,uv).r;
    float limit=depth<0.999999 ? max(dot(unproject(uv,depth)-ro,rd)-0.012,0.0) : 80.0;
    vec3 axis=normalize(Target-Origin);
    vec3 reference=abs(axis.y)>0.98 ? vec3(0,0,1) : vec3(0,1,0);
    vec3 side=normalize(cross(axis,reference));
    vec3 up=cross(side,axis);
    float beamLength=length(Target-Origin);

    // 按当前阶段收紧有限空间；碎光单独解析计算，不受体积盒裁剪。
    vec3 low=Origin-vec3(2.6);
    vec3 high=Origin+vec3(2.6);
    if(t>0.83 && t<2.05) {
        vec3 head=Origin+axis*beamLength*clamp((t-0.83)/0.24,0.0,1.0);
        low=min(low,min(Origin,head)-vec3(2.0));
        high=max(high,max(Origin,head)+vec3(2.0));
    }
    if(t>1.07) {
        float age=t-1.07;
        float waveRadius=min(age,1.65)*3.8+0.945;
        float impactExtent=max(3.85,0.22+min(age,1.15)*2.7+0.21);
        vec3 impactLow=Target-vec3(impactExtent);
        vec3 impactHigh=Target+vec3(impactExtent);
        impactLow=min(impactLow,vec3(Target.x-waveRadius,-0.52,Target.z-waveRadius));
        impactHigh=max(impactHigh,vec3(Target.x+waveRadius,0.68,Target.z+waveRadius));
        low=t>=2.05 ? impactLow : min(low,impactLow);
        high=t>=2.05 ? impactHigh : max(high,impactHigh);
    }
    vec3 safeDirection=vec3(abs(rd.x)<1e-6?1e-6:rd.x,abs(rd.y)<1e-6?1e-6:rd.y,abs(rd.z)<1e-6?1e-6:rd.z);
    vec3 entry=(low-ro)/safeDirection;
    vec3 exit=(high-ro)/safeDirection;
    vec3 nearPlane=min(entry,exit);
    vec3 farPlane=max(entry,exit);
    float begin=max(max(nearPlane.x,nearPlane.y),max(nearPlane.z,0.0));
    float end=min(min(farPlane.x,farPlane.y),min(farPlane.z,limit));
    vec3 energy=vec3(0);
    float cursor=begin;
    int budget=clamp(Steps,64,320);
    for(int i=0;i<320;++i) {
        if(i>=budget || cursor>=end) break;
        vec3 p=ro+rd*cursor;
        float hint;
        vec3 emission=field(p,t,axis,side,up,beamLength,hint);
        float stepSize=clamp(hint*0.45,0.026,0.40);
        stepSize=min(stepSize,end-cursor);
        energy+=emission*stepSize;
        cursor+=stepSize;
    }
    energy+=sparks(ro,rd,limit,t,axis,side,up);
    energy*=clamp(Intensity,0.0,4.0);
    vec3 light=1.0-exp(-energy*1.25);
    fragColor=vec4(scene.rgb+light*(1.0-scene.rgb),scene.a);
}
