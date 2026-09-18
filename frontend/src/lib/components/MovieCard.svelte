<script lang="ts">
  import type { Movie } from '../api/types';

  let { movie }: { movie: Movie } = $props();

  // if the file is missing on disk the image request fails,
  // so we fall back to the placeholder instead of a broken image icon
  let imageFailed = $state(false);

  function subtitle(m: Movie): string {
    // "2010 · Sci-Fi · 148 min", skipping the parts we don't have
    return [m.releaseYear, m.genre, m.durationMinutes ? `${m.durationMinutes} min` : null]
      .filter(Boolean)
      .join(' · ');
  }
</script>

<a class="card" href="#/movies/{movie.id}">
  <div class="art">
    {#if movie.artworkUrl && !imageFailed}
      <img
        src={movie.artworkUrl}
        alt="Artwork for {movie.title}"
        loading="lazy"
        onerror={() => (imageFailed = true)}
      />
    {:else}
      <div class="placeholder" aria-hidden="true">No artwork</div>
    {/if}
  </div>

  <div class="info">
    <h3>{movie.title}</h3>
    <p class="subtitle">{subtitle(movie) || 'No details yet'}</p>
  </div>
</a>

<style>
  .card {
    display: flex;
    flex-direction: column;
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: 10px;
    overflow: hidden;
    text-decoration: none;
    color: inherit;
    transition: transform 0.15s ease, border-color 0.15s ease;
  }

  .card:hover,
  .card:focus-visible {
    transform: translateY(-2px);
    border-color: var(--accent);
  }

  .art {
    aspect-ratio: 2 / 3;
    background: var(--surface-alt);
  }

  .art img {
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

  .info {
    padding: 0.75rem;
  }

  h3 {
    margin: 0 0 0.25rem;
    font-size: 1rem;
    line-height: 1.3;
  }

  .subtitle {
    margin: 0;
    font-size: 0.85rem;
    color: var(--text-muted);
  }
</style>
