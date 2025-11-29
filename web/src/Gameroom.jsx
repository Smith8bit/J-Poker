import { useEffect, useState, useRef } from 'react';
import { useParams, useLocation } from 'react-router-dom';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import axios from 'axios';

function Gameroom() {
    const { roomId } = useParams();
    const location = useLocation();
    const { username } = location.state || {};
    
    const [players, setPlayers] = useState(location.state?.players || []);
    const [playersNum, setPlayersNum] = useState(location.state?.playersNum || 0);
    const [isFull, setIsFull] = useState(location.state?.isFull || false);
    const [connected, setConnected] = useState(false);
    const [chatMessages, setChatMessages] = useState([]);
    const [chatInput, setChatInput] = useState('');
    const stompClientRef = useRef(null);
    const heartbeatIntervalRef = useRef(null);

    useEffect(() => {
        // Create WebSocket connection
        const socket = new SockJS('http://localhost:8080/ws');
        const client = new Client({
            webSocketFactory: () => socket,
            
            onConnect: () => {
                console.log('Connected to WebSocket');
                setConnected(true);
                
                // Subscribe to room updates
                client.subscribe(`/topic/room/${roomId}`, (message) => {
                    const data = JSON.parse(message.body);
                    console.log('Received:', data);
                    
                    if (data.type === 'PLAYER_JOINED' || data.type === 'PLAYER_LEFT') {
                        setPlayers(data.players);
                        setPlayersNum(data.playersNum);
                        setIsFull(data.isFull);
                        
                        // Add system message
                        const systemMsg = data.type === 'PLAYER_JOINED' 
                            ? `${data.username} joined the room`
                            : `${data.username} left the room`;
                        setChatMessages(prev => [...prev, { type: 'system', message: systemMsg }]);
                    }
                    
                    if (data.type === 'CHAT') {
                        setChatMessages(prev => [...prev, { 
                            type: 'chat', 
                            username: data.username, 
                            message: data.message 
                        }]);
                    }
                });
                
                // Notify others you joined
                if (client.active) {
                    client.publish({
                        destination: `/app/room/${roomId}/join`,
                        body: username
                    });
                }
                
                // Start heartbeat - send every 5 seconds
                heartbeatIntervalRef.current = setInterval(async () => {
                    try {
                        await axios.post('http://localhost:8080/api/heartbeat', {
                            username,
                            roomCode: roomId
                        });
                        console.log('Heartbeat sent');
                    } catch (err) {
                        console.error('Heartbeat failed:', err);
                    }
                }, 5000);
            },
            
            onDisconnect: () => {
                console.log('Disconnected from WebSocket');
                setConnected(false);
            },
            
            onStompError: (frame) => {
                console.error('STOMP error:', frame);
            }
        });
        
        client.activate();
        stompClientRef.current = client;
        
        // Handle browser tab/window close
        const handleBeforeUnload = () => {
            // Stop heartbeat
            if (heartbeatIntervalRef.current) {
                clearInterval(heartbeatIntervalRef.current);
            }
            
            const currentClient = stompClientRef.current;
            if (currentClient && currentClient.active) {
                try {
                    // Send leave message synchronously
                    navigator.sendBeacon(
                        'http://localhost:8080/api/leaveRoom',
                        JSON.stringify({ username, roomCode: roomId })
                    );
                } catch (err) {
                    console.log('Error sending leave beacon:', err);
                }
            }
        };
        
        window.addEventListener('beforeunload', handleBeforeUnload);
        
        // Cleanup on unmount (for navigation within app)
        return () => {
            window.removeEventListener('beforeunload', handleBeforeUnload);
            
            // Stop heartbeat
            if (heartbeatIntervalRef.current) {
                clearInterval(heartbeatIntervalRef.current);
            }
            
            const currentClient = stompClientRef.current;
            if (currentClient && currentClient.active) {
                // Notify others you're leaving (only if connected)
                try {
                    currentClient.publish({
                        destination: `/app/room/${roomId}/leave`,
                        body: username
                    });
                } catch (err) {
                    console.log('Error sending leave message:', err);
                }
                currentClient.deactivate();
            }
        };
    }, [roomId, username]);

    const sendChatMessage = (e) => {
        e.preventDefault();
        const client = stompClientRef.current;
        if (chatInput.trim() && client && client.active) {
            try {
                client.publish({
                    destination: `/app/room/${roomId}/chat`,
                    body: JSON.stringify({
                        username: username,
                        message: chatInput
                    })
                });
                setChatInput('');
            } catch (err) {
                console.error('Error sending chat:', err);
            }
        }
    };

    return (
        <div style={{ padding: '20px' }}>
            <h1>Room: {roomId}</h1>
            <div style={{ display: 'flex', gap: '20px' }}>
                {/* Players Panel */}
                <div style={{ flex: 1, border: '1px solid #ccc', padding: '10px' }}>
                    <h2>Players ({playersNum}/6) {isFull && '(Full)'}</h2>
                    <div style={{ 
                        display: 'flex', 
                        alignItems: 'center', 
                        gap: '10px',
                        marginBottom: '10px'
                    }}>
                        <div style={{
                            width: '10px',
                            height: '10px',
                            borderRadius: '50%',
                            backgroundColor: connected ? '#00ff00' : '#ff0000'
                        }}></div>
                        <span>{connected ? 'Connected' : 'Connecting...'}</span>
                    </div>
                    <ul>
                        {players.map((player, index) => (
                            <li key={index} style={{ 
                                padding: '5px',
                                backgroundColor: player === username ? '#e3f2fd' : 'transparent'
                            }}>
                                {player} {player === username && '(You)'}
                            </li>
                        ))}
                    </ul>
                </div>

                {/* Chat Panel */}
                <div style={{ flex: 2, border: '1px solid #ccc', padding: '10px' }}>
                    <h2>Chat</h2>
                    <div style={{ 
                        height: '300px', 
                        overflowY: 'auto', 
                        border: '1px solid #ddd',
                        padding: '10px',
                        marginBottom: '10px',
                        backgroundColor: '#f9f9f9'
                    }}>
                        {chatMessages.length === 0 ? (
                            <p style={{ color: '#999' }}>No messages yet...</p>
                        ) : (
                            chatMessages.map((msg, index) => (
                                <div key={index} style={{ 
                                    marginBottom: '5px',
                                    fontStyle: msg.type === 'system' ? 'italic' : 'normal',
                                    color: msg.type === 'system' ? '#666' : '#000'
                                }}>
                                    {msg.type === 'system' ? (
                                        msg.message
                                    ) : (
                                        <><strong>{msg.username}:</strong> {msg.message}</>
                                    )}
                                </div>
                            ))
                        )}
                    </div>
                    <form onSubmit={sendChatMessage} style={{ display: 'flex', gap: '10px' }}>
                        <input
                            type="text"
                            value={chatInput}
                            onChange={(e) => setChatInput(e.target.value)}
                            placeholder="Type a message..."
                            style={{ flex: 1, padding: '5px' }}
                            disabled={!connected}
                        />
                        <button type="submit" disabled={!connected}>Send</button>
                    </form>
                </div>
            </div>
        </div>
    );
}

export default Gameroom;