import './PlayersStatus.css'

function PlayersStatus({ roomPlayers = [], activePlayers = [] }) {
    
    // Exact same list as PlayerArea
    const AVATAR_LIST = [
        "squirtle",
        "charmander",
        "pikachu",
        "eevee",
        "magikarp",
        "bulbasaur",
    ];

    // Same helper function
    function getAvatarName(username) {
        if (!username) return AVATAR_LIST[0];
        let hash = 0;
        for (let i = 0; i < username.length; i++) {
            hash = username.charCodeAt(i) + ((hash << 5) - hash);
        }
        const index = Math.abs(hash) % AVATAR_LIST.length;
        return AVATAR_LIST[index];
    }

    return (
        <div className="status-container">
        {roomPlayers.map((player, index) => {

            const isActive = activePlayers.includes(player);
            const statusFolder = isActive ? "active" : "folded";
            const imgPrefix = isActive ? "" : "F_";
            
            // Use the stable avatar name
            const avatarName = getAvatarName(player);

            return (
            <div className="player-avatar" key={index}>
                <div className='player-name-status'> 
                    {player}
                </div>
                <img
                    className="img-avatar"
                    src={
                        new URL(
                        `../assets/player icons/${statusFolder}/${imgPrefix}${avatarName}.png`,
                        import.meta.url
                        ).href
                    }
                    alt={player}
                />
            </div>
            );
        })}
        </div>
    );
}

export default PlayersStatus;