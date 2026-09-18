import { defineConfig } from 'vite'
import { svelte } from '@sveltejs/vite-plugin-svelte'

export default defineConfig({
  plugins: [svelte()],
  server: {
    // send API and image requests to the gateway, so the browser sees one origin and no CORS setup is needed
    proxy: {
      '/graphql': 'http://localhost:8080',
      '/artwork': 'http://localhost:8080',
    },
  },
})