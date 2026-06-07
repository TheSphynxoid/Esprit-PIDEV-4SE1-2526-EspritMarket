import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export class CrudApiService<TResource, TWrite, TId = number> {
  constructor(
    private readonly http: HttpClient,
    private readonly resourceUrl: string,
    private readonly listUrl: string = resourceUrl
  ) {}

  list(): Observable<TResource[]> {
    return this.http.get<TResource[]>(this.listUrl);
  }

  getById(id: TId): Observable<TResource> {
    return this.http.get<TResource>(`${this.resourceUrl}/${id}`);
  }

  create(payload: TWrite): Observable<TResource> {
    return this.http.post<TResource>(this.resourceUrl, payload);
  }

  update(id: TId, payload: TWrite): Observable<TResource> {
    return this.http.put<TResource>(`${this.resourceUrl}/${id}`, payload);
  }

  delete(id: TId): Observable<void> {
    return this.http.delete<void>(`${this.resourceUrl}/${id}`);
  }
}
