import { Routes } from '@angular/router';
import { LoginComponent } from './components/login/login.component';
import { ChatRoomComponent } from './components/chat-room/chat-room.component';

export const routes: Routes = [
  { path: '', component: LoginComponent },
  { path: 'chat', component: ChatRoomComponent },
  { path: '**', redirectTo: '' }
];
