import { useState, useEffect } from "react";

import Hand from "./Hand"
import Table from "./Table"
import PlayersStatus from "./PlayersStatus"
import './Playing.css'

function Playing({ sendMessage, lastJsonMessage, username, userCredit, roomId, navigate, bigBlind }) {

    const [myHand, setMyHand] = useState([]);        // ไพ่ในมือเรา
    const [communityCards, setCommunityCards] = useState([]); // ไพ่กองกลาง
    const [pot, setPot] = useState(0);               // เงินกองกลาง
    const [currentBet, setCurrentBet] = useState(0); // ยอดเดิมพันสูงสุดปัจจุบัน
    const [currentTurn, setCurrentTurn] = useState(""); // ตาใครเล่น
    const [myStatus, setMyStatus] = useState("WAITING"); // สถานะเรา (PLAYING, FOLDED)
    const [raiseAmount, setRaiseAmount] = useState(bigBlind * 2); // ค่า Raise

    useEffect(() => {
        if (lastJsonMessage) {
            const { type, payload } = lastJsonMessage;

            if (type === 'GAME_STARTED' || type === 'NEXT_ROUND') {
                // เริ่มเกม / รอบใหม่
                setCommunityCards([]);
                setMyHand([]);
                setPot(0);
                setCurrentBet(bigBlind);
                setMyStatus("PLAYING");
            }
            
            setCurrentBet(bigBlind); 
            setRaiseAmount(bigBlind * 2);

            if (payload.currentTurn) {
                    console.log(payload.currentTurn)
                    setCurrentTurn(payload.currentTurn);
                }

            if (type === 'YOUR_HAND') {
                setMyHand(payload.hand);
                setMyStatus("PLAYING");
            }

            if (type === 'GAME_UPDATE') {
                setCommunityCards(payload.communityCards || []);
                setPot(payload.pot || 0);
                setCurrentTurn(payload.currentTurn || "");
                setCurrentBet(payload.currentBet || 0);
            }
        }
    }, [lastJsonMessage, bigBlind]);

    const sendGameAction = (actionType, amount = 0) => {
        sendMessage(JSON.stringify({
            action: "game_action",
            data: {
                roomId,
                username,
                action: actionType,
                amount: amount
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
        navigate('/Lobby', {
            state: { username, userCredit }
        });
    };
    
    return (
        <>
        <div className="handTable">
            <Hand cards={myHand} />
            <Table 
                cards={communityCards}
                currentBet={setCurrentBet}
            />
        </div>
        <div className="footer">
            <div className="chatLog"></div>
            {/* แสดงสถานะผู้เล่น ใครอยู่ ใครหมอบ */}
            <PlayersStatus />
             {/* ปุ่ม Action แสดงเมื่อเป็นผู้เล่นในตานั้น */}
            <div className="actions-container">
                <div className="betRaise">
                    <button className="action-btn">BET</button>
                    <button className="action-btn">RAISE</button>
                    <input  type="number"
                            className="bet-input"
                            placeholder="NUMBER OF BET/RAISE"
                            min={bigBlind}
                            
                    />
                </div>
                <div className="checkCallFold">
                    <button className="action-btn">CHECK</button>
                    <button className="action-btn">CALL</button>
                    <button className="action-btn">FOLD</button>
                </div>
            </div>
        </div>
        </>
    );
}

export default Playing