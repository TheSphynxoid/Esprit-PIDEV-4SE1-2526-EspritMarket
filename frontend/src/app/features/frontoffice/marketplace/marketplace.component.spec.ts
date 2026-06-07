import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { MarketplaceComponent } from './marketplace.component';

describe('MarketplaceComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MarketplaceComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    })
      .overrideComponent(MarketplaceComponent, {
        set: {
          template: ''
        }
      })
      .compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(MarketplaceComponent);
    const component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });
});