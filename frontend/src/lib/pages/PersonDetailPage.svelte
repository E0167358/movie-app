<script lang="ts">
  import { deletePerson, fetchPerson } from '../api/people';
  import { GraphQlError } from '../api/client';
  import type { Credit, Person } from '../api/types';
  import { router } from '../stores/router.svelte';
  import { toasts } from '../stores/toast.svelte';
  import ConfirmDialog from '../components/ConfirmDialog.svelte';
  import ErrorBox from '../components/ErrorBox.svelte';
  import Spinner from '../components/Spinner.svelte';

  let { id }: { id: string } = $props();

  let person = $state<Person | null>(null);
  let loading = $state(true);
  let errorMessage = $state<string | null>(null);
  let confirmingDelete = $state(false);
  let deleting = $state(false);

  let credits = $derived(person?.credits ?? []);

  function roleLabel(credit: Credit): string {
    const role = credit.role.charAt(0) + credit.role.slice(1).toLowerCase();
    return credit.characterName ? `${role} as ${credit.characterName}` : role;
  }

  async function load() {
    loading = true;
    errorMessage = null;

    try {
      const result = await fetchPerson(id);
      if (!result) {
        errorMessage = 'That person does not exist anymore';
        return;
      }
      person = result;
    } catch (error) {
      errorMessage = error instanceof GraphQlError ? error.message : 'Could not load the person';
    } finally {
      loading = false;
    }
  }

  async function confirmDelete() {
    deleting = true;
    try {
      await deletePerson(id);
      toasts.success('Person deleted');
      router.go('/people');
    } catch (error) {
      const message =
        error instanceof GraphQlError ? error.message : 'Could not delete the person';
      toasts.error(message);
    } finally {
      deleting = false;
      confirmingDelete = false;
    }
  }

  $effect(() => {
    id;
    load();
  });
</script>

<section>
  <a class="back" href="#/people">&larr; All cast &amp; creators</a>

  {#if loading}
    <Spinner label="Loading person..." />
  {:else if errorMessage}
    <ErrorBox message={errorMessage} onRetry={load} />
  {:else if person}
    <header class="detail-header">
      <div>
        <h2>{person.name}</h2>
        {#if person.birthYear}
          <p class="meta">Born {person.birthYear}</p>
        {/if}
      </div>

      <div class="header-actions">
        <a class="btn" href="#/people/{person.id}/edit">Edit</a>
        <button class="btn btn-danger" onclick={() => (confirmingDelete = true)}>Delete</button>
      </div>
    </header>

    {#if person.bio}
      <p class="bio">{person.bio}</p>
    {/if}

    <h3>Movies</h3>
    {#if credits.length === 0}
      <p class="muted">Not added to any movie yet.</p>
    {:else}
      <ul class="filmography">
        {#each credits as credit (credit.id)}
          <li>
            {#if credit.movie}
              <a href="#/movies/{credit.movie.id}">
                <span class="title">
                  {credit.movie.title}
                  {#if credit.movie.releaseYear}
                    <span class="year">({credit.movie.releaseYear})</span>
                  {/if}
                </span>
                <span class="role">{roleLabel(credit)}</span>
              </a>
            {:else}
              <!-- the movie was deleted but its credits were not removed -->
              <div class="missing">
                <span class="title">Movie #{credit.movieId} (deleted)</span>
                <span class="role">{roleLabel(credit)}</span>
              </div>
            {/if}
          </li>
        {/each}
      </ul>
    {/if}
  {/if}
</section>

{#if confirmingDelete && person}
  <ConfirmDialog
    title="Delete person"
    message="Delete {person.name}? They will also be removed from every movie."
    busy={deleting}
    onConfirm={confirmDelete}
    onCancel={() => (confirmingDelete = false)}
  />
{/if}

<style>
  .back {
    display: inline-block;
    margin-bottom: 0.75rem;
    font-size: 0.9rem;
    text-decoration: none;
  }

  .detail-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 1rem;
    flex-wrap: wrap;
    margin-bottom: 1rem;
  }

  h2 {
    margin: 0;
  }

  .meta {
    margin: 0.25rem 0 0;
    color: var(--text-muted);
  }

  .header-actions {
    display: flex;
    gap: 0.5rem;
  }

  .bio {
    max-width: 60ch;
    margin: 0 0 2rem;
  }

  h3 {
    margin: 0 0 0.75rem;
    font-size: 1rem;
  }

  .filmography {
    list-style: none;
    margin: 0;
    padding: 0;
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    gap: 0.75rem;
  }

  .filmography a,
  .missing {
    display: flex;
    flex-direction: column;
    padding: 0.7rem 0.85rem;
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: 8px;
    text-decoration: none;
    color: inherit;
  }

  .filmography a:hover,
  .filmography a:focus-visible {
    border-color: var(--accent);
  }

  .title {
    font-weight: 600;
  }

  .year,
  .role {
    font-weight: 400;
    font-size: 0.85rem;
    color: var(--text-muted);
  }

  .missing .title {
    color: var(--text-muted);
  }

  .muted {
    margin: 0;
    color: var(--text-muted);
    font-size: 0.9rem;
  }
</style>
