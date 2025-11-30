import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import axios from 'axios'
import './App.css'

function App() {
  const [username, setUsername] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();
  
  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    
    try {
      const response = await axios.post('http://localhost:8080/api/getUserInfo', {
        username
      });
      
      // Expecting response.data to have { username, cashValue }
      const { username: returnedUsername, userCredit } = response.data;
      
      navigate('/Lobby', { 
        state: { 
          username: returnedUsername, 
          userCredit 
        } 
      });
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'An error occurred');
      setLoading(false);
    }
  }
  
  return (
    <>
      <h1 className="gametitle">COM SCI<br/>POKER</h1>
      <form onSubmit={handleSubmit}>
        <input className="strInput" 
          type="text"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          placeholder="Enter username"
          required
          disabled={loading}
        />
        <br></br>
        <button type="submit" disabled={loading}>
          {loading ? 'Joining...' : 'PLAY'}
        </button>
        {error && <p style={{ color: 'red' }}>{error}</p>}
      </form>
    </>
  )
}

export default App