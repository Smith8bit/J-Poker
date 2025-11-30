import { useLocation, useNavigate } from "react-router-dom";
import { useState, useEffect } from "react";
import axios from 'axios';
import './Lobby.css';

const API_BASE_URL = 'http://localhost:8080/api';

function Lobby() {
    const location = useLocation();
    const navigate = useNavigate();
    
    const { username, userCredit } = location.state || {};

    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const [roomId, setroomId] = useState('');

    useEffect(() => {
        if (!username) {
            navigate('/');
        }
    }, [username, navigate]);

    const handleJoinRoom = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);
        
        try {
            const response = await axios.post(`${API_BASE_URL}/joinRoom`, {
                username,
                roomId
            });
            
            const { isFull } = response.data;
            
            if (isFull) {
                setError('Room is full');
                setLoading(false);
                return;
            }
            
            navigate(`/room/${roomId}`, {
                state: { username, userCredit }
            });
            
        } catch (err) {
            console.error("Join Error:", err);
            setError(err.response?.data?.message || 'Room does not exist or server error');
            setLoading(false);
        }
    }
    
    const handleCreateRoom = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);
        
        try {
            const response = await axios.post(`${API_BASE_URL}/createRoom`, {
                username
            });
            
            const { roomId: newRoomId } = response.data;
            
            navigate(`/room/${newRoomId}`, {
                state: { username, userCredit }
            });
            
        } catch (err) {
            console.error("Create Error:", err);
            setError(err.response?.data?.message || 'Failed to create room');
            setLoading(false);
        }
    }

    if (!username) return null; 
    
    return (
        <>
            <h3 id="username">{username} : ${userCredit}</h3>
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
                    <br />
                    <button type="submit" disabled={loading}>
                        {loading ? 'Joining...' : 'Join Room'}
                    </button>
                </form>

                <div id="divider"/>

                <button 
                    id="createRoom" 
                    type="button" 
                    onClick={handleCreateRoom} 
                    disabled={loading}
                >
                    {loading ? 'Creating...' : 'Create Room'}
                </button>
            </div>

            {error && <p style={{ color: 'red', marginTop: '10px' }}>{error}</p>}
            
            {/* Added functionality to go back to Login */}
            <button id="wrongName" onClick={() => navigate('/')}>
                Wait, my name is wrong.
            </button>
        </>
    )
}

export default Lobby;