import './Hand.css'
function Hand({ cards }) {

    return (
        <div className="hand">
            {/* This text will now be centered because of .hand { text-align: center } */}
            <h3>YOUR HAND</h3> 

            <div className="imgContainer">
                {cards.map((card, index) => (
                    <div key={index} className="cardWrapper">
                        <img
                            src={new URL(`../assets/cards/${card}.svg`, import.meta.url).href}
                            alt={card}
                            style={{ width: '100px', display: 'block' }} // Basic styling
                        />
                    </div>
                ))}
            </div>
        </div>
    );
}

export default Hand;