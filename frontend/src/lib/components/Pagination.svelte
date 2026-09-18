<script lang="ts">
  let {
    page,
    size,
    totalCount,
    onChange,
  }: {
    page: number;
    size: number;
    totalCount: number;
    onChange: (page: number) => void;
  } = $props();

  // page is 0 based in the API, but we show 1 based numbers to the user
  let totalPages = $derived(Math.max(1, Math.ceil(totalCount / size)));
  let firstItem = $derived(totalCount === 0 ? 0 : page * size + 1);
  let lastItem = $derived(Math.min((page + 1) * size, totalCount));
</script>

{#if totalCount > size}
  <nav class="pagination" aria-label="Pagination">
    <button class="btn" disabled={page === 0} onclick={() => onChange(page - 1)}>
      Previous
    </button>

    <span class="status" aria-live="polite">
      {firstItem}-{lastItem} of {totalCount}
    </span>

    <button
      class="btn"
      disabled={page >= totalPages - 1}
      onclick={() => onChange(page + 1)}
    >
      Next
    </button>
  </nav>
{/if}

<style>
  .pagination {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 1rem;
    margin-top: 2rem;
  }

  .status {
    font-size: 0.9rem;
    color: var(--text-muted);
  }
</style>
