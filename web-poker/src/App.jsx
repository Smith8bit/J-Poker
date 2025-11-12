import { useState } from 'react'
import PlayerList from './PlayerList/PlayerList'
import './App.css'

function App() {
  return (
    <>
      <h1>COM SCI<br></br>POKER</h1>
        <form>
          <input type="text" id="Username" placeholder='ENTER YOUR NAME'/>
          <br></br>
          <button><h2>PLAY</h2></button>
        </form>
      {/* <PlayerList/> */}
    </>
  )
}

export default App
