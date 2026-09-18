<script lang="ts">
  import { toasts } from '../stores/toast.svelte';
</script>

<div class="toast-area">
  {#each toasts.items as toast (toast.id)}
    <div class="toast {toast.kind}" role="alert">
      <span>{toast.message}</span>
      <button
        class="close"
        onclick={() => toasts.dismiss(toast.id)}
        aria-label="Close message"
      >
        &times;
      </button>
    </div>
  {/each}
</div>

<style>
  .toast-area {
    position: fixed;
    bottom: 1rem;
    right: 1rem;
    left: 1rem;
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
    align-items: flex-end;
    z-index: 100;
    pointer-events: none;
  }

  .toast {
    pointer-events: auto;
    display: flex;
    align-items: center;
    gap: 0.75rem;
    max-width: 24rem;
    padding: 0.75rem 1rem;
    border-radius: 8px;
    color: #fff;
    box-shadow: 0 4px 12px rgb(0 0 0 / 0.25);
    animation: slide-in 0.2s ease-out;
  }

  .success {
    background: var(--success);
  }

  .error {
    background: var(--danger);
  }

  .close {
    background: none;
    border: none;
    color: inherit;
    font-size: 1.25rem;
    line-height: 1;
    cursor: pointer;
    padding: 0;
  }

  @keyframes slide-in {
    from {
      opacity: 0;
      transform: translateY(0.5rem);
    }
  }

  @media (max-width: 480px) {
    .toast {
      max-width: 100%;
      width: 100%;
    }
  }
</style>
