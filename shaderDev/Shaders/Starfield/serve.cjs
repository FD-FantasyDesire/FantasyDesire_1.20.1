// 仅向本机提供本作品的四个入口文件；无需安装依赖。
"use strict";
const http = require("node:http");
const fs = require("node:fs");
const path = require("node:path");
const port = Number(process.argv[2] || 8767);
if (!Number.isInteger(port) || port < 1024 || port > 65535) throw new Error("端口应为 1024–65535 的整数。");
const types = {
  "/preview.html": "text/html; charset=utf-8",
  "/preview.js": "text/javascript; charset=utf-8",
  "/main.frag": "text/plain; charset=utf-8",
  "/README.md": "text/plain; charset=utf-8"
};
const server = http.createServer((request, response) => {
  const pathname = new URL(request.url, "http://localhost").pathname;
  const file = pathname === "/" ? "/preview.html" : pathname;
  if (!Object.hasOwn(types, file)) {
    response.writeHead(404).end("Not found");
    return;
  }
  fs.readFile(path.join(__dirname, file.slice(1)), (error, data) => {
    if (error) { response.writeHead(500).end("Read failed"); return; }
    response.writeHead(200, { "Content-Type": types[file], "Cache-Control": "no-store" });
    response.end(data);
  });
});
server.on("error", (error) => { console.error(`启动失败：${error.message}`); process.exitCode = 1; });
server.listen(port, "127.0.0.1", () => console.log(`星空预览：http://127.0.0.1:${port}/preview.html\n按 Ctrl+C 停止服务。`));
