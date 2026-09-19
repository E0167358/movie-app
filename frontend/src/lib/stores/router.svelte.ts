// tiny hash router. we use the hash (#/movies/1) instead of real paths
// so refreshing the page works without any server side routing

export interface Route {
  name: string;
  // id is set for routes like #/movies/1
  id?: string;
  // query is the "q" parameter, used by #/search?q=nolan
  query?: string;
}

// ids in this app are always numbers. anything else in the url is a typo
// or a broken link, so we show the not-found page instead of asking
// the server for a movie called "abc"
function isId(value: string): boolean {
  return /^\d+$/.test(value);
}

// exported so the tests can check routes without touching the browser url
export function parse(hash: string): Route {
  // "#/search?q=nolan" -> path "search", query "nolan"
  const withoutHash = hash.replace(/^#\/?/, '');
  const [path, queryString] = withoutHash.split('?');
  const query = new URLSearchParams(queryString ?? '').get('q') ?? '';

  const parts = path.split('/').filter(Boolean);

  if (parts.length === 0) {
    return { name: 'movies' };
  }

  const [first, second, third] = parts;

  if (first === 'movies') {
    if (second === 'new') return { name: 'movie-new' };
    if (second && !isId(second)) return { name: 'not-found' };
    if (second && third === 'edit') return { name: 'movie-edit', id: second };
    if (second) return { name: 'movie-detail', id: second };
    return { name: 'movies' };
  }

  if (first === 'people') {
    if (second === 'new') return { name: 'person-new' };
    if (second && !isId(second)) return { name: 'not-found' };
    if (second && third === 'edit') return { name: 'person-edit', id: second };
    if (second) return { name: 'person-detail', id: second };
    return { name: 'people' };
  }

  if (first === 'search') {
    return { name: 'search', query };
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

  // used by the search box: changes the url without adding a history entry,
  // otherwise every keystroke would end up in the back button
  replace(path: string) {
    const url = `${window.location.pathname}${window.location.search}#${path}`;
    window.history.replaceState(null, '', url);
    this.current = parse(`#${path}`);
  }
}

export const router = new Router();
