import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Status, StatusViewer, ContactStatuses, MyStatuses, CreateTextStatusRequest } from '../models';

@Injectable({
  providedIn: 'root'
})
export class StatusService {
  private readonly apiUrl = environment.apiUrl;

  myStatuses = signal<MyStatuses>({ estados: [], totalVistas: 0 });
  contactsStatuses = signal<ContactStatuses[]>([]);
  selectedContactStatuses = signal<ContactStatuses | null>(null);
  currentStatusIndex = signal<number>(0);

  constructor(private http: HttpClient) {}

  // Obtener mis estados
  getMyStatuses(): Observable<MyStatuses> {
    return this.http.get<MyStatuses>(`${this.apiUrl}/estados`)
      .pipe(
        tap(data => this.myStatuses.set(data))
      );
  }

  // Obtener estados de contactos
  getContactsStatuses(): Observable<ContactStatuses[]> {
    return this.http.get<ContactStatuses[]>(`${this.apiUrl}/estados/contactos`)
      .pipe(
        tap(data => this.contactsStatuses.set(data))
      );
  }

  // Publicar estado de texto
  createTextStatus(request: CreateTextStatusRequest): Observable<Status> {
    return this.http.post<Status>(`${this.apiUrl}/estados`, request)
      .pipe(
        tap(status => {
          const current = this.myStatuses();
          this.myStatuses.set({
            estados: [status, ...current.estados],
            totalVistas: current.totalVistas
          });
        })
      );
  }

  // Publicar estado con imagen
  createImageStatus(imagen: File, caption?: string): Observable<Status> {
    const formData = new FormData();
    formData.append('imagen', imagen);
    if (caption) {
      formData.append('caption', caption);
    }

    return this.http.post<Status>(`${this.apiUrl}/estados/imagen`, formData)
      .pipe(
        tap(status => {
          const current = this.myStatuses();
          this.myStatuses.set({
            estados: [status, ...current.estados],
            totalVistas: current.totalVistas
          });
        })
      );
  }

  // Marcar estado como visto
  markAsViewed(statusId: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/estados/${statusId}/vista`, {})
      .pipe(
        tap(() => {
          // Actualizar el estado en la lista
          const contacts = this.contactsStatuses();
          const updatedContacts = contacts.map(contact => ({
            ...contact,
            estados: contact.estados.map(status =>
              status.id === statusId ? { ...status, vioPorMi: true } : status
            ),
            todosVistos: contact.estados.every(s => s.id === statusId || s.vioPorMi)
          }));
          this.contactsStatuses.set(updatedContacts);
        })
      );
  }

  // Ver quién vio mi estado
  getStatusViewers(statusId: string): Observable<StatusViewer[]> {
    return this.http.get<StatusViewer[]>(`${this.apiUrl}/estados/${statusId}/vistas`);
  }

  // Eliminar mi estado
  deleteStatus(statusId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/estados/${statusId}`)
      .pipe(
        tap(() => {
          const current = this.myStatuses();
          this.myStatuses.set({
            estados: current.estados.filter(s => s.id !== statusId),
            totalVistas: current.totalVistas
          });
        })
      );
  }

  // Seleccionar estados de un contacto para ver
  selectContactStatuses(contact: ContactStatuses | null): void {
    this.selectedContactStatuses.set(contact);
    this.currentStatusIndex.set(0);
  }

  // Navegar al siguiente estado
  nextStatus(): boolean {
    const contact = this.selectedContactStatuses();
    if (!contact) return false;

    const currentIndex = this.currentStatusIndex();
    if (currentIndex < contact.estados.length - 1) {
      this.currentStatusIndex.set(currentIndex + 1);
      return true;
    }
    return false;
  }

  // Navegar al estado anterior
  previousStatus(): boolean {
    const currentIndex = this.currentStatusIndex();
    if (currentIndex > 0) {
      this.currentStatusIndex.set(currentIndex - 1);
      return true;
    }
    return false;
  }

  // Manejar nuevo estado de SignalR
  handleNewStatus(status: Status): void {
    const contacts = this.contactsStatuses();
    const contactIndex = contacts.findIndex(c => c.usuarioId === status.usuarioId);

    if (contactIndex !== -1) {
      // Agregar estado a contacto existente
      const updatedContacts = [...contacts];
      updatedContacts[contactIndex] = {
        ...updatedContacts[contactIndex],
        estados: [status, ...updatedContacts[contactIndex].estados],
        todosVistos: false,
        ultimaActualizacion: new Date()
      };
      // Mover al inicio
      const [contact] = updatedContacts.splice(contactIndex, 1);
      this.contactsStatuses.set([contact, ...updatedContacts]);
    } else {
      // Crear nuevo contacto con estado
      const newContact: ContactStatuses = {
        usuarioId: status.usuarioId,
        usuarioNombre: status.usuarioNombre,
        usuarioFoto: status.usuarioFoto,
        estados: [status],
        todosVistos: false,
        ultimaActualizacion: new Date()
      };
      this.contactsStatuses.set([newContact, ...contacts]);
    }
  }

  // Manejar estado eliminado de SignalR
  handleStatusDeleted(usuarioId: string, estadoId: string): void {
    const contacts = this.contactsStatuses();
    const updatedContacts = contacts
      .map(contact => {
        if (contact.usuarioId === usuarioId) {
          const filteredEstados = contact.estados.filter(s => s.id !== estadoId);
          return filteredEstados.length > 0 ? { ...contact, estados: filteredEstados } : null;
        }
        return contact;
      })
      .filter((c): c is ContactStatuses => c !== null);

    this.contactsStatuses.set(updatedContacts);
  }

  // Manejar vista de mi estado de SignalR
  handleStatusViewed(statusId: string, viewer: StatusViewer): void {
    const current = this.myStatuses();
    this.myStatuses.set({
      estados: current.estados.map(s =>
        s.id === statusId ? { ...s, totalVistas: s.totalVistas + 1 } : s
      ),
      totalVistas: current.totalVistas + 1
    });
  }
}
