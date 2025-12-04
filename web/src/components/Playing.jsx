import Hand from "./Hand"
import Table from "./Table"

function Playing({ sendMessage, lastJsonMessage, username, userCredit, roomId, navigate}) {

    const handleExitRoom = () => {
        // ส่งคำสั่งบอก Server
        sendMessage(JSON.stringify({
            action: "leave_room",
            data: { username, roomId }
        }));
        
        // กลับไปหน้า Lobby
        navigate('/Lobby', {
            state: { username, userCredit }
        });
    };
    
    return(
        <>
        <div className="handTable">
            <Hand cards={['AS', '2S']} />
            <Table 
                cards={['7H', 'JS']}
                currentBet={0}
            />
        </div>
        <button onClick={handleExitRoom}>exist</button>
        </>
    )
}

export default Playing