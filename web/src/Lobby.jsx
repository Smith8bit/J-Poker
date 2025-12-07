import { useEffect, useState, useRef } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import axios from 'axios';
import './Lobby.css';

function Lobby({ sendMessage, lastJsonMessage }) {
    const location = useLocation();
    const navigate = useNavigate();
    
    // Safety check: if location.state is null, default to empty object
    const { username } = location.state || {};
    
    const [userCredit, setUserCredit] = useState(0); 
    const [roomId, setroomId] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const prevMessageRef = useRef(lastJsonMessage); 

    // 1. LISTEN: Watch for messages from the server
    useEffect(() => {
        if (lastJsonMessage !== null && lastJsonMessage !== prevMessageRef.current) {
            const { type, payload } = lastJsonMessage;
            
            if(loading){
                if ((type === 'JOIN_SUCCESS' || type === 'CREATE_SUCCESS')) {
                    navigate(`/room/${payload.roomId}`, {
                        state: { username, userCredit } // Pass the updated credit
                    });
                } else if (type === 'JOIN_ERROR' || type === 'CREATE_ERROR') {
                    setError(payload.error);
                    setLoading(false);
                }
            }
            prevMessageRef.current = lastJsonMessage;
        }
    }, [lastJsonMessage, navigate, loading, username, userCredit]);

    // 2. FETCH DATA: Runs every time this page loads (mounts)
    useEffect(() => {
        const fetchUserInfo = async () => {
            if (!username) return;

            try {
                // Use 'await' to wait for the server response
                const response = await axios.post('http://localhost:8080/api/getUserInfo', {
                    username
                });
                
                // Update state with fresh data
                if (response.data && response.data.userCredit !== undefined) {
                    setUserCredit(response.data.userCredit);
                }
            } catch (err) {
                console.error(err);
                setError(err.response?.data?.message || 'Could not sync user data');
            }
        };

        fetchUserInfo();
    }, [username]); // Dependency on username ensures it runs on load

    // 3. SEND: Trigger actions via WebSocket
    const handleJoinRoom = (e) => {
        e.preventDefault(); // <--- CRITICAL: Prevents page refresh on form submit

        prevMessageRef.current = lastJsonMessage;
        setError('');
        setLoading(true);
        
        sendMessage(JSON.stringify({
            action: 'join_room',
            data: { username, roomId }
        }));
    };

    const handleCreateRoom = () => {
        prevMessageRef.current = lastJsonMessage;
        setError('');
        setLoading(true);

        sendMessage(JSON.stringify({
            action: 'create_room',
            data: { username }
        }));        
    };

    return (
        <div className="center-layout">
            <h3 id="username">{username} : ${userCredit}</h3>
            <h1>COM SCI<br/>POKER</h1>
            
            <div className="menu">
                {/* Add onSubmit here to handle "Enter" key correctly */}
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
                    {/* type="submit" works with the form onSubmit now */}
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

            <button id="wrongName" onClick={() => navigate('/')}>
                Wait, my name is wrong.
            </button>
            
            {error && <p style={{position:'fixed', color: 'red', marginTop: '10px', fontSize: '2em', left:'45%'}}>{error}</p>}
        </div>
    );
}

export default Lobby;