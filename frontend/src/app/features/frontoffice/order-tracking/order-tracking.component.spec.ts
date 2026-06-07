import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { OrderTrackingComponent } from './order-tracking.component';

describe('OrderTrackingComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrderTrackingComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    })
      .overrideComponent(OrderTrackingComponent, {
        set: {
          template: ''
        }
      })
      .compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(OrderTrackingComponent);
    const component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });
});