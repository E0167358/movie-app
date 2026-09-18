// small message list shown in the corner, so the user gets feedback
// after saving, deleting or when something fails

export type ToastKind = 'success' | 'error';

export interface Toast {
  id: number;
  kind: ToastKind;
  message: string;
}

const DEFAULT_DURATION = 4000;

class ToastStore {
  items = $state<Toast[]>([]);
  private nextId = 1;

  show(kind: ToastKind, message: string, duration = DEFAULT_DURATION) {
    const id = this.nextId++;
    this.items.push({ id, kind, message });

    if (duration > 0) {
      setTimeout(() => this.dismiss(id), duration);
    }
    return id;
  }

  success(message: string) {
    return this.show('success', message);
  }

  // errors stay a bit longer, the user may need to read them
  error(message: string) {
    return this.show('error', message, 6000);
  }

  dismiss(id: number) {
    this.items = this.items.filter((toast) => toast.id !== id);
  }

  clear() {
    this.items = [];
  }
}

export const toasts = new ToastStore();
