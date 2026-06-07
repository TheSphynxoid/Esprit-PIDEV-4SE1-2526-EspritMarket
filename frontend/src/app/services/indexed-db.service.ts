import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class IndexedDbService {
  private db: IDBDatabase | null = null;
  private readonly DB_NAME = 'esprit-market-db';
  private readonly DB_VERSION = 1;
  private readonly CARD_STORE = 'student_cards';

  constructor() {
    this.initDb();
  }

  private initDb(): void {
    if (!indexedDB) {
      console.error('IndexedDB not available');
      return;
    }

    const request = indexedDB.open(this.DB_NAME, this.DB_VERSION);

    request.onerror = () => {
      console.error('Failed to open IndexedDB:', request.error);
    };

    request.onsuccess = () => {
      this.db = request.result;
      console.log('✓ IndexedDB initialized successfully');
    };

    request.onupgradeneeded = (event) => {
      const db = (event.target as IDBOpenDBRequest).result;
      
      // Create object store if it doesn't exist
      if (!db.objectStoreNames.contains(this.CARD_STORE)) {
        db.createObjectStore(this.CARD_STORE, { keyPath: 'key' });
        console.log(`Created object store: ${this.CARD_STORE}`);
      }
    };
  }

  async saveCard(key: string, file: File): Promise<void> {
    if (!this.db) {
      console.warn('IndexedDB not ready, waiting...');
      await this.waitForDb();
    }

    return new Promise((resolve, reject) => {
      if (!this.db) {
        reject(new Error('IndexedDB not available'));
        return;
      }

      const transaction = this.db.transaction([this.CARD_STORE], 'readwrite');
      const store = transaction.objectStore(this.CARD_STORE);

      const record = {
        key,
        file,
        timestamp: Date.now()
      };

      const request = store.put(record);

      request.onsuccess = () => {
        console.log(`✓ Card saved to IndexedDB with key: "${key}"`);
        resolve();
      };

      request.onerror = () => {
        console.error(`Failed to save card:`, request.error);
        reject(request.error);
      };
    });
  }

  async getCard(key: string): Promise<File | null> {
    if (!this.db) {
      console.warn('IndexedDB not ready, waiting...');
      await this.waitForDb();
    }

    return new Promise((resolve, reject) => {
      if (!this.db) {
        reject(new Error('IndexedDB not available'));
        return;
      }

      const transaction = this.db.transaction([this.CARD_STORE], 'readonly');
      const store = transaction.objectStore(this.CARD_STORE);
      const request = store.get(key);

      request.onsuccess = () => {
        const record = request.result;
        if (record && record.file) {
          console.log(`✓ Retrieved card from IndexedDB: "${key}"`);
          resolve(record.file);
        } else {
          console.log(`No card found in IndexedDB for key: "${key}"`);
          resolve(null);
        }
      };

      request.onerror = () => {
        console.error(`Failed to retrieve card:`, request.error);
        reject(request.error);
      };
    });
  }

  async deleteCard(key: string): Promise<void> {
    if (!this.db) {
      await this.waitForDb();
    }

    return new Promise((resolve, reject) => {
      if (!this.db) {
        reject(new Error('IndexedDB not available'));
        return;
      }

      const transaction = this.db.transaction([this.CARD_STORE], 'readwrite');
      const store = transaction.objectStore(this.CARD_STORE);
      const request = store.delete(key);

      request.onsuccess = () => {
        console.log(`✓ Card deleted from IndexedDB: "${key}"`);
        resolve();
      };

      request.onerror = () => {
        console.error(`Failed to delete card:`, request.error);
        reject(request.error);
      };
    });
  }

  async clearAllCards(): Promise<void> {
    if (!this.db) {
      await this.waitForDb();
    }

    return new Promise((resolve, reject) => {
      if (!this.db) {
        reject(new Error('IndexedDB not available'));
        return;
      }

      const transaction = this.db.transaction([this.CARD_STORE], 'readwrite');
      const store = transaction.objectStore(this.CARD_STORE);
      const request = store.clear();

      request.onsuccess = () => {
        console.log('✓ All cards cleared from IndexedDB');
        resolve();
      };

      request.onerror = () => {
        console.error(`Failed to clear cards:`, request.error);
        reject(request.error);
      };
    });
  }

  private waitForDb(): Promise<void> {
    return new Promise((resolve) => {
      let attempts = 0;
      const maxAttempts = 50; // 5 seconds max

      const check = () => {
        if (this.db) {
          resolve();
        } else if (attempts < maxAttempts) {
          attempts++;
          setTimeout(check, 100);
        } else {
          console.error('IndexedDB initialization timeout');
          resolve(); // Proceed anyway
        }
      };

      check();
    });
  }
}
