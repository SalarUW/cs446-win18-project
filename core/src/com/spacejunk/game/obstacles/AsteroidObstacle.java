package com.spacejunk.game.obstacles;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.spacejunk.game.levels.Level;

/**
 * Created by vidxyz on 2/8/18.
 */

public class AsteroidObstacle extends Obstacle {

    public AsteroidObstacle(Level level) {
        this.obstacleTexture = new Texture("asteroid_crush.png");

        FileHandle handle = Gdx.files.internal("asteroid_crush.png");
        this.pixmap = new Pixmap(handle);

        this.sound = Gdx.audio.newSound(Gdx.files.internal("sounds/fire_sound.mp3"));

        this.level = level;
        this.obstacleType = OBSTACLES.ASTEROID;
    }

}

