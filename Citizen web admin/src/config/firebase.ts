import { initializeApp } from 'firebase/app';
import { getAuth } from 'firebase/auth';
import { getFirestore } from 'firebase/firestore';
import { getStorage } from 'firebase/storage';

const firebaseConfig = {
  apiKey: "AIzaSyB04_noNdFTfw70QDBQjZ-rFF1o-t3DboU",
  authDomain: "citizen-ai-7d63b.firebaseapp.com",
  projectId: "citizen-ai-7d63b",
  storageBucket: "citizen-ai-7d63b.firebasestorage.app",
  messagingSenderId: "1001811384485",
  appId: "1:1001811384485:web:01f320021b46965d87154c"
};

const app = initializeApp(firebaseConfig);

export const auth = getAuth(app);
export const db = getFirestore(app);
export const storage = getStorage(app);
export default app;
