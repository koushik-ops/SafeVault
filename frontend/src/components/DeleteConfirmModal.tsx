import { Trash2, AlertTriangle } from 'lucide-react';
import type { Backup } from '@/types/backup';
import { Button } from '@/components/ui/button';

interface DeleteConfirmModalProps {
  backup: Backup | null;
  deleting: boolean;
  onConfirm: () => void;
  onClose: () => void;
}

export function DeleteConfirmModal({ backup, deleting, onConfirm, onClose }: DeleteConfirmModalProps) {
  if (!backup) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs">
      <div className="w-full max-w-md bg-surface rounded-lg shadow-xl border border-border overflow-hidden">
        <div className="flex items-center gap-3 px-6 py-4 border-b border-border bg-danger-light text-danger">
          <AlertTriangle className="w-5 h-5 shrink-0" />
          <h2 className="text-base font-semibold">Delete Encrypted Backup?</h2>
        </div>

        <div className="p-6 space-y-4">
          <p className="text-sm text-text-secondary">
            Are you sure you want to delete <span className="font-semibold text-text-primary">{backup.originalFilename}</span>?
          </p>

          <div className="p-3 bg-surface-secondary rounded-md border border-border text-xs text-text-secondary space-y-1">
            <p>• The encrypted object will be permanently removed from Supabase Storage.</p>
            <p>• The cryptographic metadata and backup record will be marked deleted.</p>
            <p className="font-medium text-danger mt-1">• This action cannot be undone.</p>
          </div>

          <div className="flex justify-end gap-3 pt-2">
            <Button variant="outline" size="sm" onClick={onClose} disabled={deleting}>
              Cancel
            </Button>
            <Button variant="destructive" size="sm" onClick={onConfirm} disabled={deleting}>
              {deleting ? (
                <span className="flex items-center gap-2">
                  <span className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  Deleting...
                </span>
              ) : (
                <span className="flex items-center gap-1.5">
                  <Trash2 className="w-4 h-4" />
                  Confirm Delete
                </span>
              )}
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
