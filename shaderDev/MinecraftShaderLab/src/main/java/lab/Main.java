package lab;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import javax.swing.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;

public final class Main {
    public static void main(String[] args)throws Exception{
        Path root=Path.of(".").toAbsolutePath(),shader=null,minecraft=null,session=null;boolean verify=false,debug=false,smoke=false;int port=0;
        for(int i=0;i<args.length;i++){switch(args[i]){case "--root"->root=Path.of(args[++i]);case "--shader"->shader=Path.of(args[++i]);case "--minecraft"->minecraft=Path.of(args[++i]);case "--verify"->verify=true;case "--debug"->debug=true;case "--smoke"->smoke=true;case "--port"->port=Integer.parseInt(args[++i]);case "--session"->session=Path.of(args[++i]);default->throw new IllegalArgumentException("未知参数: "+args[i]);}}
        if((verify?1:0)+(debug?1:0)+(smoke?1:0)>1)throw new IllegalArgumentException("verify / debug / smoke 不能同时使用");
        boolean visible=!verify&&!debug&&!smoke;
        if(minecraft==null)minecraft=findMinecraft();
        System.out.println("Minecraft Shader Lab | Java "+System.getProperty("java.version"));System.out.println("资源包: "+(minecraft==null?"无（使用生成纹理，可传 -MinecraftJar）":minecraft));
        GLFWErrorCallback error=GLFWErrorCallback.createPrint(System.err);error.set();
        if(!glfwInit())throw new IllegalStateException("无法初始化 GLFW");
        long window=0;Controls controls=null;
        try{
            glfwDefaultWindowHints();glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR,3);glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR,2);glfwWindowHint(GLFW_OPENGL_PROFILE,GLFW_OPENGL_CORE_PROFILE);glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT,GLFW_TRUE);glfwWindowHint(GLFW_VISIBLE,visible?GLFW_TRUE:GLFW_FALSE);glfwWindowHint(GLFW_FOCUSED,visible?GLFW_TRUE:GLFW_FALSE);glfwWindowHint(GLFW_DEPTH_BITS,24);glfwWindowHint(GLFW_SAMPLES,0);
            window=glfwCreateWindow(1080,760,"Minecraft Shader Lab · OpenGL 3.2 · 拖动旋转 / 滚轮缩放",0,0);if(window==0)throw new IllegalStateException("GPU 不支持桌面 OpenGL 3.2 Core");
            glfwSetWindowPos(window,40,60);glfwMakeContextCurrent(window);GL.createCapabilities();glfwSwapInterval(1);
            System.out.println("GPU: "+glGetString(GL_RENDERER)+" | OpenGL "+glGetString(GL_VERSION));
            try(Renderer renderer=new Renderer(root,minecraft)){
                if(verify){Verification.run(renderer);return;}
                if(debug){
                    try{renderer.load(shader==null?root.resolve("examples/surface.preview.json"):shader);}catch(Exception e){System.err.println(e.getMessage());}
                    try(DebugServer server=new DebugServer(renderer,port,session)){server.run();}
                    return;
                }
                ConcurrentLinkedQueue<Controls.Action> queue=new ConcurrentLinkedQueue<>();
                final Controls[] holder=new Controls[1];SwingUtilities.invokeAndWait(()->{try{UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}catch(Exception ignored){}holder[0]=new Controls(renderer,queue,visible);});controls=holder[0];
                try{renderer.load(shader==null?root.resolve("examples/surface.preview.json"):shader);}catch(Exception e){if(smoke)throw e;System.err.println(e.getMessage());}controls.refresh();
                if(smoke){SmokeCheck.run(renderer,controls,window);return;}
                double[] lastMouse={0,0};boolean[] dragging={false};
                glfwSetCursorPosCallback(window,(win,x,y)->{if(dragging[0]){renderer.yaw+=(float)(x-lastMouse[0])*.3f;renderer.pitch=java.lang.Math.max(-85,java.lang.Math.min(85,renderer.pitch+(float)(y-lastMouse[1])*.3f));}lastMouse[0]=x;lastMouse[1]=y;});
                glfwSetMouseButtonCallback(window,(win,button,action,mods)->{if(button==GLFW_MOUSE_BUTTON_LEFT)dragging[0]=action==GLFW_PRESS;});
                glfwSetScrollCallback(window,(win,x,y)->renderer.distance=java.lang.Math.max(1.5f,java.lang.Math.min(60,renderer.distance*(float)java.lang.Math.exp(-y*.1))));
                glfwSetKeyCallback(window,(win,key,scan,action,mods)->{if(action!=GLFW_PRESS)return;if(key==GLFW_KEY_SPACE)renderer.paused=!renderer.paused;if(key==GLFW_KEY_R)queue.add(()->renderer.load(renderer.requested));if(key==GLFW_KEY_ESCAPE)glfwSetWindowShouldClose(win,true);});
                double previous=glfwGetTime(),watchTime=0,statsTime=0;int frames=0;String lastError="";
                while(!glfwWindowShouldClose(window)&&!controls.closed){
                    long frameStart=System.nanoTime();
                    glfwPollEvents();double now=glfwGetTime();double dt=java.lang.Math.min(now-previous,.1);previous=now;if(!renderer.paused)renderer.seconds+=dt*renderer.speed;
                    for(Controls.Action action;(action=queue.poll())!=null;){try{action.run();lastError="";}catch(Exception e){renderer.status=e.getMessage();controls.refresh();}}
                    if(now-watchTime>.4){watchTime=now;if(renderer.requested!=null&&renderer.changed()){try{renderer.load(renderer.requested);lastError="";}catch(Exception ignored){}controls.refresh();}}
                    try(MemoryStack stack=MemoryStack.stackPush()){
                        IntBuffer w=stack.mallocInt(1),h=stack.mallocInt(1);glfwGetFramebufferSize(window,w,h);
                        if(w.get(0)>0&&h.get(0)>0){try{renderer.render(w.get(0),h.get(0));lastError="";}catch(Exception e){if(!e.toString().equals(lastError)){lastError=e.toString();renderer.status="渲染诊断: "+e.getMessage();controls.refresh();System.err.println(renderer.status);}}if(renderer.output!=null)renderer.output.present(w.get(0),h.get(0));glfwSwapBuffers(window);}else glfwWaitEventsTimeout(.05);
                    }
                    frames++;if(now-statsTime>.5){controls.tick(renderer.seconds,frames/(now-statsTime));frames=0;statsTime=now;}
                    // 驱动可能忽略 swap interval，仍限制到 60 FPS，避免编辑时占满显卡。
                    long remaining=16_666_667L-(System.nanoTime()-frameStart);if(remaining>0)java.util.concurrent.TimeUnit.NANOSECONDS.sleep(remaining);
                }
            }
        }finally{if(controls!=null)controls.dispose();if(window!=0){Callbacks.glfwFreeCallbacks(window);glfwDestroyWindow(window);}glfwTerminate();error.free();}
    }
    static Path findMinecraft(){
        String user=System.getProperty("user.home");String gradle=System.getenv("GRADLE_USER_HOME");if(gradle==null)gradle=user+"/.gradle";
        for(Path p:List.of(Path.of(gradle,"caches/forge_gradle/minecraft_repo/versions/1.20.1/client.jar"),Path.of(user,"AppData/Roaming/.minecraft/versions/1.20.1/1.20.1.jar")))if(Files.isRegularFile(p))return p;
        return null;
    }
}
