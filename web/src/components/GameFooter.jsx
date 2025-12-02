import React from 'react';

function GameFooter({ onStartGame }) {
    return (
        <div className="footer-area">
            {/* ฝั่งซ้าย: Chat Log */}
            <div className="chat-box">
                <div>CHAT LOG</div>
                <div>SYSTEM LOG</div>
                <div style={{opacity: 0.5}}>...waiting...</div>
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
                <div className="big-blind-input">
                    BIG BLINDS: <input type="text" className="input-pill" />
                </div>
                <button className="btn-start" onClick={onStartGame}>START</button>
            </div>
        </div>
    );
}

export default GameFooter;