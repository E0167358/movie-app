import { GraphQlError } from './client';
import type { Movie } from './types';

export const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
export const MAX_BYTES = 5 * 1024 * 1024;

// checked here as well as on the server, so the user gets an answer
// immediately instead of after uploading 5 MB
export function validateFile(file: File): string | null {
  if (!ALLOWED_TYPES.includes(file.type)) {
    return 'Only JPEG, PNG and WEBP images are allowed';
  }
  if (file.size === 0) {
    return 'That file is empty';
  }
  if (file.size > MAX_BYTES) {
    return 'Image must be smaller than 5 MB';
  }
  return null;
}

export function readAsDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result as string);
    reader.onerror = () => reject(new GraphQlError('Could not read the file', 'FILE_READ'));
    reader.readAsDataURL(file);
  });
}

const UPLOAD_MUTATION = `
  mutation UploadArtwork($input: ArtworkInput!) {
    uploadArtwork(input: $input) {
      id
      title
      description
      releaseYear
      genre
      durationMinutes
      artworkUrl
    }
  }
`;

// we use XMLHttpRequest instead of fetch because only XHR reports
// how many bytes have been sent, which is what the progress bar needs
export function uploadArtwork(
  movieId: string,
  file: File,
  dataUrl: string,
  onProgress: (percent: number) => void,
): Promise<Movie> {
  return new Promise((resolve, reject) => {
    const body = JSON.stringify({
      query: UPLOAD_MUTATION,
      variables: {
        input: {
          movieId,
          fileName: file.name,
          contentType: file.type,
          base64Data: dataUrl,
        },
      },
    });

    const xhr = new XMLHttpRequest();
    xhr.open('POST', '/graphql');
    xhr.setRequestHeader('Content-Type', 'application/json');

    xhr.upload.onprogress = (event) => {
      if (event.lengthComputable) {
        onProgress(Math.round((event.loaded / event.total) * 100));
      }
    };

    // the last part of the request is the server working, so we show 100%
    // only after the response comes back
    xhr.onload = () => {
      if (xhr.status !== 200) {
        reject(new GraphQlError(`Server returned ${xhr.status}`, 'HTTP_ERROR'));
        return;
      }

      try {
        const result = JSON.parse(xhr.responseText);
        if (result.errors?.length) {
          const first = result.errors[0];
          reject(new GraphQlError(first.message, first.extensions?.code ?? 'UNKNOWN'));
          return;
        }
        onProgress(100);
        resolve(result.data.uploadArtwork as Movie);
      } catch {
        reject(new GraphQlError('Could not read the server response', 'PARSE_ERROR'));
      }
    };

    xhr.onerror = () => reject(new GraphQlError('Upload failed, please try again', 'NETWORK'));
    xhr.onabort = () => reject(new GraphQlError('Upload cancelled', 'CANCELLED'));

    xhr.send(body);
  });
}
