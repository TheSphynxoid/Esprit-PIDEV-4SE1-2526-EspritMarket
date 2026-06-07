import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { CrudApiService } from './crud-api.service';
import { ApiConfigService } from './api-config.service';
import { UserResource } from './models/api-resource.model';
import { UserRequest } from '@esprit-market/api-types';

@Injectable({ providedIn: 'root' })
export class CommonApiService {
  private readonly http = inject(HttpClient);
  private readonly apiConfig = inject(ApiConfigService);

  readonly users = new CrudApiService<UserResource, UserRequest>(
    this.http,
    this.apiConfig.buildUrl('/api/common/users')
  );
}
