import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { DelivererQuizComponent } from './deliverer-quiz.component';

describe('DelivererQuizComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DelivererQuizComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    })
      .overrideComponent(DelivererQuizComponent, {
        set: {
          template: ''
        }
      })
      .compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(DelivererQuizComponent);
    const component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });
});