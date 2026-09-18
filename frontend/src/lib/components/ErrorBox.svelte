<script lang="ts">
  let {
    message,
    onRetry,
  }: { message: string; onRetry?: () => void } = $props();
</script>

<div class="error-box" role="alert">
  <p>{message}</p>
  {#if onRetry}
    <button class="btn" onclick={onRetry}>Try again</button>
  {/if}
</div>

<style>
  .error-box {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0.75rem;
    padding: 2rem 1rem;
    border: 1px solid var(--danger);
    border-radius: 10px;
    background: var(--surface);
    text-align: center;
  }

  p {
    margin: 0;
    color: var(--danger);
  }
</style>
