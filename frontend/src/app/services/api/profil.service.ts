import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Profil {
  id?: number;
  skills: string[];
  experienceLevel: string;
  fieldOfStudy: string;
  yearsOfExperience: string;
  languages: string[];
}

@Injectable({
  providedIn: 'root'
})
export class ProfilService {
  private apiUrl = `${environment.apiBaseUrl}/api/partnership/profil`;

  constructor(private http: HttpClient) {}

  getProfil(studentId: number): Observable<Profil> {
    return this.http.get<Profil>(`${this.apiUrl}/${studentId}`);
  }

  saveOrUpdateProfil(studentId: number, profil: Profil): Observable<Profil> {
    return this.http.post<Profil>(`${this.apiUrl}/${studentId}`, profil);
  }
}
