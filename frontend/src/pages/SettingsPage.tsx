import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/hooks/useAuth';
import {
  User,
  Mail,
  Shield,
  Key,
  LogOut,
  CheckCircle2,
  AlertCircle,
  Clock,
  Lock
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { formatDate } from '@/lib/utils';

export default function SettingsPage() {
  const { user, signOut, resetPassword } = useAuth();
  const navigate = useNavigate();

  const [resetRequested, setResetRequested] = useState(false);
  const [resetError, setResetError] = useState<string | null>(null);
  const [resetLoading, setResetLoading] = useState(false);

  const handleSignOut = async () => {
    await signOut();
    navigate('/login');
  };

  const handlePasswordReset = async () => {
    if (!user?.email) return;
    setResetLoading(true);
    setResetError(null);
    const { error } = await resetPassword(user.email);
    if (error) {
      setResetError(error.message);
    } else {
      setResetRequested(true);
    }
    setResetLoading(false);
  };

  return (
    <div className="space-y-6 max-w-3xl mx-auto">
      <div>
        <h1 className="text-2xl font-bold text-text-primary">Account Settings</h1>
        <p className="text-sm text-text-secondary mt-0.5">
          Manage your SecureVault account credentials and authentication session.
        </p>
      </div>

      {/* Account Info Card */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <User className="w-4 h-4 text-primary" />
            Account Information
          </CardTitle>
          <CardDescription>Details associated with your authenticated Supabase identity</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="p-3 bg-surface-secondary rounded-lg border border-border">
              <div className="text-xs text-text-secondary mb-1 flex items-center gap-1.5">
                <Mail className="w-3.5 h-3.5 text-text-muted" />
                <span>Primary Email Address</span>
              </div>
              <p className="text-sm font-semibold text-text-primary">{user?.email || 'N/A'}</p>
            </div>

            <div className="p-3 bg-surface-secondary rounded-lg border border-border">
              <div className="text-xs text-text-secondary mb-1 flex items-center gap-1.5">
                <Shield className="w-3.5 h-3.5 text-text-muted" />
                <span>Authentication Provider</span>
              </div>
              <p className="text-sm font-semibold text-text-primary capitalize">
                {user?.app_metadata?.provider || 'Supabase Auth (JWT)'}
              </p>
            </div>

            <div className="p-3 bg-surface-secondary rounded-lg border border-border">
              <div className="text-xs text-text-secondary mb-1 flex items-center gap-1.5">
                <Clock className="w-3.5 h-3.5 text-text-muted" />
                <span>Account Created</span>
              </div>
              <p className="text-sm font-semibold text-text-primary">
                {user?.created_at ? formatDate(user.created_at) : 'N/A'}
              </p>
            </div>

            <div className="p-3 bg-surface-secondary rounded-lg border border-border">
              <div className="text-xs text-text-secondary mb-1 flex items-center gap-1.5">
                <Lock className="w-3.5 h-3.5 text-text-muted" />
                <span>Security Token Status</span>
              </div>
              <p className="text-sm font-semibold text-success flex items-center gap-1">
                <span className="w-2 h-2 rounded-full bg-success" />
                Verified & Active
              </p>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Password & Security Card */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <Key className="w-4 h-4 text-primary" />
            Password & Security
          </CardTitle>
          <CardDescription>Update your credentials or reset your master login password</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {resetRequested ? (
            <div className="p-4 bg-success-light border border-success/20 rounded-lg flex items-center gap-3 text-xs text-success">
              <CheckCircle2 className="w-5 h-5 shrink-0" />
              <span>
                Password reset instructions have been sent to <strong>{user?.email}</strong>. Please check your inbox.
              </span>
            </div>
          ) : (
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-4 bg-surface-secondary rounded-lg border border-border">
              <div>
                <p className="text-sm font-medium text-text-primary">Reset Account Password</p>
                <p className="text-xs text-text-secondary mt-0.5">
                  Sends a secure Supabase authentication link to your registered email address.
                </p>
              </div>
              <Button
                variant="outline"
                size="sm"
                onClick={handlePasswordReset}
                disabled={resetLoading}
              >
                {resetLoading ? 'Sending...' : 'Send Reset Link'}
              </Button>
            </div>
          )}

          {resetError && (
            <div className="p-3 bg-danger-light border border-danger/20 rounded-lg flex items-center gap-2 text-xs text-danger">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{resetError}</span>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Session Management */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <LogOut className="w-4 h-4 text-danger" />
            Session Termination
          </CardTitle>
          <CardDescription>Safely invalidate your current session tokens on this browser</CardDescription>
        </CardHeader>
        <CardContent className="flex justify-between items-center">
          <p className="text-xs text-text-secondary">
            Signing out clears your local access tokens. Backups remain securely encrypted in the cloud.
          </p>
          <Button variant="destructive" size="sm" onClick={handleSignOut}>
            Sign Out
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}
