// App.jsx (New "Manager" File)
import { Routes, Route } from 'react-router-dom'
import useWebSocket from 'react-use-websocket'
import Login from './Login'      // Your old App.jsx
import Lobby from './Lobby'
import Gameroom from './Gameroom'
import './App.css'

function App() {
  // 1. Establish the Global Connection Here
  // This connection will stay open even when pages change!
  const { sendMessage, lastJsonMessage } = useWebSocket('ws://localhost:8080/ws', {
    onOpen: () => console.log('WebSocket Connected'),
    shouldReconnect: (closeEvent) => true,
  });

  return (
    <>
    <Routes>
      <Route className="App" path="/" element={<Login />} />
      <Route
        className="App"
        path="/Lobby" 
        element={<Lobby sendMessage={sendMessage} lastJsonMessage={lastJsonMessage} />} 
      />
      <Route 
          path="/room/:roomId" 
          element={<Gameroom sendMessage={sendMessage} lastJsonMessage={lastJsonMessage} />} 
      />
    </Routes>
    </>
  )
}

export default App