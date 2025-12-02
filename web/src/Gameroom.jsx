import { useEffect, useState } from "react"
import { useLocation, useParams, useNavigate} from "react-router-dom";
import './Gameroom.css'

function Gameroom({ sendMessage, lastJsonMessage }) {
    const { roomId } = useParams();

    const location = useLocation();
    const navigate = useNavigate();

    const [ playersNum, setplayersNum ] = useState(0);
    const [ players, setPlayers] = useState([]);

    const { username, userCredit } = location.state || {};

    useEffect(() => {
        if (!username) {
            alert("Please login first!");
            navigate("/");
        }
    }, [username, navigate]);

    // Use for receive message form SERVER
    // To KENG: ตอนนี้ใช้แนวคิด ไม่ว่า Back End จะส่งข้อความอะไรมา จะUPDATEทั้งหน้าWeb => เปลืองButใครสน :D
    // To KENG(2): รบกวนจัดหน้าให้ตามที่ออกแบบให้หน่อย แล้วก็ขอ from รับค่า BigBlind จากหัวห้องด้วย
    useEffect(() => {
        if (lastJsonMessage !== null) {
            const { type, payload } = lastJsonMessage;

            // if (type === 'CREATE_SUCCESS') {
            //     setplayersNum(payload.playersNum);
            //     setPlayers(payload.players);
            //     console.log('Command: ',type,payload);
            // } else {
            //     setplayersNum(payload.playersNum);
            //     setPlayers(payload.players);
            //     console.log('Command: ',type,payload);
            // }

            setplayersNum(payload.playersNum);
            setPlayers(payload.players);
            console.log('Command: ',type,payload);
        }
    }, [lastJsonMessage]);

    // reconnect
    useEffect(() => {
        console.log(`Connecting to Room: ${roomId} as ${username}`);

        // To KENG: ฟังชั่นนี้ต้องทำงานเมื่อ refresh(F5) เท่านั้นฝากด้วย
        if (playersNum == 0) {
            // Send join request to rejoin with new WebSocket session
            sendMessage(JSON.stringify({
                action: "join_room",
                data: {
                    username: username,
                    roomId: roomId
                } 
            }));
        }

    }, []);

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
                    <p>PLAYER: {playersNum}/6</p>
                </div>
                
            </div>
        </>
    );
}

export default Gameroom