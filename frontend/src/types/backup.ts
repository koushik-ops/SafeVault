export interface Backup {
  id: string;
  ownerId: string;
  originalFilename: string;
  objectPath: string;
  originalSizeBytes: number;
  encryptedSizeBytes: number | null;
  mimeType: string;
  encryptionAlgorithm: string;
  formatVersion: number;
  nonce: string; // base64
  authenticationTag: string | null; // base64
  wrappedDek: string; // base64
  kmsKeyReference: string;
  status: BackupStatus;
  createdAt: string;
  updatedAt: string;
}

export type BackupStatus = 'PENDING' | 'ENCRYPTING' | 'UPLOADING' | 'COMPLETED' | 'FAILED' | 'DELETED';

export interface BackupListResponse {
  content: Backup[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface BackupSummary {
  totalBackups: number;
  totalEncryptedBytes: number;
  mostRecentBackup: Backup | null;
}

export interface UploadResponse {
  id: string;
  originalFilename: string;
  encryptedSizeBytes: number;
  encryptionAlgorithm: string;
  createdAt: string;
  status: BackupStatus;
}

export type UploadStage = 'idle' | 'preparing' | 'encrypting' | 'uploading' | 'completed' | 'error';
