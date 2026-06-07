import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { DeliveryFormComponent } from './delivery-form.component';

describe('DeliveryFormComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DeliveryFormComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    })
      .overrideComponent(DeliveryFormComponent, {
        set: {
          template: ''
        }
      })
      .compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(DeliveryFormComponent);
    const component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });
});