<script lang="ts">
  import { addCredit, fetchPeople, removeCredit } from '../api/people';
  import { GraphQlError } from '../api/client';
  import { ROLES, type Credit, type Person, type Role } from '../api/types';
  import { toasts } from '../stores/toast.svelte';

  let {
    movieId,
    credits,
    onChanged,
  }: {
    movieId: string;
    credits: Credit[];
    onChanged: () => void;
  } = $props();

  // the dropdown loads up to 100 people. with a bigger library this would
  // become a search box that queries people-service as you type
  const PEOPLE_LIMIT = 100;

  let people = $state<Person[]>([]);
  let loadingPeople = $state(true);
  let personId = $state('');
  let role = $state<Role>('ACTOR');
  let characterName = $state('');
  let saving = $state(false);
  let removingId = $state<string | null>(null);
  let formError = $state<string | null>(null);

  let cast = $derived(credits.filter((credit) => credit.role === 'ACTOR'));
  let creators = $derived(credits.filter((credit) => credit.role !== 'ACTOR'));

  function label(role: Role): string {
    return role.charAt(0) + role.slice(1).toLowerCase();
  }

  async function loadPeople() {
    loadingPeople = true;
    try {
      const result = await fetchPeople(0, PEOPLE_LIMIT);
      people = result.items;
    } catch {
      // not fatal, the rest of the page still works
      toasts.error('Could not load the list of people');
    } finally {
      loadingPeople = false;
    }
  }

  async function onAdd(event: SubmitEvent) {
    event.preventDefault();
    formError = null;

    if (!personId) {
      formError = 'Choose a person first';
      return;
    }

    saving = true;
    try {
      await addCredit({
        movieId,
        personId,
        role,
        // the server ignores this for roles other than actor, we keep the form tidy too
        characterName: role === 'ACTOR' ? characterName.trim() || null : null,
      });
      toasts.success('Added to this movie');
      personId = '';
      characterName = '';
      onChanged();
    } catch (error) {
      const message = error instanceof GraphQlError ? error.message : 'Could not add the credit';
      formError = message;
      toasts.error(message);
    } finally {
      saving = false;
    }
  }

  async function onRemove(credit: Credit) {
    removingId = credit.id;
    try {
      await removeCredit(credit.id);
      toasts.success(`${credit.person.name} removed`);
      onChanged();
    } catch (error) {
      const message =
        error instanceof GraphQlError ? error.message : 'Could not remove the credit';
      toasts.error(message);
    } finally {
      removingId = null;
    }
  }

  $effect(() => {
    loadPeople();
  });
</script>

<div class="credits">
  <div class="column">
    <h3>Cast</h3>
    {#if cast.length === 0}
      <p class="muted">No cast added yet.</p>
    {:else}
      <ul>
        {#each cast as credit (credit.id)}
          <li>
            <div>
              <a href="#/people/{credit.person.id}">{credit.person.name}</a>
              {#if credit.characterName}
                <span class="role">as {credit.characterName}</span>
              {/if}
            </div>
            <button
              class="remove"
              onclick={() => onRemove(credit)}
              disabled={removingId === credit.id}
              aria-label="Remove {credit.person.name}"
            >
              {removingId === credit.id ? '...' : '×'}
            </button>
          </li>
        {/each}
      </ul>
    {/if}
  </div>

  <div class="column">
    <h3>Creators</h3>
    {#if creators.length === 0}
      <p class="muted">No creators added yet.</p>
    {:else}
      <ul>
        {#each creators as credit (credit.id)}
          <li>
            <div>
              <a href="#/people/{credit.person.id}">{credit.person.name}</a>
              <span class="role">{label(credit.role)}</span>
            </div>
            <button
              class="remove"
              onclick={() => onRemove(credit)}
              disabled={removingId === credit.id}
              aria-label="Remove {credit.person.name}"
            >
              {removingId === credit.id ? '...' : '×'}
            </button>
          </li>
        {/each}
      </ul>
    {/if}
  </div>
</div>

<form class="add-form" onsubmit={onAdd}>
  <h3>Add cast or creator</h3>

  {#if loadingPeople}
    <p class="muted">Loading people...</p>
  {:else if people.length === 0}
    <p class="muted">
      No people yet. <a href="#/people/new">Add a person</a> first.
    </p>
  {:else}
    <div class="add-row">
      <div class="field">
        <label for="person">Person</label>
        <select id="person" bind:value={personId}>
          <option value="">Choose...</option>
          {#each people as person (person.id)}
            <option value={person.id}>{person.name}</option>
          {/each}
        </select>
      </div>

      <div class="field">
        <label for="role">Role</label>
        <select id="role" bind:value={role}>
          {#each ROLES as option (option)}
            <option value={option}>{label(option)}</option>
          {/each}
        </select>
      </div>

      {#if role === 'ACTOR'}
        <div class="field">
          <label for="character">Character (optional)</label>
          <input id="character" bind:value={characterName} placeholder="e.g. Cobb" />
        </div>
      {/if}

      <button class="btn btn-primary add-button" type="submit" disabled={saving}>
        {saving ? 'Adding...' : 'Add'}
      </button>
    </div>

    {#if formError}
      <p class="error" role="alert">{formError}</p>
    {/if}
  {/if}
</form>

<style>
  .credits {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
    gap: 1.5rem;
    margin-top: 2rem;
  }

  h3 {
    margin: 0 0 0.5rem;
    font-size: 1rem;
  }

  ul {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
  }

  li {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 0.5rem;
    padding: 0.6rem 0.75rem;
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: 8px;
  }

  li div {
    display: flex;
    flex-direction: column;
  }

  .role {
    font-size: 0.85rem;
    color: var(--text-muted);
  }

  .remove {
    background: none;
    border: none;
    color: var(--text-muted);
    font-size: 1.25rem;
    line-height: 1;
    cursor: pointer;
    padding: 0 0.25rem;
  }

  .remove:hover:not(:disabled) {
    color: var(--danger);
  }

  .muted {
    margin: 0;
    color: var(--text-muted);
    font-size: 0.9rem;
  }

  .add-form {
    margin-top: 2rem;
    padding: 1.25rem;
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: 10px;
  }

  .add-row {
    display: flex;
    gap: 1rem;
    align-items: flex-end;
    flex-wrap: wrap;
  }

  .add-row .field {
    margin-bottom: 0;
    flex: 1;
    min-width: 160px;
  }

  .add-button {
    height: 2.6rem;
  }

  .error {
    margin: 0.75rem 0 0;
    color: var(--danger);
    font-size: 0.9rem;
  }
</style>
