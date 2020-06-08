import React from "react";
import PlayingCard from "../components/PlayingCard/PlayingCard";

const GameScreen = () => {

    return (
        <div>
            <header className="App-header">
                <div style={{display: 'flex'}}>
                    <PlayingCard rank={11} suit={2}/>
                    <PlayingCard isFaceDown/>
                    <PlayingCard rank={12} suit={1}/>
                    <PlayingCard isFaceDown/>
                </div>
            </header>
        </div>
    )
}

export default GameScreen