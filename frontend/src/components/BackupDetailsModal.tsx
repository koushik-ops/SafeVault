import { X, ShieldCheck, Key, Hash, FileText, Calendar, HardDrive, Lock } from 'lucide-react';
import type { Backup } from '@/types/backup';
import { formatBytes, formatDate } from '@/lib/utils';
import { Button } from '@/components/ui/button';

interface BackupDetailsModalProps {
  backup: Backup | null;
  onClose: () => void;
}

export function BackupDetailsModal({ backup, onClose }: BackupDetailsModalProps) {
  if (!backup) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs">
      <div className="w-full max-w-2xl bg-surface rounded-lg shadow-xl border border-border overflow-hidden">
        {/* Modal Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-border bg-surface-secondary">
          <div className="flex items-center gap-3">
            <div className="flex items-center justify-center w-9 h-9 rounded-md bg-primary-light text-primary">
              <Lock className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base font-semibold text-text-primary">Cryptographic Backup Details</h2>
              <p className="text-xs text-text-secondary">Inspection of AES-256-GCM container metadata</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-md text-text-muted hover:text-text-primary hover:bg-surface-tertiary transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Content */}
        <div className="p-6 space-y-6 max-h-[75vh] overflow-y-auto">
          {/* Security Banner */}
          <div className="p-4 bg-info-light border border-info/20 rounded-lg flex items-start gap-3">
            <ShieldCheck className="w-5 h-5 text-info shrink-0 mt-0.5" />
            <div className="text-sm">
              <p className="font-medium text-text-primary">Zero Plaintext Cloud Storage Guarantee</p>
              <p className="text-text-secondary mt-0.5">
                Your backup is encrypted before storage. The cloud stores the encrypted data, not the original readable document. Plaintext data keys never touch disk or external networks.
              </p>
            </div>
          </div>

          {/* Core Properties Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="p-3 bg-surface-secondary rounded-md border border-border">
              <div className="flex items-center gap-2 text-xs text-text-secondary mb-1">
                <FileText className="w-3.5 h-3.5 text-primary" />
                <span>Original Filename</span>
              </div>
              <p className="text-sm font-medium text-text-primary truncate">{backup.originalFilename}</p>
            </div>

            <div className="p-3 bg-surface-secondary rounded-md border border-border">
              <div className="flex items-center gap-2 text-xs text-text-secondary mb-1">
                <Hash className="w-3.5 h-3.5 text-primary" />
                <span>Backup Unique Identifier (UUID)</span>
              </div>
              <p className="text-xs font-mono text-text-primary truncate">{backup.id}</p>
            </div>

            <div className="p-3 bg-surface-secondary rounded-md border border-border">
              <div className="flex items-center gap-2 text-xs text-text-secondary mb-1">
                <HardDrive className="w-3.5 h-3.5 text-primary" />
                <span>Original File Size</span>
              </div>
              <p className="text-sm font-medium text-text-primary">{formatBytes(backup.originalSizeBytes)}</p>
            </div>

            <div className="p-3 bg-surface-secondary rounded-md border border-border">
              <div className="flex items-center gap-2 text-xs text-text-secondary mb-1">
                <Lock className="w-3.5 h-3.5 text-primary" />
                <span>Encrypted Size (with GCM Tag)</span>
              </div>
              <p className="text-sm font-medium text-text-primary">
                {backup.encryptedSizeBytes ? formatBytes(backup.encryptedSizeBytes) : 'N/A'}
              </p>
            </div>

            <div className="p-3 bg-surface-secondary rounded-md border border-border">
              <div className="flex items-center gap-2 text-xs text-text-secondary mb-1">
                <Calendar className="w-3.5 h-3.5 text-primary" />
                <span>Creation Timestamp</span>
              </div>
              <p className="text-sm font-medium text-text-primary">{formatDate(backup.createdAt)}</p>
            </div>

            <div className="p-3 bg-surface-secondary rounded-md border border-border">
              <div className="flex items-center gap-2 text-xs text-text-secondary mb-1">
                <ShieldCheck className="w-3.5 h-3.5 text-primary" />
                <span>Container Format Version</span>
              </div>
              <p className="text-sm font-medium text-text-primary">Version {backup.formatVersion} (Standard Envelope)</p>
            </div>
          </div>

          {/* Cryptographic Details Section */}
          <div className="space-y-3">
            <h3 className="text-xs font-semibold text-text-secondary uppercase tracking-wider">
              Cryptographic Metadata
            </h3>

            <div className="space-y-2.5">
              <div className="p-3 rounded-md bg-surface-secondary border border-border">
                <div className="flex justify-between items-center mb-1">
                  <span className="text-xs font-medium text-text-secondary">Encryption Cipher</span>
                  <span className="text-xs px-2 py-0.5 rounded bg-success-light text-success font-medium">Verified JCA</span>
                </div>
                <p className="text-sm font-mono text-text-primary">{backup.encryptionAlgorithm} (256-bit Key)</p>
              </div>

              <div className="p-3 rounded-md bg-surface-secondary border border-border">
                <div className="flex justify-between items-center mb-1">
                  <span className="text-xs font-medium text-text-secondary">GCM Nonce / IV (96 bits)</span>
                  <span className="text-xs text-text-muted">Single-use cryptographically random</span>
                </div>
                <p className="text-xs font-mono text-text-primary break-all bg-surface p-1.5 rounded border border-border">
                  {backup.nonce}
                </p>
              </div>

              {backup.authenticationTag && (
                <div className="p-3 rounded-md bg-surface-secondary border border-border">
                  <div className="flex justify-between items-center mb-1">
                    <span className="text-xs font-medium text-text-secondary">GCM Authentication Tag (128 bits)</span>
                    <span className="text-xs text-text-muted">Tamper-detection checksum</span>
                  </div>
                  <p className="text-xs font-mono text-text-primary break-all bg-surface p-1.5 rounded border border-border">
                    {backup.authenticationTag}
                  </p>
                </div>
              )}

              <div className="p-3 rounded-md bg-surface-secondary border border-border">
                <div className="flex justify-between items-center mb-1">
                  <span className="text-xs font-medium text-text-secondary">Key Management Reference</span>
                  <span className="text-xs px-2 py-0.5 rounded bg-primary-light text-primary font-medium">Envelope Encryption</span>
                </div>
                <p className="text-xs font-mono text-text-primary break-all bg-surface p-1.5 rounded border border-border mb-2">
                  {backup.kmsKeyReference}
                </p>
                <div className="flex items-center gap-1.5 text-xs text-text-muted">
                  <Key className="w-3.5 h-3.5 text-warning" />
                  <span>Only the wrapped data key is stored. Secret key material is never exposed.</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Modal Footer */}
        <div className="flex justify-end px-6 py-3 border-t border-border bg-surface-secondary">
          <Button variant="outline" size="sm" onClick={onClose}>
            Close
          </Button>
        </div>
      </div>
    </div>
  );
}
