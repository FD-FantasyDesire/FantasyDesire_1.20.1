package lab;

import org.lwjgl.system.MemoryUtil;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import static org.lwjgl.opengl.GL33C.*;
import com.google.gson.JsonObject;

/** 使用独立 RGBA8/DEPTH24 目标；采样永远不读取当前写入的附件。 */
final class Target implements AutoCloseable {
    final int fbo,color,depth,width,height;
    Target(int w,int h){width=w;height=h;fbo=glGenFramebuffers();glBindFramebuffer(GL_FRAMEBUFFER,fbo);color=texture(GL_RGBA8,GL_RGBA,GL_UNSIGNED_BYTE);depth=texture(GL_DEPTH_COMPONENT24,GL_DEPTH_COMPONENT,GL_UNSIGNED_INT);glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,color,0);glFramebufferTexture2D(GL_FRAMEBUFFER,GL_DEPTH_ATTACHMENT,GL_TEXTURE_2D,depth,0);if(glCheckFramebufferStatus(GL_FRAMEBUFFER)!=GL_FRAMEBUFFER_COMPLETE)throw new IllegalStateException("离屏 framebuffer 不完整");glBindFramebuffer(GL_FRAMEBUFFER,0);}
    private int texture(int internal,int format,int type){int id=glGenTextures();glBindTexture(GL_TEXTURE_2D,id);glTexImage2D(GL_TEXTURE_2D,0,internal,width,height,0,format,type,(ByteBuffer)null);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);return id;}
    void bind(){glBindFramebuffer(GL_FRAMEBUFFER,fbo);glViewport(0,0,width,height);}
    void clear(){bind();glDepthMask(true);glClearColor(.035f,.05f,.08f,1);glClearDepth(1);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);}
    void clearTransparent(){bind();glDepthMask(true);glClearColor(0,0,0,0);glClearDepth(1);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);}
    void copyTo(Target target){glBindFramebuffer(GL_READ_FRAMEBUFFER,fbo);glBindFramebuffer(GL_DRAW_FRAMEBUFFER,target.fbo);glBlitFramebuffer(0,0,width,height,0,0,target.width,target.height,GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT,GL_NEAREST);target.bind();}
    void present(int w,int h){glBindFramebuffer(GL_READ_FRAMEBUFFER,fbo);glBindFramebuffer(GL_DRAW_FRAMEBUFFER,0);glBlitFramebuffer(0,0,width,height,0,0,w,h,GL_COLOR_BUFFER_BIT,GL_NEAREST);glBindFramebuffer(GL_FRAMEBUFFER,0);}
    void capture(Path path)throws IOException{
        bind();ByteBuffer pixels=MemoryUtil.memAlloc(width*height*4);
        try{glReadPixels(0,0,width,height,GL_RGBA,GL_UNSIGNED_BYTE,pixels);BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);for(int y=0;y<height;y++)for(int x=0;x<width;x++){int i=(y*width+x)*4;int c=(pixels.get(i+3)&255)<<24|(pixels.get(i)&255)<<16|(pixels.get(i+1)&255)<<8|(pixels.get(i+2)&255);image.setRGB(x,height-y-1,c);}Files.createDirectories(path.toAbsolutePath().getParent());ImageIO.write(image,"png",path.toFile());}finally{MemoryUtil.memFree(pixels);}
    }
    JsonObject dumpDepth(Path prefix)throws IOException{
        bind();float[] values=new float[width*height];glReadPixels(0,0,width,height,GL_DEPTH_COMPONENT,GL_FLOAT,values);
        BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_USHORT_GRAY);
        ByteBuffer raw=ByteBuffer.allocate(values.length*4).order(ByteOrder.LITTLE_ENDIAN);
        float min=1,max=0;int foreground=0;
        for(int y=0;y<height;y++)for(int x=0;x<width;x++){
            float d=values[y*width+x];raw.putFloat(d);min=Math.min(min,d);max=Math.max(max,d);if(d<1)foreground++;
            image.getRaster().setSample(x,height-y-1,0,Math.round(d*65535));
        }
        Path png=Path.of(prefix+".png"),binary=Path.of(prefix+".f32");ImageIO.write(image,"png",png.toFile());Files.write(binary,raw.array());
        JsonObject info=new JsonObject();info.addProperty("png",png.toString());info.addProperty("raw",binary.toString());info.addProperty("min",min);info.addProperty("max",max);info.addProperty("foregroundPixels",foreground);info.addProperty("width",width);info.addProperty("height",height);info.addProperty("format","OpenGL depth [0,1]; raw=float32 little-endian, bottom-left origin; PNG=uint16, top-left origin");return info;
    }
    @Override public void close(){glDeleteFramebuffers(fbo);glDeleteTextures(color);glDeleteTextures(depth);}
}
