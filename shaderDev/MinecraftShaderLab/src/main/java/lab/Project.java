package lab;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;

/** 正式资源与实验配置分开；不转换 GLSL 版本、不改写 shader 接口。 */
final class Project implements AutoCloseable {
    static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    final Path input, descriptor;
    final JsonObject shader, preview;
    final List<Path> roots = new ArrayList<>();
    final Map<Path, String> watched = new LinkedHashMap<>();
    final List<ZipFile> packs = new ArrayList<>();
    final Map<Integer, String> sourceMap = new LinkedHashMap<>();
    String target;
    final boolean loose;
    private int maxVersion;

    Project(Path input, Path minecraftJar) throws IOException {
        this.input = input.toAbsolutePath().normalize();
        preview = input.toString().endsWith(".preview.json") ? read(this.input) : new JsonObject();
        descriptor = resolveDescriptor(preview.has("shader") ? this.input.getParent().resolve(preview.get("shader").getAsString()).normalize() : this.input);
        shader = read(descriptor);
        if (shader.has("passes")) throw new IOException("当前加载器支持 core shader JSON。后处理链 JSON 的 passes/targets 不在此入口支持范围内。");
        loose = !descriptor.toString().replace('\\','/').contains("/assets/");
        if (preview.has("resourceRoots")) for (JsonElement e : preview.getAsJsonArray("resourceRoots")) roots.add(this.input.getParent().resolve(e.getAsString()).normalize());
        for (Path p = descriptor.getParent(); p != null; p = p.getParent()) {
            if (p.getFileName() != null && p.getFileName().toString().equals("assets")) { roots.add(p.getParent()); break; }
        }
        target = string(preview,"target", shader.getAsJsonArray("attributes") != null && shader.getAsJsonArray("attributes").toString().contains("UV1") ? "entity" : "quad");
        if (!Set.of("blocks","sky","entity","quad","screen").contains(target)) throw new IOException("未知 target: " + target);
        if (minecraftJar != null) packs.add(new ZipFile(minecraftJar.toFile()));
        watch(this.input); watch(descriptor);
    }
    static JsonObject read(Path p) throws IOException {
        try (var reader = Files.newBufferedReader(p)) { return JsonParser.parseReader(reader).getAsJsonObject(); }
        catch (RuntimeException e) { throw new IOException("JSON 无效: " + p + "\n" + e.getMessage(),e); }
    }
    private static Path resolveDescriptor(Path path) throws IOException {
        if (path.toString().endsWith(".json")) return path;
        String file = path.getFileName().toString();
        if (!file.endsWith(".vsh") && !file.endsWith(".fsh")) throw new IOException("请选择 .json / .vsh / .fsh / .preview.json");
        String key = file.endsWith(".vsh") ? "vertex" : "fragment";
        List<Path> candidates = new ArrayList<>();
        try (var files = Files.list(path.getParent())) {
            for (Path p : files.filter(p -> p.toString().endsWith(".json") && !p.toString().endsWith(".preview.json")).sorted().toList()) {
                JsonObject j = read(p);
                if (j.has(key)) {
                    String ref = j.get(key).getAsString();
                    if ((ref.substring(Math.max(ref.lastIndexOf(':'),ref.lastIndexOf('/'))+1) + file.substring(file.length()-4)).equals(file)) candidates.add(p);
                }
            }
        }
        if (candidates.size() != 1) throw new IOException("需要唯一的配套 JSON；找到 " + candidates.size() + " 个，请直接打开对应 JSON。");
        return candidates.get(0);
    }
    record Resource(String id, Path file, byte[] bytes) { }
    Resource resource(String name, Path relative) throws IOException {
        if (relative != null) {
            Path p = relative.resolve(name).normalize(); watch(p);
            if (Files.isRegularFile(p)) return new Resource(p.toString(),p,Files.readAllBytes(p));
        }
        String[] id = id(name);
        String asset = "assets/" + id[0] + "/" + id[1];
        for (Path root : roots) {
            Path p = root.resolve(asset).normalize();
            if (!p.startsWith(root.toAbsolutePath().normalize())) throw new IOException("资源路径越界: " + name);
            watch(p);
            if (Files.isRegularFile(p)) return new Resource(name,p,Files.readAllBytes(p));
        }
        if (loose && id[1].startsWith("shaders/")) {
            Path p = descriptor.getParent().resolve(id[1].replaceFirst("^shaders/(core|include)/", "")).normalize(); watch(p);
            if (Files.isRegularFile(p)) return new Resource(name,p,Files.readAllBytes(p));
        }
        for (ZipFile pack : packs) {
            ZipEntry e = pack.getEntry(asset);
            if (e != null) try (var stream = pack.getInputStream(e)) { return new Resource(name,null,stream.readAllBytes()); }
        }
        throw new FileNotFoundException("找不到资源: " + name + "（可配置 resourceRoots 或 -MinecraftJar）");
    }
    Resource stage(String key, String extension) throws IOException {
        if (!shader.has(key)) throw new IOException("JSON 缺少 " + key);
        String[] id = id(shader.get(key).getAsString());
        if (loose) {
            Path p = descriptor.getParent().resolve(Path.of(id[1]+extension).getFileName()); watch(p);
            if (Files.isRegularFile(p)) return new Resource(id[0]+":shaders/core/"+id[1]+extension,p,Files.readAllBytes(p));
        }
        return resource(id[0]+":shaders/core/"+id[1]+extension,null);
    }
    String preprocess(Resource resource) throws IOException {
        sourceMap.clear(); maxVersion=150;
        String expanded = expand(resource,new HashSet<>(),0);
        Matcher version = Pattern.compile("(?m)^\\s*#version\\s+(\\d+)([^\\r\\n]*)").matcher(expanded);
        if (!version.find()) throw new IOException("缺少桌面 GLSL #version: " + resource.id);
        if (version.group(2).contains("es")) throw new IOException("这是 GLSL ES；外置预览器直接使用 Minecraft 桌面 GLSL，不自动转换。");
        return version.replaceFirst("#version " + maxVersion);
    }
    private String expand(Resource resource, Set<String> imported, int depth) throws IOException {
        if (depth > 64) throw new IOException("include 嵌套超过 64 层");
        int sourceId=sourceMap.size(); sourceMap.put(sourceId,resource.id);
        String text=new String(resource.bytes,java.nio.charset.StandardCharsets.UTF_8).replace("\uFEFF", "");
        StringBuilder out=new StringBuilder(); boolean blockComment=false;
        String[] lines=text.split("\\R",-1);
        for (int line=0;line<lines.length;line++) {
            String original=lines[line]; StringBuilder visible=new StringBuilder();
            for (int i=0;i<original.length();i++) {
                if (blockComment) { if (i+1<original.length() && original.startsWith("*/",i)) { blockComment=false; i++; } }
                else if (original.startsWith("//",i)) break;
                else if (original.startsWith("/*",i)) { blockComment=true; visible.append(' '); i++; }
                else visible.append(original.charAt(i));
            }
            String directive=visible.toString().trim();
            Matcher ver=Pattern.compile("#\\s*version\\s+(\\d+).*").matcher(directive);
            Matcher inc=Pattern.compile("#\\s*moj_import\\s*(?:\"([^\"]+)\"|<([^>]+)>)\\s*").matcher(directive);
            if (ver.matches()) {
                maxVersion=Math.max(maxVersion,Integer.parseInt(ver.group(1)));
                out.append(depth==0?original:"// imported version").append('\n');
            } else if (inc.matches()) {
                boolean relative=inc.group(1)!=null;
                String requested=relative?inc.group(1):inc.group(2);
                String[] ref=id(requested);
                String[] current=id(resource.id);
                String base=current[1].substring(0,Math.max(0,current[1].lastIndexOf('/')+1));
                String path=(relative?base:"shaders/include/")+ref[1];
                path=Path.of(path).normalize().toString().replace('\\','/');
                String resolved=ref[0]+":"+path;
                Resource child=resource(resolved,null);
                if (imported.add(child.id)) {
                    int next=sourceMap.size();
                    out.append("#line 1 ").append(next).append('\n').append(expand(child,imported,depth+1));
                }
                out.append("#line ").append(line+2).append(' ').append(sourceId).append('\n');
            } else out.append(original).append('\n');
        }
        return out.toString();
    }
    static String[] id(String name) throws IOException {
        String[] result=name.contains(":")?name.split(":",2):new String[]{"minecraft",name};
        if (!result[0].matches("[a-z0-9_.-]+") || !result[1].matches("[a-z0-9_./-]+") || result[1].contains("..") || result[1].startsWith("/")) throw new IOException("资源位置无效: "+name);
        return result;
    }
    void watch(Path p) { watched.put(p,stamp(p)); }
    static String stamp(Path p) { try { return Files.getLastModifiedTime(p)+":"+Files.size(p); } catch(IOException e){return "missing";} }
    boolean changed(){return watched.entrySet().stream().anyMatch(e->!e.getValue().equals(stamp(e.getKey())));}
    static JsonObject object(JsonObject j,String key){return j.has(key)?j.getAsJsonObject(key):new JsonObject();}
    static String string(JsonObject j,String key,String fallback){return j.has(key)?j.get(key).getAsString():fallback;}
    static boolean bool(JsonObject j,String key,boolean fallback){return j.has(key)?j.get(key).getAsBoolean():fallback;}
    @Override public void close(){for(ZipFile z:packs)try{z.close();}catch(IOException ignored){}}
}
