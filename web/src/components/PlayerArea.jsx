import React from 'react';

function PlayerArea({ players }) {
    // สร้าง Array 6 ช่องเสมอ (ถ้า players มีน้อยกว่า 6 ก็ให้เป็นช่องว่าง)
    const totalSeats = 6;
    const seats = Array.from({ length: totalSeats }).map((_, i) => {
        return players[i] || null; // ถ้ามีผู้เล่นใส่ข้อมูล ถ้าไม่มีใส่ null
    });

    return (
        <div className="main-area">
            <div className="player-row-bg">
                {seats.map((player, index) => (
                    <div key={index} className={`player-slot ${!player ? 'empty' : ''}`}>
                        {player ? (
                            <>
                                <div className="player-name">{player.username} {player.host ? '👑 (HOST)' : ''}</div>
                                {/* ถ้า Backend ส่ง avatar มาก็ใช้ player.avatar ตรง class */}
                                <div className="avatar-sprite bulbasaur"></div> 
                            </>
                        ) : (
                            <>
                                <div className="player-name" style={{opacity: 0.3}}>EMPTY</div>
                                <div className="avatar-sprite empty-slot"></div>
                            </>
                        )}
                    </div>
                ))}
            </div>
        </div>
    );
}

export default PlayerArea;