package lab;

import com.google.gson.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import static org.lwjgl.glfw.GLFW.*;

/** 复用正常启动路径，在不可见窗口中验收 GUI 构建与首次绘制；无需桌面自动化。 */
final class SmokeCheck {
    static void run(Renderer renderer, Controls controls, long window) throws Exception {
        Path output = renderer.root.resolve("build/smoke");
        Files.createDirectories(output);
        renderer.paused = true;
        renderer.seconds = 2.75;
        renderer.render(800, 600);
        renderer.output.capture(output.resolve("viewport.png"));
        JsonObject report = new JsonObject();
        report.addProperty("result", "PASS");
        report.addProperty("glfwVisible", glfwGetWindowAttrib(window, GLFW_VISIBLE) == GLFW_TRUE);
        if (glfwGetWindowAttrib(window, GLFW_VISIBLE) != GLFW_FALSE) throw new IllegalStateException("smoke 不应创建可见窗口");
        SwingUtilities.invokeAndWait(() -> {
            if (controls.frame.isVisible()) throw new IllegalStateException("smoke 控制台不应可见");
            if (controls.uniformModel.getRowCount() != renderer.program.uniforms.size()) throw new IllegalStateException("GUI 未同步 uniform");
            report.addProperty("swingVisible", controls.frame.isVisible());
            report.addProperty("uniformRows", controls.uniformModel.getRowCount());
            report.addProperty("loadedPath", controls.path.getText());
            layout(controls.frame.getRootPane());
            report.add("ui", describe(controls.frame.getRootPane()));
            JRootPane pane = controls.frame.getRootPane();
            BufferedImage image = new BufferedImage(pane.getWidth(), pane.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            try {
                pane.printAll(graphics);
                ImageIO.write(image, "png", output.resolve("controls.png").toFile());
            } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
            finally { graphics.dispose(); }
        });
        Files.writeString(output.resolve("report.json"), Project.JSON.toJson(report));
        System.out.println("SMOKE PASS: hidden GUI + OpenGL frame -> " + output);
    }
    private static void layout(Component component) {
        if(component instanceof Container container){container.doLayout();for(Component child:container.getComponents())layout(child);}
    }
    private static JsonObject describe(Component component) {
        JsonObject json = new JsonObject();
        json.addProperty("type", component.getClass().getSimpleName());
        json.addProperty("x", component.getX()); json.addProperty("y", component.getY());
        json.addProperty("width", component.getWidth()); json.addProperty("height", component.getHeight());
        if (component instanceof AbstractButton button) json.addProperty("text", button.getText());
        else if (component instanceof JLabel label) json.addProperty("text", label.getText());
        else if (component instanceof JTextComponent text) json.addProperty("text", text.getText());
        if (component instanceof Container container) {
            JsonArray children = new JsonArray();
            for (Component child : container.getComponents()) children.add(describe(child));
            json.add("children", children);
        }
        return json;
    }
}
