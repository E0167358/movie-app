<script lang="ts">
  let {
    title,
    message,
    confirmLabel = 'Delete',
    busy = false,
    onConfirm,
    onCancel,
  }: {
    title: string;
    message: string;
    confirmLabel?: string;
    busy?: boolean;
    onConfirm: () => void;
    onCancel: () => void;
  } = $props();

  function onKeydown(event: KeyboardEvent) {
    if (event.key === 'Escape' && !busy) {
      onCancel();
    }
  }
</script>

<svelte:window onkeydown={onKeydown} />

<!-- clicking the dark background closes the dialog, same as Cancel -->
<div
  class="backdrop"
  role="presentation"
  onclick={() => !busy && onCancel()}
>
  <div
    class="dialog"
    role="alertdialog"
    aria-modal="true"
    aria-labelledby="confirm-title"
    onclick={(event) => event.stopPropagation()}
  >
    <h3 id="confirm-title">{title}</h3>
    <p>{message}</p>

    <div class="actions">
      <button class="btn" onclick={onCancel} disabled={busy}>Cancel</button>
      <button class="btn btn-danger" onclick={onConfirm} disabled={busy}>
        {busy ? 'Working...' : confirmLabel}
      </button>
    </div>
  </div>
</div>

<style>
  .backdrop {
    position: fixed;
    inset: 0;
    background: rgb(0 0 0 / 0.6);
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 1rem;
    z-index: 200;
  }

  .dialog {
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: 12px;
    padding: 1.5rem;
    max-width: 24rem;
    width: 100%;
  }

  h3 {
    margin: 0 0 0.5rem;
  }

  p {
    margin: 0 0 1.5rem;
    color: var(--text-muted);
  }

  .actions {
    display: flex;
    justify-content: flex-end;
    gap: 0.75rem;
  }
</style>
