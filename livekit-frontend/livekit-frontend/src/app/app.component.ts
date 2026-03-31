import { Component, ElementRef, ViewChild, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { Room, RoomEvent, Track, RemoteTrack, RemoteTrackPublication, RemoteParticipant, LocalParticipant, LocalTrackPublication } from 'livekit-client';
import { ApiService } from './api.service';

interface ChatMessage {
  sender: string;
  text: string;
  isPrivate?: boolean;
}

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnDestroy {
  // Join Form State
  roomName: string = '';
  participantName: string = '';
  isModerator: boolean = false;
  token: string | null = null;
  
  // New Join Screen State
  joinMode: 'join' | 'create' = 'join';
  activeRooms: any[] = [];
  isPrivate: boolean = false;
  password: string = '';
  recordings: any[] = [];
  
  // Connection & Room State
  room: Room | null = null;
  roomId: string | null = null;
  activeEgressId: string | null = null;
  livekitUrl: string | null = null;
  connected: boolean = false;
  
  // Media Controls State
  audioEnabled: boolean = true;
  videoEnabled: boolean = true;
  isScreenSharing: boolean = false;
  
  // Advanced Features State
  isHandRaised: boolean = false;
  isRecording: boolean = false;
  showSidebar: boolean = true;
  activeTab: 'chat' | 'people' | 'recordings' = 'chat';
  
  // Data State
  messages: ChatMessage[] = [];
  raisedHands: Set<string> = new Set();
  chatInput: string = '';
  
  // Only used if isModerator is true
  waitingUsers: string[] = [];

  @ViewChild('localVideo', { static: false }) localVideoElement!: ElementRef<HTMLVideoElement>;
  @ViewChild('screenShareVideo', { static: false }) screenShareElement!: ElementRef<HTMLVideoElement>;
  
  remoteParticipants: { participant: RemoteParticipant, trackPubs: RemoteTrackPublication[] }[] = [];

  constructor(private cdr: ChangeDetectorRef, private apiService: ApiService) { 
      this.loadActiveRooms();
  }

  async loadActiveRooms() {
      try {
          this.activeRooms = await this.apiService.getAllRooms();
      } catch (e) {
          console.error("Failed to load rooms", e);
      }
  }

  async fetchToken() {
    if (!this.roomName || !this.participantName) {
      alert("Please enter room name and your name");
      return;
    }

    try {
      if (this.joinMode === 'create') {
          const roomData = await this.apiService.createRoom(this.roomName, this.participantName, this.isPrivate, this.password);
          this.roomId = roomData.roomId;
      } else {
          // Join mode: always try to find the room by name if manual entry or to refresh metadata
          const rooms = await this.apiService.getAllRooms();
          const found = rooms.find(r => r.roomName === this.roomName);
          if (!found) {
              alert("Room not found. Please create it or check the name.");
              return;
          }
          this.roomId = found.roomId;
          this.isPrivate = found.isPrivate;

          if (this.isPrivate && !this.password) {
              alert("This room is private. Please enter the password.");
              this.cdr.detectChanges();
              return;
          }
      }

      const joinData = await this.apiService.joinRoom(this.roomId!, this.participantName, this.password);
      this.token = joinData.token;
      this.livekitUrl = joinData.livekitUrl;
      this.isModerator = (joinData.role === 'HOST');

      const historicalTranscripts = await this.apiService.getTranscript(this.roomId!);
      historicalTranscripts.forEach(t => {
          this.messages.push({ 
              sender: t.participantIdentity === 'System' ? 'System' : `Transcript (${t.participantIdentity})`, 
              text: t.text 
          });
      });

      await this.loadRecordings();
      const activeRec = this.recordings.find(r => r.status === 'STARTING' || r.status === 'ACTIVE');
      if (activeRec) {
          this.isRecording = true;
          this.activeEgressId = activeRec.egressId;
      }

      await this.connectToRoom();
      this.connected = true;

    } catch (error: any) {
      console.error("Connection failed:", error);
      alert(error.message || "Could not join room. Check backend logs.");
    }
  }

  async loadRecordings() {
      if (!this.roomId) return;
      try {
          this.recordings = await this.apiService.getRecordings(this.roomId);
      } catch (e) {
          console.error("Failed to load recordings", e);
      }
  }

  async selectRoom(room: any) {
      this.roomName = room.roomName;
      this.roomId = room.roomId;
      this.joinMode = 'join';
      this.isPrivate = room.isPrivate;
  }

  checkRoomPrivacy() {
      if (this.joinMode === 'join' && this.roomName) {
          const found = this.activeRooms.find(r => r.roomName.toLowerCase() === this.roomName.toLowerCase());
          if (found) {
              this.isPrivate = found.isPrivate;
          } else {
              this.isPrivate = false;
          }
      }
  }

  setJoinMode(mode: 'join' | 'create') {
      this.joinMode = mode;
      this.isPrivate = false;
      this.password = '';
      this.roomName = '';
      if (mode === 'join') {
          this.loadActiveRooms();
      }
  }

  async connectToRoom() {
    if (!this.token) return;

    this.room = new Room();

    this.room
      .on(RoomEvent.ParticipantConnected, (participant: RemoteParticipant) => {
        this.updateRemoteParticipants();
      })
      .on(RoomEvent.ParticipantDisconnected, (participant: RemoteParticipant) => {
        this.updateRemoteParticipants();
      })
      .on(RoomEvent.TrackSubscribed, (track: RemoteTrack, publication: RemoteTrackPublication, participant: RemoteParticipant) => {
        this.addRemoteTrack(track, participant);
      })
      .on(RoomEvent.TrackUnsubscribed, (track: RemoteTrack, publication: RemoteTrackPublication, participant: RemoteParticipant) => {
        this.removeRemoteTrack(track);
      })
      .on(RoomEvent.DataReceived, (payload: Uint8Array, participant?: RemoteParticipant, kind?: any, topic?: string) => {
         this.handleDataReceived(payload, participant);
      })
      .on(RoomEvent.TranscriptionReceived, (transcription: any[]) => {
         transcription.forEach(t => {
            if (t.text && t.text.trim().length > 0) {
               this.messages.push({ sender: 'Transcript', text: t.text });
               this.cdr.detectChanges();
            }
         });
      })
      .on(RoomEvent.Disconnected, () => {
        this.handleDisconnect();
      });

    try {
      const url = this.livekitUrl || 'ws://localhost:7880';
      await this.room.connect(url, this.token);
      await this.publishLocalMedia();
    } catch (error) {
      console.error('Failed to connect to room:', error);
      alert('Failed to connect to LiveKit Room.');
    }
  }

  async publishLocalMedia() {
      if(!this.room) return;
      await this.room.localParticipant.enableCameraAndMicrophone();
      this.audioEnabled = true;
      this.videoEnabled = true;
      this.attachLocalVideo();
  }

  /* --- Advanced Feature Handlers via Data Channels --- */

  sendData(data: any) {
     if(!this.room || !this.room.localParticipant) return;
     const payload = new TextEncoder().encode(JSON.stringify(data));
     // Send to everyone in room via DataChannel
     this.room.localParticipant.publishData(payload, { reliable: true });
  }

  handleDataReceived(payload: Uint8Array, participant?: RemoteParticipant) {
      const dataStr = new TextDecoder().decode(payload);
      try {
          const data = JSON.parse(dataStr);
          
          switch(data.action) {
             case 'join_request':
                if (this.isModerator && !this.waitingUsers.includes(data.identity)) {
                   this.waitingUsers.push(data.identity);
                }
                break;
             case 'admit':
                if (data.target === this.participantName && !this.connected) {
                   this.connected = true;
                   this.publishLocalMedia(); // User is admitted, activate their camera
                   this.messages.push({ sender: 'System', text: 'You have been admitted by the host.'});
                }
                break;
             case 'mute':
                if (data.target === this.participantName) {
                   this.toggleAudio(false); // Force mute
                   this.messages.push({ sender: 'System', text: 'You were muted by the moderator.'});
                }
                break;
             case 'kick':
                if (data.target === this.participantName) {
                   alert("You have been removed from the session by the moderator.");
                   this.disconnect();
                }
                break;
             case 'raise_hand':
                if (data.state) {
                   this.raisedHands.add(data.identity);
                } else {
                   this.raisedHands.delete(data.identity);
                }
                break;
             case 'chat':
                this.messages.push({ sender: data.sender, text: data.text });
                // Auto switch to chat tab when a new message arrives
                if (this.activeTab !== 'chat') {
                   this.unreadMessages++;
                }
                break;
             case 'screenshare':
                // Optional: We can listen to intent, but TrackSubscribed handles the actual video UI
                break;
          }
          this.cdr.detectChanges();
      } catch (e) {
          console.error("Failed to parse data message", e);
      }
  }

  /* --- Chat --- */

  unreadMessages: number = 0;

  openChat() {
    this.activeTab = 'chat';
    this.unreadMessages = 0;
    this.showSidebar = true;
  }

  sendChatMessage() {
    const text = this.chatInput.trim();
    if (!text) return;
    // Push locally first so sender sees it immediately
    this.messages.push({ sender: this.participantName + ' (You)', text });
    // Broadcast to everyone else
    this.sendData({ action: 'chat', sender: this.participantName, text });
    this.chatInput = '';
    this.cdr.detectChanges();
    // Scroll the message list to the bottom
    setTimeout(() => {
      const el = document.getElementById('chat-messages');
      if (el) el.scrollTop = el.scrollHeight;
    }, 50);
  }

  onChatKeydown(event: KeyboardEvent) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendChatMessage();
    }
  }

  /* --- Moderator Actions --- */
  
  admitUser(identity: string) {
      this.sendData({ action: 'admit', target: identity });
      this.waitingUsers = this.waitingUsers.filter(u => u !== identity);
      this.messages.push({ sender: 'System', text: `Admitted ${identity}`});
  }

  muteUser(identity: string) {
      this.sendData({ action: 'mute', target: identity });
  }

  kickUser(identity: string) {
      this.sendData({ action: 'kick', target: identity });
  }


  /* --- Local Media Toggles --- */

  async toggleAudio(state?: boolean) {
      if(!this.room) return;
      this.audioEnabled = state !== undefined ? state : !this.audioEnabled;
      await this.room.localParticipant.setMicrophoneEnabled(this.audioEnabled);
      this.cdr.detectChanges();
  }

  async toggleVideo() {
     if(!this.room) return;
     this.videoEnabled = !this.videoEnabled;
     await this.room.localParticipant.setCameraEnabled(this.videoEnabled);
     
     if (this.videoEnabled) {
         this.attachLocalVideo();
     }
     this.cdr.detectChanges();
  }

  async toggleScreenShare() {
     if(!this.room) return;
     try {
       this.isScreenSharing = !this.isScreenSharing;
       await this.room.localParticipant.setScreenShareEnabled(this.isScreenSharing);
       
       if (this.isScreenSharing) {
           // Attach local screenshare track to center stage if published successfully
           setTimeout(() => {
             this.room!.localParticipant.videoTrackPublications.forEach((pub: LocalTrackPublication) => {
               if (pub.track && pub.source === Track.Source.ScreenShare && this.screenShareElement) {
                   pub.track.attach(this.screenShareElement.nativeElement);
               }
             });
           }, 200);
       }
     } catch (e) {
         console.warn("Screen share cancelled or failed.", e);
         this.isScreenSharing = false;
     }
     this.cdr.detectChanges();
  }

  toggleHand() {
      this.isHandRaised = !this.isHandRaised;
      this.sendData({ action: 'raise_hand', state: this.isHandRaised, identity: this.participantName });
  }

  async toggleRecord() {
      if (!this.roomId) return;
      
      try {
          if (!this.isRecording) {
              const res = await this.apiService.startRecording(this.roomId);
              this.activeEgressId = res.egressId;
              this.isRecording = true;
              this.messages.push({ sender: 'System', text: "Recording started." });
          } else {
              if (this.activeEgressId) {
                  await this.apiService.stopRecording(this.activeEgressId);
                  this.isRecording = false;
                  this.activeEgressId = null;
                  this.messages.push({ sender: 'System', text: "Recording stopped." });
              }
          }
          this.cdr.detectChanges();
      } catch (e: any) {
          console.error("Recording error:", e);
          this.messages.push({ sender: 'System', text: `Error: ${e.message}` });
      }
  }

  async endRoom() {
      if (!this.roomId || !confirm("Are you sure you want to end this room for everyone?")) return;
      try {
          await this.apiService.deleteRoom(this.roomId);
          this.disconnect();
      } catch (e) {
          console.error("Failed to end room", e);
      }
  }


  /* --- UI Helpers & Track Management --- */

  hasVideo(participant: RemoteParticipant): boolean {
     return participant.isCameraEnabled;
  }
  
  hasAudio(participant: RemoteParticipant): boolean {
     return participant.isMicrophoneEnabled;
  }
  
  getHandRaised(identity: string): boolean {
     return this.raisedHands.has(identity);
  }

  getParticipantCount(): number {
     return (this.room ? this.room.remoteParticipants.size : 0) + (this.connected || this.isModerator ? 1 : 0);
  }

  attachLocalVideo() {
    if (!this.room || !this.room.localParticipant) return;
    setTimeout(() => {
      this.room!.localParticipant.videoTrackPublications.forEach((pub: LocalTrackPublication) => {
        if (pub.track && pub.source === Track.Source.Camera && this.localVideoElement) {
          pub.track.attach(this.localVideoElement.nativeElement);
        }
      });
    }, 100);
  }

  addRemoteTrack(track: RemoteTrack, participant: RemoteParticipant) {
    // Screen share → route to central stage
    if (track.source === Track.Source.ScreenShare) {
      this.isScreenSharing = true;
      this.cdr.detectChanges();
      setTimeout(() => {
        if (this.screenShareElement) {
          track.attach(this.screenShareElement.nativeElement);
        }
      }, 100);
      return;
    }

    // Only handle Camera and Microphone tracks for the grid
    if (track.source !== Track.Source.Camera && track.source !== Track.Source.Microphone) return;

    // 1. Update the participants list → triggers *ngFor to add the tile
    this.updateRemoteParticipants();
    // 2. Force Angular to synchronously apply DOM changes
    this.cdr.detectChanges();

    const elementId = `media-${participant.identity}-${track.source}`;

    // 3. Poll until the dedicated .video-track div appears in the DOM (max 2s)
    let attempts = 0;
    const tryAttach = () => {
      attempts++;
      const container = document.getElementById(`video-track-${participant.identity}`);

      if (!container) {
        if (attempts < 20) {
          setTimeout(tryAttach, 100);
        } else {
          console.warn(`[LiveKit] Could not find container for ${participant.identity}`);
        }
        return;
      }

      // Remove any previous element for this track (e.g. on reconnect)
      const existing = document.getElementById(elementId);
      if (existing) {
        existing.remove();
      }

      // Use LiveKit's built-in attach() to get a properly wired media element
      // track.attach() with no args creates the element AND binds the stream
      const element = track.attach() as HTMLMediaElement;
      element.id = elementId;

      // Apply styles so the video fills the container tile
      element.style.width = '100%';
      element.style.height = '100%';
      element.style.objectFit = 'cover';
      element.style.display = 'block';
      element.style.position = 'absolute';
      element.style.top = '0';
      element.style.left = '0';

      container.appendChild(element);

      // Attempt play in case autoplay was blocked
      (element as HTMLVideoElement).play?.().catch(() => {});

      this.updateRemoteParticipants();
      this.cdr.detectChanges();
    };

    tryAttach();
  }


  removeRemoteTrack(track: RemoteTrack) {
    track.detach();
    if (track.source === Track.Source.ScreenShare) {
        this.isScreenSharing = false;
        this.cdr.detectChanges();
    } else {
        const elements = document.querySelectorAll(`[id$="-${track.source}"]`);
        elements.forEach(el => el.remove());
        this.updateRemoteParticipants();
    }
  }

  updateRemoteParticipants() {
     if (!this.room) return;
     this.remoteParticipants = Array.from(this.room.remoteParticipants.values()).map(p => ({
         participant: p,
         trackPubs: Array.from(p.trackPublications.values())
     }));
     this.cdr.detectChanges();
  }

  handleDisconnect() {
    if (this.roomId && this.participantName) {
      this.apiService.leaveRoom(this.roomId, this.participantName);
    }
    this.token = null;
    this.room = null;
    this.connected = false;
    this.remoteParticipants = [];
    this.raisedHands.clear();
    this.waitingUsers = [];
    this.audioEnabled = true;
    this.videoEnabled = true;
    this.isScreenSharing = false;
    this.roomId = null;
    this.livekitUrl = null;
  }

  disconnect() {
    if (this.room) {
      this.room.disconnect();
    }
  }

  ngOnDestroy() {
    this.disconnect();
  }
}
