import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  FolderLock,
  Search,
  Filter,
  Eye,
  Download,
  Trash2,
  FileText,
  ShieldCheck,
  Lock,
  ChevronLeft,
  ChevronRight,
  ArrowUpDown
} from 'lucide-react';
import { apiRequest, apiDownload } from '@/lib/api';
import type { BackupListResponse, Backup } from '@/types/backup';
import { formatBytes, formatDate } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { BackupDetailsModal } from '@/components/BackupDetailsModal';
import { RestoreModal } from '@/components/RestoreModal';
import { DeleteConfirmModal } from '@/components/DeleteConfirmModal';

export default function BackupsPage() {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [sortOrder, setSortOrder] = useState<'desc' | 'asc'>('desc');

  // Modal States
  const [detailsBackup, setDetailsBackup] = useState<Backup | null>(null);
  const [restoreBackup, setRestoreBackup] = useState<Backup | null>(null);
  const [deleteBackup, setDeleteBackup] = useState<Backup | null>(null);

  // Fetch Backups
  const { data, isLoading, isError, error } = useQuery<BackupListResponse>({
    queryKey: ['backups', search, page, sortOrder],
    queryFn: () =>
      apiRequest<BackupListResponse>(
        `/backups?page=${page}&size=10&sort=createdAt,${sortOrder}${
          search ? `&search=${encodeURIComponent(search)}` : ''
        }`
      ),
  });

  // Delete Mutation
  const deleteMutation = useMutation({
    mutationFn: (backupId: string) =>
      apiRequest<void>(`/backups/${backupId}`, { method: 'DELETE' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['backups'] });
      queryClient.invalidateQueries({ queryKey: ['backup-summary'] });
      setDeleteBackup(null);
    },
  });

  const handleDownloadEncrypted = async (backup: Backup) => {
    try {
      const { blob, filename } = await apiDownload(`/backups/${backup.id}/download-encrypted`);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = filename || `${backup.originalFilename}.enc`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (err) {
      console.error('Download encrypted file failed', err);
    }
  };

  const toggleSort = () => {
    setSortOrder((prev) => (prev === 'desc' ? 'asc' : 'desc'));
    setPage(0);
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div>
        <h1 className="text-2xl font-bold text-text-primary">My Backups</h1>
        <p className="text-sm text-text-secondary mt-0.5">
          View, restore, inspect cryptographic properties, and manage your encrypted archives.
        </p>
      </div>

      <Card>
        <CardHeader className="pb-4">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
            <div className="relative flex-1 max-w-sm">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-text-muted" />
              <Input
                placeholder="Search by filename..."
                value={search}
                onChange={(e) => {
                  setSearch(e.target.value);
                  setPage(0);
                }}
                className="pl-9 h-9"
              />
            </div>

            <div className="flex items-center gap-2">
              <Button variant="outline" size="sm" onClick={toggleSort} className="h-9 gap-1.5 text-xs">
                <ArrowUpDown className="w-3.5 h-3.5" />
                Sort: {sortOrder === 'desc' ? 'Newest First' : 'Oldest First'}
              </Button>
            </div>
          </div>
        </CardHeader>

        <CardContent>
          {isLoading ? (
            <div className="space-y-3 py-4">
              {[1, 2, 3, 4, 5].map((i) => (
                <div key={i} className="h-12 skeleton rounded-md" />
              ))}
            </div>
          ) : isError ? (
            <div className="text-center py-8 text-danger text-sm">
              Failed to load backups. {(error as Error)?.message || 'Please try again.'}
            </div>
          ) : data && data.content.length > 0 ? (
            <div className="space-y-4">
              {/* Desktop Table View */}
              <div className="hidden md:block overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead>
                    <tr className="border-b border-border text-xs font-semibold text-text-secondary uppercase">
                      <th className="pb-3 px-3">File Name</th>
                      <th className="pb-3 px-3">Original Size</th>
                      <th className="pb-3 px-3">Encrypted Size</th>
                      <th className="pb-3 px-3">Created Date</th>
                      <th className="pb-3 px-3">Encryption</th>
                      <th className="pb-3 px-3 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border">
                    {data.content.map((backup) => (
                      <tr key={backup.id} className="hover:bg-surface-secondary transition-colors">
                        <td className="py-3 px-3 font-medium text-text-primary">
                          <div className="flex items-center gap-2.5 max-w-xs">
                            <FileText className="w-4 h-4 text-primary shrink-0" />
                            <span className="truncate" title={backup.originalFilename}>
                              {backup.originalFilename}
                            </span>
                          </div>
                        </td>
                        <td className="py-3 px-3 text-text-secondary text-xs">
                          {formatBytes(backup.originalSizeBytes)}
                        </td>
                        <td className="py-3 px-3 text-text-secondary text-xs">
                          {backup.encryptedSizeBytes ? formatBytes(backup.encryptedSizeBytes) : 'N/A'}
                        </td>
                        <td className="py-3 px-3 text-text-secondary text-xs">
                          {formatDate(backup.createdAt)}
                        </td>
                        <td className="py-3 px-3">
                          <span className="inline-flex items-center gap-1 text-[11px] font-medium text-success bg-success-light px-2 py-0.5 rounded-full">
                            <ShieldCheck className="w-3 h-3" />
                            AES-256-GCM
                          </span>
                        </td>
                        <td className="py-3 px-3 text-right">
                          <div className="flex items-center justify-end gap-1">
                            <Button
                              variant="ghost"
                              size="sm"
                              className="h-8 px-2 text-xs"
                              onClick={() => setDetailsBackup(backup)}
                              title="Inspect cryptographic metadata"
                            >
                              <Eye className="w-3.5 h-3.5 mr-1" />
                              Details
                            </Button>
                            <Button
                              variant="outline"
                              size="sm"
                              className="h-8 px-2.5 text-xs text-primary"
                              onClick={() => setRestoreBackup(backup)}
                              title="Restore and decrypt plaintext file"
                            >
                              <Download className="w-3.5 h-3.5 mr-1" />
                              Restore
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              className="h-8 px-2 text-xs text-text-muted hover:text-text-primary"
                              onClick={() => handleDownloadEncrypted(backup)}
                              title="Download raw encrypted ciphertext (.enc)"
                            >
                              <Lock className="w-3.5 h-3.5" />
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              className="h-8 px-2 text-xs text-danger hover:bg-danger-light"
                              onClick={() => setDeleteBackup(backup)}
                              title="Delete backup"
                            >
                              <Trash2 className="w-3.5 h-3.5" />
                            </Button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* Mobile Card List View */}
              <div className="md:hidden space-y-3">
                {data.content.map((backup) => (
                  <div key={backup.id} className="p-4 bg-surface-secondary rounded-lg border border-border space-y-3">
                    <div className="flex items-start justify-between gap-2">
                      <div className="flex items-center gap-2 min-w-0">
                        <FileText className="w-4 h-4 text-primary shrink-0" />
                        <span className="font-medium text-sm text-text-primary truncate">{backup.originalFilename}</span>
                      </div>
                      <span className="text-[10px] text-success bg-success-light px-1.5 py-0.5 rounded font-medium shrink-0">
                        AES-256
                      </span>
                    </div>

                    <div className="grid grid-cols-2 gap-2 text-xs text-text-secondary">
                      <div>Size: {formatBytes(backup.originalSizeBytes)}</div>
                      <div>Date: {formatDate(backup.createdAt)}</div>
                    </div>

                    <div className="flex items-center justify-between pt-2 border-t border-border">
                      <Button variant="ghost" size="sm" className="h-7 text-xs px-2" onClick={() => setDetailsBackup(backup)}>
                        Details
                      </Button>
                      <div className="flex gap-1">
                        <Button variant="outline" size="sm" className="h-7 text-xs px-2 text-primary" onClick={() => setRestoreBackup(backup)}>
                          Restore
                        </Button>
                        <Button variant="ghost" size="sm" className="h-7 text-xs px-2 text-danger" onClick={() => setDeleteBackup(backup)}>
                          <Trash2 className="w-3.5 h-3.5" />
                        </Button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>

              {/* Pagination Controls */}
              <div className="flex items-center justify-between pt-4 border-t border-border text-xs text-text-secondary">
                <div>
                  Showing {data.number * data.size + 1} to{' '}
                  {Math.min((data.number + 1) * data.size, data.totalElements)} of {data.totalElements} backups
                </div>

                <div className="flex items-center gap-1">
                  <Button
                    variant="outline"
                    size="sm"
                    className="h-8 px-2.5"
                    disabled={data.first}
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                  >
                    <ChevronLeft className="w-4 h-4" />
                  </Button>
                  <span className="px-2 font-medium text-text-primary">
                    Page {data.number + 1} of {Math.max(1, data.totalPages)}
                  </span>
                  <Button
                    variant="outline"
                    size="sm"
                    className="h-8 px-2.5"
                    disabled={data.last}
                    onClick={() => setPage((p) => p + 1)}
                  >
                    <ChevronRight className="w-4 h-4" />
                  </Button>
                </div>
              </div>
            </div>
          ) : (
            /* Empty Search or Initial State */
            <div className="text-center py-12">
              <FolderLock className="w-10 h-10 text-text-muted mx-auto mb-2" />
              <h3 className="text-sm font-semibold text-text-primary">No backups found</h3>
              <p className="text-xs text-text-secondary mt-1">
                {search ? 'Try clearing your search query.' : 'Upload confidential documents to view them here.'}
              </p>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Modals */}
      <BackupDetailsModal backup={detailsBackup} onClose={() => setDetailsBackup(null)} />
      <RestoreModal backup={restoreBackup} onClose={() => setRestoreBackup(null)} />
      <DeleteConfirmModal
        backup={deleteBackup}
        deleting={deleteMutation.isPending}
        onConfirm={() => deleteBackup && deleteMutation.mutate(deleteBackup.id)}
        onClose={() => setDeleteBackup(null)}
      />
    </div>
  );
}
