import { fileURLToPath, URL } from 'node:url'

<<<<<<< HEAD

import fs from "fs";
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from "path";
=======
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

>>>>>>> 4b4f327403194786859b432d751c59338d13d02e
// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
  ],
<<<<<<< HEAD
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
=======
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  }
>>>>>>> 4b4f327403194786859b432d751c59338d13d02e
})
