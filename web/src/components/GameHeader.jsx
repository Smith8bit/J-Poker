import React from "react";
import { useNavigate } from 'react-router-dom';
import coin from "../assets/coin/coin.png";

function GameHeader({ userCredit, playersCount, roomId, onExit }) {

    return (
        <div className="top-bar">
            {/* มุมซ้าย: ประตูออก + เงิน */}
            <div className="top-left">
                <button className="btn-exit" onClick={onExit}>
                    🚪
                </button>
            </div>

            {/* ตรงกลาง: Player Count */}
            <div className="top-center">
                <div className="coin-display">
                    <img src={coin} className="coin-icon"></img>
                    <span>{userCredit ? userCredit.toLocaleString() : '0'}</span>
                </div>
                <div className="player-badge">
                    <h3>PLAYER: {playersCount}/6</h3>
                </div>
            </div>

            {/* มุมขวา: Room Code */}
            <div className="top-right">
                ROOM CODE: {roomId}
            </div>
        </div>
    );
}

export default GameHeader;