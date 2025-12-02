import React from 'react';

function PlayerArea({ players }) {
    return (
        <div className="main-area">
            <div className="player-row-bg">
                {players.map((player, index) => (
                    <div key={index} className="player-slot">
                        <div className="player-name">{player.name}</div>
                        {/* ถ้ามีรูป ให้ใช้ img tag แทน div นี้ */}
                        <div className={`avatar-sprite ${player.avatar}`}></div>
                    </div>
                ))}
                
                {/* ถ้าอยากให้แสดงที่ว่างเปล่าๆ ด้วย (เพื่อให้ครบ 6 ช่องเสมอ) */}
                {Array.from({ length: 6 - players.length }).map((_, idx) => (
                    <div key={`empty-${idx}`} className="player-slot empty">
                        <div className="player-name">EMPTY</div>
                        <div className="avatar-sprite empty-slot"></div>
                    </div>
                ))}
            </div>
        </div>
    );
}

export default PlayerArea;