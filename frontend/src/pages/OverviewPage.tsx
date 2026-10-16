import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  FolderLock,
  HardDrive,
  Clock,
  ShieldCheck,
  Upload,
  ArrowRight,
  FileText,
  Eye,
  Download,
  Lock,
  Sparkles,
  Info
} from 'lucide-react';
import { apiRequest, apiDownload } from '@/lib/api';
import type { BackupSummary, BackupListResponse, Backup } from '@/types/backup';
import { formatBytes, formatDate } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { BackupDetailsModal } from '@/components/BackupDetailsModal';
import { RestoreModal } from '@/components/RestoreModal';

export default function OverviewPage() {
  const navigate = useNavigate();
  const [selectedBackupForDetails, setSelectedBackupForDetails] = useState<Backup | null>(null);
  const [selectedBackupForRestore, setSelectedBackupForRestore] = useState<Backup | null>(null);

  // Fetch summary metrics
  const { data: summary, isLoading: loadingSummary } = useQuery<BackupSummary>({
    queryKey: ['backup-summary'],
    queryFn: () => apiRequest<BackupSummary>('/backups/summary'),
  });

  // Fetch recent 5 backups
  const { data: recentData, isLoading: loadingRecent } = useQuery<BackupListResponse>({
    queryKey: ['recent-backups'],
    queryFn: () => apiRequest<BackupListResponse>('/backups?page=0&size=5&sort=createdAt,desc'),
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
      console.error('Download failed', err);
    }
  };

  const totalBackups = summary?.totalBackups ?? 0;
  const totalEncryptedBytes = summary?.totalEncryptedBytes ?? 0;
  const mostRecent = summary?.mostRecentBackup;

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-text-primary">Dashboard Overview</h1>
          <p className="text-sm text-text-secondary mt-0.5">
            Manage your encrypted financial documents and monitor storage health.
          </p>
        </div>
        <Link to="/upload">
          <Button className="gap-2 shadow-sm">
            <Upload className="w-4 h-4" />
            Upload New Backup
          </Button>
        </Link>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0">
            <CardTitle className="text-xs font-semibold text-text-secondary uppercase tracking-wider">
              Total Backups
            </CardTitle>
            <div className="p-2 bg-primary-light rounded-md text-primary">
              <FolderLock className="w-4 h-4" />
            </div>
          </CardHeader>
          <CardContent>
            {loadingSummary ? (
              <div className="h-8 w-16 skeleton" />
            ) : (
              <div className="text-2xl font-bold text-text-primary">{totalBackups}</div>
            )}
            <p className="text-xs text-text-muted mt-1">Encrypted documents stored</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0">
            <CardTitle className="text-xs font-semibold text-text-secondary uppercase tracking-wider">
              Encrypted Storage
            </CardTitle>
            <div className="p-2 bg-info-light rounded-md text-info">
              <HardDrive className="w-4 h-4" />
            </div>
          </CardHeader>
          <CardContent>
            {loadingSummary ? (
              <div className="h-8 w-24 skeleton" />
            ) : (
              <div className="text-2xl font-bold text-text-primary">{formatBytes(totalEncryptedBytes)}</div>
            )}
            <p className="text-xs text-text-muted mt-1">Private Supabase bucket</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0">
            <CardTitle className="text-xs font-semibold text-text-secondary uppercase tracking-wider">
              Most Recent Backup
            </CardTitle>
            <div className="p-2 bg-surface-tertiary rounded-md text-text-secondary">
              <Clock className="w-4 h-4" />
            </div>
          </CardHeader>
          <CardContent>
            {loadingSummary ? (
              <div className="h-8 w-32 skeleton" />
            ) : mostRecent ? (
              <div className="text-sm font-semibold text-text-primary truncate" title={mostRecent.originalFilename}>
                {mostRecent.originalFilename}
              </div>
            ) : (
              <div className="text-sm text-text-muted">No backups yet</div>
            )}
            <p className="text-xs text-text-muted mt-1">
              {mostRecent ? formatDate(mostRecent.createdAt) : 'Upload your first document'}
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0">
            <CardTitle className="text-xs font-semibold text-text-secondary uppercase tracking-wider">
              Encryption Status
            </CardTitle>
            <div className="p-2 bg-success-light rounded-md text-success">
              <ShieldCheck className="w-4 h-4" />
            </div>
          </CardHeader>
          <CardContent>
            <div className="flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-full bg-success animate-pulse" />
              <span className="text-base font-semibold text-text-primary">AES-256-GCM</span>
            </div>
            <p className="text-xs text-text-muted mt-1">AWS KMS Envelope Protected</p>
          </CardContent>
        </Card>
      </div>

      {/* Main Grid: Recent Backups & How It Works */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Recent Backups (2 Cols) */}
        <div className="lg:col-span-2 space-y-4">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between">
              <div>
                <CardTitle className="text-base">Recent Backups</CardTitle>
                <CardDescription>Your latest securely archived files</CardDescription>
              </div>
              <Link to="/backups" className="text-xs text-primary hover:underline font-medium flex items-center gap-1">
                View all <ArrowRight className="w-3 h-3" />
              </Link>
            </CardHeader>
            <CardContent>
              {loadingRecent ? (
                <div className="space-y-3">
                  {[1, 2, 3].map((i) => (
                    <div key={i} className="h-14 skeleton rounded-md" />
                  ))}
                </div>
              ) : recentData && recentData.content.length > 0 ? (
                <div className="divide-y divide-border">
                  {recentData.content.map((backup) => (
                    <div
                      key={backup.id}
                      className="py-3 flex items-center justify-between gap-4 hover:bg-surface-secondary px-2 rounded-md transition-colors"
                    >
                      <div className="flex items-center gap-3 min-w-0">
                        <div className="flex items-center justify-center w-8 h-8 rounded-md bg-primary-light text-primary shrink-0">
                          <FileText className="w-4 h-4" />
                        </div>
                        <div className="min-w-0">
                          <p className="text-sm font-medium text-text-primary truncate">
                            {backup.originalFilename}
                          </p>
                          <div className="flex items-center gap-2 text-xs text-text-muted mt-0.5">
                            <span>{formatBytes(backup.originalSizeBytes)}</span>
                            <span>•</span>
                            <span>{formatDate(backup.createdAt)}</span>
                          </div>
                        </div>
                      </div>

                      <div className="flex items-center gap-1.5 shrink-0">
                        <Button
                          variant="ghost"
                          size="sm"
                          className="h-8 px-2 text-xs"
                          onClick={() => setSelectedBackupForDetails(backup)}
                          title="View cryptographic details"
                        >
                          <Eye className="w-3.5 h-3.5 mr-1" />
                          Details
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          className="h-8 px-2.5 text-xs text-primary"
                          onClick={() => setSelectedBackupForRestore(backup)}
                        >
                          <Download className="w-3.5 h-3.5 mr-1" />
                          Restore
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                /* Empty state for first time users */
                <div className="text-center py-10 px-4">
                  <div className="inline-flex items-center justify-center w-12 h-12 rounded-full bg-primary-light text-primary mb-3">
                    <Sparkles className="w-6 h-6" />
                  </div>
                  <h3 className="text-base font-semibold text-text-primary">No backups yet</h3>
                  <p className="text-sm text-text-secondary max-w-sm mx-auto mt-1 mb-4">
                    Protect your confidential financial documents with military-grade AES-256-GCM encryption before storing in the cloud.
                  </p>
                  <Button onClick={() => navigate('/upload')} size="sm">
                    <Upload className="w-4 h-4 mr-2" />
                    Upload Your First Document
                  </Button>
                </div>
              )}
            </CardContent>
          </Card>
        </div>

        {/* How It Works & Security Status (1 Col) */}
        <div className="space-y-6">
          {/* How SecureVault Works */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base flex items-center gap-2">
                <Info className="w-4 h-4 text-primary" />
                How SecureVault Works
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-3 text-xs text-text-secondary">
              <div className="flex gap-2.5">
                <div className="w-5 h-5 rounded-full bg-primary-light text-primary flex items-center justify-center font-bold text-[10px] shrink-0">
                  1
                </div>
                <div>
                  <strong className="text-text-primary">Client-to-Server Transport:</strong> Your confidential file is uploaded over TLS/HTTPS directly to the authenticated backend.
                </div>
              </div>

              <div className="flex gap-2.5">
                <div className="w-5 h-5 rounded-full bg-primary-light text-primary flex items-center justify-center font-bold text-[10px] shrink-0">
                  2
                </div>
                <div>
                  <strong className="text-text-primary">Envelope Key Generation:</strong> AWS KMS issues a fresh 256-bit data key in server memory.
                </div>
              </div>

              <div className="flex gap-2.5">
                <div className="w-5 h-5 rounded-full bg-primary-light text-primary flex items-center justify-center font-bold text-[10px] shrink-0">
                  3
                </div>
                <div>
                  <strong className="text-text-primary">AES-256-GCM Ciphering:</strong> The file is encrypted with a unique 96-bit nonce and 128-bit integrity tag. The plaintext key is wiped immediately.
                </div>
              </div>

              <div className="flex gap-2.5">
                <div className="w-5 h-5 rounded-full bg-primary-light text-primary flex items-center justify-center font-bold text-[10px] shrink-0">
                  4
                </div>
                <div>
                  <strong className="text-text-primary">Private Cloud Storage:</strong> Only the encrypted ciphertext is saved in private Supabase Storage.
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Security Status Box */}
          <Card className="bg-surface-secondary border-dashed border-border">
            <CardContent className="pt-6 space-y-2">
              <div className="flex items-center gap-2 text-sm font-semibold text-text-primary">
                <Lock className="w-4 h-4 text-primary" />
                <span>Zero-Knowledge Storage</span>
              </div>
              <p className="text-xs text-text-secondary leading-relaxed">
                Even if cloud storage infrastructure were compromised, attackers cannot read your financial documents without your authorized backend KMS session.
              </p>
              <Link to="/security" className="inline-block pt-1 text-xs text-primary font-medium hover:underline">
                Read full cryptographic architecture →
              </Link>
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Modals */}
      <BackupDetailsModal
        backup={selectedBackupForDetails}
        onClose={() => setSelectedBackupForDetails(null)}
      />

      <RestoreModal
        backup={selectedBackupForRestore}
        onClose={() => setSelectedBackupForRestore(null)}
      />
    </div>
  );
}
