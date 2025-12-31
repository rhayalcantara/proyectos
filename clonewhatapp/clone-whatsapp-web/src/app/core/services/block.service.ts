import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { BlockStatus, BlockedUser } from '../models';

@Injectable({
  providedIn: 'root'
})
export class BlockService {
  private readonly apiUrl = environment.apiUrl;

  blockedUsers = signal<BlockedUser[]>([]);

  constructor(private http: HttpClient) {}

  getBlockedUsers(): Observable<BlockedUser[]> {
    return this.http.get<BlockedUser[]>(`${this.apiUrl}/users/blocked`)
      .pipe(
        tap(users => this.blockedUsers.set(users))
      );
  }

  blockUser(userId: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/users/block/${userId}`, {})
      .pipe(
        tap(() => {
          // Refrescar lista de bloqueados
          this.getBlockedUsers().subscribe();
        })
      );
  }

  unblockUser(userId: string): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.apiUrl}/users/block/${userId}`)
      .pipe(
        tap(() => {
          this.blockedUsers.set(this.blockedUsers().filter(u => u.id !== userId));
        })
      );
  }

  getBlockStatus(userId: string): Observable<BlockStatus> {
    return this.http.get<BlockStatus>(`${this.apiUrl}/users/${userId}/blocked-status`);
  }

  isUserBlocked(userId: string): boolean {
    return this.blockedUsers().some(u => u.id === userId);
  }
}
