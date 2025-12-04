import "./Table.css"

function Table({ cards, currentBet, bigBlind }) {
    
    const comCards = [...cards];
    const items_needed = 5 - cards.length;
    for (let i = 0; i < items_needed; i++) {
        comCards.push('back');
    }

    return (
        <div className="table">
            <div className="info-text bet">CURRENT BET: {currentBet}</div>
            
            <div className="communityCards">
                {comCards.map((card, index) => (
                    <div className="card-wrapper" key={index}>
                        <img
                            className="card-img" 
                            src={new URL(`../assets/cards/${card}.svg`, import.meta.url).href}
                            alt={card}
                        />
                    </div>
                ))}
            </div>
            
            <div className="info-text bigBlind">BIG BLIND: {bigBlind}</div>
        </div>
    )
}

export default Table