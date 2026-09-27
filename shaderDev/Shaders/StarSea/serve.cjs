// 仅向回环地址提供本作品和明确指定的 Starfield 共享源码。
"use strict";
const http = require("node:http");
const fs = require("node:fs");
const path = require("node:path");
const port = Number(process.argv[2] || 8768);
if (!Number.isInteger(port) || port < 1024 || port > 65535) throw new Error("端口应为 1024–65535 的整数。");
const files = Object.fromEntries(["preview.html", "preview.js", "main.vsh", "main.fsh", "sky.glsl", "scene.vsh", "scene.fsh", "README.md"].map(name => [`/${name}`, name]));
files["/starfield.glsl"] = "../Starfield/main.frag";
const server = http.createServer((request, response) => {
    const pathname = new URL(request.url, "http://localhost").pathname;
    const name = pathname === "/" ? "/preview.html" : pathname;
    if (!Object.hasOwn(files, name)) { response.writeHead(404).end("Not found"); return; }
    fs.readFile(path.join(__dirname, files[name]), (error, data) => {
        if (error) { response.writeHead(500).end("Read failed"); return; }
        const type = name.endsWith(".html") ? "text/html" : name.endsWith(".js") ? "text/javascript" : "text/plain";
        response.writeHead(200, { "Content-Type": `${type}; charset=utf-8`, "Cache-Control": "no-store" }); response.end(data);
    });
});
server.on("error", error => { console.error(`启动失败：${error.message}`); process.exitCode = 1; });
server.listen(port, "127.0.0.1", () => console.log(`星海预览：http://127.0.0.1:${port}/preview.html\n按 Ctrl+C 停止服务。`));
