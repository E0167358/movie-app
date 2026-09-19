<script lang="ts">
  import { router } from '../stores/router.svelte';

  // wait a moment after the last keystroke, so typing "nolan"
  // sends one request instead of five
  const DEBOUNCE_MS = 300;

  let value = $state(router.current.query ?? '');
  let timer: ReturnType<typeof setTimeout> | undefined;

  function goToSearch(replace: boolean) {
    const path = `/search?q=${encodeURIComponent(value.trim())}`;

    // while typing we replace the url, so the back button is not filled
    // with one entry per keystroke
    if (replace && router.current.name === 'search') {
      router.replace(path);
    } else {
      router.go(path);
    }
  }

  function onInput() {
    clearTimeout(timer);
    timer = setTimeout(() => goToSearch(true), DEBOUNCE_MS);
  }

  function onSubmit(event: SubmitEvent) {
    event.preventDefault();
    clearTimeout(timer);
    goToSearch(false);
  }

  function clear() {
    value = '';
    clearTimeout(timer);
    router.go('/movies');
  }

  // keep the box in sync when the user navigates away or presses back
  $effect(() => {
    if (router.current.name !== 'search') {
      value = '';
    }
  });
</script>

<form class="search" onsubmit={onSubmit} role="search">
  <input
    type="search"
    placeholder="Search movies and people"
    aria-label="Search movies and people"
    bind:value
    oninput={onInput}
  />
  {#if value}
    <button type="button" class="clear" onclick={clear} aria-label="Clear search">
      &times;
    </button>
  {/if}
</form>

<style>
  .search {
    position: relative;
    flex: 1;
    min-width: 160px;
    max-width: 22rem;
  }

  input {
    width: 100%;
    padding: 0.45rem 2rem 0.45rem 0.75rem;
    font-size: 0.95rem;
    font-family: inherit;
    color: var(--text);
    background: var(--surface-alt);
    border: 1px solid var(--border);
    border-radius: 999px;
  }

  input:focus {
    outline: 2px solid var(--accent);
    outline-offset: -1px;
  }

  /* hide the browser's own clear button, we have our own */
  input::-webkit-search-cancel-button {
    display: none;
  }

  .clear {
    position: absolute;
    right: 0.5rem;
    top: 50%;
    transform: translateY(-50%);
    background: none;
    border: none;
    color: var(--text-muted);
    font-size: 1.25rem;
    line-height: 1;
    cursor: pointer;
  }
</style>
