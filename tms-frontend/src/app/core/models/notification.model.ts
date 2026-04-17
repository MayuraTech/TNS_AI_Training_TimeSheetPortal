export interface Notification {
  id: number;
  userId: number;
  type: string;
  message: string;
  read: boolean;
  deepLink?: string;
  createdAt: string;
}
