import { useState } from 'react';
import { ShieldCheck, Download, AlertTriangle, ArrowRight, CheckCircle2, Lock } from 'lucide-react';
import type { Backup } from '@/types/backup';
import { apiDownload } from '@/lib/api';
import { Button } from '@/components/ui/button';

interface RestoreModalProps {
  backup: Backup | null;
  onClose: () => void;
  onSuccess?: () => void;
}

export function RestoreModal({ backup, onClose }: RestoreModalProps) {
  const [restoring, setRestoring] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  if (!backup) return null;

  const handleRestore = async () => {
    setRestoring(true);
    setError(null);

    try {
      // Calls POST /api/backups/{id}/restore and triggers browser file download
      const { blob, filename } = await apiDownload(`/backups/${backup.id}/restore`, undefined, 'POST');
      
      // Create local object URL to trigger clean browser download
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = filename || backup.originalFilename;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);

      setSuccess(true);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Unable to restore this backup because its integrity check failed.';
      setError(message);
    } finally {
      setRestoring(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs">
      <div className="w-full max-w-lg bg-surface rounded-lg shadow-xl border border-border overflow-hidden">
        {/* Header */}
        <div className="flex items-center gap-3 px-6 py-4 border-b border-border bg-surface-secondary">
          <div className="flex items-center justify-center w-9 h-9 rounded-md bg-primary-light text-primary">
            <Download className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-base font-semibold text-text-primary">Restore & Decrypt Backup</h2>
            <p className="text-xs text-text-secondary">Verify authentication tag and restore plaintext document</p>
          </div>
        </div>

        {/* Content */}
        <div className="p-6 space-y-5">
          {error && (
            <div className="p-4 bg-danger-light border border-danger/20 rounded-lg flex items-start gap-3">
              <AlertTriangle className="w-5 h-5 text-danger shrink-0 mt-0.5" />
              <div className="text-sm">
                <p className="font-medium text-danger">Restoration Halted</p>
                <p className="text-text-secondary mt-1">{error}</p>
              </div>
            </div>
          )}

          {success ? (
            <div className="text-center py-4 space-y-3">
              <div className="inline-flex items-center justify-center w-12 h-12 bg-success-light rounded-full text-success">
                <CheckCircle2 className="w-7 h-7" />
              </div>
              <h3 className="text-base font-semibold text-text-primary">File Restored Successfully!</h3>
              <p className="text-sm text-text-secondary max-w-sm mx-auto">
                Authentication tag was verified. The restored file <span className="font-medium text-text-primary">{backup.originalFilename}</span> has been downloaded to your computer.
              </p>
              <div className="pt-2">
                <Button onClick={onClose} className="w-full">
                  Done
                </Button>
              </div>
            </div>
          ) : (
            <>
              {/* Target File Info */}
              <div className="p-3 bg-surface-secondary rounded-md border border-border">
                <div className="text-xs text-text-secondary mb-1">Target Document:</div>
                <div className="text-sm font-semibold text-text-primary">{backup.originalFilename}</div>
                <div className="text-xs text-text-muted mt-1 font-mono">ID: {backup.id}</div>
              </div>

              {/* Workflow Steps Explanation */}
              <div className="space-y-2">
                <div className="text-xs font-semibold text-text-secondary uppercase tracking-wider">
                  Secure Restoration Pipeline
                </div>
                <div className="p-3 bg-surface-secondary rounded-lg border border-border space-y-2 text-xs text-text-secondary">
                  <div className="flex items-center gap-2">
                    <span className="w-4 h-4 rounded-full bg-primary/10 text-primary flex items-center justify-center font-bold text-[10px]">1</span>
                    <span>Download encrypted ciphertext from private Supabase bucket</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="w-4 h-4 rounded-full bg-primary/10 text-primary flex items-center justify-center font-bold text-[10px]">2</span>
                    <span>Request KMS to unwrap the Data Encryption Key (DEK)</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="w-4 h-4 rounded-full bg-primary/10 text-primary flex items-center justify-center font-bold text-[10px]">3</span>
                    <span>Verify 128-bit AES-GCM authentication tag for tamper detection</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="w-4 h-4 rounded-full bg-primary/10 text-primary flex items-center justify-center font-bold text-[10px]">4</span>
                    <span>Stream verified original document to your browser</span>
                  </div>
                </div>
              </div>

              <div className="flex items-center gap-2 text-xs text-text-muted">
                <ShieldCheck className="w-4 h-4 text-success" />
                <span>If any byte has been tampered with, restoration fails automatically.</span>
              </div>

              {/* Actions */}
              <div className="flex justify-end gap-3 pt-2">
                <Button variant="outline" size="sm" onClick={onClose} disabled={restoring}>
                  Cancel
                </Button>
                <Button size="sm" onClick={handleRestore} disabled={restoring}>
                  {restoring ? (
                    <span className="flex items-center gap-2">
                      <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                      Verifying & Decrypting...
                    </span>
                  ) : (
                    <span className="flex items-center gap-1.5">
                      <Download className="w-4 h-4" />
                      Restore Original File
                    </span>
                  )}
                </Button>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
