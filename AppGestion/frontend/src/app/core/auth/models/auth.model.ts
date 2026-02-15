export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  nombre: string;
  email: string;
  password: string;
  rol?: string;
}

export interface AuthResponse {
  token: string;
  type: string;
  email: string;
  rol: string;
  expiresAt: string;
}
