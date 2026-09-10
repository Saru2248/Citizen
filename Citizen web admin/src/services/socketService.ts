import { io, Socket } from 'socket.io-client';

let socket: Socket | null = null;

export const initSocket = (): Socket => {
  if (!socket) {
    const socketUrl = window.location.origin.includes('3000') || window.location.origin.includes('5173')
      ? 'http://localhost:8000'
      : window.location.origin;

    socket = io(socketUrl, {
      transports: ['polling', 'websocket'],
      reconnectionAttempts: 10,
    });

    socket.on('connect', () => {
      console.log(`[Socket.IO Admin] Connected: ${socket?.id}`);
    });

    socket.on('disconnect', () => {
      console.log('[Socket.IO Web Admin] Disconnected');
    });
  }
  return socket;
};

export const getSocket = (): Socket | null => socket;
