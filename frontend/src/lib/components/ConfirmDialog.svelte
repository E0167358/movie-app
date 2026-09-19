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

  let dialog = $state<HTMLDialogElement | null>(null);

  // the native <dialog> element handles the dark backdrop, the focus trap
  // and the Escape key for us, so we don't have to build those ourselves
  $effect(() => {
    dialog?.showModal();
  });

  // fired when the user presses Escape
  function onEscape(event: Event) {
    if (busy) {
      // don't let the dialog close while the delete request is running
      event.preventDefault();
      return;
    }
    onCancel();
  }
</script>

<dialog bind:this={dialog} oncancel={onEscape} aria-labelledby="confirm-title">
  <h3 id="confirm-title">{title}</h3>
  <p>{message}</p>

  <div class="actions">
    <button class="btn" onclick={onCancel} disabled={busy}>Cancel</button>
    <button class="btn btn-danger" onclick={onConfirm} disabled={busy}>
      {busy ? 'Working...' : confirmLabel}
    </button>
  </div>
</dialog>

<style>
  dialog {
    max-width: 24rem;
    width: calc(100% - 2rem);
    padding: 1.5rem;
    color: var(--text);
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: 12px;
  }

  dialog::backdrop {
    background: rgb(0 0 0 / 0.6);
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
