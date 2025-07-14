import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// All assets will be placed into /static/ inside the final JAR
export default defineConfig({
  plugins: [react()],
  base: '/static/', // base path for Spring Boot to serve static content
  build: {
    outDir: '../backend/src/main/resources/static', // Vite will output directly to Spring Boot resources
    emptyOutDir: true,
  },
})
