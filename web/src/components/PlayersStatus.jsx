import './PlayersStatus.css';

function PlayersStatus({ players, activePlayerIds, currentActorId, playerBets, myUsername }) {
    
    // สุ่มสี avatar สำหรับแต่ละผู้เล่น
    const getAvatarColor = (index) => {
        const colors = ['#ff6b6b', '#4ecdc4', '#45b7d1', '#f9ca24', '#6c5ce7', '#a29bfe'];
        return colors[index % colors.length];
    };

    return (
        <div className="players-status-container">
            {players && players.map((player, idx) => {
                const isActive = activePlayerIds?.includes(player.id);
                const isCurrentTurn = player.id === currentActorId;
                const playerBet = playerBets?.[player.id] || 0;
                const isMe = player.username === myUsername;

                return (
                    <div key={idx} className="player-status">
                        {/* Avatar Pixel Art */}
                        <div 
                            className={`player-avatar ${!isActive ? 'folded' : ''} ${isCurrentTurn ? 'current-turn' : ''}`}
                            style={{ backgroundColor: getAvatarColor(idx) }}
                        >
                            <div className="pixel-face">
                                <div className="pixel-eyes">
                                    <div className="pixel-eye"></div>
                                    <div className="pixel-eye"></div>
                                </div>
                                <div className="pixel-mouth"></div>
                            </div>
                            
                            {/* Card indicator - แสดงว่ายังมีไพ่ */}
                            {isActive && (
                                <div className="card-indicator">🂠</div>
                            )}
                        </div>
                        
                        {/* Player name */}
                        <div className={`player-name ${isMe ? 'my-name' : ''}`}>
                            {player.username}
                            {isMe && ' (YOU)'}
                        </div>

                        {/* Big Blinds indicator */}
                        {idx === 1 && (
                            <div className="blinds-indicator">BB</div>
                        )}
                    </div>
                );
            })}
        </div>
    );
}

export default PlayersStatus;