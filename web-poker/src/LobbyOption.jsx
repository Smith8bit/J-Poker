import { useState } from "react"
import { useLocation } from "react-router-dom"

function LobbyOption () {
    const location = useLocation();
    const { username } = location.state; 
    return (
        <h1>
            hello {username}
        </h1>
    )
}

export default LobbyOption