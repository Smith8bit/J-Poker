import { useEffect, useState } from "react"
import { useLocation, useParams } from "react-router-dom";
import useWebSocket, { ReadyState } from "react-use-websocket";

function Gameroom() {
    
    return (
        <>
            <h1>This is Room: {roomId}</h1>
            <h2>Status: {connectionStatus}</h2>
            <h2>Player: {playersNum}/6</h2>
            <h2>You are {username}</h2>
        </>
    )
}

export default Gameroom