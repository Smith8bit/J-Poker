import { useLocation, useNavigate } from "react-router-dom";
import './Lobby.css'
import { useState } from "react";
import axios from 'axios';

function Lobby() {
    const location = useLocation();
    const navigate = useNavigate();
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const [roomId, setroomId] = useState('');
    const { username, moneyAmount } = location.state;
    
    const handleJoinRoom = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);
        
        try {
            const response = await axios.post('http://localhost:8080/api/joinRoom', {
                username,
                roomId
            });
            
            const { isFull, playersNum, players } = response.data;
            
            if (isFull) {
                setError('Room is full');
                setLoading(false);
                return;
            }
            
            // Navigate to room with all data
            navigate(`/room/${roomId}`, {
                state: {
                    username,
                    isFull,
                    playersNum,
                    players
                }
            });
            
        } catch (err) {
            setError(err.response?.data?.message || err.message || 'An error occurred');
            setLoading(false);
        }
    }
    
    const handleCreateRoom = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);
        
        try {
            const response = await axios.post('http://localhost:8080/api/createRoom', {
                username
            });
            
            const { roomId, isFull, playersNum, players } = response.data;
            
            // Navigate to the newly created room
            navigate(`/room/${roomId}`, {
                state: {
                    username,
                    isFull,
                    playersNum,
                    players
                }
            });
            
        } catch (err) {
            setError(err.response?.data?.message || err.message || 'An error occurred');
            setLoading(false);
        }
    }
    
    return (
        <>
        <h3 id="username">{username} {moneyAmount}</h3>
        <h1>COM SCI<br/>POKER</h1>
        <div className="menu">
            <form onSubmit={handleJoinRoom}>
                <input 
                type="text"
                value={roomId}
                onChange={(e) => setroomId(e.target.value)}
                placeholder="Enter Room Code"
                required
                disabled={loading}
                />
                <br></br>
                <button type="submit" disabled={loading}>
                    {loading ? 'Joining...' : 'Join Room'}
                </button>
            </form>

            <div id="divider"/>

            <button id="createRoom" type="button" onClick={handleCreateRoom} disabled={loading}>
                {loading ? 'Creating...' : 'Create Room'}
            </button>
        </div>
        {error && <p style={{ color: 'red' }}>{error}</p>}
        <button id="wrongName">Wait, my name is wrong.</button>
        </>
    )
}

export default Lobby