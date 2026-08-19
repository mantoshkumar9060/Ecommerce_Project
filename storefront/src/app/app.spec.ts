import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { App } from './app';
import { StoreApiService } from './store-api.service';

describe('App', () => {
  const token = signal(null);
  const sessionExpired = signal(false);
  const api = { token, sessionExpired, search: () => of({ data: [] }) };

  beforeEach(async () => {
    token.set(null);
    sessionExpired.set(false);
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [{ provide: StoreApiService, useValue: api }]
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('shows the sign-in page when a saved session expires', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    sessionExpired.set(true);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Sign in');
  });
});
