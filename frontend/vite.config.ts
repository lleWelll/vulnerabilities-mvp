import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    // OWASP-10: Security Misconfiguration - dev server without host restrictions can be exposed accidentally.
    // Исправление: bind только loopback, фиксируем порт и разрешённые hostnames для локальной разработки.
    host: "127.0.0.1",
    port: 5173,
    strictPort: true,
    allowedHosts: ["localhost", "127.0.0.1"],
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true
      }
    }
  }
});
