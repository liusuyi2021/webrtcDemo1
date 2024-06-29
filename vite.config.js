import { fileURLToPath, URL } from "node:url";

import fs from "fs";
import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import path from "path";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    host: '0.0.0.0', // 允许从外部访问
    // 启用 HTTPS
    https: {
      cert: fs.readFileSync(path.join(__dirname, "src/ssl/zabbix.crt")),
      key: fs.readFileSync(path.join(__dirname, "src/ssl/zabbix.key")),
    },
  },
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
});
