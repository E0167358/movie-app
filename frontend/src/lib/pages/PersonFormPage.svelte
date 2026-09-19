<script lang="ts">
  import { createPerson, fetchPerson, updatePerson } from '../api/people';
  import { GraphQlError } from '../api/client';
  import {
    toOptionalNumber,
    toOptionalText,
    validatePerson,
    type Errors,
    type PersonFormValues,
  } from '../api/validation';
  import { router } from '../stores/router.svelte';
  import { toasts } from '../stores/toast.svelte';
  import Spinner from '../components/Spinner.svelte';
  import ErrorBox from '../components/ErrorBox.svelte';

  let { id }: { id?: string } = $props();

  let values = $state<PersonFormValues>({ name: '', bio: '', birthYear: '' });
  let errors = $state<Errors>({});
  // starts false so the "add person" form renders straight away
  let loading = $state(false);
  let saving = $state(false);
  let loadError = $state<string | null>(null);

  async function load() {
    if (!id) return;

    loading = true;
    loadError = null;

    try {
      const person = await fetchPerson(id);
      if (!person) {
        loadError = 'That person does not exist anymore';
        return;
      }
      values = {
        name: person.name,
        bio: person.bio ?? '',
        birthYear: person.birthYear?.toString() ?? '',
      };
    } catch (error) {
      loadError = error instanceof GraphQlError ? error.message : 'Could not load the person';
    } finally {
      loading = false;
    }
  }

  async function onSubmit(event: SubmitEvent) {
    event.preventDefault();

    errors = validatePerson(values);
    if (Object.keys(errors).length > 0) {
      return;
    }

    const input = {
      name: values.name.trim(),
      bio: toOptionalText(values.bio),
      birthYear: toOptionalNumber(values.birthYear),
    };

    saving = true;
    try {
      const person = id ? await updatePerson(id, input) : await createPerson(input);
      toasts.success(id ? 'Person updated' : 'Person added');
      router.go(`/people/${person.id}`);
    } catch (error) {
      const message =
        error instanceof GraphQlError ? error.message : 'Could not save the person';
      errors = { form: message };
      toasts.error(message);
    } finally {
      saving = false;
    }
  }

  $effect(() => {
    load();
  });
</script>

<section class="form-page">
  <a class="back" href={id ? `#/people/${id}` : '#/people'}>&larr; Back</a>
  <h2>{id ? 'Edit person' : 'Add person'}</h2>

  {#if loading}
    <Spinner label="Loading person..." />
  {:else if loadError}
    <ErrorBox message={loadError} onRetry={load} />
  {:else}
    <form onsubmit={onSubmit} novalidate>
      <div class="field">
        <label for="name">Name *</label>
        <input
          id="name"
          bind:value={values.name}
          aria-invalid={Boolean(errors.name)}
          aria-describedby={errors.name ? 'name-error' : undefined}
        />
        {#if errors.name}
          <span class="field-error" id="name-error">{errors.name}</span>
        {/if}
      </div>

      <div class="field">
        <label for="birthYear">Birth year</label>
        <input
          id="birthYear"
          type="text"
          inputmode="numeric"
          bind:value={values.birthYear}
          aria-invalid={Boolean(errors.birthYear)}
        />
        {#if errors.birthYear}
          <span class="field-error">{errors.birthYear}</span>
        {/if}
      </div>

      <div class="field">
        <label for="bio">Bio</label>
        <textarea
          id="bio"
          rows="5"
          bind:value={values.bio}
          aria-invalid={Boolean(errors.bio)}
        ></textarea>
        {#if errors.bio}
          <span class="field-error">{errors.bio}</span>
        {/if}
      </div>

      {#if errors.form}
        <p class="form-error" role="alert">{errors.form}</p>
      {/if}

      <div class="actions">
        <a class="btn" href={id ? `#/people/${id}` : '#/people'}>Cancel</a>
        <button class="btn btn-primary" type="submit" disabled={saving}>
          {saving ? 'Saving...' : id ? 'Save changes' : 'Add person'}
        </button>
      </div>
    </form>
  {/if}
</section>

<style>
  .form-page {
    max-width: 640px;
  }

  .back {
    display: inline-block;
    margin-bottom: 0.5rem;
    font-size: 0.9rem;
    text-decoration: none;
  }

  h2 {
    margin: 0 0 1.5rem;
  }

  .form-error {
    color: var(--danger);
    margin: 0 0 1rem;
  }

  .actions {
    display: flex;
    justify-content: flex-end;
    gap: 0.75rem;
    margin-top: 1rem;
  }
</style>
