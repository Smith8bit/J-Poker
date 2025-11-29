import { useLocation, useNavigate } from "react-router-dom";
import './Lobby.css'
import { useState } from "react";

function Lobby() {
    const location = useLocation();
    const navigate = useNavigate();
    const [roomCode, setroomCode] = useState();
    const { username } = location.state;
    
    const handleJoinRoom = (e) => {
  
    }

    const handleCreateRoom = (e) => {

    }
    
    return (
        <>
        <h3 id="username">{username}</h3>
        <h1>COM SCI<br/>POKER</h1>
        <div className="menu">
            <form onSubmit={handleJoinRoom}>
                <input 
                type="text"
                value={roomCode}
                onChange={(e) => setroomCode(e.target.value)}
                placeholder="Enter Room Code"
                required
                />
                <br></br>
                <button type="submit">Join Room</button>
            </form>

            <div id="divider"/>

            <button id="createRoom" type="sumbit" onClick={handleCreateRoom}>Create Room</button>
        </div>
        <button id="wrongName">Wait, my name is wrong.</button>
        </>
    )
}

export default Lobby