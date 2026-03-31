import { Injectable } from '@angular/core';
import { environment } from '../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private baseUrl = environment.apiUrl;
  private livekitBaseUrl = environment.livekitBaseUrl;

  constructor() { }

  async getToken(roomName: string, userName: string): Promise<any> {
    const response = await fetch(`${this.livekitBaseUrl}/token?room=${roomName}&user=${userName}`);
    if (!response.ok) {
      throw new Error(`Failed to fetch token: ${response.statusText}`);
    }
    return response.json();
  }

  async getAllRooms(): Promise<any[]> {
    const response = await fetch(`${this.baseUrl}/rooms`);
    if (!response.ok) {
      throw new Error('Failed to fetch rooms');
    }
    return response.json();
  }

  async createRoom(roomName: string, createdBy: string, isPrivate: boolean = false, password?: string): Promise<any> {
    const response = await fetch(`${this.baseUrl}/rooms`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ roomName, createdBy, isPrivate, password })
    });
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Failed to create room');
    }
    return response.json();
  }

  async joinRoom(roomId: string, userName: string, password?: string): Promise<any> {
    const response = await fetch(`${this.baseUrl}/rooms/join`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ roomId, userName, password })
    });
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Failed to join room');
    }
    return response.json();
  }

  async leaveRoom(roomId: string, userName: string): Promise<void> {
    const response = await fetch(`${this.baseUrl}/rooms/leave`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ roomId, userName })
    });
    if (!response.ok) {
      console.warn('Failed to notify backend about leaving room');
    }
  }

  async deleteRoom(roomId: string): Promise<void> {
    const response = await fetch(`${this.baseUrl}/rooms/${roomId}`, {
      method: 'DELETE'
    });
    if (!response.ok) {
      throw new Error('Failed to delete room');
    }
  }

  async startRecording(roomId: string): Promise<{ egressId: string }> {
    const response = await fetch(`${this.baseUrl}/rooms/${roomId}/record/start`, {
      method: 'POST'
    });
    if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || 'Failed to start recording');
    }
    return response.json();
  }

  async stopRecording(egressId: string): Promise<void> {
    const response = await fetch(`${this.baseUrl}/rooms/record/stop?egressId=${egressId}`, {
      method: 'POST'
    });
    if (!response.ok) {
        throw new Error('Failed to stop recording');
    }
  }

  async getRecordings(roomId: string): Promise<any[]> {
    const response = await fetch(`${this.baseUrl}/rooms/${roomId}/recordings`);
    if (!response.ok) {
      throw new Error('Failed to fetch recordings');
    }
    return response.json();
  }

  async getTranscript(roomId: string): Promise<any[]> {
    const response = await fetch(`${this.baseUrl}/rooms/${roomId}/transcript`);
    if (!response.ok) {
      throw new Error('Failed to fetch transcript');
    }
    return response.json();
  }
}
