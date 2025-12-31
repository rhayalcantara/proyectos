import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Contact, AddContactRequest, UpdateContactRequest } from '../models';

@Injectable({
  providedIn: 'root'
})
export class ContactService {
  private readonly apiUrl = environment.apiUrl;

  contacts = signal<Contact[]>([]);

  constructor(private http: HttpClient) {}

  getContacts(): Observable<Contact[]> {
    return this.http.get<Contact[]>(`${this.apiUrl}/contacts`)
      .pipe(
        tap(contacts => this.contacts.set(contacts))
      );
  }

  addContact(request: AddContactRequest): Observable<Contact> {
    return this.http.post<Contact>(`${this.apiUrl}/contacts`, request)
      .pipe(
        tap(contact => {
          this.contacts.set([...this.contacts(), contact]);
        })
      );
  }

  updateContact(id: string, request: UpdateContactRequest): Observable<Contact> {
    return this.http.put<Contact>(`${this.apiUrl}/contacts/${id}`, request)
      .pipe(
        tap(updatedContact => {
          const contacts = this.contacts();
          const index = contacts.findIndex(c => c.id === id);
          if (index !== -1) {
            const updatedContacts = [...contacts];
            updatedContacts[index] = updatedContact;
            this.contacts.set(updatedContacts);
          }
        })
      );
  }

  deleteContact(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/contacts/${id}`)
      .pipe(
        tap(() => {
          this.contacts.set(this.contacts().filter(c => c.id !== id));
        })
      );
  }

  searchContacts(query: string): Contact[] {
    const lowerQuery = query.toLowerCase();
    return this.contacts().filter(c =>
      c.nombre.toLowerCase().includes(lowerQuery) ||
      c.numeroTelefono.includes(query) ||
      c.nombrePersonalizado?.toLowerCase().includes(lowerQuery)
    );
  }
}
