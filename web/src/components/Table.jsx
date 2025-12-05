import "./Table.css"

function Table({ cards, pot, currentBet, bigBlind }) {
    
    const safeCards = cards || [];
    const comCards = [...safeCards];
    
    const items_needed = 5 - safeCards.length;
    for (let i = 0; i < items_needed; i++) {
        comCards.push('back');
    }

    return (
        <div className="table">
            <div className="info-text bet">CURRENT BET: {currentBet}</div>
            <div className="">POT: {pot}</div>
            <div className="communityCards">
                {comCards.map((card, index) => {
                    const isBack = card === 'back';
                    const fileName = isBack 
                        ? 'back.svg'
                        : `${card.rank}${card.suit}.svg`;

                    return (
                        <div className="card-wrapper" key={index}>
                            <img
                                className="card-img" 
                                src={new URL(`../assets/cards/${fileName}`, import.meta.url).href}
                                alt={isBack ? "Card Back" : `${card.rank}${card.suit}`}
                            />
                        </div>
                    );
                })}
            </div>
            
            <div className="info-text bigBlind">BIG BLIND: {bigBlind}</div>
        </div>
    )
}

export default Table;