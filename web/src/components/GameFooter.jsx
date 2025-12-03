import React, {useState} from 'react';

function GameFooter({ onStartGame, IsHost }) {

    const [bigBlind, setBigBlind] = useState(100); //Default BigBlind

    const handleStartGame = () => {
        const blindValue = parseInt(bigBlind, 10);
        if (isNaN(blindValue) || blindValue <= 0) return;
        onStartGame(blindValue);
    }

    return (
        <div className="footer-area">
            {/* ฝั่งซ้าย: Chat Log */}
            <div className="chat-box">
                <div style={{opacity: 0.5}}>SYSTEM LOG...</div>
                <div>Welcome to Room!</div>
            </div>

            {/* ตรงกลาง: Game Rule */}
            <div className="rules-box">
                <h3>GAME RULE</h3>
                <ol>
                    <li>BIG BLINDS is Red.</li>
                    <li>SMALL BLINDS is Yellow.</li>
                    <li>Winner take all.</li>
                </ol>
            </div>

            {/* ฝั่งขวา: Actions */}
            <div className="action-box">
                {/* form รับค่า Bigblind */}
                <div className="big-blind-input">
                    <label>BIG BLINDS:</label>
                    <input 
                        type="number" 
                        className="input-pill"
                        value={bigBlind}
                        onChange={(e) => setBigBlind(e.target.value)}
                        disabled={!IsHost}
                    />
                </div>
                <button className="btn-start" 
                        onClick={handleStartGame} 
                        disabled={!IsHost}
                        style={{ display: IsHost ? 'block' : 'none' }}
                >
                    START
                </button>
            </div>
        </div>
    );
}

export default GameFooter;