import { useEffect, useState } from "react"
import { useLocation, useParams, useNavigate } from "react-router-dom";
import GameHeader from "./components/GameHeader";
import PlayerArea from "./components/PlayerArea";
import GameFooter from "./components/GameFooter";
import Playing from "./components/Playing";
import './Gameroom.css';
import coin from "./assets/coin/coin.png";


function Gameroom({ sendMessage, lastJsonMessage }) {
    const { roomId } = useParams();
    const location = useLocation();
    const navigate = useNavigate();

    // State
    const [ playersNum, setPlayersNum ] = useState(0);
    const [ players, setPlayers] = useState([]);
    const [chatMessages, setChatMessages] = useState([]);
    const [ isHost, setIsHost ] = useState(false);
    const [ isPlaying, setIsPlaying] = useState(false);

    // Receive user data from Lobby.jsx
    const { username, userCredit } = location.state || {};

    // Redirect if user typed URL manually
    useEffect(() => {
        if (!username) {
            alert("Please login first!");
            navigate("/");
        }
    }, [username, navigate]);

    // Handle Messages from Server
    useEffect(() => {
        if (lastJsonMessage !== null) {
            const { type, payload } = lastJsonMessage;

            console.log("Received message:", type, payload); // Debug log

            // Handle Join/Updates
            if (type === 'JOIN_SUCCESS' || type === 'PLAYER_JOINED' || type === 'PLAYER_LEFT') {
                if (type === 'PLAYER_JOINED') {
                    setChatMessages(prev => [...prev, "Someone has join the room."]);
                } else if (type === 'PLAYER_LEFT') {
                    setChatMessages(prev => [...prev, "Someone has left the room."]);
                }
                
                setPlayersNum(payload.playersNum);
                setPlayers(payload.players);

                // Check if I am the host
                const me = payload.players.find(p => p.username === username);
                if (me) setIsHost(me.host);
            }
            
            // Handle Create Success (if applicable here)
            if (type === 'CREATE_SUCCESS') {
                setChatMessages(prev => [...prev, "Create Room Success."]);
                setChatMessages(prev => [...prev, `Room Code: ${roomId}`]);
                setIsHost(true);
                setPlayersNum(payload.playersNum);
                setPlayers(payload.players);
            }

            // Handle Game Started - สำหรับทุกคนในห้อง
            if (type === 'GAME_STARTED') {
                setChatMessages(prev => [...prev, "Game Started!"]);
                setIsPlaying(true);
            }

            // Handle Game State - เมื่อได้รับ state จาก game engine
            if (type === 'GAME_STATE') {
                // ถ้ายังไม่ได้เข้าโหมดเล่น ให้เข้าเลย
                if (!isPlaying) {
                    setIsPlaying(true);
                }
            }
        }
    }, [lastJsonMessage, username, isPlaying]);

    // New Reconnect method -> let Backend manages = Backend trigger reconnect logic
    useEffect(() => {
        if (username && roomId) {
            sendMessage(JSON.stringify({
                action: "join_room",
                data: { username, roomId }
            }));
        }
    }, [username, roomId, sendMessage]);

    // Start (get BigBlind from Footer)
    const handleStartGame = (bigBlindValue) => {
        console.log(`Start Game Big Blind = ${bigBlindValue}`);
        
        sendMessage(JSON.stringify({
            action: "start_game",
            data: {
                roomId: roomId,
                bigBlind: parseInt(bigBlindValue) || 100
            }
        }));

        // ไม่ต้อง setIsPlaying ที่นี่ เพราะจะรอ broadcast จาก server
    };

    const handleExitRoom = () => {
        sendMessage(JSON.stringify({
            action: "leave_room",
            data: { username, roomId }
        }));
        
        navigate('/Lobby', {
            state: { username, userCredit }
        });
    };

    if (isPlaying) {
        return (
                <Playing 
                    sendMessage={sendMessage} 
                    lastJsonMessage={lastJsonMessage}
                    username={username}
                    chatMessages={chatMessages}
                    roomId={roomId}
                    navigate={navigate}
                    onExit={handleExitRoom}
                />
        )
    }

    return (
        <div className="game-container font-pixel">
            
            {/* Header: แสดงเงินและจำนวนคน */}
            <GameHeader 
                userCredit={userCredit} 
                playersCount={playersNum} 
                roomId={roomId} 
                onExit={handleExitRoom}
            /> 

            {/* Player Area: แสดงตัวละครรอบโต๊ะ */}
            <PlayerArea players={players} />

            {/* Footer: แชท, กฎ, และปุ่ม Start */}
            <GameFooter 
                onStartGame={handleStartGame} 
                IsHost={isHost}
                chatMessages={chatMessages}
            />

        </div>
    );
}

export default Gameroom