import './Hand.css'

function Hand({ cards }) {

    return (
        <div className="hand">
            <h3>YOUR HAND</h3> 

            <div className="imgContainer">
                {cards.map((card, index) => (
                    <div key={index} className="cardWrapper">
                        {/* 🔥 แก้ตรงนี้: ใช้ card.rank + card.suit */}
                        <img
                            src={new URL(`../assets/cards/${card.rank}${card.suit}.svg`, import.meta.url).href}
                            alt={`${card.rank}${card.suit}`}
                        /> 
                        {/* console.log(card) เอาไว้ดูได้ แต่อย่าลืมว่ามันคือ Object */}
                    </div>
                ))}
            </div>
        </div>
    );
}

export default Hand;