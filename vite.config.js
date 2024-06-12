import { fileURLToPath, URL } from 'node:url'


import fs from "fs";
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from "path";
// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
  ],
  server: {
    https: {
      cert: fs.readFileSync(path.join(__dirname, "src/ssl/cert.crt")),
      key: fs.readFileSync(path.join(__dirname, "src/ssl/cert.key")),
    },
    // disableHostCheck: true,
    // host: '0.0.0.0',
   }
  // resolve: {
  //   alias: {
  //     '@': fileURLToPath(new URL('./src', import.meta.url))
  //   }
  // }
})
