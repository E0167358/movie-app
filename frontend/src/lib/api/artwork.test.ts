import { afterEach, describe, expect, it, vi } from 'vitest';
import { MAX_BYTES, readAsDataUrl, uploadArtwork, validateFile } from './artwork';

function fakeFile(type: string, size: number, name = 'poster.png'): File {
  const file = new File(['x'], name, { type });
  // File.size is read only, so we override it for the test
  Object.defineProperty(file, 'size', { value: size });
  return file;
}

describe('validateFile', () => {
  it('accepts jpeg, png and webp', () => {
    for (const type of ['image/jpeg', 'image/png', 'image/webp']) {
      expect(validateFile(fakeFile(type, 1000))).toBeNull();
    }
  });

  it('rejects other file types', () => {
    for (const type of ['application/pdf', 'image/gif', 'text/plain', '']) {
      expect(validateFile(fakeFile(type, 1000))).toBe(
        'Only JPEG, PNG and WEBP images are allowed',
      );
    }
  });

  it('rejects an empty file', () => {
    expect(validateFile(fakeFile('image/png', 0))).toBe('That file is empty');
  });

  it('accepts a file of exactly 5 MB', () => {
    expect(validateFile(fakeFile('image/png', MAX_BYTES))).toBeNull();
  });

  it('rejects a file one byte over 5 MB', () => {
    expect(validateFile(fakeFile('image/png', MAX_BYTES + 1))).toBe(
      'Image must be smaller than 5 MB',
    );
  });
});

describe('readAsDataUrl', () => {
  it('reads a file into a data url', async () => {
    const file = new File(['hello'], 'poster.png', { type: 'image/png' });

    const dataUrl = await readAsDataUrl(file);

    expect(dataUrl.startsWith('data:image/png;base64,')).toBe(true);
  });
});

// a fake XMLHttpRequest so we can test the upload without a server
class FakeXhr {
  static last: FakeXhr;

  upload = { onprogress: null as ((event: ProgressEvent) => void) | null };
  onload: (() => void) | null = null;
  onerror: (() => void) | null = null;
  onabort: (() => void) | null = null;
  status = 200;
  responseText = '';
  sentBody = '';
  openedWith: [string, string] = ['', ''];
  headers: Record<string, string> = {};

  constructor() {
    FakeXhr.last = this;
  }

  open(method: string, url: string) {
    this.openedWith = [method, url];
  }

  setRequestHeader(name: string, value: string) {
    this.headers[name] = value;
  }

  send(body: string) {
    this.sentBody = body;
  }

  // helpers the tests use to act like a server
  emitProgress(loaded: number, total: number) {
    this.upload.onprogress?.({ lengthComputable: true, loaded, total } as ProgressEvent);
  }

  finish(body: unknown, status = 200) {
    this.status = status;
    this.responseText = JSON.stringify(body);
    this.onload?.();
  }
}

function useFakeXhr() {
  vi.stubGlobal('XMLHttpRequest', FakeXhr);
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('uploadArtwork', () => {
  const file = fakeFile('image/png', 1000);
  const dataUrl = 'data:image/png;base64,AAAA';
  const movieResponse = {
    data: { uploadArtwork: { id: '1', title: 'Inception', artworkUrl: '/artwork/1' } },
  };

  it('posts the mutation with the file details', async () => {
    useFakeXhr();

    const promise = uploadArtwork('1', file, dataUrl, () => {});
    FakeXhr.last.finish(movieResponse);
    await promise;

    expect(FakeXhr.last.openedWith).toEqual(['POST', '/graphql']);
    expect(FakeXhr.last.headers['Content-Type']).toBe('application/json');

    const body = JSON.parse(FakeXhr.last.sentBody);
    expect(body.variables.input).toEqual({
      movieId: '1',
      fileName: 'poster.png',
      contentType: 'image/png',
      base64Data: dataUrl,
    });
  });

  it('reports progress while uploading', async () => {
    useFakeXhr();
    const progress: number[] = [];

    const promise = uploadArtwork('1', file, dataUrl, (percent) => progress.push(percent));
    FakeXhr.last.emitProgress(25, 100);
    FakeXhr.last.emitProgress(50, 100);
    FakeXhr.last.finish(movieResponse);
    await promise;

    // the last 100 comes after the server answered, not when the bytes were sent
    expect(progress).toEqual([25, 50, 100]);
  });

  it('returns the updated movie', async () => {
    useFakeXhr();

    const promise = uploadArtwork('1', file, dataUrl, () => {});
    FakeXhr.last.finish(movieResponse);

    await expect(promise).resolves.toMatchObject({ id: '1', artworkUrl: '/artwork/1' });
  });

  it('rejects with the message from the server', async () => {
    useFakeXhr();

    const promise = uploadArtwork('1', file, dataUrl, () => {});
    FakeXhr.last.finish({
      errors: [
        {
          message: 'Only JPEG, PNG and WEBP images are allowed',
          extensions: { code: 'INVALID_ARGUMENT' },
        },
      ],
    });

    await expect(promise).rejects.toThrow('Only JPEG, PNG and WEBP images are allowed');
  });

  it('rejects when the server returns an http error', async () => {
    useFakeXhr();

    const promise = uploadArtwork('1', file, dataUrl, () => {});
    FakeXhr.last.finish({}, 500);

    await expect(promise).rejects.toThrow('500');
  });

  it('rejects when the connection fails', async () => {
    useFakeXhr();

    const promise = uploadArtwork('1', file, dataUrl, () => {});
    FakeXhr.last.onerror?.();

    await expect(promise).rejects.toThrow('Upload failed');
  });
});
