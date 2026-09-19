// runs before every test file
import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/svelte';
import { afterEach } from 'vitest';

// remove the rendered components between tests so they don't affect each other
afterEach(() => {
  cleanup();
});
