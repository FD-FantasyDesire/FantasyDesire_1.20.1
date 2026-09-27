package lab;

import com.google.gson.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import static org.lwjgl.opengl.GL33C.*;

/** 实际 GPU 集成检查：编译、绘制、资源重载、错误恢复与场景深度隔离。 */
final class Verification {
    static void run(Renderer r)throws Exception{
        Path out=r.root.resolve("build/verification");Files.createDirectories(out);List<String> passed=new ArrayList<>();
        for(String preset:List.of("surface","entity","sky","depth","fantasy-rift","fantasy-void","fantasy-frost")){
            Path p=r.root.resolve("examples/"+preset+".preview.json");
            r.load(p);r.seconds=2.75;r.render(800,600);r.output.capture(out.resolve(preset+".png"));checkImage(out.resolve(preset+".png"));
            r.render(537,311);r.render(800,600);passed.add("GPU draw + resize: "+preset);
        }
        // 读取使用者本机资源，不复制或分发游戏文件。
        Path originals=r.root.resolve("../../src/main/resources/assets/fantasydesire/shaders/core").normalize();
        require(Files.isDirectory(originals),"找不到主项目正式 shader 目录: "+originals);
        try(var files=Files.list(originals)){
            for(Path file:files.filter(p->p.toString().endsWith(".json")).sorted().toList())try(Project project=new Project(file,r.minecraftJar);ShaderProgram shader=new ShaderProgram(project)){passed.add("Existing core shader: "+file.getFileName());}
        }
        Path scratch=out.resolve("reload");Files.createDirectories(scratch);
        Files.writeString(scratch.resolve("test.vsh"),"#version 150\nin vec3 Position;\nin vec2 UV0;\nout vec2 uv;\nvoid main(){gl_Position=vec4(Position,1);uv=UV0;}\n");
        String valid="#version 150\nin vec2 uv;out vec4 fragColor;\nvoid main(){fragColor=vec4(uv,0.3,1);}\n";
        Files.writeString(scratch.resolve("test.fsh"),valid);
        Files.writeString(scratch.resolve("test.json"),"{\"vertex\":\"test\",\"fragment\":\"test\",\"attributes\":[\"Position\",\"UV0\"],\"uniforms\":[],\"samplers\":[]}");
        Files.writeString(scratch.resolve("test.preview.json"),"{\"shader\":\"test.json\",\"target\":\"screen\",\"state\":{\"depthTest\":false,\"depthWrite\":false,\"cull\":false}}");
        r.load(scratch.resolve("test.preview.json"));int previous=r.program.id;
        Files.writeString(scratch.resolve("test.fsh"),valid+"BROKEN_SHADER\n");require(r.changed(),"阶段修改未被检测");
        try{r.load(scratch.resolve("test.preview.json"));throw new AssertionError("编译错误被接受");}catch(IOException expected){require(r.program.id==previous&&glIsProgram(previous),"编译失败破坏旧程序");}
        r.render(320,240);passed.add("Compile failure retains last valid program");
        Files.writeString(scratch.resolve("test.fsh"),valid.replace("0.3","0.7"));require(r.changed(),"修复未被检测");r.load(scratch.resolve("test.preview.json"));require(!glIsProgram(previous),"旧程序未释放");r.render(320,240);passed.add("Repair reload + old program disposal");
        Files.writeString(scratch.resolve("test.fsh"),"#version 150\nin vec3 uv;out vec4 fragColor;void main(){fragColor=vec4(uv,1);}");
        previous=r.program.id;try{r.load(scratch.resolve("test.preview.json"));throw new AssertionError("链接错误被接受");}catch(IOException expected){require(r.program.id==previous,"链接错误破坏旧程序");}passed.add("Link failure is rejected transactionally");
        Files.writeString(scratch.resolve("test.fsh"),valid);r.load(scratch.resolve("test.fsh"));require(r.project.descriptor.equals(scratch.resolve("test.json")),"直接打开 fsh 未找到 JSON");r.load(scratch.resolve("test.vsh"));passed.add("Direct fsh / vsh companion JSON resolution");
        String descriptor=Files.readString(scratch.resolve("test.json"));
        r.load(scratch.resolve("test.preview.json"));Files.writeString(scratch.resolve("test.json"),"{");
        try{r.load(scratch.resolve("test.preview.json"));throw new AssertionError("无效 JSON 被接受");}catch(IOException expected){}
        Files.writeString(scratch.resolve("test.json"),descriptor);require(r.changed(),"配套 JSON 修复后未触发重载");r.load(scratch.resolve("test.preview.json"));passed.add("Malformed companion JSON repair is watched");
        String imported="#version 150\n#moj_import <lab_test.glsl>\nin vec2 uv;out vec4 fragColor;void main(){fragColor=lab_color(uv);}\n";
        Files.writeString(scratch.resolve("test.fsh"),imported);Files.writeString(scratch.resolve("lab_test.glsl"),"#version 150\nvec4 lab_color(vec2 p){return vec4(p,0.5,1);}\n");
        r.load(scratch.resolve("test.preview.json"));Files.writeString(scratch.resolve("lab_test.glsl"),"#version 150\nvec4 lab_color(vec2 p){return vec4(p,0.6,1);}\n");require(r.changed(),"include 修改未触发重载");r.load(scratch.resolve("test.preview.json"));r.render(320,240);passed.add("Include expansion, version directives and dependency reload");
        if(r.minecraftJar!=null)try(java.util.zip.ZipFile jar=new java.util.zip.ZipFile(r.minecraftJar.toFile())){
            for(String name:List.of("rendertype_solid","rendertype_entity_cutout")){
                var entry=jar.getEntry("assets/minecraft/shaders/core/"+name+".json");if(entry==null)continue;
                Path p=scratch.resolve(name+".json");try(var stream=jar.getInputStream(entry)){Files.write(p,stream.readAllBytes());}
                try(Project vanilla=new Project(p,r.minecraftJar);ShaderProgram compiled=new ShaderProgram(vanilla)){passed.add("Vanilla 1.20.1 shader + Mojang includes: "+name);}
            }
        }
        r.load(r.root.resolve("examples/surface.preview.json"));
        r.target="screen";r.depthTest=false;r.depthWrite=false;r.cull=false;r.render(320,240);passed.add("Integer UV1/UV2 + normalized Color/Normal vertex upload");
        r.load(r.root.resolve("examples/depth.preview.json"));r.render(320,240);
        require(r.blocks.depth!=r.scene.depth&&r.scene.depth!=r.output.depth,"深度目标必须独立");
        float[] terrain=new float[320*240],scene=new float[320*240];glBindTexture(GL_TEXTURE_2D,r.blocks.depth);glGetTexImage(GL_TEXTURE_2D,0,GL_DEPTH_COMPONENT,GL_FLOAT,terrain);glBindTexture(GL_TEXTURE_2D,r.scene.depth);glGetTexImage(GL_TEXTURE_2D,0,GL_DEPTH_COMPONENT,GL_FLOAT,scene);
        int nearer=0;for(int i=0;i<terrain.length;i++)if(scene[i]<terrain[i]-1e-6)nearer++;require(nearer>20,"实体未进入完整场景深度");passed.add("Separate terrain / scene depth with entity occlusion ("+nearer+" pixels)");
        JsonObject report=new JsonObject();report.addProperty("gpu",glGetString(GL_RENDERER));report.addProperty("opengl",glGetString(GL_VERSION));report.addProperty("result","PASS");report.add("checks",Project.JSON.toJsonTree(passed));Files.writeString(out.resolve("report.json"),Project.JSON.toJson(report));System.out.println(Project.JSON.toJson(report));
    }
    static void checkImage(Path p)throws IOException{BufferedImage image=ImageIO.read(p.toFile());Set<Integer> colors=new HashSet<>();for(int y=0;y<image.getHeight();y+=7)for(int x=0;x<image.getWidth();x+=7)colors.add(image.getRGB(x,y));require(colors.size()>30,"画面疑似空白: "+p);}
    static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
