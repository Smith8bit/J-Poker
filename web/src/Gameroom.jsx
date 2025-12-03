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
            if (payload && payload.players) {
                setplayersNum(payload.playersNum);
                setPlayers(payload.players);
                console.log('Command: ',type,payload);
            }
            
            //รอ update backend สำหรับดูว่าใครเป็นหัวห้อง
            if (type === 'ROOM_INFO' && payload.hostName === username) {
                setIsHost(true);
            }
        }
    }, [lastJsonMessage]);

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

    }, []);

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

    return (
        <div className="game-container font-pixel">
            
            {/* file in components/GaneHeader */}
            {/* Header: แสดงเงินและจำนวนคน */}
            <GameHeader 
                userCredit={userCredit} 
                playersCount={playersNum} 
                roomId={roomId} 
            /> 

            {/* file in components/PlayerArea */}
            {/* Player Area: แสดงตัวละครรอบโต๊ะ */}
            <PlayerArea players={players} />

            {/* file in components/GameFooter */}
            {/* Footer: แชท, กฎ, และปุ่ม Start (ส่ง prop isHost ไปเช็คได้ถ้าอยากให้ปุ่มขึ้นเฉพาะหัวห้อง) */}
            <GameFooter 
                onStartGame={handleStartGame} 
                IsHost={true} //สมมุติไปก่อน รอทำbackend    ยังเหลือ Ishost(เก็บไว้กับห้องหรือไม่ก็เก็บไว้ใน ram ดีกว่า) กับ bigblinds อยู่ที่ยังไม่เชื่อม backend
                />

        </div>
    );
}

export default Gameroom