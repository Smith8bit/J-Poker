import "./Table.css"

function Table({ cards, currentBet }) {
    
    const comCards = [...cards];
    const items_needed = 5 - cards.length;
    for (let i = 0; i < items_needed; i++) {
        comCards.push('back');
    }

    return (
        <div className="table">
            <div className="bet">
                {currentBet} 
            </div>
            
            <div className="communityCards">
                {comCards.map((card, index) => (
                    <div key={index}>
                        <img
                            src={new URL(`../assets/cards/${card}.svg`, import.meta.url).href}
                            alt={card}
                            style={{ width: '100px', display: 'block' }}
                        />
                    </div>
                ))}
            </div>
            
            <div className="bigBlind"></div>
        </div>
    )
}

export default Table