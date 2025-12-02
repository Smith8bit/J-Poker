import { useEffect, useState } from "react"
import { useLocation, useParams, useNavigate} from "react-router-dom";
import './Gameroom.css'

function Gameroom({ sendMessage, lastJsonMessage }) {
    const { roomId } = useParams();

    const location = useLocation();
    const navigate = useNavigate();

    const [ playersNum, setplayersNum ] = useState(0);

    const { username, userCredit } = location.state || {};

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

    // Use for receive message form SERVER
    useEffect(() => {
        if (lastJsonMessage !== null) {
            const { type, payload } = lastJsonMessage;

            if (type === 'PLAYER_JOINED' || type === 'JOIN_SUCCESS') {
                setplayersNum(payload.playersNum);
            } else if (type === 'CREATE_SUCCESS') {
                setplayersNum(payload.playersNum || 1);
            }
        }
    }, [lastJsonMessage]);

    return (
        <>
            <h3 id="Room_ID">Room ID: {roomId}</h3>
            <div style={{ padding: '20px', color: 'white', backgroundColor: '#35654d', minHeight: '100vh' }}>
                <h1>Game Room</h1>
                
                <div style={{ border: '1px solid white', padding: '10px', marginBottom: '20px' }}>
                    <p>Player: {username}</p>
                    <p>Credits: ${userCredit}</p>
                </div>


                <div className="game-board">
                    <p>Waiting for other players...</p>
                    <h3>PLAYER: {playersNum}/6</h3>
                </div>
                
            </div>
        </>
    );
}

export default Gameroom