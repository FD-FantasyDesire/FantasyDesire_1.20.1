package lab;

import com.google.gson.*;
import org.joml.*;
import org.lwjgl.system.MemoryStack;
import java.io.*;
import java.nio.*;
import java.util.*;
import static org.lwjgl.opengl.GL33C.*;

/** 与 1.20.1 core JSON 对齐的程序、uniform 和混合状态；GPU 编译/链接失败严格报错。 */
final class ShaderProgram implements AutoCloseable {
    record Attribute(String name,int offset,int size,int type,boolean integer,boolean normalized) { }
    record Uniform(String name,String type,int count,float[] defaults,int location) { }
    final int id;
    final List<Attribute> attributes=new ArrayList<>();
    final Map<String,Uniform> uniforms=new LinkedHashMap<>();
    final Map<String,Integer> samplers=new LinkedHashMap<>();
    final List<String> warnings=new ArrayList<>();
    final int stride;
    final JsonObject blend;
    final Project project;

    ShaderProgram(Project project) throws IOException {
        this.project=project;
        JsonObject json=project.shader;
        int offset=0;
        if (!json.has("attributes")) throw new IOException("请显式声明 attributes，以匹配 Java 端 DefaultVertexFormat");
        Set<String> names=new HashSet<>();
        for(JsonElement a:json.getAsJsonArray("attributes")) {
            String n=a.getAsString();
            if(!names.add(n))throw new IOException("重复属性: "+n);
            Attribute attr=switch(n) {
                case "Position" -> new Attribute(n,offset,3,GL_FLOAT,false,false);
                case "Color" -> new Attribute(n,offset,4,GL_UNSIGNED_BYTE,false,true);
                case "UV0", "UV" -> new Attribute(n,offset,2,GL_FLOAT,false,false);
                case "UV1", "UV2" -> new Attribute(n,offset,2,GL_SHORT,true,false);
                case "Normal" -> new Attribute(n,offset,3,GL_BYTE,false,true);
                default -> throw new IOException("未知顶点属性: "+n);
            };
            attributes.add(attr);
            offset+=attr.size*(attr.type==GL_FLOAT?4:attr.type==GL_SHORT?2:1);
            if(n.equals("Normal"))offset++;
        }
        stride=offset;
        if(!names.contains("Position"))throw new IOException("试验网格需要 Position 属性");
        blend=Project.object(json,"blend");
        validateBlend();
        int vs=0,fs=0,program=0;
        try {
            vs=compile(GL_VERTEX_SHADER,project.stage("vertex",".vsh"));
            fs=compile(GL_FRAGMENT_SHADER,project.stage("fragment",".fsh"));
            program=glCreateProgram();glAttachShader(program,vs);glAttachShader(program,fs);
            for(int i=0;i<attributes.size();i++)glBindAttribLocation(program,i,attributes.get(i).name);
            glLinkProgram(program);
            if(glGetProgrami(program,GL_LINK_STATUS)==GL_FALSE)throw new IOException("链接失败:\n"+glGetProgramInfoLog(program));
            if(json.has("uniforms")) for(JsonElement e:json.getAsJsonArray("uniforms")) {
                JsonObject u=e.getAsJsonObject();String name=u.get("name").getAsString(),type=u.get("type").getAsString();int count=u.get("count").getAsInt();
                int expected=switch(type){case "int","float"->count>=1&&count<=4?count:-1;case "matrix2x2"->4;case "matrix3x3"->9;case "matrix4x4"->16;default->-1;};
                if(expected!=count||expected<1||uniforms.containsKey(name))throw new IOException("uniform 类型、count 或名称无效: "+name);
                float[] values=values(u.get("values"),count);
                int location=glGetUniformLocation(program,name);
                uniforms.put(name,new Uniform(name,type,count,values,location));
                if(location<0)warnings.add("未使用/已优化 uniform: "+name);
            }
            if(json.has("samplers"))for(JsonElement e:json.getAsJsonArray("samplers")) {
                String name=e.getAsJsonObject().get("name").getAsString();
                if(samplers.put(name,glGetUniformLocation(program,name))!=null)throw new IOException("重复 sampler: "+name);
            }
            if(samplers.size()>glGetInteger(GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS))throw new IOException("sampler 数量超过 GPU 限制");
            validateActive(program);
            id=program;
        }catch(Exception e){if(program!=0)glDeleteProgram(program);if(e instanceof IOException io)throw io;throw new IOException("Shader JSON 无效: "+e.getMessage(),e);}
        finally{if(vs!=0)glDeleteShader(vs);if(fs!=0)glDeleteShader(fs);}
    }
    private int compile(int type,Project.Resource resource)throws IOException {
        String source=project.preprocess(resource);int s=glCreateShader(type);
        glShaderSource(s,source);glCompileShader(s);
        if(glGetShaderi(s,GL_COMPILE_STATUS)==GL_FALSE){String log=glGetShaderInfoLog(s);glDeleteShader(s);throw new IOException(resource.id()+"\n"+log+"\n源码索引: "+project.sourceMap);}
        return s;
    }
    private void validateActive(int program)throws IOException {
        try(MemoryStack stack=MemoryStack.stackPush()) {
            IntBuffer size=stack.mallocInt(1),type=stack.mallocInt(1);
            for(int i=0;i<glGetProgrami(program,GL_ACTIVE_UNIFORMS);i++) {
                String n=glGetActiveUniform(program,i,size,type);int t=type.get(0);
                if(samplers.containsKey(n)){if(t!=GL_SAMPLER_2D)throw new IOException("当前 sampler 支持 sampler2D: "+n);continue;}
                Uniform u=uniforms.get(n);
                if(u==null)throw new IOException("GLSL 中活动 uniform 未在 JSON 声明: "+n);
                int expected=switch(u.type){case "int"->new int[]{GL_INT,GL_INT_VEC2,GL_INT_VEC3,GL_INT_VEC4}[u.count-1];case "float"->new int[]{GL_FLOAT,GL_FLOAT_VEC2,GL_FLOAT_VEC3,GL_FLOAT_VEC4}[u.count-1];case "matrix2x2"->GL_FLOAT_MAT2;case "matrix3x3"->GL_FLOAT_MAT3;default->GL_FLOAT_MAT4;};
                if(t!=expected||size.get(0)!=1)throw new IOException("GLSL 与 JSON uniform 类型不一致: "+n);
            }
            for(int i=0;i<glGetProgrami(program,GL_ACTIVE_ATTRIBUTES);i++) {
                String n=glGetActiveAttrib(program,i,size,type);
                if(n.startsWith("gl_"))continue;
                Attribute a=attributes.stream().filter(v->v.name.equals(n)).findFirst().orElseThrow(()->new IOException("活动顶点属性未在 JSON 声明: "+n));
                int expected=a.integer?GL_INT_VEC2:a.size==2?GL_FLOAT_VEC2:a.size==3?GL_FLOAT_VEC3:GL_FLOAT_VEC4;
                if(type.get(0)!=expected)throw new IOException("顶点属性类型不匹配: "+n);
            }
        }
    }
    static float[] values(JsonElement element,int count)throws IOException {
        JsonArray a=element.isJsonArray()?element.getAsJsonArray():new JsonArray();if(!element.isJsonArray())a.add(element);
        if(a.size()!=1&&a.size()!=count)throw new IOException("期望 "+count+" 个数值，实际 "+a.size());
        float[] values=new float[count];for(int i=0;i<count;i++){values[i]=a.get(a.size()==1?0:i).getAsFloat();if(!Float.isFinite(values[i]))throw new IOException("uniform 不允许 NaN/Infinity");}return values;
    }
    void use(){glUseProgram(id);for(Uniform u:uniforms.values())set(u.name,u.defaults);}
    void set(String name,float...values){
        Uniform u=uniforms.get(name);if(u==null||u.location<0)return;
        if(values.length!=u.count)throw new IllegalArgumentException(name+" 需要 "+u.count+" 个值");
        if(u.type.equals("int")){int[] ints=new int[values.length];for(int i=0;i<ints.length;i++)ints[i]=(int)values[i];switch(ints.length){case 1->glUniform1iv(u.location,ints);case 2->glUniform2iv(u.location,ints);case 3->glUniform3iv(u.location,ints);case 4->glUniform4iv(u.location,ints);}}
        else switch(u.type){case "matrix2x2"->glUniformMatrix2fv(u.location,false,values);case "matrix3x3"->glUniformMatrix3fv(u.location,false,values);case "matrix4x4"->glUniformMatrix4fv(u.location,false,values);default->{switch(values.length){case 1->glUniform1fv(u.location,values);case 2->glUniform2fv(u.location,values);case 3->glUniform3fv(u.location,values);case 4->glUniform4fv(u.location,values);}}}
    }
    void matrix(String name,Matrix4f m){set(name,m.get(new float[16]));}
    void overrides(JsonObject values)throws IOException{for(var e:values.entrySet()){Uniform u=uniforms.get(e.getKey());if(u==null)throw new IOException("JSON 未声明 uniform: "+e.getKey());set(e.getKey(),values(e.getValue(),u.count));}}
    void applyBlend(){
        int equation=equation();int sr=factor(Project.string(blend,"srcrgb","one")),dr=factor(Project.string(blend,"dstrgb","zero"));
        int sa=factor(Project.string(blend,"srcalpha","one")),da=factor(Project.string(blend,"dstalpha","zero"));
        boolean separate=blend.has("srcalpha")||blend.has("dstalpha");
        if(equation==GL_FUNC_ADD&&sr==GL_ONE&&dr==GL_ZERO&&sa==GL_ONE&&da==GL_ZERO){glDisable(GL_BLEND);return;}
        glEnable(GL_BLEND);glBlendEquation(equation);glBlendFuncSeparate(sr,dr,separate?sa:sr,separate?da:dr);
    }
    private int equation(){return switch(Project.string(blend,"func","add").toLowerCase(Locale.ROOT)){case "add"->GL_FUNC_ADD;case "subtract"->GL_FUNC_SUBTRACT;case "reversesubtract","reverse_subtract"->GL_FUNC_REVERSE_SUBTRACT;case "min"->GL_MIN;case "max"->GL_MAX;default->throw new IllegalArgumentException("未知 blend func");};}
    private static int factor(String name){return switch(name.trim().toLowerCase(Locale.ROOT).replace("_", "").replace("one","1").replace("zero","0").replace("minus","-")){case "0"->GL_ZERO;case "1"->GL_ONE;case "srccolor"->GL_SRC_COLOR;case "1-srccolor"->GL_ONE_MINUS_SRC_COLOR;case "dstcolor"->GL_DST_COLOR;case "1-dstcolor"->GL_ONE_MINUS_DST_COLOR;case "srcalpha"->GL_SRC_ALPHA;case "1-srcalpha"->GL_ONE_MINUS_SRC_ALPHA;case "dstalpha"->GL_DST_ALPHA;case "1-dstalpha"->GL_ONE_MINUS_DST_ALPHA;default->throw new IllegalArgumentException("未知混合因子: "+name);};}
    private void validateBlend()throws IOException{try{equation();for(String name:List.of("srcrgb","dstrgb","srcalpha","dstalpha"))if(blend.has(name))factor(blend.get(name).getAsString());}catch(IllegalArgumentException e){throw new IOException(e);}}
    @Override public void close(){glDeleteProgram(id);}
}
