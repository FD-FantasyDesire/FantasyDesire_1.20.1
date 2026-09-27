package lab;

import com.google.gson.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Swing 仅编辑控制值；所有 OpenGL 和场景修改都排入渲染线程。 */
final class Controls {
    interface Action { void run()throws Exception; }
    final ConcurrentLinkedQueue<Action> actions;
    final Renderer renderer;
    final JFrame frame=new JFrame("Minecraft Shader Lab · 控制台");
    final JTextField path=new JTextField();
    final JTextArea log=new JTextArea(7,30);
    final JLabel clock=new JLabel("0.000 秒");
    final JComboBox<String> target=new JComboBox<>(new String[]{"blocks","sky","entity","quad","screen"});
    final JComboBox<String> entity=new JComboBox<>(new String[]{"zombie","creeper"});
    final JCheckBox depth=new JCheckBox("深度测试"),write=new JCheckBox("写入深度"),cull=new JCheckBox("背面剔除"),overlay=new JCheckBox("叠加在原材质上");
    final JCheckBox terrain=new JCheckBox("地面 / 台阶",true),actorVisible=new JCheckBox("实体遮挡物",true);
    final DefaultTableModel uniformModel=new DefaultTableModel(new String[]{"Uniform","类型","值 / 绑定"},0){@Override public boolean isCellEditable(int r,int c){return c==2;}};
    boolean updating=false;
    volatile boolean closed=false;
    Controls(Renderer renderer,ConcurrentLinkedQueue<Action> actions){this(renderer,actions,true);}
    Controls(Renderer renderer,ConcurrentLinkedQueue<Action> actions,boolean visible){
        this.renderer=renderer;this.actions=actions;
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);frame.addWindowListener(new WindowAdapter(){@Override public void windowClosed(WindowEvent e){closed=true;}});
        JPanel body=new JPanel();body.setLayout(new BoxLayout(body,BoxLayout.Y_AXIS));body.setBorder(new EmptyBorder(14,14,14,14));
        JLabel title=new JLabel("MINECRAFT SHADER LAB");title.setFont(title.getFont().deriveFont(Font.BOLD,21));body.add(title);body.add(new JLabel("桌面 GLSL · 保存即预览 · Minecraft 1.20.1 core"));body.add(Box.createVerticalStrut(12));
        path.setMaximumSize(new Dimension(Integer.MAX_VALUE,28));body.add(path);
        body.add(row(button("打开文件",()->chooseOpen()),button("加载路径",()->queue(()->renderer.load(Path.of(path.getText())))),button("重新加载",()->queue(()->renderer.load(renderer.requested)))));
        JComboBox<String> presets=new JComboBox<>(new String[]{"surface","entity","sky","depth","fantasy-rift","fantasy-void","fantasy-frost"});body.add(row(new JLabel("示例"),presets,button("打开示例",()->{String name=(String)presets.getSelectedItem();queue(()->renderer.load(renderer.root.resolve("examples/"+name+".preview.json")));})));
        body.add(label("试验对象"));body.add(row(target,entity));
        target.addActionListener(e->{if(!updating){String value=(String)target.getSelectedItem();queue(()->{renderer.target=value;renderer.depthTest=!value.equals("sky")&&!value.equals("screen");renderer.depthWrite=value.equals("blocks")||value.equals("entity");renderer.cull=renderer.depthWrite;});}});
        entity.addActionListener(e->{if(!updating){String value=(String)entity.getSelectedItem();queue(()->renderer.entity=value);}});
        body.add(row(depth,write,cull));body.add(row(overlay));
        depth.addActionListener(e->{if(!updating){boolean value=depth.isSelected();queue(()->renderer.depthTest=value);}});
        write.addActionListener(e->{if(!updating){boolean value=write.isSelected();queue(()->renderer.depthWrite=value);}});
        cull.addActionListener(e->{if(!updating){boolean value=cull.isSelected();queue(()->renderer.cull=value);}});
        overlay.addActionListener(e->{if(!updating){boolean value=overlay.isSelected();queue(()->renderer.overlay=value);}});
        terrain.addActionListener(e->{if(!updating){boolean v=terrain.isSelected();queue(()->renderer.showTerrain=v);}});actorVisible.addActionListener(e->{if(!updating){boolean v=actorVisible.isSelected();queue(()->renderer.showEntity=v);}});body.add(row(terrain,actorVisible));
        body.add(label("时间与相机"));
        body.add(row(button("暂停 / 播放",()->queue(()->renderer.paused=!renderer.paused)),button("步进 1/20 s",()->queue(()->{renderer.paused=true;renderer.seconds+=.05;})),button("归零",()->queue(()->renderer.seconds=0))));
        JSpinner time=new JSpinner(new SpinnerNumberModel(0.0,0.0,1e7,.05));JSpinner speed=new JSpinner(new SpinnerNumberModel(1.0,0.0,8.0,.25));
        body.add(row(clock,time,button("定位",()->{double v=((Number)time.getValue()).doubleValue();queue(()->{renderer.seconds=v;renderer.paused=true;});})));
        speed.addChangeListener(e->{double v=((Number)speed.getValue()).doubleValue();queue(()->renderer.speed=v);});body.add(row(new JLabel("播放速度"),speed,button("重置相机",()->queue(()->{renderer.yaw=35;renderer.pitch=25;renderer.distance=9;}))));
        body.add(new JLabel("画面左键拖动：环绕；滚轮：缩放；空格：暂停"));
        body.add(label("Uniform 参数（双击数值编辑，向量使用 [1,2,3]）"));
        JTable table=new JTable(uniformModel);table.setRowHeight(24);table.getColumnModel().getColumn(0).setPreferredWidth(150);JScrollPane tableScroll=new JScrollPane(table);tableScroll.setPreferredSize(new Dimension(390,210));body.add(tableScroll);
        uniformModel.addTableModelListener(e->{if(!updating&&e.getType()==javax.swing.event.TableModelEvent.UPDATE&&e.getColumn()==2){int row=e.getFirstRow();String name=uniformModel.getValueAt(row,0).toString(),value=uniformModel.getValueAt(row,2).toString();queue(()->renderer.override(name,value));}});
        body.add(row(button("清除参数覆盖",()->queue(()->renderer.overrides.entrySet().clear())),button("保存预览配置",()->chooseSave()),button("截图 PNG",()->queue(()->{Path p=renderer.root.resolve("captures/preview-"+System.currentTimeMillis()+".png");renderer.output.capture(p);renderer.status="截图已保存: "+p;}))));
        body.add(label("编译 / 资源诊断"));log.setEditable(false);log.setLineWrap(true);log.setWrapStyleWord(true);body.add(new JScrollPane(log));
        for(Component component:body.getComponents())if(component instanceof JComponent c)c.setAlignmentX(Component.LEFT_ALIGNMENT);
        frame.add(new JScrollPane(body));frame.setSize(510,920);frame.setLocation(1140,60);
        if(visible)frame.setVisible(true);else{frame.addNotify();frame.validate();}
    }
    private static JLabel label(String text){JLabel l=new JLabel(text);l.setBorder(new EmptyBorder(12,0,6,0));l.setFont(l.getFont().deriveFont(Font.BOLD));return l;}
    private static JPanel row(Component...components){JPanel p=new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));for(Component c:components)p.add(c);p.setMaximumSize(new Dimension(Integer.MAX_VALUE,42));return p;}
    private static JButton button(String label,Runnable action){JButton b=new JButton(label);b.addActionListener(e->action.run());return b;}
    private void queue(Action action){actions.add(()->{action.run();refresh();});}
    private void chooseOpen(){JFileChooser chooser=new JFileChooser(renderer.root.resolve("examples").toFile());chooser.setDialogTitle("选择 .json / .fsh / .vsh / .preview.json");if(chooser.showOpenDialog(frame)==JFileChooser.APPROVE_OPTION){Path p=chooser.getSelectedFile().toPath();path.setText(p.toString());queue(()->renderer.load(p));}}
    private void chooseSave(){JFileChooser chooser=new JFileChooser(renderer.root.resolve("examples").toFile());chooser.setSelectedFile(new File("custom.preview.json"));if(chooser.showSaveDialog(frame)==JFileChooser.APPROVE_OPTION){Path out=chooser.getSelectedFile().toPath().toAbsolutePath();queue(()->save(out));}}
    private void save(Path out)throws IOException{
        if(renderer.project==null)throw new IOException("请先加载 shader");
        JsonObject json=renderer.project.preview.deepCopy();
        json.addProperty("shader",out.getParent().relativize(renderer.project.descriptor).toString().replace('\\','/'));json.addProperty("target",renderer.target);json.addProperty("entity",renderer.entity);json.addProperty("overlay",renderer.overlay);
        JsonObject uniforms=Project.object(json,"uniforms"),bindings=Project.object(json,"bindings");renderer.overrides.entrySet().forEach(e->{uniforms.add(e.getKey(),e.getValue());bindings.remove(e.getKey());});json.add("uniforms",uniforms);json.add("bindings",bindings);
        JsonObject state=new JsonObject();state.addProperty("depthTest",renderer.depthTest);state.addProperty("depthWrite",renderer.depthWrite);state.addProperty("cull",renderer.cull);json.add("state",state);
        JsonObject camera=new JsonObject();camera.addProperty("yaw",renderer.yaw);camera.addProperty("pitch",renderer.pitch);camera.addProperty("distance",renderer.distance);camera.addProperty("fov",renderer.fov);json.add("camera",camera);
        JsonObject scene=new JsonObject();scene.addProperty("terrain",renderer.showTerrain);scene.addProperty("entity",renderer.showEntity);json.add("scene",scene);
        // 配置另存到别处时同步重定位纹理与资源目录。
        if(json.has("resourceRoots")){JsonArray roots=new JsonArray();for(JsonElement r:json.getAsJsonArray("resourceRoots"))roots.add(out.getParent().relativize(renderer.project.input.getParent().resolve(r.getAsString()).normalize()).toString().replace('\\','/'));json.add("resourceRoots",roots);}
        JsonObject textures=Project.object(json,"textures");for(var e:textures.entrySet()){String s=e.getValue().getAsString();if(!s.startsWith("@")&&!s.contains(":"))e.setValue(new JsonPrimitive(out.getParent().relativize(renderer.project.input.getParent().resolve(s).normalize()).toString().replace('\\','/')));}
        Files.writeString(out,Project.JSON.toJson(json));renderer.status="配置已保存: "+out;
    }
    void refresh(){
        String status=renderer.status;String file=renderer.requested==null?"":renderer.requested.toString();String selected=renderer.target,actor=renderer.entity;
        boolean dt=renderer.depthTest,dw=renderer.depthWrite,cl=renderer.cull,ov=renderer.overlay,st=renderer.showTerrain,se=renderer.showEntity;
        java.util.List<Object[]> rows=new java.util.ArrayList<>();
        if(renderer.program!=null){
            for(var u:renderer.program.uniforms.values()){
                JsonObject values=Project.object(renderer.project.preview,"uniforms"),bindings=Project.object(renderer.project.preview,"bindings");
                Object v=renderer.overrides.has(u.name())?renderer.overrides.get(u.name()):bindings.has(u.name())?"← "+bindings.get(u.name()):values.has(u.name())?values.get(u.name()):Project.JSON.toJson(u.defaults());
                rows.add(new Object[]{u.name(),u.type()+"["+u.count()+"]",v.toString().replace("\n", "")});
            }
            if(!renderer.program.warnings.isEmpty())status+="\n"+String.join("\n",renderer.program.warnings);
        }
        String text=status;
        SwingUtilities.invokeLater(()->{updating=true;path.setText(file);target.setSelectedItem(selected);entity.setSelectedItem(actor);depth.setSelected(dt);write.setSelected(dw);cull.setSelected(cl);overlay.setSelected(ov);terrain.setSelected(st);actorVisible.setSelected(se);log.setText(text);uniformModel.setRowCount(0);rows.forEach(uniformModel::addRow);updating=false;});
    }
    void tick(double seconds,double fps){SwingUtilities.invokeLater(()->clock.setText(String.format(java.util.Locale.ROOT,"%.2f s · %.0f FPS",seconds,fps)));}
    void dispose(){SwingUtilities.invokeLater(frame::dispose);}
}
