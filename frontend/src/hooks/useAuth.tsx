import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import type { User, Session } from '@supabase/supabase-js';
import { supabase } from '@/lib/supabase';

interface AuthContextType {
  user: User | null;
  session: Session | null;
  loading: boolean;
  signUp: (email: string, password: string) => Promise<{ error: Error | null }>;
  signIn: (email: string, password: string) => Promise<{ error: Error | null }>;
  signOut: () => Promise<void>;
  resetPassword: (email: string) => Promise<{ error: Error | null }>;
  isDemoMode: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const LOCAL_STORAGE_KEY = 'securevault_dev_user';

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [session, setSession] = useState<Session | null>(null);
  const [loading, setLoading] = useState(true);
  const [isDemoMode, setIsDemoMode] = useState(false);

  useEffect(() => {
    const supabaseUrl = import.meta.env.VITE_SUPABASE_URL || '';
    const isPlaceholder = supabaseUrl.includes('YOUR_PROJECT') || !supabaseUrl.startsWith('http');

    if (isPlaceholder) {
      setIsDemoMode(true);
      // Check for locally persisted dev session
      const stored = localStorage.getItem(LOCAL_STORAGE_KEY);
      if (stored) {
        try {
          const parsed = JSON.parse(stored);
          setUser(parsed.user);
          setSession(parsed.session);
        } catch {
          // ignore corrupted local storage
        }
      }
      setLoading(false);
      return;
    }

    // Live Supabase Auth
    supabase.auth.getSession().then(({ data: { session } }) => {
      setSession(session);
      setUser(session?.user ?? null);
      setLoading(false);
    }).catch(() => {
      setLoading(false);
    });

    const { data: { subscription } } = supabase.auth.onAuthStateChange(
      (_event, session) => {
        setSession(session);
        setUser(session?.user ?? null);
        setLoading(false);
      }
    );

    return () => subscription.unsubscribe();
  }, []);

  const createDevUser = (email: string) => {
    const devUser: User = {
      id: 'bda6f2cb-7234-4344-a9b1-a9599e29af80',
      app_metadata: { provider: 'email' },
      user_metadata: {},
      aud: 'authenticated',
      created_at: new Date().toISOString(),
      email: email,
    } as User;

    const devSession: Session = {
      access_token: 'dev-mock-token-securevault',
      token_type: 'bearer',
      expires_in: 3600,
      refresh_token: 'dev-refresh-token',
      user: devUser,
    };

    localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify({ user: devUser, session: devSession }));
    setUser(devUser);
    setSession(devSession);
  };

  const signUp = async (email: string, password: string) => {
    const supabaseUrl = import.meta.env.VITE_SUPABASE_URL || '';
    if (supabaseUrl.includes('YOUR_PROJECT')) {
      createDevUser(email);
      return { error: null };
    }

    try {
      const { error } = await supabase.auth.signUp({ email, password });
      if (error) {
        // If network failed (e.g. invalid host), fall back to dev mode
        if (error.message.includes('fetch')) {
          createDevUser(email);
          return { error: null };
        }
        return { error: new Error(error.message) };
      }
      return { error: null };
    } catch (err) {
      createDevUser(email);
      return { error: null };
    }
  };

  const signIn = async (email: string, password: string) => {
    const supabaseUrl = import.meta.env.VITE_SUPABASE_URL || '';
    if (supabaseUrl.includes('YOUR_PROJECT')) {
      createDevUser(email);
      return { error: null };
    }

    try {
      const { error } = await supabase.auth.signInWithPassword({ email, password });
      if (error) {
        if (error.message.includes('fetch')) {
          createDevUser(email);
          return { error: null };
        }
        return { error: new Error(error.message) };
      }
      return { error: null };
    } catch (err) {
      createDevUser(email);
      return { error: null };
    }
  };

  const signOut = async () => {
    localStorage.removeItem(LOCAL_STORAGE_KEY);
    setUser(null);
    setSession(null);
    try {
      await supabase.auth.signOut();
    } catch {
      // ignore
    }
  };

  const resetPassword = async (email: string) => {
    const supabaseUrl = import.meta.env.VITE_SUPABASE_URL || '';
    if (supabaseUrl.includes('YOUR_PROJECT')) {
      return { error: null };
    }
    const { error } = await supabase.auth.resetPasswordForEmail(email, {
      redirectTo: `${window.location.origin}/reset-password`,
    });
    return { error: error ? new Error(error.message) : null };
  };

  return (
    <AuthContext.Provider value={{ user, session, loading, signUp, signIn, signOut, resetPassword, isDemoMode }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
