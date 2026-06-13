import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/auth": "http://localhost:9000",
      "/posts": "http://localhost:9000",
      "/replies": "http://localhost:9000",
      "/users": "http://localhost:9000",
      "/admin": "http://localhost:9000",
      "/actuator": "http://localhost:9000",
      "/livez": "http://localhost:9000",
      "/readyz": "http://localhost:9000"
    }
  }
});
