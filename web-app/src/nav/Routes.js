import React from "react";
import {Switch, Route, Redirect} from "react-router-dom";
import HomeScreen from "../screens/HomeScreen";
import GameScreen from "../screens/GameScreen";

const Routes = () => {
    return (
        <Switch>
            <Route exact path='/' component={HomeScreen} />
            <Route exact path='/game' component={GameScreen} />
        </Switch>
    )
}

export default Routes