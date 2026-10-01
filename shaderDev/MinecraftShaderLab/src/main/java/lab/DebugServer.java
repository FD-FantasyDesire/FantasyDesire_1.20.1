package lab;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.opengl.GL33C.*;

/** 独立隐藏进程的本机调试协议；所有 GL 操作只在创建上下文的线程执行。 */
final class DebugServer implements AutoCloseable {
    private record Reply(int code, String contentType, byte[] body) { }
    private record Request(long id, String method, String path, JsonObject body,
                           CompletableFuture<Reply> reply) { }
    private final Renderer renderer;
    private final HttpServer server;
    private final ExecutorService workers = Executors.newFixedThreadPool(4);
    private final BlockingQueue<Request> commands = new ArrayBlockingQueue<>(32);
    private final AtomicLong requests = new AtomicLong();
    private final String token = UUID.randomUUID().toString();
    private final Path sessionFile;
    private final Path captureRoot;
    private final JsonObject session = new JsonObject();
    private boolean running = true;
    private byte[] lastFrame;
    private int width = 800, height = 600;
    private long frameNumber;
    private String lastError = "";

    DebugServer(Renderer renderer, int port, Path sessionFile) throws IOException {
        this.renderer = renderer;
        long pid = ProcessHandle.current().pid();
        this.sessionFile = sessionFile == null
            ? renderer.root.resolve("build/debug/session-" + pid + ".json") : sessionFile;
        captureRoot = renderer.root.resolve("build/debug/captures-" + pid);
        // Windows 的短路径 TEMP 会使部分 JDK 17 的 selector 内部管道连接失败。
        if (System.getProperty("os.name").startsWith("Windows") && System.getProperty("jdk.net.unixdomain.tmpdir") == null) {
            Path sockets = Files.createDirectories(renderer.root.resolve("build/debug/sockets")).toRealPath();
            System.setProperty("jdk.net.unixdomain.tmpdir", sockets.toString());
        }
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 16);
        server.setExecutor(workers);
        server.createContext("/", this::handle);
        session.addProperty("protocol", 1);
        session.addProperty("pid", pid);
        session.addProperty("baseUrl", "http://127.0.0.1:" + server.getAddress().getPort());
        session.addProperty("token", token);
        session.addProperty("startedAt", Instant.now().toString());
        session.addProperty("sessionFile", this.sessionFile.toString());
        session.addProperty("visible", false);
        session.addProperty("state", "ready");
    }

    void run() throws IOException, InterruptedException {
        renderer.paused = true;
        server.start();
        writeSession();
        System.out.println("SHADERLAB_DEBUG_READY " + session);
        while (running) {
            glfwPollEvents();
            Request request = commands.poll(200, TimeUnit.MILLISECONDS);
            if (request == null || request.reply.isCancelled()) continue;
            try {
                request.reply.complete(dispatch(request));
            } catch (Exception e) {
                lastError = e.getMessage() == null ? e.toString() : e.getMessage();
                String kind = request.path.equals("/v1/load") || request.path.equals("/v1/reload")
                    ? "load_error" : request.path.equals("/v1/render") ? "render_error" : "invalid_request";
                int status = kind.equals("invalid_request") ? 400 : 422;
                request.reply.complete(failure(request.id, status, kind, lastError));
            }
        }
    }

    private void handle(HttpExchange exchange) throws IOException {
        Reply reply;
        long id = requests.incrementAndGet();
        try {
            if (exchange.getRequestHeaders().containsKey("Origin") ||
                !token.equals(exchange.getRequestHeaders().getFirst("X-ShaderLab-Token"))) {
                reply = failure(id, 403, "forbidden", "需要本进程的 X-ShaderLab-Token；不接受浏览器跨源请求");
            } else {
                byte[] bytes = exchange.getRequestBody().readNBytes(262145);
                if (bytes.length > 262144) {
                    reply = failure(id, 413, "too_large", "请求体最多 256 KiB");
                } else {
                    JsonObject body = bytes.length == 0 ? new JsonObject()
                        : JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
                    CompletableFuture<Reply> future = new CompletableFuture<>();
                    Request request = new Request(id, exchange.getRequestMethod(),
                        exchange.getRequestURI().getPath(), body, future);
                    if (!commands.offer(request)) {
                        reply = failure(id, 503, "busy", "调试命令队列已满");
                    } else {
                        try { reply = future.get(30, TimeUnit.SECONDS); }
                        catch (TimeoutException e) {
                            future.cancel(false);
                            reply = failure(id, 504, "timeout", "命令超时；已开始的 GPU 操作可能仍在执行，请查询状态");
                        }
                    }
                }
            }
        } catch (Exception e) {
            reply = failure(id, 400, "invalid_request", e.getMessage());
        }
        try (exchange) {
            exchange.getResponseHeaders().set("Content-Type", reply.contentType);
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.sendResponseHeaders(reply.code, reply.body.length);
            exchange.getResponseBody().write(reply.body);
        }
    }

    private Reply dispatch(Request request) throws Exception {
        String route = request.method + " " + request.path;
        JsonObject result;
        switch (route) {
            case "GET /v1/status" -> result = status();
            case "POST /v1/load" -> {
                only(request.body, "path");
                String path = request.body.get("path").getAsString();
                renderer.load(renderer.root.resolve(path).normalize());
                lastError = "";
                result = status();
            }
            case "POST /v1/reload" -> {
                only(request.body);
                if (renderer.requested == null) throw new IOException("尚未指定 shader");
                renderer.load(renderer.requested);
                lastError = "";
                result = status();
            }
            case "POST /v1/configure" -> {
                configure(request.body);
                lastError = "";
                result = status();
            }
            case "POST /v1/render" -> {
                only(request.body, "width", "height", "time", "attachments");
                int w = integer(request.body, "width", width, 16, 4096);
                int h = integer(request.body, "height", height, 16, 4096);
                if ((long)w * h > 8_388_608) throw new IOException("渲染尺寸最多 8 百万像素");
                double time = number(request.body, "time", renderer.seconds, 0, 1e7);
                boolean attachments = bool(request.body, "attachments", false);
                if (renderer.program == null) throw new IOException("尚无有效 shader，请先 load");
                renderer.seconds = time;
                renderer.measureGpu = true;
                try { renderer.render(w, h); } finally { renderer.measureGpu = false; }
                glFinish();
                width = w; height = h;
                Path folder = captureRoot.resolve(String.format(Locale.ROOT, "frame-%05d", ++frameNumber));
                Path png = folder.resolve("color.png");
                renderer.output.capture(png);
                lastFrame = Files.readAllBytes(png);
                result = status();
                result.addProperty("capture", png.toString());
                result.addProperty("customGpuMs", renderer.gpuTimersAvailable ? renderer.customGpuMs : null);
                result.addProperty("sha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(lastFrame)));
                if (attachments) {
                    renderer.blocks.capture(folder.resolve("terrain-color.png"));
                    renderer.scene.capture(folder.resolve("scene-color.png"));
                    JsonObject depths = new JsonObject();
                    depths.add("terrain", renderer.blocks.dumpDepth(folder.resolve("terrain-depth")));
                    depths.add("scene", renderer.scene.dumpDepth(folder.resolve("scene-depth")));
                    result.add("depth", depths);
                }
                lastError = "";
                result.addProperty("lastError", "");
                Files.writeString(folder.resolve("state.json"), Project.JSON.toJson(result));
            }
            case "GET /v1/frame.png" -> {
                if (lastFrame == null) return failure(request.id, 404, "no_frame", "请先 render");
                return new Reply(200, "image/png", lastFrame);
            }
            case "POST /v1/shutdown" -> {
                only(request.body);
                result = new JsonObject(); result.addProperty("stopping", true);
                running = false;
            }
            default -> { return failure(request.id, 404, "not_found", "未知接口: " + route); }
        }
        JsonObject response = new JsonObject();
        response.addProperty("ok", true); response.addProperty("requestId", request.id);
        response.add("result", result);
        return json(200, response);
    }

    private JsonObject status() {
        JsonObject data = new JsonObject();
        data.addProperty("protocol", 1);
        data.addProperty("pid", ProcessHandle.current().pid());
        data.addProperty("visible", false);
        data.addProperty("gpu", glGetString(GL_RENDERER));
        data.addProperty("opengl", glGetString(GL_VERSION));
        data.addProperty("loaded", renderer.program != null);
        data.addProperty("requested", renderer.requested == null ? null : renderer.requested.toString());
        data.addProperty("descriptor", renderer.project == null ? null : renderer.project.descriptor.toString());
        data.addProperty("generation", renderer.generation);
        data.addProperty("target", renderer.target);
        data.addProperty("volumeScale", renderer.volumeScale);
        data.addProperty("entity", renderer.entity);
        data.addProperty("time", renderer.seconds);
        data.addProperty("paused", renderer.paused);
        data.addProperty("width", width); data.addProperty("height", height);
        data.addProperty("frame", frameNumber);
        data.addProperty("status", renderer.status);
        data.addProperty("lastError", lastError);
        JsonObject camera = new JsonObject();
        camera.addProperty("yaw", renderer.yaw); camera.addProperty("pitch", renderer.pitch);
        camera.addProperty("distance", renderer.distance); camera.addProperty("fov", renderer.fov);
        data.add("camera", camera);
        JsonObject state = new JsonObject();
        state.addProperty("depthTest", renderer.depthTest); state.addProperty("depthWrite", renderer.depthWrite);
        state.addProperty("cull", renderer.cull); state.addProperty("overlay", renderer.overlay);
        data.add("state", state);
        JsonObject scene = new JsonObject();
        scene.addProperty("terrain", renderer.showTerrain); scene.addProperty("entity", renderer.showEntity);
        data.add("scene", scene);
        JsonArray uniforms = new JsonArray();
        if (renderer.program != null) for (var u : renderer.program.uniforms.values()) {
            JsonObject uniform = new JsonObject();
            uniform.addProperty("name", u.name()); uniform.addProperty("type", u.type());
            uniform.addProperty("count", u.count()); uniform.addProperty("active", u.location() >= 0);
            uniform.add("defaults", Project.JSON.toJsonTree(u.defaults()));
            JsonObject configured = Project.object(renderer.project.preview, "uniforms");
            JsonObject bindings = Project.object(renderer.project.preview, "bindings");
            if (configured.has(u.name())) uniform.add("configured", configured.get(u.name()));
            if (bindings.has(u.name())) uniform.add("binding", bindings.get(u.name()));
            if (renderer.overrides.has(u.name())) uniform.add("override", renderer.overrides.get(u.name()));
            if (u.location() >= 0) {
                if (u.type().equals("int")) {
                    int[] uploaded = new int[u.count()]; glGetUniformiv(renderer.program.id, u.location(), uploaded);
                    uniform.add("uploaded", Project.JSON.toJsonTree(uploaded));
                } else {
                    float[] uploaded = new float[u.count()]; glGetUniformfv(renderer.program.id, u.location(), uploaded);
                    uniform.add("uploaded", Project.JSON.toJsonTree(uploaded));
                }
            }
            uniforms.add(uniform);
        }
        data.add("uniforms", uniforms);
        data.add("textures", renderer.project == null ? new JsonObject() : Project.object(renderer.project.preview, "textures"));
        data.add("warnings", Project.JSON.toJsonTree(renderer.program == null ? List.of() : renderer.program.warnings));
        data.add("watched", Project.JSON.toJsonTree(renderer.watched.keySet().stream().map(Path::toString).toList()));
        return data;
    }

    private void configure(JsonObject body) throws IOException {
        only(body, "time", "camera", "target", "entity", "state", "scene", "uniforms", "clearOverrides");
        double time = number(body, "time", renderer.seconds, 0, 1e7);
        String target = Project.string(body, "target", renderer.target);
        String entity = Project.string(body, "entity", renderer.entity);
        if (!Set.of("blocks", "sky", "entity", "quad", "screen", "volume").contains(target)) throw new IOException("未知 target");
        if (!Set.of("zombie", "creeper").contains(entity)) throw new IOException("未知 entity");
        JsonObject camera = Project.object(body, "camera"), state = Project.object(body, "state"), scene = Project.object(body, "scene");
        only(camera, "yaw", "pitch", "distance", "fov");
        only(state, "depthTest", "depthWrite", "cull", "overlay");
        only(scene, "terrain", "entity");
        float yaw = (float)number(camera, "yaw", renderer.yaw, -3600, 3600);
        float pitch = (float)number(camera, "pitch", renderer.pitch, -85, 85);
        float distance = (float)number(camera, "distance", renderer.distance, 1.5, 60);
        float fov = (float)number(camera, "fov", renderer.fov, 15, 120);
        boolean depth = bool(state, "depthTest", renderer.depthTest), write = bool(state, "depthWrite", renderer.depthWrite);
        boolean cull = bool(state, "cull", renderer.cull), overlay = bool(state, "overlay", renderer.overlay);
        boolean terrain = bool(scene, "terrain", renderer.showTerrain), actor = bool(scene, "entity", renderer.showEntity);
        boolean clear = bool(body, "clearOverrides", false);
        JsonObject uniforms = Project.object(body, "uniforms");
        for (var e : uniforms.entrySet()) {
            var u = renderer.program == null ? null : renderer.program.uniforms.get(e.getKey());
            if (u == null) throw new IOException("未知 uniform: " + e.getKey());
            ShaderProgram.values(e.getValue(), u.count());
        }
        // 参数全部验证通过后才应用，避免半个请求生效。
        renderer.seconds = time; renderer.paused = true;
        renderer.target = target; renderer.entity = entity;
        renderer.yaw = yaw; renderer.pitch = pitch; renderer.distance = distance; renderer.fov = fov;
        renderer.depthTest = depth; renderer.depthWrite = write; renderer.cull = cull; renderer.overlay = overlay;
        renderer.showTerrain = terrain; renderer.showEntity = actor;
        if (clear) renderer.overrides.entrySet().clear();
        uniforms.entrySet().forEach(e -> renderer.overrides.add(e.getKey(), e.getValue()));
    }

    private static void only(JsonObject body, String... names) throws IOException {
        Set<String> allowed = Set.of(names);
        for (String key : body.keySet()) if (!allowed.contains(key)) throw new IOException("未知字段: " + key);
    }
    private static double number(JsonObject body, String key, double fallback, double min, double max) throws IOException {
        if (!body.has(key)) return fallback;
        JsonElement value = body.get(key);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw new IOException(key + " 必须是数值");
        double n = value.getAsDouble();
        if (!Double.isFinite(n) || n < min || n > max) throw new IOException(key + " 超出范围 [" + min + ", " + max + "]");
        return n;
    }
    private static int integer(JsonObject body, String key, int fallback, int min, int max) throws IOException {
        double value = number(body, key, fallback, min, max);
        if (value != Math.rint(value)) throw new IOException(key + " 必须是整数");
        return (int)value;
    }
    private static boolean bool(JsonObject body, String key, boolean fallback) throws IOException {
        if (!body.has(key)) return fallback;
        JsonElement value = body.get(key);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) throw new IOException(key + " 必须是布尔值");
        return value.getAsBoolean();
    }
    private static Reply json(int code, JsonObject body) {
        return new Reply(code, "application/json; charset=utf-8", Project.JSON.toJson(body).getBytes(StandardCharsets.UTF_8));
    }
    private static Reply failure(long id, int code, String kind, String message) {
        JsonObject response = new JsonObject(), error = new JsonObject();
        response.addProperty("ok", false); response.addProperty("requestId", id);
        error.addProperty("kind", kind); error.addProperty("message", message);
        response.add("error", error);
        return json(code, response);
    }
    private void writeSession() throws IOException {
        Files.createDirectories(sessionFile.toAbsolutePath().getParent());
        Path temp = sessionFile.resolveSibling(sessionFile.getFileName() + ".tmp");
        Files.writeString(temp, Project.JSON.toJson(session));
        try { Files.move(temp, sessionFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException e) { Files.move(temp, sessionFile, StandardCopyOption.REPLACE_EXISTING); }
    }
    @Override public void close() {
        // 允许 shutdown 的 HTTP 应答写完，再关闭监听端口与工作线程。
        server.stop(1);
        workers.shutdownNow();
        session.addProperty("state", "stopped");
        session.addProperty("stoppedAt", Instant.now().toString());
        try { writeSession(); } catch (IOException e) { System.err.println(e.getMessage()); }
    }
}
