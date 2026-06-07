import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { FormInputComponent } from './form-input.component';

describe('FormInputComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FormInputComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    })
      .overrideComponent(FormInputComponent, {
        set: {
          template: ''
        }
      })
      .compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(FormInputComponent);
    const component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });
});