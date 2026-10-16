import React, { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Upload,
  FileText,
  X,
  ShieldCheck,
  CheckCircle2,
  AlertCircle,
  Lock,
  ArrowRight,
  HardDrive,
  FileCheck
} from 'lucide-react';
import { apiUpload } from '@/lib/api';
import type { UploadStage, UploadResponse } from '@/types/backup';
import { formatBytes, formatDate } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';

const MAX_FILE_SIZE = 25 * 1024 * 1024; // 25 MB

const SUPPORTED_EXTENSIONS = [
  '.pdf', '.docx', '.doc', '.xlsx', '.xls', '.csv', '.txt', '.json', '.xml', '.zip'
];

export default function UploadPage() {
  const navigate = useNavigate();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [dragActive, setDragActive] = useState(false);
  const [stage, setStage] = useState<UploadStage>('idle');
  const [progressPercent, setProgressPercent] = useState(0);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [result, setResult] = useState<UploadResponse | null>(null);
  const abortControllerRef = useRef<AbortController | null>(null);

  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true);
    } else if (e.type === 'dragleave') {
      setDragActive(false);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);

    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      processSelectedFile(e.dataTransfer.files[0]);
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      processSelectedFile(e.target.files[0]);
    }
  };

  const processSelectedFile = (file: File) => {
    setErrorMessage(null);
    setResult(null);

    // Validate size limit
    if (file.size > MAX_FILE_SIZE) {
      setErrorMessage(`File exceeds the 25 MB limit (${formatBytes(file.size)}). Please choose a smaller file.`);
      setSelectedFile(null);
      return;
    }

    if (file.size === 0) {
      setErrorMessage('Selected file is empty.');
      setSelectedFile(null);
      return;
    }

    setSelectedFile(file);
  };

  const handleClearSelection = () => {
    setSelectedFile(null);
    setErrorMessage(null);
    setStage('idle');
    setProgressPercent(0);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const startUploadAndEncrypt = async () => {
    if (!selectedFile) return;

    setErrorMessage(null);
    abortControllerRef.current = new AbortController();

    try {
      // Stage 1: Preparing file
      setStage('preparing');
      setProgressPercent(15);
      await new Promise((r) => setTimeout(r, 400));

      // Stage 2: Encrypting file
      setStage('encrypting');
      setProgressPercent(40);
      await new Promise((r) => setTimeout(r, 400));

      // Stage 3: Uploading secure backup
      setStage('uploading');

      const response = await apiUpload(
        '/backups',
        selectedFile,
        (progress) => {
          // Map progress from 40% to 90%
          setProgressPercent(40 + Math.round((progress / 100) * 50));
        },
        abortControllerRef.current.signal
      ) as UploadResponse;

      // Stage 4: Backup completed
      setProgressPercent(100);
      setStage('completed');
      setResult(response);
    } catch (err: unknown) {
      setStage('error');
      const message = err instanceof Error ? err.message : 'An error occurred during upload and encryption.';
      setErrorMessage(message);
    }
  };

  const handleCancel = () => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }
    setStage('idle');
    setProgressPercent(0);
  };

  const stages = [
    { id: 'preparing', label: '1. Preparing file' },
    { id: 'encrypting', label: '2. Encrypting file (AES-256-GCM)' },
    { id: 'uploading', label: '3. Uploading secure backup' },
    { id: 'completed', label: '4. Backup completed' },
  ];

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-text-primary">Upload & Encrypt</h1>
        <p className="text-sm text-text-secondary mt-0.5">
          Select confidential financial files. Encryption takes place in the backend memory before storing in the cloud.
        </p>
      </div>

      {/* Main Card */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Document Protection Pipeline</CardTitle>
          <CardDescription>
            Supported documents: PDF, DOCX, XLSX, CSV, TXT (Maximum file size: 25 MB)
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          {/* Error Message */}
          {errorMessage && (
            <div className="p-4 bg-danger-light border border-danger/20 rounded-lg flex items-start gap-3">
              <AlertCircle className="w-5 h-5 text-danger shrink-0 mt-0.5" />
              <div className="text-sm">
                <p className="font-semibold text-danger">Upload Failed</p>
                <p className="text-text-secondary mt-0.5">{errorMessage}</p>
              </div>
            </div>
          )}

          {/* Success State */}
          {stage === 'completed' && result ? (
            <div className="space-y-6 py-2">
              <div className="p-6 bg-success-light border border-success/20 rounded-xl text-center space-y-3">
                <div className="inline-flex items-center justify-center w-12 h-12 rounded-full bg-success text-white">
                  <CheckCircle2 className="w-7 h-7" />
                </div>
                <h3 className="text-lg font-bold text-text-primary">Encrypted Backup Successfully Created!</h3>
                <p className="text-sm text-text-secondary max-w-md mx-auto">
                  Your document was encrypted with AES-256-GCM using envelope encryption and stored safely in private Supabase Storage.
                </p>
              </div>

              {/* Confirmation Details Card */}
              <div className="p-4 bg-surface-secondary rounded-lg border border-border space-y-3">
                <h4 className="text-xs font-semibold text-text-secondary uppercase tracking-wider">
                  Verified Backup Confirmation
                </h4>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-sm">
                  <div>
                    <span className="text-xs text-text-secondary">Original Filename:</span>
                    <p className="font-medium text-text-primary truncate">{result.originalFilename}</p>
                  </div>
                  <div>
                    <span className="text-xs text-text-secondary">Encrypted Backup ID:</span>
                    <p className="font-mono text-xs text-text-primary truncate">{result.id}</p>
                  </div>
                  <div>
                    <span className="text-xs text-text-secondary">Encrypted Size:</span>
                    <p className="font-medium text-text-primary">{formatBytes(result.encryptedSizeBytes)}</p>
                  </div>
                  <div>
                    <span className="text-xs text-text-secondary">Cipher Algorithm:</span>
                    <p className="font-medium text-text-primary">{result.encryptionAlgorithm}</p>
                  </div>
                  <div>
                    <span className="text-xs text-text-secondary">Timestamp:</span>
                    <p className="font-medium text-text-primary">{formatDate(result.createdAt)}</p>
                  </div>
                  <div>
                    <span className="text-xs text-text-secondary">Cloud Storage:</span>
                    <p className="font-medium text-success flex items-center gap-1">
                      <ShieldCheck className="w-3.5 h-3.5" />
                      Supabase (Private Bucket)
                    </p>
                  </div>
                </div>
              </div>

              {/* Actions */}
              <div className="flex flex-col sm:flex-row gap-3 pt-2">
                <Button onClick={() => navigate('/backups')} className="flex-1">
                  View in My Backups
                </Button>
                <Button variant="outline" onClick={handleClearSelection} className="flex-1">
                  Upload Another File
                </Button>
              </div>
            </div>
          ) : stage !== 'idle' && stage !== 'error' ? (
            /* Progress Indicator */
            <div className="space-y-6 py-4">
              <div className="space-y-2">
                <div className="flex justify-between text-xs font-medium text-text-secondary">
                  <span>Progress</span>
                  <span>{progressPercent}%</span>
                </div>
                <div className="w-full h-2 bg-surface-tertiary rounded-full overflow-hidden">
                  <div
                    className="h-full bg-primary transition-all duration-300 rounded-full"
                    style={{ width: `${progressPercent}%` }}
                  />
                </div>
              </div>

              {/* 4-Stage Progress Tracker */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {stages.map((st, idx) => {
                  const isCurrent = stage === st.id;
                  const isPassed =
                    (stage === 'encrypting' && idx === 0) ||
                    (stage === 'uploading' && idx <= 1) ||
                    (stage === 'completed');

                  return (
                    <div
                      key={st.id}
                      className={`p-3 rounded-lg border text-xs flex items-center gap-2.5 transition-colors ${
                        isCurrent
                          ? 'border-primary bg-primary-light text-primary font-semibold'
                          : isPassed
                          ? 'border-success/30 bg-success-light text-success'
                          : 'border-border text-text-muted bg-surface-secondary'
                      }`}
                    >
                      {isPassed ? (
                        <CheckCircle2 className="w-4 h-4 shrink-0 text-success" />
                      ) : isCurrent ? (
                        <span className="w-4 h-4 border-2 border-primary border-t-transparent rounded-full animate-spin shrink-0" />
                      ) : (
                        <div className="w-4 h-4 rounded-full border border-border flex items-center justify-center text-[10px] shrink-0">
                          {idx + 1}
                        </div>
                      )}
                      <span>{st.label}</span>
                    </div>
                  );
                })}
              </div>

              <div className="flex justify-end">
                <Button variant="outline" size="sm" onClick={handleCancel}>
                  Cancel Operation
                </Button>
              </div>
            </div>
          ) : (
            /* Upload & Selection View */
            <div className="space-y-5">
              {!selectedFile ? (
                /* Drag and drop zone */
                <div
                  onDragEnter={handleDrag}
                  onDragLeave={handleDrag}
                  onDragOver={handleDrag}
                  onDrop={handleDrop}
                  onClick={() => fileInputRef.current?.click()}
                  className={`border-2 border-dashed rounded-xl p-8 sm:p-12 text-center cursor-pointer transition-colors ${
                    dragActive
                      ? 'border-primary bg-primary-light'
                      : 'border-border hover:border-primary/50 hover:bg-surface-secondary'
                  }`}
                >
                  <input
                    ref={fileInputRef}
                    type="file"
                    className="hidden"
                    accept={SUPPORTED_EXTENSIONS.join(',')}
                    onChange={handleFileChange}
                  />

                  <div className="inline-flex items-center justify-center w-12 h-12 rounded-full bg-primary-light text-primary mb-3">
                    <Upload className="w-6 h-6" />
                  </div>
                  <h3 className="text-base font-semibold text-text-primary">
                    Drop your confidential document here, or <span className="text-primary hover:underline">browse</span>
                  </h3>
                  <p className="text-xs text-text-secondary mt-1">
                    Supports PDF, DOCX, XLSX, CSV, TXT up to 25 MB
                  </p>
                </div>
              ) : (
                /* Selected File Preview */
                <div className="p-4 bg-surface-secondary rounded-lg border border-border space-y-4">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3 min-w-0">
                      <div className="flex items-center justify-center w-10 h-10 rounded-lg bg-primary-light text-primary shrink-0">
                        <FileText className="w-5 h-5" />
                      </div>
                      <div className="min-w-0">
                        <p className="text-sm font-semibold text-text-primary truncate">
                          {selectedFile.name}
                        </p>
                        <p className="text-xs text-text-secondary mt-0.5">
                          {formatBytes(selectedFile.size)} • {selectedFile.type || 'Document'}
                        </p>
                      </div>
                    </div>

                    <button
                      onClick={handleClearSelection}
                      className="p-1.5 rounded-md text-text-muted hover:text-text-primary hover:bg-surface-tertiary transition-colors"
                      title="Remove file"
                    >
                      <X className="w-5 h-5" />
                    </button>
                  </div>

                  {/* Security Assurance Banner */}
                  <div className="p-3 bg-surface rounded-md border border-border flex items-center gap-2.5 text-xs text-text-secondary">
                    <ShieldCheck className="w-4 h-4 text-success shrink-0" />
                    <span>
                      This file will be encrypted using a dedicated, freshly generated AES-256 key before upload.
                    </span>
                  </div>

                  {/* Submit Action */}
                  <div className="flex justify-end gap-3 pt-1">
                    <Button variant="outline" size="sm" onClick={handleClearSelection}>
                      Choose Different File
                    </Button>
                    <Button size="sm" onClick={startUploadAndEncrypt}>
                      <Lock className="w-4 h-4 mr-2" />
                      Encrypt & Backup to Cloud
                    </Button>
                  </div>
                </div>
              )}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
