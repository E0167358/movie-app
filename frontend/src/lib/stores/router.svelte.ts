// tiny hash router. we use the hash (#/movies/1) instead of real paths
// so refreshing the page works without any server side routing

export interface Route {
  name: string;
  // id is set for routes like #/movies/1
  id?: string;
}

function parse(hash: string): Route {
  // "#/movies/1" -> ["movies", "1"]
  const parts = hash.replace(/^#\/?/, '').split('/').filter(Boolean);

  if (parts.length === 0) {
    return { name: 'movies' };
  }

  const [first, second] = parts;

  if (first === 'movies') {
    if (second === 'new') return { name: 'movie-new' };
    if (second) return { name: 'movie-detail', id: second };
    return { name: 'movies' };
  }

  if (first === 'people') {
    if (second === 'new') return { name: 'person-new' };
    if (second) return { name: 'person-detail', id: second };
    return { name: 'people' };
  }

  if (first === 'search') {
    return { name: 'search' };
  }

  return { name: 'not-found' };
}

class Router {
  current = $state<Route>(parse(window.location.hash));

  constructor() {
    window.addEventListener('hashchange', () => {
      this.current = parse(window.location.hash);
      window.scrollTo(0, 0);
    });
  }

  go(path: string) {
    window.location.hash = path;
  }
}

export const router = new Router();
