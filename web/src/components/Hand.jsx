import './Hand.css'

function Hand({ cards }) {

    return (
        <div className="hand">
            <h3>YOUR HAND</h3> 

            <div className="imgContainer">
                {cards.map((card, index) => (
                    <div key={index} className="cardWrapper">
                        <img
                            src={new URL(`../assets/cards/${card.rank}${card.suit}.svg`, import.meta.url).href}
                            alt={`${card.rank}${card.suit}`}
                        /> 
                    </div>
                ))}
            </div>
        </div>
    );
}

export default Hand;