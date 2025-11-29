import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import './App.css'

function App() {
  const [username, setUsername] = useState('');
  const navigate = useNavigate();
  
  const handleSubmit = (e) => {
    e.preventDefault();
    navigate('/LobbyOption', {state: {username}});
  }
  
  return (
    <>
      <h1>COM SCI<br/>POKER</h1>
      <form onSubmit={handleSubmit}>
        <input 
          type="text"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          placeholder="Enter username"
          required
        />
        <br></br>
        <button type="submit">Join Game</button>
      </form>
    </>
  )
}

export default App
