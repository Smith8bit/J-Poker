import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import './index.css'
import App from './App.jsx'
import Lobby from './Lobby.jsx'
import Gameroom from './Gameroom.jsx'


createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<App />} />
        <Route path="/Lobby" element={<Lobby />} />
        <Route path='/room/roomId' element={<Gameroom />}/>
      </Routes>
    </BrowserRouter>
  </StrictMode>
)