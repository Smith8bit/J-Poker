import React from 'react';

// Unified Avatar List (Matches PlayersStatus)
const AVATAR_LIST = [
    "squirtle",
    "charmander",
    "pikachu",
    "eevee",
    "magikarp",
    "bulbasaur",
];

function getAvatarName(username) {
    if (!username) return AVATAR_LIST[0];
    let hash = 0;
    for (let i = 0; i < username.length; i++) {
        hash = username.charCodeAt(i) + ((hash << 5) - hash);
    }
    const index = Math.abs(hash) % AVATAR_LIST.length;
    return AVATAR_LIST[index];
}

function PlayerArea({ players, activePlayers }) {
    // Ensure we always have 6 seats
    const totalSeats = 6;
    const seats = Array.from({ length: totalSeats }).map((_, i) => {
        return players[i] || null; 
    });

    return (
        <div className="main-area">
            <div className="player-row-bg">
                {seats.map((player, index) => {
                    
                    let isActive = true; // Default to true (Lobby mode)
                    let avatarName = "pikachu"; 

                    if (player) {
                        // 1. Check if Active (Only if activePlayers list is provided)
                        if (activePlayers && Array.isArray(activePlayers)) {
                            isActive = activePlayers.includes(player.username);
                        }

                        // 2. Get Stable Avatar based on Username
                        avatarName = getAvatarName(player.username);
                    }

                    // 3. Construct URL
                    const statusFolder = isActive ? "active" : "folded";
                    const imgPrefix = isActive ? "" : "F_";

                    const avatarSrc = player ? new URL(
                        `../assets/player icons/${statusFolder}/${imgPrefix}${avatarName}.png`,
                        import.meta.url
                    ).href : null;

                    return (
                        <div 
                            key={index} 
                            className={`player-slot ${!player ? 'empty' : ''} ${!isActive ? 'folded-state' : ''}`}
                        >
                            {player ? (
                                <>
                                    <div className="player-name">
                                        {/* Show Crown if Host */}
                                        {player.host && <span style={{ marginRight: '10px', fontSize: '1.5rem'}}>👑</span>}
                                        {player.username}
                                    </div>
                                    <div className="player-stack">
                                        💰 ${player.stack}
                                    </div>
                                    <div className="avatar-container">
                                        <img 
                                            src={avatarSrc} 
                                            alt={avatarName}
                                            className={`avatar-sprite ${!isActive ? 'folded-img' : ''}`}
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