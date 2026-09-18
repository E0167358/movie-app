<script lang="ts">
  import {
    ALLOWED_TYPES,
    readAsDataUrl,
    uploadArtwork,
    validateFile,
  } from '../api/artwork';
  import { GraphQlError } from '../api/client';
  import type { Movie } from '../api/types';
  import { toasts } from '../stores/toast.svelte';

  let {
    movieId,
    currentArtworkUrl,
    onUploaded,
  }: {
    movieId: string;
    currentArtworkUrl: string | null;
    onUploaded: (movie: Movie) => void;
  } = $props();

  type Phase = 'idle' | 'reading' | 'uploading' | 'done';

  let phase = $state<Phase>('idle');
  let percent = $state(0);
  let preview = $state<string | null>(null);
  let fileError = $state<string | null>(null);
  let fileInput = $state<HTMLInputElement | null>(null);

  let busy = $derived(phase === 'reading' || phase === 'uploading');

  let statusText = $derived(
    phase === 'reading'
      ? 'Reading file...'
      : phase === 'uploading'
        ? `Uploading... ${percent}%`
        : phase === 'done'
          ? 'Upload complete'
          : '',
  );

  async function onFileSelected(event: Event) {
    const input = event.currentTarget as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    fileError = validateFile(file);
    if (fileError) {
      preview = null;
      input.value = '';
      return;
    }

    try {
      phase = 'reading';
      percent = 0;

      // the same data url is used for the preview and for the upload,
      // so the file is only read once
      const dataUrl = await readAsDataUrl(file);
      preview = dataUrl;

      phase = 'uploading';
      const movie = await uploadArtwork(movieId, file, dataUrl, (value) => {
        percent = value;
      });

      phase = 'done';
      toasts.success('Artwork uploaded');
      onUploaded(movie);
    } catch (error) {
      phase = 'idle';
      preview = null;
      const message =
        error instanceof GraphQlError ? error.message : 'Upload failed, please try again';
      fileError = message;
      toasts.error(message);
    } finally {
      // let the user pick the same file again after an error
      if (fileInput) fileInput.value = '';
    }
  }
</script>

<div class="upload">
  <div class="art-preview">
    {#if preview}
      <img src={preview} alt="New artwork preview" />
    {:else if currentArtworkUrl}
      <img src={currentArtworkUrl} alt="Current artwork" />
    {:else}
      <div class="placeholder">No artwork</div>
    {/if}
  </div>

  <div class="controls">
    <label class="btn" class:disabled={busy}>
      {currentArtworkUrl ? 'Replace artwork' : 'Upload artwork'}
      <input
        bind:this={fileInput}
        type="file"
        accept={ALLOWED_TYPES.join(',')}
        onchange={onFileSelected}
        disabled={busy}
      />
    </label>

    <p class="hint">JPEG, PNG or WEBP, up to 5 MB</p>

    {#if busy || phase === 'done'}
      <div class="progress">
        <!-- the bar is also readable by screen readers through these aria values -->
        <div
          class="bar"
          role="progressbar"
          aria-valuenow={phase === 'reading' ? 0 : percent}
          aria-valuemin="0"
          aria-valuemax="100"
          aria-label="Upload progress"
        >
          <div
            class="fill"
            class:indeterminate={phase === 'reading'}
            style="width: {phase === 'reading' ? 100 : percent}%"
          ></div>
        </div>
        <span class="status" aria-live="polite">{statusText}</span>
      </div>
    {/if}

    {#if fileError}
      <p class="error">{fileError}</p>
    {/if}
  </div>
</div>

<style>
  .upload {
    display: flex;
    gap: 1.25rem;
    flex-wrap: wrap;
  }

  .art-preview {
    width: 160px;
    aspect-ratio: 2 / 3;
    background: var(--surface-alt);
    border: 1px solid var(--border);
    border-radius: 10px;
    overflow: hidden;
    flex-shrink: 0;
  }

  .art-preview img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }

  .placeholder {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--text-muted);
    font-size: 0.85rem;
  }

  .controls {
    flex: 1;
    min-width: 200px;
  }

  /* the real file input is hidden, the label acts as the button */
  .controls input[type='file'] {
    position: absolute;
    width: 1px;
    height: 1px;
    opacity: 0;
  }

  label.disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  .hint {
    margin: 0.5rem 0 0;
    font-size: 0.85rem;
    color: var(--text-muted);
  }

  .progress {
    margin-top: 1rem;
  }

  .bar {
    height: 8px;
    background: var(--surface-alt);
    border-radius: 999px;
    overflow: hidden;
  }

  .fill {
    height: 100%;
    background: var(--accent);
    transition: width 0.15s ease;
  }

  /* while the file is being read we don't know a percentage yet,
     so the bar slides instead of filling up */
  .fill.indeterminate {
    animation: slide 1s ease-in-out infinite;
  }

  @keyframes slide {
    0% {
      transform: translateX(-100%);
    }
    100% {
      transform: translateX(100%);
    }
  }

  .status {
    display: block;
    margin-top: 0.4rem;
    font-size: 0.85rem;
    color: var(--text-muted);
  }

  .error {
    margin: 0.75rem 0 0;
    color: var(--danger);
    font-size: 0.9rem;
  }
</style>
