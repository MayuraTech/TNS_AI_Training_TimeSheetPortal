export type Role = 'EMPLOYEE' | 'MANAGER' | 'HR' | 'ADMIN';

export interface User {
  userId: number;
  fullName: string;
  email: string;
  roles: Role[];
  activeRole: Role;
  timezone: string;
  forcePasswordChange: boolean;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}
