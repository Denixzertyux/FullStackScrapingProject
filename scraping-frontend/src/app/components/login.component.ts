import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../services/auth.service'; // Ajustează calea dacă e nevoie

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="login-container">
      <h2>Autentificare Autobrand</h2>
      <input type="text" [(ngModel)]="username" placeholder="Nume utilizator" />
      <input type="password" [(ngModel)]="password" placeholder="Parola" />
      <button (click)="onLogin()">Intră în cont</button>
    </div>
  `,
  styles: [`
    .login-container {
      max-width: 300px;
      margin: 100px auto;
      display: flex;
      flex-direction: column;
      gap: 15px;
      text-align: center;
      font-family: Arial, sans-serif;
    }
    input { padding: 10px; border: 1px solid #ccc; border-radius: 5px; }
    button { padding: 10px; background-color: #0056b3; color: white; border: none; border-radius: 5px; cursor: pointer; }
    button:hover { background-color: #004494; }
  `]
})
export class LoginComponent {
  username = '';
  password = '';

  constructor(private authService: AuthService) {}

  onLogin(): void {
    // Validare simplă la nivel de frontend
    if (this.username === 'admin' && this.password === 'autobrand2026') {
      this.authService.login(this.username, this.password);
    } else {
      alert('Nume de utilizator sau parolă incorecte!');
    }
  }
}