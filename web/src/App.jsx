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
      const response = await axios.post('http://localhost:8080/api/join', {
        username
      });
      
      // Expecting response.data to have { username, cashValue }
      const { username: returnedUsername, moneyAmount } = response.data;
      
      navigate('/Lobby', { 
        state: { 
          username: returnedUsername, 
          moneyAmount 
        } 
      });
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'An error occurred');
      setLoading(false);
    }
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
          disabled={loading}
        />
        <br></br>
        <button type="submit" disabled={loading}>
          {loading ? 'Joining...' : 'Join Game'}
        </button>
        {error && <p style={{ color: 'red' }}>{error}</p>}
      </form>
    </>
  )
}

export default App