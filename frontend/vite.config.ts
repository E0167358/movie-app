// defineConfig comes from vitest so the "test" section below is typed
import { defineConfig } from 'vitest/config';
import { svelte } from '@sveltejs/vite-plugin-svelte';

// vitest sets this variable when it runs the tests
const testing = Boolean(process.env.VITEST);

export default defineConfig({
  plugins: [svelte()],

  server: {
    // send API and image requests to the gateway, so the browser sees one
    // origin and no CORS setup is needed
    proxy: {
      '/graphql': 'http://localhost:8080',
      '/artwork': 'http://localhost:8080',
    },
  },

  // svelte ships a browser build and a server build. the tests render
  // components in jsdom, which behaves like a browser, so we ask for the
  // browser build. we only set this while testing: setting it during dev
  // would replace vite's own defaults and the app would not start
  ...(testing ? { resolve: { conditions: ['browser'] } } : {}),

  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/tests/setup.ts'],
    include: ['src/**/*.test.ts'],
  },
});
