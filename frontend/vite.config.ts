/// <reference types="vitest" />
import { defineConfig } from "vite";
import { fileURLToPath, URL } from "url";
import vue from "@vitejs/plugin-vue";
import vueJsx from "@vitejs/plugin-vue-jsx";
import checker from 'vite-plugin-checker';
import VueDevTools from 'vite-plugin-vue-devtools'
import AutoImport from 'unplugin-auto-import/vite';
import Components from 'unplugin-vue-components/vite';
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers';
import { VueRouterAutoImports } from 'unplugin-vue-router';
import VueRouter from 'unplugin-vue-router/vite';
import viteCompression from 'vite-plugin-compression';
import tsconfigPaths from 'vite-tsconfig-paths'

export default defineConfig({
  test: {
    exclude: [
      "packages/template/*",
      "node_modules/**/*.spec.js",
      "node_modules/**/*.test.js",
      "node_modules/**/test.js"
    ],
    globals: true,
    environment: "jsdom",
    "setupFiles": [
      "./tests/setupVitest.js"
    ]
  },
  css: {
    preprocessorOptions: {
      scss: {
        charset: false
      }
    }
  },
  plugins: [
    VueDevTools(),
    tsconfigPaths(),
    checker({
      vueTsc: true,
      eslint: {
        lintCommand: 'eslint "./src/**/*.{vue,ts,tsx}" "./tests/**/*.ts"',
      }
    }),
    vue({
      template: {
        compilerOptions: {
          isCustomElement: (tag) => /^x-/.test(tag),
        },
      },
    }),
    VueRouter(),
    vueJsx(),
    AutoImport({
      resolvers: [ElementPlusResolver({ importStyle: 'sass' })],
      imports: [
        'vue',
        'vue-router',
        'vitest',
        VueRouterAutoImports
      ],
      dts: true, // generate TypeScript declaration
    }),
    Components({
      resolvers: [ElementPlusResolver({ importStyle: 'sass' })]
    }),
    viteCompression()
  ],
  server: {
    proxy: {
      "/api": "http://localhost:9081",
      "/attachments": "http://localhost:9081",
      "/logout": "http://localhost:9081",
      "/testability": "http://localhost:9081",
    },
  },
  base: "/",
  build: {
    outDir: "../backend/src/main/resources/static/",
    chunkSizeWarningLimit: 1000,
    sourcemap: true,
    rollupOptions: {
      input: {
        main: fileURLToPath(new URL("index.html", import.meta.url))
      },
    },
  },
});
