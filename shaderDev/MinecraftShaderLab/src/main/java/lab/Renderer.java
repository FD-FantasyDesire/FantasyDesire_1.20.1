package lab;

import com.google.gson.*;
import org.joml.Matrix4f;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import static org.lwjgl.opengl.GL33C.*;

final class Renderer implements AutoCloseable {
    final Path root,minecraftJar;
    Project project;
    ShaderProgram program;
    Textures textures;
    final ShaderProgram baseline,background;
    final Textures baseTextures=new Textures();
    final Mesh terrain=Mesh.terrain(),plane=Mesh.plane(),sky=Mesh.sky(),volume=Mesh.volume();
    Mesh effectPlane=Mesh.plane();
    Target blocks,scene,output,volumeColor;
    float volumeScale=1;
    int resolvePass=0,renderWidth,renderHeight;
    String target="blocks",entity="zombie";
    boolean depthTest=true,depthWrite=true,cull=true,overlay=false;
    boolean paused=false,showTerrain=true,showEntity=true;
    double seconds=0,speed=1;
    float yaw=35,pitch=25,distance=9,fov=60;
    final JsonObject overrides=new JsonObject();
    Map<Path,String> watched=new LinkedHashMap<>();
    Path requested;
    String status="请选择 shader";
    long generation=0;
    boolean measureGpu=false;
    final boolean gpuTimersAvailable=GL.getCapabilities().OpenGL33||GL.getCapabilities().GL_ARB_timer_query;
    double customGpuMs=0;

    Renderer(Path root,Path minecraftJar)throws IOException{
        this.root=root;this.minecraftJar=minecraftJar;
        Project bp=new Project(root.resolve("examples/surface.preview.json"),minecraftJar);baseline=new ShaderProgram(bp);
        Project sp=new Project(root.resolve("examples/sky.preview.json"),minecraftJar);background=new ShaderProgram(sp);
    }
    void load(Path path)throws IOException{
        boolean different=requested==null||!requested.equals(path.toAbsolutePath().normalize());
        requested=path.toAbsolutePath().normalize();Project next=null;ShaderProgram compiled=null;Textures loaded=null;
        try{
            next=new Project(requested,minecraftJar);compiled=new ShaderProgram(next);loaded=new Textures();
            compiled.use();compiled.overrides(Project.object(next.preview,"uniforms"));
            for(var binding:Project.object(next.preview,"bindings").entrySet())validateBinding(compiled,binding.getKey(),binding.getValue());
            for(var sampler:compiled.samplers.entrySet())if(sampler.getValue()>=0){String source=textureSource(next,sampler.getKey());if(source.equals("@surface")||source.equals("@volume_color"))continue;if(source.startsWith("@scene")||source.equals("@block_depth")){if(!Set.of("screen","volume").contains(next.target))throw new IOException("场景采样只在 screen / volume pass 中有效: "+sampler.getKey());}else loaded.get(next,source);}
            float nextVolumeScale=number(next.preview,"volumeScale",1,.25f,1);
            var renderSize=compiled.uniforms.get("RenderSize");var resolve=compiled.uniforms.get("ResolvePass");
            if(nextVolumeScale!=1&&(!Set.of("screen","volume").contains(next.target)||renderSize==null||!renderSize.type().equals("float")||renderSize.count()!=2||resolve==null||!resolve.type().equals("int")||resolve.count()!=1||!compiled.samplers.containsKey("AuraSampler")||!Project.object(next.preview,"textures").has("AuraSampler")||!textureSource(next,"AuraSampler").equals("@volume_color")))throw new IOException("volumeScale 需要 screen / volume、RenderSize(vec2)、ResolvePass(int) 和 AuraSampler=@volume_color");
            for(var sampler:compiled.samplers.entrySet())if(sampler.getValue()>=0&&textureSource(next,sampler.getKey()).equals("@volume_color")&&!Set.of("screen","volume").contains(next.target))throw new IOException("@volume_color 仅可用于 screen / volume");
            String nextEntity=Project.string(next.preview,"entity","zombie");
            if(!Set.of("zombie","creeper").contains(nextEntity))throw new IOException("entity 支持 zombie / creeper");
            if(next.preview.has("scale")){float scale=next.preview.get("scale").getAsFloat();if(!Float.isFinite(scale)||scale<=0||scale>100)throw new IOException("scale 必须位于 (0,100]");}
            JsonObject state=Project.object(next.preview,"state"),camera=Project.object(next.preview,"camera"),sceneConfig=Project.object(next.preview,"scene");
            boolean nextDepth=Project.bool(state,"depthTest",!Set.of("sky","screen","volume").contains(next.target));
            boolean nextWrite=Project.bool(state,"depthWrite",next.target.equals("blocks")||next.target.equals("entity"));
            boolean nextCull=Project.bool(state,"cull",Set.of("blocks","entity","volume").contains(next.target));
            boolean nextOverlay=Project.bool(next.preview,"overlay",false),nextTerrain=Project.bool(sceneConfig,"terrain",true),nextActor=Project.bool(sceneConfig,"entity",true);
            float nextYaw=number(camera,"yaw",35,-3600,3600),nextPitch=number(camera,"pitch",25,-85,85),nextDistance=number(camera,"distance",9,1.5f,60),nextFov=number(camera,"fov",60,15,120);
            JsonObject mesh=Project.object(next.preview,"mesh");float[] uv=mesh.has("uv")?ShaderProgram.values(mesh.get("uv"),4):new float[]{0,0,1,1};
            float meshWidth=number(mesh,"width",2,.001f,100),meshHeight=number(mesh,"height",2,.001f,100);
            number(mesh,"depth",2,.001f,100);
            float copiesValue=number(mesh,"copies",1,1,4096);
            if(copiesValue!=(int)copiesValue)throw new IOException("mesh.copies 必须是 1–4096 的整数");
            int copies=(int)copiesValue;
            if(copies>1&&!next.target.equals("quad"))throw new IOException("mesh.copies 仅适用于 quad");
            Project.bool(mesh,"billboard",false);
            String billboardMode=Project.string(mesh,"billboardMode","spherical");
            if(!Set.of("spherical","vertical").contains(billboardMode))throw new IOException("billboardMode 支持 spherical / vertical");
            String quadStage=Project.string(next.preview,"quadStage","after_entities");
            if(!Set.of("before_entities","after_entities").contains(quadStage))throw new IOException("quadStage 支持 before_entities / after_entities");
            if(program!=null)program.close();if(project!=null)project.close();if(textures!=null)textures.close();
            project=next;program=compiled;textures=loaded;target=next.target;entity=nextEntity;overrides.entrySet().clear();
            volumeScale=nextVolumeScale;
            depthTest=nextDepth;depthWrite=nextWrite;cull=nextCull;overlay=nextOverlay;showTerrain=nextTerrain;showEntity=nextActor;
            if(different){yaw=nextYaw;pitch=nextPitch;distance=nextDistance;fov=nextFov;}
            effectPlane.close();effectPlane=copies>1?Mesh.batchPlane(uv,meshWidth,meshHeight,copies):Mesh.plane(uv,meshWidth,meshHeight);
            generation++;status="已加载 · "+project.descriptor.getFileName()+" · "+program.stride+" 字节/顶点";
            watched=new LinkedHashMap<>(next.watched);glUseProgram(0);
        }catch(Exception e){
            Map<Path,String> retry=new LinkedHashMap<>();watched.keySet().forEach(p->retry.put(p,Project.stamp(p)));retry.put(requested,Project.stamp(requested));if(next!=null)retry.putAll(next.watched);
            if(requested.toString().endsWith(".preview.json"))try{JsonObject config=Project.read(requested);Path failedShader=requested.getParent().resolve(config.get("shader").getAsString()).normalize();retry.put(failedShader,Project.stamp(failedShader));}catch(Exception ignored){}
            watched=retry;
            if(compiled!=null)compiled.close();if(loaded!=null)loaded.close();if(next!=null)next.close();
            status="加载失败，保留上一版画面:\n"+e.getMessage();System.err.println(status);
            if(e instanceof IOException io)throw io;throw new IOException(status,e);
        }
    }
    private static float number(JsonObject object,String key,float fallback,float min,float max)throws IOException{float value=object.has(key)?object.get(key).getAsFloat():fallback;if(!Float.isFinite(value)||value<min||value>max)throw new IOException(key+" 必须在 "+min+" 到 "+max+" 之间");return value;}
    boolean changed(){return watched.entrySet().stream().anyMatch(e->!e.getValue().equals(Project.stamp(e.getKey())));}
    void resize(int w,int h){if(output!=null&&output.width==w&&output.height==h)return;if(output!=null){blocks.close();scene.close();output.close();}blocks=new Target(w,h);scene=new Target(w,h);output=new Target(w,h);}
    void render(int w,int h)throws IOException{
        customGpuMs=gpuTimersAvailable?0:-1;
        renderWidth=w;renderHeight=h;resolvePass=0;
        resize(w,h);glDisable(GL_FRAMEBUFFER_SRGB);glDisable(GL_SCISSOR_TEST);glColorMask(true,true,true,true);glDepthFunc(GL_LEQUAL);glFrontFace(GL_CCW);glCullFace(GL_BACK);
        float yr=(float)java.lang.Math.toRadians(yaw),pr=(float)java.lang.Math.toRadians(pitch);
        Vector3f eye=new Vector3f((float)(distance*java.lang.Math.cos(pr)*java.lang.Math.sin(yr)),(float)(distance*java.lang.Math.sin(pr))+1,(float)(distance*java.lang.Math.cos(pr)*java.lang.Math.cos(yr)));
        Matrix4f view=new Matrix4f().lookAt(eye,new Vector3f(0,1,0),new Vector3f(0,1,0));Matrix4f projection=new Matrix4f().perspective((float)java.lang.Math.toRadians(fov),(float)w/h,.05f,256);
        blocks.clear();
        draw(sky,target.equals("sky")?program:background,new Matrix4f(view).m30(0).m31(0).m32(0),projection,new Matrix4f().scaling(100),eye,"@white",target.equals("sky"));
        if(showTerrain){
            if(target.equals("blocks")&&overlay)draw(terrain,baseline,view,projection,new Matrix4f(),eye,"minecraft:textures/block/stone_bricks.png",false);
            draw(terrain,target.equals("blocks")?program:baseline,view,projection,new Matrix4f(),eye,"minecraft:textures/block/stone_bricks.png",target.equals("blocks"));
        }
        blocks.copyTo(scene);
        drawEntity(view,projection,eye);
        scene.copyTo(output);
        if(target.equals("quad")&&project!=null){
            boolean beforeEntities=Project.string(project.preview,"quadStage","after_entities").equals("before_entities");
            // 保留干净的 scene 附件；背景效果在输出中先画，实体随后覆盖，且不修改地形深度。
            if(beforeEntities)blocks.copyTo(output);
            Matrix4f model=new Matrix4f().translation(0,1.5f,0);
            JsonObject mesh=Project.object(project.preview,"mesh");
            if(Project.bool(mesh,"billboard",false)){
                if(Project.string(mesh,"billboardMode","spherical").equals("vertical"))model.rotateY(yr);
                else model.mul(new Matrix4f(view).m30(0).m31(0).m32(0).invert());
            }
            float s=project!=null&&project.preview.has("scale")?project.preview.get("scale").getAsFloat():1.5f;model.scale(s);
            draw(effectPlane,program,view,projection,model,eye,"@checker",true);
            if(beforeEntities)drawEntity(view,projection,eye);
        }
        if(target.equals("volume")&&project!=null){
            JsonObject mesh=Project.object(project.preview,"mesh");
            Matrix4f model=new Matrix4f().translation(0,1.5f,0).scale(
                number(mesh,"width",2,.001f,100)*.5f,number(mesh,"height",2,.001f,100)*.5f,number(mesh,"depth",2,.001f,100)*.5f);
            // scene 与 output 的附件独立；体积可以读取实体与方块深度。
            if(volumeScale<1){
                int lowWidth=Math.max(1,(int)Math.ceil(w*volumeScale)),lowHeight=Math.max(1,(int)Math.ceil(h*volumeScale));
                if(volumeColor==null||volumeColor.width!=lowWidth||volumeColor.height!=lowHeight){if(volumeColor!=null)volumeColor.close();volumeColor=new Target(lowWidth,lowHeight);}
                volumeColor.clearTransparent();
                renderWidth=lowWidth;renderHeight=lowHeight;
                draw(volume,program,view,projection,model,eye,"@checker",true);
                output.bind();renderWidth=w;renderHeight=h;resolvePass=1;
            }
            draw(volume,program,view,projection,model,eye,"@checker",true);
        }
        if(target.equals("screen")){
            resolvePass=volumeScale<1?0:-1;
            // screen 同样可先积分低分辨率云层，再由原分辨率阶段重建并绘制细节。
            if(volumeScale<1){
                int lowWidth=Math.max(1,(int)Math.ceil(w*volumeScale)),lowHeight=Math.max(1,(int)Math.ceil(h*volumeScale));
                if(volumeColor==null||volumeColor.width!=lowWidth||volumeColor.height!=lowHeight){if(volumeColor!=null)volumeColor.close();volumeColor=new Target(lowWidth,lowHeight);}
                volumeColor.clearTransparent();renderWidth=lowWidth;renderHeight=lowHeight;
                draw(plane,program,new Matrix4f(),new Matrix4f(),new Matrix4f(),eye,"@checker",true,view,projection);
                output.bind();renderWidth=w;renderHeight=h;resolvePass=1;
            }
            draw(plane,program,new Matrix4f(),new Matrix4f(),new Matrix4f(),eye,"@checker",true,view,projection);
        }
        glDepthMask(true);glDisable(GL_BLEND);glUseProgram(0);
        int error=glGetError();if(error!=GL_NO_ERROR)throw new IOException("OpenGL 错误: 0x"+Integer.toHexString(error));
    }
    private void drawEntity(Matrix4f view,Matrix4f projection,Vector3f eye)throws IOException{
        if(showEntity)try(Mesh actor=Mesh.entity(seconds,entity)){
            String skin=entity.equals("creeper")?"minecraft:textures/entity/creeper/creeper.png":"minecraft:textures/entity/zombie/zombie.png";
            if(target.equals("entity")&&overlay)draw(actor,baseline,view,projection,new Matrix4f(),eye,skin,false);
            draw(actor,target.equals("entity")?program:baseline,view,projection,new Matrix4f(),eye,skin,target.equals("entity"));
        }
    }
    private void draw(Mesh mesh,ShaderProgram shader,Matrix4f view,Matrix4f projection,Matrix4f model,Vector3f eye,String surface,boolean custom)throws IOException{draw(mesh,shader,view,projection,model,eye,surface,custom,view,projection);}
    private void draw(Mesh mesh,ShaderProgram shader,Matrix4f view,Matrix4f projection,Matrix4f model,Vector3f eye,String surface,boolean custom,Matrix4f sceneView,Matrix4f sceneProjection)throws IOException{
        if(shader==null)return;
        boolean isSky=mesh==sky;
        enable(GL_DEPTH_TEST,custom?depthTest:!isSky);glDepthMask(custom?depthWrite:!isSky);enable(GL_CULL_FACE,custom?cull:!isSky);
        shader.use();shader.applyBlend();
        shader.matrix("ModelViewMat",new Matrix4f(view).mul(model));shader.matrix("ProjMat",projection);shader.matrix("TextureMat",new Matrix4f());shader.matrix("EffectLocalMat",new Matrix4f());
        shader.matrix("InvProjMat",new Matrix4f(projection).invert());
        shader.set("IViewRotMat",new Matrix3f(sceneView).invert().get(new float[9]));
        shader.set("ColorModulator",1,1,1,1);shader.set("FogStart",128);shader.set("FogEnd",256);shader.set("FogColor",.035f,.05f,.08f,1);shader.set("FogShape",0);
        shader.set("GameTime",(float)((seconds*20%24000)/24000));shader.set("ScreenSize",output.width,output.height);shader.set("ChunkOffset",0,0,0);shader.set("LineWidth",1);shader.set("GlintAlpha",1);
        Vector3f light0=new Vector3f(.2f,1,-.7f).normalize(),light1=new Vector3f(-.2f,1,.7f).normalize();shader.set("Light0_Direction",light0.x,light0.y,light0.z);shader.set("Light1_Direction",light1.x,light1.y,light1.z);
        if(custom){shader.overrides(Project.object(project.preview,"uniforms"));for(var e:Project.object(project.preview,"bindings").entrySet())bind(shader,e.getKey(),e.getValue(),sceneView,sceneProjection,eye);shader.overrides(overrides);}
        if(custom){shader.set("RenderSize",renderWidth,renderHeight);shader.set("ResolvePass",resolvePass);}
        Textures pool=custom?textures:baseTextures;Project source=shader.project;
        int unit=0;
        for(var sampler:shader.samplers.entrySet()){
            if(sampler.getValue()<0)continue;
            String texture=custom?textureSource(project,sampler.getKey()):defaultTexture(sampler.getKey());
            int textureId=switch(texture){case "@surface"->pool.optional(source,surface);case "@scene_depth"->scene.depth;case "@block_depth"->blocks.depth;case "@scene_color"->scene.color;case "@volume_color"->resolvePass==1?volumeColor.color:pool.get(source,"@white");default->pool.get(source,texture);};
            if(custom&&!Set.of("screen","volume").contains(target)&&(texture.equals("@scene_depth")||texture.equals("@block_depth")||texture.equals("@scene_color")))throw new IOException("采样场景输入需要 screen / volume 模式");
            glActiveTexture(GL_TEXTURE0+unit);glBindTexture(GL_TEXTURE_2D,textureId);glUniform1i(sampler.getValue(),unit++);
        }
        // 隐藏调试模式记录实际 GPU draw 时间，不把截图编码和 HTTP 耗时算进效果成本。
        if(custom&&measureGpu&&gpuTimersAvailable){
            int query=glGenQueries();
            try{glBeginQuery(GL_TIME_ELAPSED,query);try{mesh.draw(shader);}finally{glEndQuery(GL_TIME_ELAPSED);}
                customGpuMs+=glGetQueryObjectui64(query,GL_QUERY_RESULT)/1_000_000.0;
            }finally{glDeleteQueries(query);}
        }else mesh.draw(shader);
        glActiveTexture(GL_TEXTURE0);
    }
    private static String defaultTexture(String name)throws IOException{return switch(name){case "Sampler0"->"@surface";case "Sampler1"->"@overlay";case "Sampler2"->"@lightmap";case "DepthSampler"->"@scene_depth";case "BlockDepthSampler"->"@block_depth";default->throw new IOException("请在 textures 中绑定 sampler: "+name);};}
    private static String textureSource(Project p,String name)throws IOException{JsonObject textures=Project.object(p.preview,"textures");return textures.has(name)?textures.get(name).getAsString():defaultTexture(name);}
    private static String bindingSource(JsonElement value){return value.isJsonObject()?value.getAsJsonObject().get("source").getAsString():value.getAsString();}
    private static void validateBinding(ShaderProgram s,String name,JsonElement value)throws IOException{
        var u=s.uniforms.get(name);if(u==null)throw new IOException("binding 未在 JSON 声明: "+name);
        String source=bindingSource(value);int count=switch(source){case "seconds","ticks","game_time"->1;case "camera_position","camera_relative"->3;case "inverse_view_projection","inverse_view_projection_rotation"->16;default->throw new IOException("未知 binding source: "+source);};
        if(u.count()!=count||!u.type().equals(count==16?"matrix4x4":"float"))throw new IOException("binding 类型不匹配: "+name);
        if(value.isJsonObject())for(String key:List.of("scale","offset"))if(value.getAsJsonObject().has(key)&&!Float.isFinite(value.getAsJsonObject().get(key).getAsFloat()))throw new IOException("binding 参数非有限值");
        if(source.equals("camera_relative")){if(!value.isJsonObject()||!value.getAsJsonObject().has("value"))throw new IOException("camera_relative 需要 value 世界坐标");ShaderProgram.values(value.getAsJsonObject().get("value"),3);}
    }
    private void bind(ShaderProgram s,String name,JsonElement value,Matrix4f view,Matrix4f projection,Vector3f eye){
        String source=bindingSource(value);
        switch(source){
            case "inverse_view_projection"->s.matrix(name,new Matrix4f(projection).mul(view).invert());
            case "inverse_view_projection_rotation"->s.matrix(name,new Matrix4f(projection).mul(new Matrix4f(view).m30(0).m31(0).m32(0)).invert());
            case "camera_position"->s.set(name,eye.x,eye.y,eye.z);
            case "camera_relative"->{JsonArray a=value.getAsJsonObject().getAsJsonArray("value");s.set(name,a.get(0).getAsFloat()-eye.x,a.get(1).getAsFloat()-eye.y,a.get(2).getAsFloat()-eye.z);}
            default->{double time=source.equals("ticks")?seconds*20:source.equals("game_time")?(seconds*20%24000)/24000:seconds;if(value.isJsonObject()){JsonObject v=value.getAsJsonObject();time=time*(v.has("scale")?v.get("scale").getAsDouble():1)+(v.has("offset")?v.get("offset").getAsDouble():0);}s.set(name,(float)time);}
        }
    }
    void override(String name,String json)throws IOException{if(program==null)throw new IOException("请先打开 shader");var u=program.uniforms.get(name);if(u==null)throw new IOException("未知 uniform");JsonElement value=JsonParser.parseString(json);ShaderProgram.values(value,u.count());overrides.add(name,value);}
    static void enable(int state,boolean enabled){if(enabled)glEnable(state);else glDisable(state);}
    @Override public void close(){if(program!=null)program.close();if(project!=null)project.close();if(textures!=null)textures.close();if(volumeColor!=null)volumeColor.close();baseline.close();baseline.project.close();background.close();background.project.close();baseTextures.close();terrain.close();plane.close();effectPlane.close();sky.close();volume.close();if(output!=null){blocks.close();scene.close();output.close();}}
}
