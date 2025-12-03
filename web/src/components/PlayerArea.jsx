import React from 'react';

import pikachu from "../assets/player icons/active/pikachu.png";
import bulbasaur from '../assets/player icons/active/bulbasaur.png';
import charmander from '../assets/player icons/active/charmander.png';
import squirtle from '../assets/player icons/active/squirtle.png';
import magikarp from '../assets/player icons/active/magikarp.png';
import eevee from '../assets/player icons/active/eevee.png';

const NON_HOST_AVATARS = [
    bulbasaur, 
    charmander, 
    squirtle, 
    magikarp, 
    eevee
];

function PlayerArea({ players }) {
    // สร้าง Array 6 ช่องเสมอ (ถ้า players มีน้อยกว่า 6 ก็ให้เป็นช่องว่าง)
    const totalSeats = 6;
    const seats = Array.from({ length: totalSeats }).map((_, i) => {
        return players[i] || null; // ถ้ามีผู้เล่นใส่ข้อมูล ถ้าไม่มีใส่ null
    });

    return (
        <div className="main-area">
            <div className="player-row-bg">
                {seats.map((player, index) => {
                    
                    let avatarSrc = null;

                    if (player) {
                        if (player.host) {
                            avatarSrc = pikachu;
                        } else {
                            avatarSrc = NON_HOST_AVATARS[index % NON_HOST_AVATARS.length];
                        }
                    }

                    return (
                        <div key={index} className={`player-slot ${!player ? 'empty' : ''}`}>
                            {player ? (
                                <>
                                    <div className="player-name">
                                        {player.username}
                                    </div>
                                    <div className="player-stack">
                                        💰 ${player.stack}
                                    </div>
                                    <div className="avatar-container">
                                        {/* แสดงรูปที่คำนวณได้ */}
                                        <img 
                                            src={avatarSrc} 
                                            alt="Avatar"
                                            className={`avatar-sprite ${player.host ? 'pikachu' : ''}`} // ใส่ class เผื่อไว้แต่ง CSS
                                            style={{ width: '64px', height: '64px', objectFit: 'contain' }}
                                        />
                                    </div>
                                </>
                            ) : (
                                <>
                                    <div className="player-name" style={{opacity: 0.3}}>EMPTY</div>
                                    <div className="avatar-sprite empty-slot"></div>
                                </>
                            )}
                        </div>
                    );
                })}
            </div>
        </div>
    );
}

export default PlayerArea;