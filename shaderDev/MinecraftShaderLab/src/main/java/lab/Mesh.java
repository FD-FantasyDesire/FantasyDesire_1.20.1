package lab;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import java.nio.*;
import java.util.*;
import static org.lwjgl.opengl.GL33C.*;

final class Mesh implements AutoCloseable {
    record Vertex(float x,float y,float z,float u,float v,float nx,float ny,float nz,int color) { }
    final List<Vertex> vertices=new ArrayList<>();
    private final int vao=glGenVertexArrays(),vbo=glGenBuffers();
    void quad(float[][] p,float u0,float v0,float u1,float v1,Vector3f normal,int color,Matrix4f pose){
        float[][] uv={{u0,v1},{u1,v1},{u1,v0},{u0,v0}};
        Vector3f n=pose.transformDirection(new Vector3f(normal)).normalize();
        for(int i:new int[]{0,1,2,2,3,0}){Vector3f pos=pose.transformPosition(new Vector3f(p[i]));vertices.add(new Vertex(pos.x,pos.y,pos.z,uv[i][0],uv[i][1],n.x,n.y,n.z,color));}
    }
    void box(float x,float y,float z,float w,float h,float d,int color,Matrix4f pose){box(x,y,z,w,h,d,color,pose,null);}
    void box(float x,float y,float z,float w,float h,float d,int color,Matrix4f pose,float[][] uv){
        float X=x+w,Y=y+h,Z=z+d;
        float[][][] faces={{{x,y,Z},{X,y,Z},{X,Y,Z},{x,Y,Z}},{{X,y,z},{x,y,z},{x,Y,z},{X,Y,z}},{{X,y,Z},{X,y,z},{X,Y,z},{X,Y,Z}},{{x,y,z},{x,y,Z},{x,Y,Z},{x,Y,z}},{{x,Y,Z},{X,Y,Z},{X,Y,z},{x,Y,z}},{{x,y,z},{X,y,z},{X,y,Z},{x,y,Z}}};
        Vector3f[] normals={new Vector3f(0,0,1),new Vector3f(0,0,-1),new Vector3f(1,0,0),new Vector3f(-1,0,0),new Vector3f(0,1,0),new Vector3f(0,-1,0)};
        for(int i=0;i<6;i++){float[] t=uv==null?new float[]{0,0,1,1}:uv[i];quad(faces[i],t[0],t[1],t[2],t[3],normals[i],color,pose);}
    }
    private void limb(Matrix4f pose,float x,float y,float z,float w,float h,float d,int u,int v,int tw,int th){
        // Minecraft 盒模型贴图展开；尺寸以 1/16 格计，保存每个肢体独立的 UV 岛。
        float[][] px={{u+d,v+d,u+d+w,v+d+h},{u+2*d+w,v+d,u+2*d+2*w,v+d+h},{u,v+d,u+d,v+d+h},{u+d+w,v+d,u+2*d+w,v+d+h},{u+d,v,u+d+w,v+d},{u+d+w,v,u+d+2*w,v+d}};
        for(float[] a:px){a[0]/=tw;a[2]/=tw;a[1]/=th;a[3]/=th;}
        box(x/16,y/16,z/16,w/16,h/16,d/16,0xffffffff,pose,px);
    }
    static Mesh terrain(){
        Mesh m=new Mesh();Matrix4f identity=new Matrix4f();
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)m.box(x,-1,z,1,1,1,(x+z)%2==0?0xffdadde4:0xffa7b4c5,identity);
        m.box(-3,0,-2,1,1,1,0xffffffff,identity);m.box(-3,0,-3,1,2,1,0xffffffff,identity);m.box(-2,0,-3,1,3,1,0xff90c2de,identity);
        m.box(2,0,-2,1,2.5f,1,0xffb1bdd0,identity);m.box(1,0,1,1,0.6f,1,0xffcfb497,identity);
        return m;
    }
    static Mesh entity(double time,String model){
        Mesh m=new Mesh();float swing=(float)java.lang.Math.sin(time*3)*0.6f;
        if(model.equals("creeper")){
            m.limb(new Matrix4f(),-4,16,-4,8,8,8,0,0,64,32);m.limb(new Matrix4f(),-4,6,-2,8,12,4,16,16,64,32);
            for(int side:new int[]{-1,1})for(int front:new int[]{-1,1})m.limb(new Matrix4f().translate(side*.13f,.375f,front*.18f).rotateX(swing*side*front),-2,-6,-2,4,6,4,0,16,64,32);
        }else{
            m.limb(new Matrix4f().translate(0,1.5f,0).rotateY((float)java.lang.Math.sin(time*.7)*.25f),-4,0,-4,8,8,8,0,0,64,64);
            m.limb(new Matrix4f(),-4,12,-2,8,12,4,16,16,64,64);
            m.limb(new Matrix4f().translate(-.375f,1.5f,0).rotateX(swing),-2,-12,-2,4,12,4,40,16,64,64);
            m.limb(new Matrix4f().translate(.375f,1.5f,0).rotateX(-swing),-2,-12,-2,4,12,4,40,16,64,64);
            m.limb(new Matrix4f().translate(-.125f,.75f,0).rotateX(-swing),-2,-12,-2,4,12,4,0,16,64,64);
            m.limb(new Matrix4f().translate(.125f,.75f,0).rotateX(swing),-2,-12,-2,4,12,4,0,16,64,64);
        }
        return m;
    }
    static Mesh plane(){return plane(new float[]{0,0,1,1},2,2);}
    static Mesh plane(float[] uv,float width,float height){Mesh m=new Mesh();float x=width*.5f,y=height*.5f;m.quad(new float[][]{{-x,-y,0},{x,-y,0},{x,y,0},{-x,y,0}},uv[0],uv[3],uv[2],uv[1],new Vector3f(0,0,1),0xffffffff,new Matrix4f());return m;}
    static Mesh sky(){Mesh m=new Mesh();m.box(-1,-1,-1,2,2,2,0xffffffff,new Matrix4f());return m;}
    void draw(ShaderProgram program){
        ByteBuffer data=MemoryUtil.memAlloc(vertices.size()*program.stride);
        try{
            for(Vertex v:vertices){int start=data.position();for(ShaderProgram.Attribute a:program.attributes){data.position(start+a.offset());switch(a.name()){
                case "Position"->data.putFloat(v.x).putFloat(v.y).putFloat(v.z);
                case "Color"->data.put((byte)(v.color>>16)).put((byte)(v.color>>8)).put((byte)v.color).put((byte)(v.color>>24));
                case "UV0","UV"->data.putFloat(v.u).putFloat(v.v);
                case "UV1"->data.putShort((short)0).putShort((short)10);
                case "UV2"->data.putShort((short)240).putShort((short)240);
                case "Normal"->data.put((byte)(v.nx*127)).put((byte)(v.ny*127)).put((byte)(v.nz*127)).put((byte)0);
            }}data.position(start+program.stride);}data.flip();
            glBindVertexArray(vao);glBindBuffer(GL_ARRAY_BUFFER,vbo);glBufferData(GL_ARRAY_BUFFER,data,GL_DYNAMIC_DRAW);
            for(int i=0;i<8;i++)glDisableVertexAttribArray(i);
            for(int i=0;i<program.attributes.size();i++){var a=program.attributes.get(i);glEnableVertexAttribArray(i);if(a.integer())glVertexAttribIPointer(i,a.size(),a.type(),program.stride,a.offset());else glVertexAttribPointer(i,a.size(),a.type(),a.normalized(),program.stride,a.offset());}
            glDrawArrays(GL_TRIANGLES,0,vertices.size());glBindVertexArray(0);
        }finally{MemoryUtil.memFree(data);}
    }
    @Override public void close(){glDeleteBuffers(vbo);glDeleteVertexArrays(vao);}
}
