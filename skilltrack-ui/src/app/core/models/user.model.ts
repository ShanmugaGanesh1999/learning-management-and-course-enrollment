export interface UserProfile {
  id: number | null;
  username: string | null;
  email: string | null;
  fullName: string | null;
  role: 'STUDENT' | 'INSTRUCTOR' | 'ADMIN' | null;
}
