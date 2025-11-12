import { useState } from "react"
import Fushigidane from "../assets/1.png"
import Hitokage from "../assets/2.png"
import Zenigame from "../assets/3.png"
import './PlayerList.css'
export default function PlayerList(){
    const [data, setData] = useState([                  //Sample player-info
        { id: 1, name: "PINIONNNN", photo_id: 1 },
        { id: 2, name: "MONKEYZ", photo_id: 2 },
        { id: 3, name: "KENGKAK", photo_id: 3 },
        { id: 4, name: "TP_MONKEY", photo_id: 1 },
        { id: 5, name: "DOEDOHH", photo_id: 2 },
        { id: 6, name: "JUSAJAI", photo_id: 3 }
    ]);
    return(
        <>
            <ul class="player-list">
                {data.map((item) => (           //loop player in data list
                    <li key={item.id}>
                    <h2>{item.name}</h2>
                    <img src={item.photo_id == 1 ? Fushigidane :
                              item.photo_id == 2 ? Hitokage :
                              Zenigame
                    }/>
                    </li>
                ))}
            </ul>
        </>
    );
}