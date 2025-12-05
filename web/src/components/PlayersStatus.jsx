import './PlayerStatus.css'

function PlayersStatus({ roomPlayers = [], activePlayers = [] }) {
    
    const playerAvatars = [
        "pikachu",
        "bulbasaur",
        "charmander",
        "squirtle",
        "magikarp",
        "eevee",
    ];

    return (
        <div className="status-container">
        {roomPlayers.map((player, index) => {

            const isActive = activePlayers.includes(player);
            const statusFolder = isActive ? "active" : "folded";
            const imgPrefix = isActive ? "" : "F_";

            return (
            <div className="player-avatar" key={index}>
                <div className='player-name'> 
                    {player}
                </div>
                <img
                className="img-avatar"
                src={
                    new URL(
                    `../assets/player icons/${statusFolder}/${imgPrefix}${
                        playerAvatars[index % playerAvatars.length]
                    }.png`,
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