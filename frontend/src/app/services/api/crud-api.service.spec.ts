import { HttpClient, provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { CrudApiService } from './crud-api.service';

interface TestResource {
  id: number;
  name: string;
}

interface TestWrite {
  name: string;
}

describe('CrudApiService', () => {
  let service: CrudApiService<TestResource, TestWrite, number>;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });

    const http = TestBed.inject(HttpClient);
    service = new CrudApiService<TestResource, TestWrite, number>(http, '/api/items');
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should request resource list', () => {
    const payload: TestResource[] = [{ id: 1, name: 'item' }];

    service.list().subscribe((result) => {
      expect(result).toEqual(payload);
    });

    const req = httpMock.expectOne('/api/items');
    expect(req.request.method).toBe('GET');
    req.flush(payload);
  });

  it('should request one resource by id', () => {
    const payload: TestResource = { id: 7, name: 'single' };

    service.getById(7).subscribe((result) => {
      expect(result).toEqual(payload);
    });

    const req = httpMock.expectOne('/api/items/7');
    expect(req.request.method).toBe('GET');
    req.flush(payload);
  });

  it('should create a resource', () => {
    const body: TestWrite = { name: 'new' };
    const payload: TestResource = { id: 5, name: 'new' };

    service.create(body).subscribe((result) => {
      expect(result).toEqual(payload);
    });

    const req = httpMock.expectOne('/api/items');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush(payload);
  });

  it('should update a resource', () => {
    const body: TestWrite = { name: 'updated' };
    const payload: TestResource = { id: 2, name: 'updated' };

    service.update(2, body).subscribe((result) => {
      expect(result).toEqual(payload);
    });

    const req = httpMock.expectOne('/api/items/2');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(body);
    req.flush(payload);
  });

  it('should delete a resource', () => {
    service.delete(3).subscribe((result) => {
      expect(result).toBeNull();
    });

    const req = httpMock.expectOne('/api/items/3');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
