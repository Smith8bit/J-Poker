import { useEffect, useState } from "react"
import { useLocation, useParams, useNavigate} from "react-router-dom";
import GameHeader from "./components/GameHeader";
import PlayerArea from "./components/PlayerArea";
import GameFooter from "./components/GameFooter";
import './Gameroom.css';

function Gameroom({ sendMessage, lastJsonMessage }) {
    const { roomId } = useParams();
    const location = useLocation();
    const navigate = useNavigate();
    const [ playersNum, setplayersNum ] = useState(0);
    const { username, userCredit } = location.state || {};
    const players = [
                    { name: username, avatar: 'bulbasaur' },
                    { name: 'BOT_1', avatar: 'charmander' }
                ] //For Test Frontend

    useEffect(() => {
        if (!username) {
            alert("Please login first!");
            navigate("/");
        }
    }, [username, navigate]);

    useEffect(() => {
        if (username && roomId) {
            console.log(`Connecting to Room: ${roomId} as ${username}`);
        }
    }, [username, roomId]);

    // reconnect
    useEffect(() => {
        if (username && roomId) {
            console.log(`Reconnecting to Room: ${roomId} as ${username}`);
            
            // Send join request to rejoin with new WebSocket session
            sendMessage(JSON.stringify({
                action: "join_room",
                data: {
                    username: username,
                    roomId: roomId
                }
            }));
        }
    }, [username, roomId, sendMessage]);

    // Use for receive message form SERVER
    useEffect(() => {
        if (lastJsonMessage !== null) {
            const { type, payload } = lastJsonMessage;
            console.log(lastJsonMessage);

            if (type === 'CREATE_SUCCESS') {
                setplayersNum(1);
            } else {
                setplayersNum(payload.playersNum);
                // console.log(payload.players);
            }
        }
    }, [lastJsonMessage]);

    const handleStartGame = () => {
        console.log("Start Game Clicked");
    };

    return (
        <div className="game-container font-pixel">
            
            {/* file in components/GaneHeader */}
            <GameHeader 
                userCredit={userCredit} 
                playersCount={playersNum} 
                roomId={roomId} 
            /> 

            {/* file in components/PlayerArea */}
            <PlayerArea players={players} />

            {/* file in components/GameFooter */}
            <GameFooter onStartGame={handleStartGame} />

        </div>
    );
}

export default Gameroom