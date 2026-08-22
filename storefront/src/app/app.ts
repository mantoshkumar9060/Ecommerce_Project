import {Component, computed, effect, signal} from '@angular/core';
import {CommonModule, CurrencyPipe} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {
  AddressRequest,
  StoreApiService,
  Address,
  AiProductResult,
  Cart,
  Order,
  PaymentDetails,
  Product,
  Profile
} from './store-api.service';

@Component({
  selector: 'app-root',
  imports: [CommonModule, FormsModule, CurrencyPipe],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  protected readonly api: StoreApiService;
  protected readonly products = signal<Product[]>([]);
  protected readonly activeCategory = signal('All');
  protected readonly favouriteIds = signal<Set<number>>(this.readFavourites());
  protected readonly cart = signal<Cart>({items: []});
  protected readonly orders = signal<Order[]>([]);
  protected readonly payment = signal<PaymentDetails | null>(null);
  protected readonly profile = signal<Profile | null>(null);
  protected readonly addresses = signal<Address[]>([]);
  protected selectedAddressId = 0;
  protected showAddressForm = false;
  protected addressForm: AddressRequest = {
    recipientName: '',
    mobileNumber: '',
    line1: '',
    line2: '',
    landmark: '',
    city: '',
    state: '',
    postalCode: '',
    country: 'India',
    addressType: 'HOME',
    defaultAddress: false
  };
  protected readonly selectedProduct = signal<Product | null>(null);
  protected readonly view = signal<'shop' | 'wishlist' | 'cart' | 'orders' | 'account' | 'payment' | 'detail' | 'assistant'>('shop');
  protected readonly loading = signal(false);
  protected readonly message = signal('');
  protected readonly mobileMenuOpen = signal(false);
  protected readonly aiLoading = signal(false);
  protected readonly aiAnswer = signal('');
  protected readonly aiProducts = signal<AiProductResult[]>([]);
  protected aiQuestion = '';
  protected readonly cartCount = computed(() => this.cart().items.reduce((sum, item) => sum + item.quantity, 0));
  protected readonly favouriteProducts = computed(() => this.products().filter(product => this.favouriteIds().has(product.id)));
  protected readonly displayProducts = computed(() => {
    const limit = this.priceLimit();
    const sorted = this.products().filter(product => !limit || (product.brands ?? []).some(brand => brand.price <= limit));
    return [...sorted].sort((left, right) => {
      const leftPrice = left.brands?.[0]?.price ?? 0;
      const rightPrice = right.brands?.[0]?.price ?? 0;
      if (this.sortOption() === 'price_asc') return leftPrice - rightPrice;
      if (this.sortOption() === 'price_desc') return rightPrice - leftPrice;
      if (this.sortOption() === 'name') return left.name.localeCompare(right.name);
      return right.id - left.id;
    });
  });
  protected query = '';
  protected email = '';
  protected password = '';
  protected name = '';
  protected shippingAddress = '';
  protected registerMode = false;
  protected paymentMethod: 'UPI' | 'CARD' | 'NET_BANKING' | 'WALLET' | 'COD' = 'UPI';
  protected upiId = '';
  protected cardNumber = '';
  protected cardExpiry = '';
  protected cardHolder = '';
  protected readonly sortOption = signal<'newest' | 'price_asc' | 'price_desc' | 'name'>('newest');
  protected readonly priceLimit = signal(0);
  protected readonly categories = [
    {label: 'All', query: ''}, {label: 'Men', query: 'Menswear'},
    {label: 'Women', query: 'Womenswear'}, {label: 'Kids', query: 'Kidswear'},
    {label: 'Home & Living', query: 'Home'}, {label: 'Beauty', query: 'Beauty'}
  ];

  constructor(api: StoreApiService) {
    this.api = api;
    effect(() => {
      if (api.sessionExpired()) {
        this.cart.set({items: []});
        this.view.set('account');
        this.notice('Your session has expired. Please sign in again.');
      }
    });
    this.search();
    if (api.token()) {
      this.loadCart();
      this.loadProfile();
      this.loadAddresses();
    }
  }

  protected search(): void {
    this.loading.set(true);
    this.api.search(this.query).subscribe({
      next: result => {
        this.products.set(result.data ?? []);
        this.loading.set(false);
      }, error: () => {
        this.products.set([]);
        this.loading.set(false);
        this.notice('Unable to load products. Please verify that Product Service and the API Gateway are running.');
      }
    });
  }

  protected selectCategory(label: string, query: string): void {
    this.activeCategory.set(label);
    this.query = query;
    this.mobileMenuOpen.set(false);
    this.search();
  }

  protected toggleFavourite(id: number): void {
    const next = new Set(this.favouriteIds());
    next.has(id) ? next.delete(id) : next.add(id);
    this.favouriteIds.set(next);
    localStorage.setItem('stylehub_favourites', JSON.stringify([...next]));
  }

  protected setSort(value: string): void {
    this.sortOption.set(value as 'newest' | 'price_asc' | 'price_desc' | 'name');
  }

  protected setPriceLimit(value: string): void {
    this.priceLimit.set(Number(value));
  }

  protected imageFor(product: Product): string {
    const name = product.name.toLowerCase();
    if (name.includes('shoe') || name.includes('sneaker')) return 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=800&q=85';
    if (name.includes('dress')) return 'https://images.unsplash.com/photo-1595777457583-95e059d581b8?auto=format&fit=crop&w=800&q=85';
    if (name.includes('jacket')) return 'https://images.unsplash.com/photo-1551028719-00167b16eac5?auto=format&fit=crop&w=800&q=85';
    if (name.includes('jeans')) return 'https://images.unsplash.com/photo-1542272604-787c3835535d?auto=format&fit=crop&w=800&q=85';
    if (name.includes('shirt')) return 'https://images.unsplash.com/photo-1596755389378-c31d21fd1273?auto=format&fit=crop&w=800&q=85';
    return 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=800&q=85';
  }

  protected galleryFor(product: Product): string[] {
    const images = product.brands?.flatMap(brand => brand.images ?? [])
      .map(image => `http://localhost:8080/api/v1/products/${product.id}/images/${image.id}`) ?? [];
    if (images.length) return images.slice(0, 4);
    const primary = this.imageFor(product);
    return [primary, `${primary}&sat=-18`, `${primary}&con=8`, `${primary}&fm=jpg`];
  }

  protected openProduct(product: Product): void {
    this.selectedProduct.set(product);
    this.view.set('detail');
    window.scrollTo({top: 0, behavior: 'smooth'});
  }

  protected authenticate(): void {
    if (this.registerMode) {
      this.api.register({name: this.name, email: this.email, password: this.password}).subscribe({
        next: () => {
          this.registerMode = false;
          this.password = '';
          this.notice('Your account has been created. Please sign in with your credentials.');
        },
        error: error => this.notice(error.error?.message ?? 'Unable to create the account.')
      });
      return;
    }
    this.api.login({email: this.email, password: this.password}).subscribe({
      next: () => {
        this.notice('Welcome back. You are now signed in.');
        this.loadCart();
        this.loadProfile();
        this.loadAddresses();
        this.search();
        this.view.set('shop');
      }, error: error => this.notice(error.error?.message ?? 'The email address or password is incorrect.')
    });
  }

  protected add(product: Product, brandId: number): void {
    if (!this.api.token()) {
      this.view.set('account');
      this.notice('Please sign in before adding an item to your bag.');
      return;
    }
    this.api.addToCart(product.id, brandId).subscribe({
      next: cart => {
        this.cart.set(cart);
        this.notice(`${product.name} has been added to your bag.`);
      }, error: () => this.notice('Unable to update your bag.')
    });
  }

  protected askAi(): void {
    const question = this.aiQuestion.trim();
    if (!this.api.token()) {
      this.view.set('account');
      this.notice('Please sign in to use the shopping assistant.');
      return;
    }
    if (!question) return;
    this.aiLoading.set(true);
    this.aiAnswer.set('');
    this.aiProducts.set([]);
    this.api.aiChat(question).subscribe({
      next: response => {
        this.aiAnswer.set(response.answer);
        this.aiProducts.set(response.products ?? []);
        this.aiLoading.set(false);
      }, error: error => {
        this.aiLoading.set(false);
        this.notice(error.error?.message ?? 'The assistant is temporarily unavailable.');
      }
    });
  }

  protected productForAi(result: AiProductResult): Product | undefined {
    return this.products().find(product => product.id === result.productId);
  }

  protected openAiProduct(result: AiProductResult): void {
    const product = this.productForAi(result);
    if (product) this.openProduct(product); else this.notice('Product details are not loaded. Search the catalogue and try again.');
  }

  protected addAiProduct(result: AiProductResult): void {
    const product = this.productForAi(result);
    if (product) this.add(product, result.brandId); else this.notice('Product details are not loaded. Search the catalogue and try again.');
  }

  protected loadCart(): void {
    if (this.api.token()) this.api.cart().subscribe({
      next: cart => this.cart.set(cart),
      error: () => this.notice('Unable to load your bag.')
    });
  }

  protected changeQuantity(itemId: number, quantity: number): void {
    if (quantity < 1) return;
    this.api.updateCartItem(itemId, quantity).subscribe({
      next: cart => this.cart.set(cart),
      error: () => this.notice('Unable to update the item quantity.')
    });
  }

  protected remove(itemId: number): void {
    this.api.removeCartItem(itemId).subscribe({
      next: () => this.loadCart(),
      error: () => this.notice('Unable to remove the item from your bag.')
    });
  }

  protected checkout(): void {
    const address = this.addresses().find(item => item.id === Number(this.selectedAddressId));
    const shippingAddress = address ? [address.recipientName, address.line1, address.line2, address.city, address.state, address.postalCode, address.country].filter(Boolean).join(', ') : this.shippingAddress;
    this.api.createOrder(shippingAddress).subscribe({
      next: order => {
        this.cart.set({items: []});
        this.shippingAddress = '';
        this.api.paymentForOrder(order.id).subscribe({
          next: payment => {
            this.payment.set(payment);
            this.view.set('payment');
          }, error: () => this.notice('Order created, but the payment could not be loaded.')
        });
      }, error: error => this.notice(error.error?.message ?? 'Unable to place the order. Your bag may be empty.')
    });
  }

  protected payNow(): void {
    const payment = this.payment();
    if (!payment) return;
    if (this.paymentMethod === 'UPI' && !/^[\w.-]+@[\w.-]+$/.test(this.upiId)) {
      this.notice('Enter a valid UPI ID.');
      return;
    }
    if (this.paymentMethod === 'CARD' && (!/^\d{16}$/.test(this.cardNumber.replace(/\s/g, '')) || !/^\d{2}\/\d{2}$/.test(this.cardExpiry) || !this.cardHolder.trim())) {
      this.notice('Enter valid card details. Card data is not stored.');
      return;
    }
    this.api.completePayment(payment.checkoutReference).subscribe({
      next: () => {
        this.notice(`${this.paymentMethod} sandbox payment submitted. Your order will update shortly.`);
        setTimeout(() => {
          this.loadOrders();
          this.view.set('orders');
        }, 1200);
      }, error: error => this.notice(error.error?.message ?? 'Payment could not be completed.')
    });
  }

  protected loadOrders(): void {
    if (this.api.token()) this.api.orders().subscribe({
      next: orders => this.orders.set(orders),
      error: () => this.notice('Unable to load your orders.')
    });
  }

  protected logout(): void {
    this.api.logout();
    this.cart.set({items: []});
    this.profile.set(null);
    this.addresses.set([]);
    this.view.set('shop');
    this.notice('You have been signed out.');
  }

  protected show(view: 'shop' | 'wishlist' | 'cart' | 'orders' | 'account' | 'payment' | 'detail' | 'assistant'): void {
    this.view.set(view);
    if (view === 'cart') this.loadCart();
    if (view === 'orders') this.loadOrders();
    window.scrollTo({top: 0, behavior: 'smooth'});
  }

  protected loadProfile(): void {
    this.api.profile().subscribe({next: profile => this.profile.set(profile)});
  }

  protected loadAddresses(): void {
    this.api.addresses().subscribe({
      next: addresses => {
        this.addresses.set(addresses);
        this.selectedAddressId = addresses.find(address => address.defaultAddress)?.id ?? 0;
      }
    });
  }

  protected saveAddress(): void {
    this.api.addAddress(this.addressForm).subscribe({
      next: () => {
        this.addressForm = {
          recipientName: '',
          mobileNumber: '',
          line1: '',
          line2: '',
          landmark: '',
          city: '',
          state: '',
          postalCode: '',
          country: 'India',
          addressType: 'HOME',
          defaultAddress: false
        };
        this.showAddressForm = false;
        this.loadAddresses();
        this.notice('Address saved.');
      }, error: error => this.notice(error.error?.message ?? 'Unable to save address.')
    });
  }

  protected makeDefaultAddress(id: number): void {
    this.api.setDefaultAddress(id).subscribe({
      next: () => this.loadAddresses(),
      error: () => this.notice('Unable to update default address.')
    });
  }

  protected deleteAddress(id: number): void {
    this.api.deleteAddress(id).subscribe({
      next: () => this.loadAddresses(),
      error: () => this.notice('Unable to delete address.')
    });
  }

  private notice(text: string): void {
    this.message.set(text);
    setTimeout(() => this.message.set(''), 4000);
  }

  private readFavourites(): Set<number> {
    try {
      return new Set<number>(JSON.parse(localStorage.getItem('stylehub_favourites') ?? '[]'));
    } catch {
      return new Set();
    }
  }
}
