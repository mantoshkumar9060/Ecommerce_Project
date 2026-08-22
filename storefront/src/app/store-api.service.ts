import { Injectable, signal } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface AuthToken { accessToken: string; tokenType: string; userId: number; role: string; }
export interface Category { id: number; name: string; }
export interface Product { id: number; name: string; brands?: Brand[]; }
export interface Brand { id: number; name: string; price: number; images?: ProductImage[]; }
export interface ProductImage { id: number; url: string; }
export interface CartItem { id: number; productId: number; brandId: number; productName?: string; brandName?: string; quantity: number; unitPrice?: number; }
export interface Cart { id?: number; userId?: number; items: CartItem[]; total?: number; }
export interface Order { id: number; shippingAddress: string; status: string; total: number; items: CartItem[]; }
export interface PaymentDetails { paymentId: number; orderId: number; amount: number; status: string; checkoutReference: string; }
export interface Profile { id: number; name: string; email: string; role: string; }
export interface Address { id: number; recipientName: string; mobileNumber: string; line1: string; line2?: string; landmark?: string; city: string; state: string; postalCode: string; country: string; addressType: 'HOME' | 'WORK' | 'OTHER'; defaultAddress: boolean; }
export interface AddressRequest { recipientName: string; mobileNumber: string; line1: string; line2?: string; landmark?: string; city: string; state: string; postalCode: string; country: string; addressType: 'HOME' | 'WORK' | 'OTHER'; defaultAddress: boolean; }
export interface AiProductResult { productId: number; brandId: number; productName: string; brandName: string; category: string; price: number; score: number; }
export interface AiChatResponse { answer: string; products: AiProductResult[]; }

@Injectable({ providedIn: 'root' })
export class StoreApiService {
  private readonly baseUrl = 'http://localhost:8080';
  private readonly tokenStorageKey = 'ecommerce_token';
  private expiryTimer?: ReturnType<typeof setTimeout>;
  readonly token = signal<AuthToken | null>(null);
  readonly sessionExpired = signal(false);

  constructor(private readonly http: HttpClient) {
    const savedToken = this.readToken();
    this.token.set(savedToken);
    if (savedToken) this.scheduleExpiry(savedToken);
  }

  register(body: { name: string; email: string; password: string }): Observable<AuthToken> {
    return this.http.post<AuthToken>(`${this.baseUrl}/api/v1/auth/register`, body);
  }

  login(body: { email: string; password: string }): Observable<AuthToken> {
    return this.http.post<AuthToken>(`${this.baseUrl}/api/v1/auth/login`, body).pipe(tap(token => this.saveToken(token)));
  }

  logout(): void {
    this.clearSession(false);
  }
  categories(): Observable<{ data: Category[] }> { return this.http.get<{ data: Category[] }>(`${this.baseUrl}/api/v1/products/list/categories`); }
  search(keyword: string): Observable<{ data: Product[] }> { return this.http.get<{ data: Product[] }>(`${this.baseUrl}/api/v1/products/list/search`, { params: { keyword } }); }
  cart(): Observable<Cart> { return this.http.get<Cart>(`${this.baseUrl}/api/v1/carts`, this.authOptions()); }
  addToCart(productId: number, brandId: number, quantity = 1): Observable<Cart> { return this.http.post<Cart>(`${this.baseUrl}/api/v1/carts/items`, { productId, brandId, quantity }, this.authOptions()); }
  updateCartItem(itemId: number, quantity: number): Observable<Cart> { return this.http.patch<Cart>(`${this.baseUrl}/api/v1/carts/items/${itemId}`, { quantity }, this.authOptions()); }
  removeCartItem(itemId: number): Observable<void> { return this.http.delete<void>(`${this.baseUrl}/api/v1/carts/items/${itemId}`, this.authOptions()); }
  createOrder(shippingAddress: string): Observable<Order> { return this.http.post<Order>(`${this.baseUrl}/api/v1/orders`, { shippingAddress }, this.authOptions()); }
  orders(): Observable<Order[]> { return this.http.get<Order[]>(`${this.baseUrl}/api/v1/orders`, this.authOptions()); }
  paymentForOrder(orderId: number): Observable<PaymentDetails> { return this.http.get<PaymentDetails>(`${this.baseUrl}/api/v1/payments/orders/${orderId}`, this.authOptions()); }
  completePayment(checkoutReference: string): Observable<void> { return this.http.post<void>(`${this.baseUrl}/api/v1/payments/callback`, { checkoutReference, successful: true }, this.authOptions()); }
  profile(): Observable<Profile> { return this.http.get<Profile>(`${this.baseUrl}/api/v1/auth/me`, this.authOptions()); }
  addresses(): Observable<Address[]> { return this.http.get<Address[]>(`${this.baseUrl}/api/v1/addresses`, this.authOptions()); }
  addAddress(address: AddressRequest): Observable<Address> { return this.http.post<Address>(`${this.baseUrl}/api/v1/addresses`, address, this.authOptions()); }
  updateAddress(id: number, address: AddressRequest): Observable<Address> { return this.http.put<Address>(`${this.baseUrl}/api/v1/addresses/${id}`, address, this.authOptions()); }
  deleteAddress(id: number): Observable<void> { return this.http.delete<void>(`${this.baseUrl}/api/v1/addresses/${id}`, this.authOptions()); }
  setDefaultAddress(id: number): Observable<Address> { return this.http.patch<Address>(`${this.baseUrl}/api/v1/addresses/${id}/default`, {}, this.authOptions()); }
  aiChat(question: string, limit = 5): Observable<AiChatResponse> { return this.http.post<AiChatResponse>(`${this.baseUrl}/api/v1/ai/chat`, { question, limit }, this.authOptions()); }

  private authOptions() { return { headers: new HttpHeaders({ Authorization: `Bearer ${this.token()?.accessToken ?? ''}` }) }; }
  private saveToken(token: AuthToken): void {
    localStorage.setItem(this.tokenStorageKey, JSON.stringify(token));
    this.sessionExpired.set(false);
    this.token.set(token);
    this.scheduleExpiry(token);
  }

  private readToken(): AuthToken | null {
    try {
      const token = JSON.parse(localStorage.getItem(this.tokenStorageKey) ?? 'null') as AuthToken | null;
      if (!token || this.isExpired(token)) {
        localStorage.removeItem(this.tokenStorageKey);
        if (token) this.sessionExpired.set(true);
        return null;
      }
      return token;
    } catch {
      localStorage.removeItem(this.tokenStorageKey);
      return null;
    }
  }

  private scheduleExpiry(token: AuthToken): void {
    if (this.expiryTimer) clearTimeout(this.expiryTimer);
    const expiresAt = this.expiresAt(token);
    if (!expiresAt) {
      this.clearSession(true);
      return;
    }
    this.expiryTimer = setTimeout(() => this.clearSession(true), Math.max(0, expiresAt - Date.now()));
  }

  private clearSession(expired: boolean): void {
    if (this.expiryTimer) clearTimeout(this.expiryTimer);
    this.expiryTimer = undefined;
    localStorage.removeItem(this.tokenStorageKey);
    this.token.set(null);
    this.sessionExpired.set(expired);
  }

  private isExpired(token: AuthToken): boolean {
    const expiresAt = this.expiresAt(token);
    return !expiresAt || expiresAt <= Date.now();
  }

  private expiresAt(token: AuthToken): number | null {
    try {
      const payload = token.accessToken.split('.')[1];
      if (!payload) return null;
      const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
      const decoded = JSON.parse(atob(base64.padEnd(base64.length + ((4 - base64.length % 4) % 4), '='))) as { exp?: number };
      return typeof decoded.exp === 'number' ? decoded.exp * 1000 : null;
    } catch {
      return null;
    }
  }
}
