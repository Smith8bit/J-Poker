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
    const [ players, setPlayers] = useState([]);
    const [ isHost, setIsHost ] = useState([]);
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
            const currentUsername = username;

            // if (type === 'CREATE_SUCCESS') {
            //     setplayersNum(payload.playersNum);
            //     setPlayers(payload.players);
            //     console.log('Command: ',type,payload);
            // } else {
            //     setplayersNum(payload.playersNum);
            //     setPlayers(payload.players);
            //     console.log('Command: ',type,payload);
            // }
            if (payload && payload.players) {
                const receivedPlayers = payload.players;
                setplayersNum(receivedPlayers.length);
                setPlayers(receivedPlayers);
                console.log('Command: ', type, payload);
            

                const currentPlayer = receivedPlayers.find(p => p.username === currentUsername);
                
                if (currentPlayer) {
                    console.log("My player Data:",currentPlayer)
                    setIsHost(currentPlayer.host);
                }
            }
        }
    }, [lastJsonMessage, username]);

    // reconnect
    useEffect(() => {
        // To KENG: ฟังชั่นนี้ต้องทำงานเมื่อ refresh(F5) เท่านั้นฝากด้วย
        if (username && roomId) {
            console.log(`Connecting to Room: ${roomId} as ${username}`);
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

    useEffect(() => {
    // ฟังก์ชันที่จะทำงานตอนปิด Tab หรือ Refresh
    const handleUnload = () => {
        // ส่งข้อความสุดท้ายบอก Server (ถ้า Socket ยังเปิดอยู่)
        if (username && roomId) {
            sendMessage(JSON.stringify({
                action: "leave_room",
                data: { 
                    username,
                    roomId   
                }
            }));
        }
    };

    window.addEventListener("beforeunload", handleUnload);

    return () => {
        window.removeEventListener("beforeunload", handleUnload);
    };
}, [sendMessage, username, roomId]); // dependency

    // Start (get BigBlind from Footer)
    const handleStartGame = (bigBlindValue) => {
        console.log(`Start Game Big Blind = ${bigBlindValue}`);
        
        sendMessage(JSON.stringify({
            action: "start_game",
            data: {
                roomId: roomId,
                bigBlind: parseInt(bigBlindValue) || 100 // กันเหนียวถ้าไม่ได้ใส่ค่า
            }
        }));
    };

    const handleExitRoom = () => {
        // ส่งคำสั่งบอก Server
        sendMessage(JSON.stringify({
            action: "leave_room",
            data: { username, roomId }
        }));
        
        // กลับไปหน้า Lobby
        navigate('/lobby');
    };

    return (
        <div className="game-container font-pixel">
            
            {/* file in components/GaneHeader */}
            {/* Header: แสดงเงินและจำนวนคน */}
            <GameHeader 
                userCredit={userCredit} 
                playersCount={playersNum} 
                roomId={roomId} 
                onExit={handleExitRoom}
            /> 

            {/* file in components/PlayerArea */}
            {/* Player Area: แสดงตัวละครรอบโต๊ะ */}
            <PlayerArea players={players} />

            {/* file in components/GameFooter */}
            {/* Footer: แชท, กฎ, และปุ่ม Start (ส่ง prop isHost ไปเช็คได้ถ้าอยากให้ปุ่มขึ้นเฉพาะหัวห้อง) */}
            <GameFooter 
                onStartGame={handleStartGame} 
                IsHost={isHost}
                />

        </div>
    );
}

export default Gameroom