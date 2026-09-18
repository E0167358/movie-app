<script lang="ts">
  let { label = 'Loading...' }: { label?: string } = $props();
</script>

<div class="spinner-box" role="status" aria-live="polite">
  <div class="spinner"></div>
  <span>{label}</span>
</div>

<style>
  .spinner-box {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0.75rem;
    padding: 3rem 1rem;
    color: var(--text-muted);
  }

  .spinner {
    width: 2rem;
    height: 2rem;
    border: 3px solid var(--border);
    border-top-color: var(--accent);
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
  }

  @keyframes spin {
    to {
      transform: rotate(360deg);
    }
  }

  /* respect the system setting for people who don't want animation */
  @media (prefers-reduced-motion: reduce) {
    .spinner {
      animation-duration: 2s;
    }
  }
</style>
