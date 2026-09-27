package lab;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.*;
import java.util.*;
import org.lwjgl.system.MemoryUtil;
import static org.lwjgl.opengl.GL33C.*;

final class Textures implements AutoCloseable {
    final Map<String,Integer> textures=new HashMap<>();
    Textures(){
        BufferedImage checker=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);
        BufferedImage white=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);
        BufferedImage overlay=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<16;y++)for(int x=0;x<16;x++){
            checker.setRGB(x,y,((x/4+y/4)%2==0)?0xffb9cedc:0xff536a7e);white.setRGB(x,y,0xffffffff);
            int alpha=y<8?0xb2:(int)((1-x/15f*.75f)*255);overlay.setRGB(x,y,y<8?(alpha<<24)|0xff0000:(alpha<<24)|0xffffff);
        }
        textures.put("@checker",upload(checker,false));textures.put("@white",upload(white,false));textures.put("@lightmap",upload(white,false));textures.put("@overlay",upload(overlay,false));
    }
    int get(Project project,String key)throws IOException{
        Integer cached=textures.get(key);if(cached!=null)return cached;
        Project.Resource resource=project.resource(key,key.contains(":")?null:project.input.getParent());
        BufferedImage image=ImageIO.read(new ByteArrayInputStream(resource.bytes()));
        if(image==null)throw new IOException("无法解码纹理: "+key);
        int id=upload(image,true);textures.put(key,id);return id;
    }
    int optional(Project project,String key){try{return get(project,key);}catch(IOException e){int fallback=textures.get("@checker");textures.put(key,fallback);return fallback;}}
    static int upload(BufferedImage image,boolean mipmap){
        int id=glGenTextures();glBindTexture(GL_TEXTURE_2D,id);
        ByteBuffer data=MemoryUtil.memAlloc(image.getWidth()*image.getHeight()*4);
        try{for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){int c=image.getRGB(x,y);data.put((byte)(c>>16)).put((byte)(c>>8)).put((byte)c).put((byte)(c>>24));}data.flip();glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA8,image.getWidth(),image.getHeight(),0,GL_RGBA,GL_UNSIGNED_BYTE,data);}finally{MemoryUtil.memFree(data);}
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,mipmap?GL_NEAREST_MIPMAP_LINEAR:GL_NEAREST);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_REPEAT);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_REPEAT);if(mipmap)glGenerateMipmap(GL_TEXTURE_2D);return id;
    }
    @Override public void close(){new HashSet<>(textures.values()).forEach(v->glDeleteTextures(v));textures.clear();}
}
