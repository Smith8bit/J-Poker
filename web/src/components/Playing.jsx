import { useState, useEffect } from "react";
import Hand from "./Hand"
import Table from "./Table"
import PlayersStatus from "./PlayersStatus"
import './Playing.css'

function Playing({ sendMessage, lastJsonMessage, username, userCredit, roomId, navigate }) {

    const [gameState, setGameState] = useState(null);
    const [myPlayer, setMyPlayer] = useState(null);
    const [betAmount, setBetAmount] = useState(0);

    useEffect(() => {
        if (lastJsonMessage !== null) {
            const { type, payload } = lastJsonMessage;

            if (type === 'GAME_STATE') {
                setGameState(payload);
                
                // Find my player info
                const me = payload.players.find(p => p.username === username);
                setMyPlayer(me);
                console.log("receive GAME_STATE "+{payload})
                
                // Set default bet amount to current bet or big blind
                if (payload.currentBet > 0) {
                    setBetAmount(payload.currentBet * 2);
                }
            }

            if (type === 'ERROR') {
                alert(payload.error);
            }

            if (type === 'GAME_OVER') {
                let msg = "Game Over!\n";
                payload.winners.forEach(w => {
                    msg += `${w.username} wins $${w.amount} (${w.handRank})\n`;
                });

                alert(msg);

                navigate(`/Lobby`, { 
                    state: { 
                        username: username, 
                        roomId: roomId,
                        userCredit: userCredit
                    } 
                });
            }
        }
    }, [lastJsonMessage, username]);

    const handleAction = (actionType, amount = null) => {
        const data = {
            actionType: actionType,
            roomId: roomId
        };

        if (amount !== null) {
            data.amount = amount;
        }

        sendMessage(JSON.stringify({
            action: "game_action",
            data: data
        }));
    };

    const isMyTurn = () => {
        if (!gameState || !myPlayer) return false;
        return gameState.currentActorId === myPlayer.id;
    };

    const canCheck = () => {
        if (!gameState || !myPlayer) return false;
        const myBet = gameState.playerBets[myPlayer.id] || 0;
        const result = myBet === gameState.currentBet;
        console.log(`canCheck: myBet=${myBet}, currentBet=${gameState.currentBet}, result=${result}`);
        return result;
    };

    const getCallAmount = () => {
        if (!gameState || !myPlayer) return 0;
        const myContribution = gameState.playerBets[myPlayer.id] || 0; 
        const diff = gameState.currentBet - myContribution;
        return diff > 0 ? diff : 0;
    };

    if (!gameState) {
        return (
            <div className="playing-container">
                <p>Loading game...</p>
            </div>
        );
    }

    return (
        <>
        <div className="handTable">
            {myPlayer && myPlayer.hand && (
                <Hand cards={myPlayer.hand} />
            )}
            
            <Table 
                cards={gameState.board || []}
                pot={gameState.pot}
                currentBet={gameState.currentBet}
                bigBlind={gameState.bigBlind}
            />

            <PlayersStatus 
                players={gameState.players}
                activePlayerIds={gameState.activePlayerIds}
                currentActorId={gameState.currentActorId}
                playerBets={gameState.playerBets}
                myUsername={username}
            />
        </div>
        
        <div className="footer">
            <div className="chat-box">
                <div style={{opacity: 0.5}}>SYSTEM LOG...</div>
                <div>Welcome to Room!</div>
            </div>
            
            {/* แสดงสถานะผู้เล่น ใครอยู่ ใครหมอบ */}
            <PlayersStatus roomPlayers={['A', 'B', 'C', 'D', 'E', 'F']} activePlayers={['D', 'F']} />

             {/* ปุ่ม Action แสดงเมื่อเป็นผู้เล่นในตานั้น */}
            <div className="actions-container">
                <div className="betRaise">
                    <button 
                        className={`action-btn ${!isMyTurn() || gameState.currentBet > 0 ? 'disabled' : ''}`}
                        onClick={() => isMyTurn() && gameState.currentBet === 0 && handleAction('bet', betAmount)}
                        disabled={!isMyTurn() || gameState.currentBet > 0}
                    >
                        BET
                    </button>
                    <button 
                        className={`action-btn ${!isMyTurn() || gameState.currentBet === 0 ? 'disabled' : ''}`}
                        onClick={() => isMyTurn() && gameState.currentBet > 0 && handleAction('raise', betAmount - gameState.currentBet)}
                        disabled={!isMyTurn() || gameState.currentBet === 0 || betAmount <= gameState.currentBet}
                    >
                        RAISE
                    </button>
                    <input  
                        type="number"
                        className="bet-input"
                        placeholder="NUMBER OF BET/RAISE"
                        value={betAmount}
                        onChange={(e) => setBetAmount(parseInt(e.target.value) || 0)}
                        min={gameState.currentBet || 0}
                        max={myPlayer?.stack || 0}
                    />
                </div>
                <div className="checkCallFold">
                    {/* ปุ่ม CHECK: กดได้เมื่อ canCheck เป็น true */}
                    <button 
                        className={`action-btn ${!isMyTurn() || !canCheck() ? 'disabled' : ''}`}
                        onClick={() => isMyTurn() && canCheck() && handleAction('check')}
                        disabled={!isMyTurn() || !canCheck()}
                    > 
                        CHECK
                    </button>

                    {/* ปุ่ม CALL: กดได้เมื่อ canCheck เป็น false (ต้องจ่ายตังเพิ่ม) */}
                    <button 
                        className={`action-btn ${!isMyTurn() || canCheck() ? 'disabled' : ''}`}
                        onClick={() => isMyTurn() && !canCheck() && handleAction('call')}
                        disabled={!isMyTurn() || canCheck()}
                    >
                        {/* โชว์ยอดส่วนต่างที่ต้องจ่ายเพิ่ม */}
                        CALL ${getCallAmount()}
                    </button>

                    {/* ปุ่ม FOLD */}
                    <button 
                        className={`action-btn ${!isMyTurn() ? 'disabled' : ''}`}
                        onClick={() => isMyTurn() && handleAction('fold')}
                        disabled={!isMyTurn()}
                    >
                        FOLD
                    </button>
                </div>
            </div>
        </div>
        </>
    );
}

export default Playing