import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import './Lobby.css'

function Lobby({ sendMessage, lastJsonMessage }) {
    const location = useLocation();
    const navigate = useNavigate();
    const { username, userCredit } = location.state || {};
    const [roomId, setroomId] = useState('');
    const [loading, setLoading] = useState(false);
    const [fromLobby, setFromLobby] = useState(true);
    const [error, setError] = useState('');

    // 1. LISTEN: Watch for messages from the server
    useEffect(() => {
        if (lastJsonMessage !== null) {
            const { type, payload } = lastJsonMessage;
            
            if (type === 'JOIN_SUCCESS' || type === 'CREATE_SUCCESS') {
                navigate(`/room/${payload.roomId}`, {
                    
                    state: { username, userCredit, fromLobby }
                });
            } else {
                setError(payload.error);
                setLoading(false);
            }

        }
    }, [lastJsonMessage, navigate]);

    // 2. SEND: Trigger actions via WebSocket
    const handleJoinRoom = () => {
        setError('');
        setLoading(true);
        
        // Send JSON request
        sendMessage(JSON.stringify({
            action: 'join_room',
            data: { username, roomId }
        }));
    }

    const handleCreateRoom = () => {
        setError('');
        setLoading(true);

        sendMessage(JSON.stringify({
            action: 'create_room',
            data: { username }
        }));        
    }

    return (
        <div class="center-layout">
            <h3 id="username">{username} : ${userCredit}</h3>
            <h1>COM SCI<br/>POKER</h1>
            
            <div className="menu">
                <form>
                    <input 
                        type="text"
                        value={roomId}
                        onChange={(e) => setroomId(e.target.value)}
                        placeholder="Enter Room Code"
                        required
                        disabled={loading}
                    />
                    <br />
                    <button type="submit" disabled={loading} onClick={handleJoinRoom}>
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

            
            {/* Added functionality to go back to Login */}
            <button id="wrongName" onClick={() => navigate('/')}>
                Wait, my name is wrong.
            </button>
            
            {error && <p style={{ color: 'red', marginTop: '10px' }}>{error}</p>}
        </div>
    )
}

export default Lobby;